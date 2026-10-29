import { CONTEXT_PATTERN, NODE_PATTERN, button, clear, el, field, formatDate, formatRemaining, modal, parseDuration, toLocalInput, toast } from "./ui.js";
import { notify, now, state } from "./state.js";

const SUGGESTIONS = ["*", "muxe.permissions.editor", "muxe.tag.*", "muxe.medal.*", "meta.prefix."];
const memory = new Map();

export function contextsText(contexts) {
  return Object.keys(contexts || {}).sort().map((key) => key + "=" + contexts[key]).join(" ");
}

export function parseContexts(text) {
  const contexts = {};
  const parts = String(text || "").split(/[\s,]+/).filter(Boolean);
  for (const part of parts) {
    const separator = part.indexOf("=");
    if (separator <= 0) {
      throw new Error("Contexto inválido: " + part + " (use chave=valor)");
    }
    const key = part.substring(0, separator).toLowerCase();
    const value = part.substring(separator + 1).toLowerCase();
    if (!CONTEXT_PATTERN.test(key) || !CONTEXT_PATTERN.test(value)) {
      throw new Error("Contexto inválido: " + part);
    }
    contexts[key] = value;
  }
  if (Object.keys(contexts).length > 8) {
    throw new Error("No máximo 8 contextos por permissão");
  }
  return contexts;
}

function sameSlot(a, b) {
  return a.key === b.key && contextsText(a.contexts) === contextsText(b.contexts);
}

export function putNode(nodes, node) {
  const existing = nodes.find((candidate) => sameSlot(candidate, node));
  const next = nodes.filter((candidate) => !sameSlot(candidate, node));
  next.push({ key: node.key, value: node.value, contexts: node.contexts || {}, expiresAt: node.expiresAt || null, addedAt: existing ? existing.addedAt : null, updatedAt: null });
  return next;
}

export function removeNode(nodes, node) {
  return nodes.filter((candidate) => !sameSlot(candidate, node));
}

export function validateKey(raw) {
  const key = String(raw || "").trim().toLowerCase();
  if (!NODE_PATTERN.test(key)) {
    throw new Error("Permissão inválida: use letras minúsculas, números e . _ - + *, até 128 caracteres");
  }
  return key;
}

export function expiryInputs(initial) {
  const mode = el("select", null,
    el("option", { value: "never", text: "Permanente" }),
    el("option", { value: "duration", text: "Por um tempo" }),
    el("option", { value: "date", text: "Até uma data" }));
  const duration = el("input", { type: "text", placeholder: "30d, 12h, 1w, 1mo", class: "mono" });
  const date = el("input", { type: "datetime-local" });
  const sync = () => {
    duration.hidden = mode.value !== "duration";
    date.hidden = mode.value !== "date";
  };
  if (initial) {
    mode.value = "date";
    date.value = toLocalInput(initial);
  }
  mode.addEventListener("change", sync);
  sync();
  return {
    element: el("div", { class: "stack" }, mode, duration, date),
    read() {
      if (mode.value === "never") {
        return null;
      }
      if (mode.value === "duration") {
        const millis = parseDuration(duration.value);
        if (!millis) {
          throw new Error("Duração inválida. Exemplos: 30d, 12h, 1w, 1mo, 1d12h");
        }
        return now() + millis;
      }
      const millis = new Date(date.value).getTime();
      if (!date.value || Number.isNaN(millis)) {
        throw new Error("Escolha a data de expiração");
      }
      if (millis <= now()) {
        throw new Error("A data de expiração já passou");
      }
      return millis;
    },
  };
}

export async function nodeDialog(title, node) {
  const key = el("input", { type: "text", class: "mono", value: node ? node.key : "", placeholder: "muxe.exemplo.permissao", list: "node-suggestions" });
  const value = el("select", null, el("option", { value: "true", text: "true (permitir)" }), el("option", { value: "false", text: "false (negar)" }));
  value.value = node && node.value === false ? "false" : "true";
  const contexts = el("input", { type: "text", class: "mono", value: node ? contextsText(node.contexts) : "", placeholder: "server=lobby world=lobby" });
  const expiry = expiryInputs(node ? node.expiresAt : null);
  const body = el("div", { class: "stack" },
    field("Permissão", key, "Use * para tudo e prefixo.* para um grupo de permissões."),
    field("Valor", value),
    field("Contextos (opcional)", contexts, "Separe por espaço. Sem contextos, vale em todo lugar."),
    field("Expiração", expiry.element),
    suggestions());
  return modal(title, body, [
    { label: "Cancelar", className: "ghost" },
    {
      label: node ? "Aplicar" : "Adicionar",
      className: "primary",
      run: () => {
        try {
          return { key: validateKey(key.value), value: value.value === "true", contexts: parseContexts(contexts.value), expiresAt: expiry.read() };
        } catch (error) {
          toast(error.message, "bad");
          return false;
        }
      },
    },
  ]);
}

function suggestions() {
  const list = el("datalist", { id: "node-suggestions" });
  const keys = new Set(SUGGESTIONS);
  for (const group of state.working.groups.values()) {
    keys.add("group." + group.id);
    for (const node of group.nodes) {
      keys.add(node.key);
    }
  }
  for (const tag of state.working.tags.values()) {
    keys.add("muxe.tag." + tag.id);
  }
  for (const medal of state.working.medals.values()) {
    keys.add("muxe.medal." + medal.id);
  }
  for (const key of Array.from(keys).sort()) {
    list.appendChild(el("option", { value: key }));
  }
  return list;
}

export function nodeTable(options) {
  const card = el("div", { class: "card" });
  const memo = memory.get(options.key) || { search: "", filter: "all" };
  memory.set(options.key, memo);
  const search = el("input", { type: "search", placeholder: "Buscar permissão ou contexto…", value: memo.search });
  const filter = el("select", null,
    el("option", { value: "all", text: "Todas" }),
    el("option", { value: "true", text: "Só true" }),
    el("option", { value: "false", text: "Só false" }),
    el("option", { value: "temporary", text: "Temporárias" }),
    el("option", { value: "context", text: "Com contexto" }));
  filter.value = memo.filter;
  const body = el("div");

  const update = (nodes) => {
    options.update(nodes);
    notify();
  };

  const add = async () => {
    const node = await nodeDialog("Adicionar permissão");
    if (node) {
      update(putNode(options.nodes(), node));
    }
  };

  const bulk = async () => {
    const text = el("textarea", { class: "mono", placeholder: "muxe.fly\n-muxe.build\nmuxe.chat.color server=lobby\nmuxe.kit.vip 30d" });
    const result = await modal("Adicionar várias permissões", el("div", { class: "stack" },
      el("p", { class: "muted", text: "Uma por linha. Comece com - para false. Contextos como chave=valor e duração como 30d são opcionais." }),
      text), [
      { label: "Cancelar", className: "ghost" },
      {
        label: "Adicionar todas",
        className: "primary",
        run: () => {
          try {
            const parsed = parseBulk(text.value);
            if (!parsed.length) {
              toast("Nenhuma permissão informada", "bad");
              return false;
            }
            return parsed;
          } catch (error) {
            toast(error.message, "bad");
            return false;
          }
        },
      },
    ]);
    if (result) {
      let nodes = options.nodes();
      for (const node of result) {
        nodes = putNode(nodes, node);
      }
      update(nodes);
      toast(result.length + " permissões adicionadas", "ok");
    }
  };

  const render = () => {
    clear(body);
    const term = search.value.trim().toLowerCase();
    const current = now();
    const visible = options.nodes()
      .filter((node) => !options.exclude || !options.exclude(node))
      .filter((node) => !term || node.key.includes(term) || contextsText(node.contexts).includes(term))
      .filter((node) => {
        switch (filter.value) {
          case "true": return node.value;
          case "false": return !node.value;
          case "temporary": return Boolean(node.expiresAt);
          case "context": return Object.keys(node.contexts || {}).length > 0;
          default: return true;
        }
      })
      .sort((a, b) => a.key.localeCompare(b.key) || contextsText(a.contexts).localeCompare(contextsText(b.contexts)));
    if (!visible.length) {
      body.appendChild(el("div", { class: "empty", text: options.emptyText || "Nenhuma permissão." }));
      return;
    }
    const rows = visible.map((node) => {
      const toggle = el("button", { type: "button", class: "toggle " + (node.value ? "on" : "off"), text: node.value ? "true" : "false", title: "Alternar valor" });
      toggle.addEventListener("click", () => update(putNode(options.nodes(), { ...node, value: !node.value })));
      const contexts = Object.keys(node.contexts || {}).length
        ? el("div", { class: "chips" }, Object.keys(node.contexts).sort().map((key) => el("span", { class: "badge accent mono", text: key + "=" + node.contexts[key] })))
        : el("span", { class: "muted", text: "global" });
      let expiry;
      if (!node.expiresAt) {
        expiry = el("span", { class: "muted", text: "permanente" });
      } else if (node.expiresAt <= current) {
        expiry = el("span", { class: "badge bad", text: "expirou " + formatDate(node.expiresAt) });
      } else {
        expiry = el("span", { class: "badge warn", title: formatDate(node.expiresAt), text: "expira em " + formatRemaining(node.expiresAt - current) });
      }
      const edit = button("Editar", async () => {
        const changed = await nodeDialog("Editar permissão", node);
        if (changed) {
          update(putNode(removeNode(options.nodes(), node), changed));
        }
      }, "small ghost");
      const remove = button("Remover", () => update(removeNode(options.nodes(), node)), "small danger");
      return el("tr", null,
        el("td", { class: "mono", text: node.key }),
        el("td", null, toggle),
        el("td", null, contexts),
        el("td", null, expiry),
        el("td", { class: "actions" }, edit, " ", remove));
    });
    body.appendChild(el("div", { class: "table-wrap" }, el("table", null,
      el("thead", null, el("tr", null, el("th", { text: "Permissão" }), el("th", { text: "Valor" }), el("th", { text: "Contextos" }), el("th", { text: "Expiração" }), el("th"))),
      el("tbody", null, rows))));
  };

  search.addEventListener("input", () => {
    memo.search = search.value;
    render();
  });
  filter.addEventListener("change", () => {
    memo.filter = filter.value;
    render();
  });
  card.appendChild(el("h2", null, options.title || "Permissões", el("span", { class: "hint", text: options.hint || "" })));
  card.appendChild(el("div", { class: "row" },
    el("div", { class: "grow" }, search), filter, button("+ Adicionar", add, "primary"), button("Em massa", bulk)));
  card.appendChild(el("div", { class: "spacer-8" }));
  card.appendChild(body);
  render();
  return card;
}

export function parseBulk(text) {
  const nodes = [];
  const lines = String(text || "").split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith("#"));
  for (const line of lines) {
    const parts = line.split(/\s+/);
    let key = parts.shift();
    let value = true;
    if (key.startsWith("-") || key.startsWith("!")) {
      value = false;
      key = key.substring(1);
    }
    const contextParts = [];
    let expiresAt = null;
    for (const part of parts) {
      if (part.includes("=")) {
        contextParts.push(part);
      } else {
        const millis = parseDuration(part);
        if (!millis) {
          throw new Error("Linha inválida: " + line);
        }
        expiresAt = now() + millis;
      }
    }
    nodes.push({ key: validateKey(key), value, contexts: parseContexts(contextParts.join(" ")), expiresAt });
  }
  return nodes;
}

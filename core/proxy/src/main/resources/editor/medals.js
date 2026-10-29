import { medalChatLine, medalDisplay, medalListEntry, renderMc, swatches } from "./colors.js";
import { contextsText } from "./nodes.js";
import { MEDAL_PREFIX, groupsByWeight, inheritedChain, isChanged, medalKey, medalsByWeight, notify, now, state, tagsByWeight } from "./state.js";
import { ID_PATTERN, button, confirmDialog, el, field, formatDate, formatRemaining, modal, toast } from "./ui.js";

const SAMPLE_PLAYER = "tadeu";
const SAMPLE_MESSAGE = "Olá!";
const WILDCARDS = ["*", MEDAL_PREFIX + "*"];
const preview = { tag: "vip" };

function validName(name) {
  return name.length >= 1 && name.length <= 48;
}

function validSymbol(symbol) {
  const length = Array.from(symbol).length;
  return symbol.trim().length > 0 && length >= 1 && length <= 4;
}

function isActive(node) {
  return !node.expiresAt || node.expiresAt > now();
}

function grantingNodes(group, medalId) {
  return group.nodes.filter((node) => node.value && node.key === medalKey(medalId));
}

function grantsByWildcard(group) {
  return group.nodes.some((node) => node.value && WILDCARDS.includes(node.key) && isActive(node));
}

function grantsDirectly(group, medalId) {
  return grantingNodes(group, medalId).some(isActive) || grantsByWildcard(group);
}

function groupGrants(groupId, medalId) {
  const chain = new Set([groupId, ...inheritedChain(groupId)]);
  for (const id of chain) {
    const group = state.working.groups.get(id);
    if (group && grantsDirectly(group, medalId)) {
      return id;
    }
  }
  return null;
}

export function medalGrants(medalId) {
  const direct = [];
  const wildcard = [];
  const inherited = [];
  for (const group of groupsByWeight()) {
    if (grantingNodes(group, medalId).length) {
      direct.push(group);
    } else if (grantsByWildcard(group)) {
      wildcard.push(group);
    } else if (groupGrants(group.id, medalId)) {
      inherited.push(group);
    }
  }
  return { direct, wildcard, inherited };
}

export function medalSource(user, medalId) {
  const own = user.nodes.find((node) => node.value && node.key === medalKey(medalId) && Object.keys(node.contexts || {}).length === 0 && isActive(node));
  if (own) {
    return { kind: "own", node: own };
  }
  const groups = user.nodes
    .filter((node) => node.value && node.key.startsWith("group.") && Object.keys(node.contexts || {}).length === 0 && isActive(node))
    .map((node) => node.key.substring("group.".length));
  groups.push(state.defaultGroup);
  for (const groupId of groups) {
    if (groupGrants(groupId, medalId)) {
      return { kind: "group", group: groupId };
    }
  }
  return null;
}

function previewTagSelect(onChange) {
  const select = el("select", null, el("option", { value: "", text: "Sem tag" }));
  for (const tag of tagsByWeight()) {
    select.appendChild(el("option", { value: tag.id, text: tag.name + " (" + tag.id + ")" }));
  }
  select.value = state.working.tags.has(preview.tag) ? preview.tag : "";
  select.addEventListener("change", () => {
    preview.tag = select.value;
    onChange();
  });
  return select;
}

function previews(medal) {
  const tag = state.working.tags.get(preview.tag) || null;
  return el("div", { class: "stack" },
    field("Chat", renderMc(medalChatLine(medal, tag, SAMPLE_PLAYER, SAMPLE_MESSAGE))),
    field("/medalha", renderMc(medalListEntry(medal))));
}

function previewCard(medal) {
  const box = el("div");
  const render = () => box.replaceChildren(previews(medal));
  render();
  return el("div", { class: "card" },
    el("h2", { text: "Prévia" }),
    field("Tag na prévia", previewTagSelect(render)),
    el("div", { class: "spacer-8" }),
    box);
}

function readMedal(fields) {
  const name = fields.name.value.trim();
  const symbol = fields.symbol.value.trim();
  if (!validName(name)) {
    toast("O nome tem de 1 a 48 caracteres", "bad");
    return null;
  }
  if (!validSymbol(symbol)) {
    toast("O símbolo tem de 1 a 4 caracteres", "bad");
    return null;
  }
  const weight = Number.parseInt(fields.weight.value, 10);
  if (Number.isNaN(weight) || Math.abs(weight) > 1000000) {
    toast("Peso inválido", "bad");
    return null;
  }
  return { name, symbol, weight };
}

export async function createMedalDialog(nav) {
  const heaviest = medalsByWeight().reduce((max, medal) => Math.max(max, medal.weight), 0);
  const draft = { id: "", name: "Medalha", symbol: "✦", color: "&e", weight: heaviest + 10 };
  const id = el("input", { type: "text", class: "mono", placeholder: "ex.: coroa" });
  const name = el("input", { type: "text", placeholder: "ex.: Coroa", maxlength: "48" });
  const symbol = el("input", { type: "text", class: "mono", placeholder: "ex.: ♔", maxlength: "8" });
  const weight = el("input", { type: "number", value: String(draft.weight) });
  const previewBox = el("div");
  const colorBox = el("div");
  const refresh = () => {
    draft.name = name.value.trim() || "Medalha";
    draft.symbol = symbol.value.trim() || "✦";
    previewBox.replaceChildren(previews(draft));
    colorBox.replaceChildren(swatches(draft.color, "Cor da medalha", (color) => {
      draft.color = color;
      refresh();
    }));
  };
  name.addEventListener("input", refresh);
  symbol.addEventListener("input", refresh);
  refresh();
  const body = el("div", { class: "stack" },
    el("div", { class: "grid-2" }, field("Id", id, "Minúsculas, números, _ - +"), field("Nome", name)),
    el("div", { class: "grid-2" }, field("Símbolo", symbol, "1 a 4 caracteres, ex.: ☕ ou ⊰⊱"), field("Peso", weight, "Menor peso aparece primeiro no /medalha")),
    field("Cor", colorBox),
    previewBox);
  const created = await modal("Nova medalha", body, [
    { label: "Cancelar", className: "ghost" },
    {
      label: "Criar",
      className: "primary",
      run: () => {
        const medalId = id.value.trim().toLowerCase();
        if (!ID_PATTERN.test(medalId)) {
          toast("Id inválido", "bad");
          return false;
        }
        if (state.working.medals.has(medalId)) {
          toast("Já existe uma medalha com esse id", "bad");
          return false;
        }
        const values = readMedal({ name, symbol, weight });
        return values ? { id: medalId, color: draft.color, ...values } : false;
      },
    },
  ]);
  if (created) {
    state.working.medals.set(created.id, created);
    notify();
    nav.go("medals", created.id);
  }
}

async function deleteMedal(medal, nav) {
  const groups = medalGrants(medal.id).direct;
  let message = "A medalha " + medal.name + " será excluída ao salvar.";
  if (groups.length) {
    message += " Os cargos " + groups.map((group) => group.id).join(", ") + " deixam de dar esta medalha.";
  }
  message += " Jogadores que a escolheram ficam sem medalha.";
  if (!await confirmDialog("Excluir medalha", message, "Excluir", true)) {
    return;
  }
  for (const group of groups) {
    group.nodes = group.nodes.filter((node) => node.key !== medalKey(medal.id));
  }
  for (const entry of state.users.values()) {
    if (entry.working.selectedMedal === medal.id) {
      entry.working.selectedMedal = null;
    }
  }
  state.working.medals.delete(medal.id);
  notify();
  nav.go("medals", null);
}

function nodeBadges(node) {
  return [
    Object.keys(node.contexts || {}).length ? el("span", { class: "badge accent mono", text: contextsText(node.contexts) }) : null,
    node.expiresAt
      ? el("span", { class: node.expiresAt <= now() ? "badge bad" : "badge warn", title: formatDate(node.expiresAt), text: formatRemaining(node.expiresAt - now()) })
      : null,
  ];
}

function detailsCard(medal, nav) {
  const name = el("input", { type: "text", value: medal.name, maxlength: "48" });
  name.addEventListener("change", () => {
    if (!validName(name.value.trim())) {
      toast("O nome tem de 1 a 48 caracteres", "bad");
      name.value = medal.name;
      return;
    }
    medal.name = name.value.trim();
    notify();
  });
  const symbol = el("input", { type: "text", class: "mono", value: medal.symbol, maxlength: "8" });
  symbol.addEventListener("change", () => {
    if (!validSymbol(symbol.value.trim())) {
      toast("O símbolo tem de 1 a 4 caracteres", "bad");
      symbol.value = medal.symbol;
      return;
    }
    medal.symbol = symbol.value.trim();
    notify();
  });
  const weight = el("input", { type: "number", value: String(medal.weight) });
  weight.addEventListener("change", () => {
    const value = Number.parseInt(weight.value, 10);
    if (Number.isNaN(value) || Math.abs(value) > 1000000) {
      toast("Peso inválido", "bad");
      weight.value = String(medal.weight);
      return;
    }
    medal.weight = value;
    notify();
  });
  return el("div", { class: "card" },
    el("div", { class: "row" },
      renderMc(medalDisplay(medal), true),
      el("h2", { text: medal.name }),
      el("span", { class: "badge mono", text: medal.id }),
      el("div", { class: "grow" }),
      button("Excluir", () => deleteMedal(medal, nav), "small danger")),
    el("div", { class: "spacer-8" }),
    el("div", { class: "grid-3" },
      field("Nome", name),
      field("Símbolo", symbol, "1 a 4 caracteres"),
      field("Peso", weight, "Menor peso aparece primeiro")),
    el("div", { class: "spacer-8" }),
    field("Cor", swatches(medal.color, "Cor da medalha", (color) => {
      medal.color = color;
      notify();
    })),
    el("div", { class: "spacer-8" }),
    el("p", { class: "section-note", text: "Liberada para quem tem a permissão " + medalKey(medal.id) + ", direta ou por cargo." }));
}

function grantGroup(group, medalId) {
  group.nodes = group.nodes.concat([{ key: medalKey(medalId), value: true, contexts: {}, expiresAt: null, addedAt: null, updatedAt: null }]);
  notify();
}

function removeGrant(group, node) {
  group.nodes = group.nodes.filter((candidate) => candidate !== node);
  notify();
}

function grantsCard(medal, nav) {
  const grants = medalGrants(medal.id);
  const list = el("div", { class: "stack" });
  for (const group of grants.direct) {
    for (const node of grantingNodes(group, medal.id)) {
      list.appendChild(el("div", { class: "row" },
        el("button", { type: "button", class: "btn ghost small", onclick: () => nav.go("groups", group.id), text: group.displayName }),
        el("span", { class: "badge mono", text: group.id }),
        nodeBadges(node),
        el("div", { class: "grow" }),
        button("Remover", () => removeGrant(group, node), "small danger")));
    }
  }
  if (!grants.direct.length) {
    list.appendChild(el("p", { class: "muted", text: "Nenhum cargo dá esta medalha diretamente." }));
  }
  const select = el("select", null, el("option", { value: "", text: "Escolha um cargo" }));
  for (const group of groupsByWeight()) {
    if (!grants.direct.includes(group)) {
      select.appendChild(el("option", { value: group.id, text: group.displayName + " (" + group.id + ")" }));
    }
  }
  const add = button("Dar ao cargo", () => {
    const group = state.working.groups.get(select.value);
    if (group) {
      grantGroup(group, medal.id);
    }
  }, "primary");
  return el("div", { class: "card" },
    el("h2", null, "Cargos", el("span", { class: "hint", text: "adiciona ou remove " + medalKey(medal.id) + " no cargo" })),
    list,
    grants.inherited.length ? el("p", { class: "section-note", text: "Também por herança: " + grants.inherited.map((group) => group.id).join(", ") }) : null,
    grants.wildcard.length ? el("p", { class: "section-note", text: "Por curinga (* ou " + MEDAL_PREFIX + "*): " + grants.wildcard.map((group) => group.id).join(", ") }) : null,
    el("div", { class: "spacer-8" }),
    el("div", { class: "row" }, el("div", { class: "grow" }, select), add));
}

export function medalsPage(view, nav) {
  const medals = medalsByWeight();
  const selected = view.id && state.working.medals.get(view.id) ? state.working.medals.get(view.id) : null;
  const list = el("div", { class: "list" });
  for (const medal of medals) {
    list.appendChild(el("button", {
      type: "button",
      class: "list-item" + (selected && selected.id === medal.id ? " active" : "") + (isChanged("medal", medal.id) ? " changed" : ""),
      onclick: () => nav.go("medals", medal.id),
    },
    renderMc(medalDisplay(medal), true),
    el("span", { text: medal.name }),
    el("span", { class: "meta", text: "peso " + medal.weight })));
  }
  const left = el("div", { class: "card" },
    el("div", { class: "row" }, el("h2", { text: "Medalhas" }), el("div", { class: "grow" }), button("+ Nova", () => createMedalDialog(nav), "small primary")),
    list);
  const right = selected
    ? el("div", null, detailsCard(selected, nav), previewCard(selected), grantsCard(selected, nav))
    : el("div", { class: "card empty" }, "Escolha uma medalha à esquerda ou crie uma nova.");
  return el("div", null,
    el("div", { class: "page-head" }, el("h1", { text: "Medalhas" }), el("span", { class: "muted", text: medals.length + " medalhas" })),
    el("div", { class: "split" }, left, right));
}

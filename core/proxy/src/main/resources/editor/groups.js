import { api } from "./api.js";
import { renderMc, tagDisplay } from "./colors.js";
import { nodeTable } from "./nodes.js";
import { groupsByWeight, inheritedChain, isChanged, notify, parentsOf, state, tagsByWeight } from "./state.js";
import { ID_PATTERN, button, clear, confirmDialog, el, field, modal, toast } from "./ui.js";

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function isParentNode(node) {
  return node.value && node.key.startsWith("group.") && Object.keys(node.contexts || {}).length === 0 && !node.expiresAt;
}

function tagSelect(selected) {
  const select = el("select", null, el("option", { value: "", text: "Sem tag" }));
  for (const tag of tagsByWeight()) {
    select.appendChild(el("option", { value: tag.id, text: tag.name + " (" + tag.id + ")" }));
  }
  select.value = selected || "";
  return select;
}

function groupSelect(exclude, placeholder) {
  const select = el("select", null, el("option", { value: "", text: placeholder }));
  for (const group of groupsByWeight()) {
    if (!exclude.has(group.id)) {
      select.appendChild(el("option", { value: group.id, text: group.displayName + " (" + group.id + ")" }));
    }
  }
  return select;
}

export async function createGroupDialog(nav, base) {
  const id = el("input", { type: "text", class: "mono", placeholder: "ex.: helper" });
  const name = el("input", { type: "text", placeholder: "ex.: Helper" });
  const weight = el("input", { type: "number", value: base ? String(base.weight + 1) : "0" });
  const tag = tagSelect(null);
  const mode = el("select", null,
    el("option", { value: "empty", text: "Começar vazio" }),
    el("option", { value: "copy", text: "Copiar as permissões de um cargo" }),
    el("option", { value: "inherit", text: "Herdar de um cargo" }));
  const source = groupSelect(new Set(), "Escolha o cargo base");
  if (base) {
    mode.value = "copy";
    source.value = base.id;
  }
  const sourceField = field("Cargo base", source);
  const sync = () => {
    sourceField.hidden = mode.value === "empty";
  };
  mode.addEventListener("change", sync);
  sync();
  const body = el("div", { class: "stack" },
    el("div", { class: "grid-2" }, field("Id", id, "Minúsculas, números, _ - +"), field("Nome", name)),
    el("div", { class: "grid-2" }, field("Peso", weight, "Maior peso = cargo mais alto"), field("Tag", tag)),
    field("Como começar", mode),
    sourceField);
  const created = await modal("Novo cargo", body, [
    { label: "Cancelar", className: "ghost" },
    {
      label: "Criar",
      className: "primary",
      run: () => {
        const groupId = id.value.trim().toLowerCase();
        if (!ID_PATTERN.test(groupId)) {
          toast("Id inválido", "bad");
          return false;
        }
        if (state.working.groups.has(groupId)) {
          toast("Já existe um cargo com esse id", "bad");
          return false;
        }
        const displayName = name.value.trim() || groupId;
        if (displayName.length > 32) {
          toast("O nome tem até 32 caracteres", "bad");
          return false;
        }
        const parsedWeight = Number.parseInt(weight.value, 10);
        if (Number.isNaN(parsedWeight)) {
          toast("Peso inválido", "bad");
          return false;
        }
        let nodes = [];
        if (mode.value !== "empty") {
          const from = state.working.groups.get(source.value);
          if (!from) {
            toast("Escolha o cargo base", "bad");
            return false;
          }
          nodes = mode.value === "copy"
            ? clone(from.nodes).map((node) => ({ ...node, addedAt: null, updatedAt: null }))
            : [{ key: "group." + from.id, value: true, contexts: {}, expiresAt: null, addedAt: null, updatedAt: null }];
        }
        return { id: groupId, displayName, weight: parsedWeight, tag: tag.value || null, nodes };
      },
    },
  ]);
  if (created) {
    state.working.groups.set(created.id, created);
    notify();
    nav.go("groups", created.id);
  }
}

async function deleteGroup(group, nav) {
  const users = state.defaultGroup === group.id;
  if (users) {
    toast("O cargo padrão não pode ser removido", "bad");
    return;
  }
  const referencing = groupsByWeight().filter((other) => other.id !== group.id && other.nodes.some((node) => node.key === "group." + group.id));
  const tracks = Array.from(state.working.tracks.values()).filter((track) => track.groups.includes(group.id));
  let message = "O cargo " + group.displayName + " será excluído ao salvar.";
  if (referencing.length) {
    message += " Ele deixa de ser herdado por: " + referencing.map((other) => other.id).join(", ") + ".";
  }
  if (tracks.length) {
    message += " Sai das trilhas: " + tracks.map((track) => track.id).join(", ") + ".";
  }
  message += " Jogadores que tinham esse cargo ficam sem ele.";
  if (!await confirmDialog("Excluir cargo", message, "Excluir", true)) {
    return;
  }
  for (const other of referencing) {
    other.nodes = other.nodes.filter((node) => node.key !== "group." + group.id);
  }
  for (const track of tracks) {
    track.groups = track.groups.filter((id) => id !== group.id);
  }
  state.working.groups.delete(group.id);
  notify();
  nav.go("groups", null);
}

function detailsCard(group, nav) {
  const name = el("input", { type: "text", value: group.displayName, maxlength: "32" });
  name.addEventListener("change", () => {
    const value = name.value.trim();
    if (!value || value.length > 32) {
      toast("O nome tem de 1 a 32 caracteres", "bad");
      name.value = group.displayName;
      return;
    }
    group.displayName = value;
    notify();
  });
  const weight = el("input", { type: "number", value: String(group.weight) });
  weight.addEventListener("change", () => {
    const value = Number.parseInt(weight.value, 10);
    if (Number.isNaN(value) || Math.abs(value) > 1000000) {
      toast("Peso inválido", "bad");
      weight.value = String(group.weight);
      return;
    }
    group.weight = value;
    notify();
  });
  const tag = tagSelect(group.tag);
  tag.addEventListener("change", () => {
    group.tag = tag.value || null;
    notify();
  });
  const currentTag = group.tag ? state.working.tags.get(group.tag) : null;
  const isDefault = group.id === state.defaultGroup;
  return el("div", { class: "card" },
    el("div", { class: "row" },
      el("h2", { text: group.displayName }),
      el("span", { class: "badge mono", text: group.id }),
      isDefault ? el("span", { class: "badge ok", text: "padrão" }) : null,
      el("div", { class: "grow" }),
      button("Duplicar", () => createGroupDialog(nav, group), "small"),
      button("Excluir", () => deleteGroup(group, nav), "small danger")),
    el("div", { class: "grid-3" },
      field("Nome de exibição", name),
      field("Peso", weight, "Define a ordem e o cargo principal"),
      field("Tag do cargo", tag, "Quem tem o cargo pode usar essa tag")),
    currentTag ? el("div", { class: "row", style: null }, el("span", { class: "label", text: "Prévia:" }), renderMc(tagDisplay(currentTag), true)) : null);
}

function inheritanceCard(group) {
  const parents = parentsOf(group);
  const chips = el("div", { class: "chips" });
  for (const parent of parents) {
    const target = state.working.groups.get(parent);
    chips.appendChild(el("span", { class: "chip" },
      el("span", { text: target ? target.displayName : parent }),
      el("span", { class: "muted mono", text: parent }),
      el("button", {
        type: "button",
        title: "Parar de herdar",
        text: "×",
        onclick: () => {
          group.nodes = group.nodes.filter((node) => !(isParentNode(node) && node.key === "group." + parent));
          notify();
        },
      })));
  }
  if (!parents.length) {
    chips.appendChild(el("span", { class: "muted", text: "Não herda de nenhum cargo." }));
  }
  const exclude = new Set([group.id, ...parents]);
  for (const [id] of state.working.groups) {
    if (inheritedChain(id).has(group.id)) {
      exclude.add(id);
    }
  }
  const select = groupSelect(exclude, "Escolha um cargo");
  const add = button("Herdar", () => {
    if (!select.value) {
      return;
    }
    group.nodes = group.nodes.concat([{ key: "group." + select.value, value: true, contexts: {}, expiresAt: null, addedAt: null, updatedAt: null }]);
    notify();
  }, "primary");
  const chain = Array.from(inheritedChain(group.id)).filter((id) => !parents.includes(id));
  return el("div", { class: "card" },
    el("h2", null, "Herança", el("span", { class: "hint", text: "o cargo recebe todas as permissões dos cargos herdados" })),
    chips,
    chain.length ? el("p", { class: "section-note", text: "Também herda indiretamente: " + chain.join(", ") }) : null,
    el("div", { class: "spacer-8" }),
    el("div", { class: "row" }, el("div", { class: "grow" }, select), add));
}

function membersCard(group, nav) {
  const body = el("div", null, el("p", { class: "section-note", text: "Jogadores com este cargo direto (até 200)." }));
  const load = button("Carregar membros", async () => {
    load.disabled = true;
    try {
      const result = await api.members(group.id);
      clear(body);
      body.appendChild(el("p", { class: "section-note", text: result.total + " jogador(es)" }));
      const list = el("div", { class: "chips" });
      for (const member of result.members) {
        list.appendChild(el("button", { type: "button", class: "chip", onclick: () => nav.go("users", member.id) }, member.username || member.id));
      }
      body.appendChild(list);
    } catch (error) {
      toast(error.friendly || error.message, "bad");
      load.disabled = false;
    }
  }, "small");
  return el("div", { class: "card" }, el("div", { class: "row" }, el("h2", { text: "Membros" }), el("div", { class: "grow" }), load), body);
}

export function groupsPage(view, nav) {
  const groups = groupsByWeight();
  const selected = view.id && state.working.groups.get(view.id) ? state.working.groups.get(view.id) : null;
  const list = el("div", { class: "list" });
  for (const group of groups) {
    const tag = group.tag ? state.working.tags.get(group.tag) : null;
    list.appendChild(el("button", {
      type: "button",
      class: "list-item" + (selected && selected.id === group.id ? " active" : "") + (isChanged("group", group.id) ? " changed" : ""),
      onclick: () => nav.go("groups", group.id),
    },
    tag ? renderMc(tagDisplay(tag), true) : el("span", { class: "badge", text: "sem tag" }),
    el("span", { text: group.displayName }),
    el("span", { class: "meta", text: "peso " + group.weight })));
  }
  const left = el("div", { class: "card" },
    el("div", { class: "row" }, el("h2", { text: "Cargos" }), el("div", { class: "grow" }), button("+ Novo", () => createGroupDialog(nav), "small primary")),
    list);
  let right;
  if (!selected) {
    right = el("div", { class: "card empty" }, "Escolha um cargo à esquerda ou crie um novo.");
  } else {
    right = el("div", null,
      detailsCard(selected, nav),
      inheritanceCard(selected),
      nodeTable({
        key: "group:" + selected.id,
        title: "Permissões",
        hint: "herança fica no quadro acima",
        nodes: () => selected.nodes,
        update: (nodes) => {
          selected.nodes = nodes;
        },
        exclude: isParentNode,
        emptyText: "Este cargo não tem permissões próprias.",
      }),
      membersCard(selected, nav));
  }
  return el("div", null,
    el("div", { class: "page-head" }, el("h1", { text: "Cargos" }), el("span", { class: "muted", text: groups.length + " cargos" })),
    el("div", { class: "split" }, left, right));
}

import { api } from "./api.js";
import { renderMc, tagDisplay } from "./colors.js";
import { contextsText, expiryInputs, nodeTable, parseContexts, putNode, removeNode } from "./nodes.js";
import { groupsByWeight, isChanged, loadUser, notify, now, state, tagsByWeight, tracksById } from "./state.js";
import { button, el, field, formatDate, formatRemaining, modal, toast } from "./ui.js";

const TAG_PREFIX = "muxe.tag.";
const loading = new Set();

function isGroupNode(node) {
  return node.value && node.key.startsWith("group.");
}

function isTagNode(node) {
  return node.value && node.key.startsWith(TAG_PREFIX) && state.working.tags.has(node.key.substring(TAG_PREFIX.length));
}

function expiryBadge(node) {
  if (!node.expiresAt) {
    return el("span", { class: "badge", text: "permanente" });
  }
  if (node.expiresAt <= now()) {
    return el("span", { class: "badge bad", text: "expirado" });
  }
  return el("span", { class: "badge warn", title: formatDate(node.expiresAt), text: formatRemaining(node.expiresAt - now()) });
}

async function giveGroup(user) {
  const group = el("select", null);
  for (const candidate of groupsByWeight()) {
    group.appendChild(el("option", { value: candidate.id, text: candidate.displayName + " (" + candidate.id + ")" }));
  }
  const expiry = expiryInputs(null);
  const contexts = el("input", { type: "text", class: "mono", placeholder: "server=lobby (opcional)" });
  const result = await modal("Dar cargo a " + (user.username || user.id), el("div", { class: "stack" },
    field("Cargo", group), field("Duração", expiry.element), field("Contextos", contexts)), [
    { label: "Cancelar", className: "ghost" },
    {
      label: "Dar cargo",
      className: "primary",
      run: () => {
        try {
          return { key: "group." + group.value, value: true, contexts: parseContexts(contexts.value), expiresAt: expiry.read() };
        } catch (error) {
          toast(error.message, "bad");
          return false;
        }
      },
    },
  ]);
  if (result) {
    user.nodes = putNode(user.nodes, result);
    notify();
  }
}

function moveOnTrack(user, track, from, to) {
  let nodes = user.nodes;
  if (from) {
    nodes = nodes.filter((node) => !(node.key === "group." + from && Object.keys(node.contexts || {}).length === 0));
  }
  if (to) {
    nodes = putNode(nodes, { key: "group." + to, value: true, contexts: {}, expiresAt: null });
  }
  user.nodes = nodes;
  notify();
}

function trackButtons(user) {
  const owned = new Set(user.nodes.filter((node) => isGroupNode(node) && Object.keys(node.contexts || {}).length === 0).map((node) => node.key.substring(6)));
  const rows = [];
  for (const track of tracksById()) {
    if (!track.groups.length) {
      continue;
    }
    let index = -1;
    track.groups.forEach((group, position) => {
      if (owned.has(group)) {
        index = position;
      }
    });
    const current = index >= 0 ? track.groups[index] : null;
    const next = track.groups[index + 1];
    const previous = index > 0 ? track.groups[index - 1] : null;
    rows.push(el("div", { class: "row" },
      el("span", { class: "badge mono", text: track.id }),
      el("span", { class: "muted", text: current ? "está em " + current : "fora da trilha" }),
      el("div", { class: "grow" }),
      next ? button("Promover para " + next, () => moveOnTrack(user, track, current, next), "small") : null,
      current ? button(previous ? "Rebaixar para " + previous : "Tirar da trilha", () => moveOnTrack(user, track, current, previous), "small ghost") : null));
  }
  return rows;
}

function groupsCard(user) {
  const nodes = user.nodes.filter(isGroupNode).sort((a, b) => a.key.localeCompare(b.key));
  const list = el("div", { class: "stack" });
  for (const node of nodes) {
    const groupId = node.key.substring(6);
    const group = state.working.groups.get(groupId);
    const tag = group && group.tag ? state.working.tags.get(group.tag) : null;
    list.appendChild(el("div", { class: "row" },
      tag ? renderMc(tagDisplay(tag), true) : null,
      el("strong", { text: group ? group.displayName : groupId }),
      group ? null : el("span", { class: "badge bad", text: "cargo inexistente" }),
      Object.keys(node.contexts || {}).length ? el("span", { class: "badge accent mono", text: contextsText(node.contexts) }) : null,
      expiryBadge(node),
      el("div", { class: "grow" }),
      button("Remover", () => {
        user.nodes = removeNode(user.nodes, node);
        notify();
      }, "small danger")));
  }
  if (!nodes.length) {
    list.appendChild(el("p", { class: "muted", text: "Sem cargos diretos: o jogador é " + state.defaultGroup + "." }));
  }
  return el("div", { class: "card" },
    el("div", { class: "row" }, el("h2", { text: "Cargos" }), el("div", { class: "grow" }), button("+ Dar cargo", () => giveGroup(user), "small primary")),
    list,
    el("div", { class: "spacer-8" }),
    el("div", { class: "stack" }, trackButtons(user)));
}

function tagsCard(user) {
  const selected = el("select", null, el("option", { value: "", text: "Automática (tag do cargo principal)" }));
  for (const tag of tagsByWeight()) {
    selected.appendChild(el("option", { value: tag.id, text: tag.name + " (" + tag.id + ")" }));
  }
  selected.value = user.selectedTag || "";
  selected.addEventListener("change", () => {
    user.selectedTag = selected.value || null;
    notify();
  });
  const decorative = el("div", { class: "stack" });
  for (const tag of tagsByWeight().filter((candidate) => candidate.decorative)) {
    const node = user.nodes.find((candidate) => candidate.key === TAG_PREFIX + tag.id && Object.keys(candidate.contexts || {}).length === 0);
    const owned = Boolean(node && node.value);
    decorative.appendChild(el("div", { class: "row" },
      renderMc(tagDisplay(tag), true),
      owned ? expiryBadge(node) : el("span", { class: "muted", text: "não tem" }),
      el("div", { class: "grow" }),
      owned
        ? button("Tirar", () => {
          user.nodes = removeNode(user.nodes, node);
          notify();
        }, "small danger")
        : button("Dar", async () => {
          const expiry = expiryInputs(null);
          const expiresAt = await modal("Dar a tag " + tag.name, field("Duração", expiry.element), [
            { label: "Cancelar", className: "ghost" },
            {
              label: "Dar tag",
              className: "primary",
              run: () => {
                try {
                  return { value: expiry.read() };
                } catch (error) {
                  toast(error.message, "bad");
                  return false;
                }
              },
            },
          ]);
          if (expiresAt) {
            user.nodes = putNode(user.nodes, { key: TAG_PREFIX + tag.id, value: true, contexts: {}, expiresAt: expiresAt.value });
            notify();
          }
        }, "small")));
  }
  return el("div", { class: "card" },
    el("h2", { text: "Tags" }),
    field("Tag escolhida", selected, "Se o jogador não puder usar a tag escolhida, aparece a tag do cargo principal."),
    el("div", { class: "spacer-8" }),
    el("div", { class: "label", text: "Tags decorativas" }),
    el("div", { class: "spacer-8" }),
    decorative);
}

function profileCard(user) {
  return el("div", { class: "card" },
    el("div", { class: "user-head" },
      el("div", { class: "avatar", text: (user.username || "?").charAt(0).toUpperCase() }),
      el("div", null,
        el("div", { class: "row" }, el("h2", { text: user.username || "Sem nome" }), user.premium ? el("span", { class: "badge ok", text: "original" }) : el("span", { class: "badge", text: "pirata" })),
        el("div", { class: "muted mono", text: user.id }),
        el("div", { class: "muted", text: "Primeiro login " + formatDate(user.firstSeenAt) + " · último " + formatDate(user.lastSeenAt) }))));
}

function searchBar(nav) {
  const input = el("input", { type: "search", placeholder: "Nome do jogador ou UUID", maxlength: "36" });
  const run = async () => {
    const name = input.value.trim();
    if (!name) {
      return;
    }
    try {
      const user = await api.searchUser(name);
      loadUser(user);
      nav.go("users", user.id);
    } catch (error) {
      toast(error.friendly || error.message, "bad");
    }
  };
  input.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      run();
    }
  });
  return el("div", { class: "card" }, el("div", { class: "row" }, el("div", { class: "grow" }, input), button("Buscar", run, "primary")));
}

function ensureLoaded(id) {
  if (state.users.has(id) || loading.has(id)) {
    return;
  }
  loading.add(id);
  api.user(id).then((user) => {
    loadUser(user);
    notify();
  }).catch((error) => toast(error.friendly || error.message, "bad")).finally(() => loading.delete(id));
}

export function usersPage(view, nav) {
  const recent = el("div", { class: "list" });
  for (const [id, entry] of state.users) {
    recent.appendChild(el("button", {
      type: "button",
      class: "list-item" + (view.id === id ? " active" : "") + (isChanged("user", id) ? " changed" : ""),
      onclick: () => nav.go("users", id),
    }, el("span", { text: entry.working.username || id })));
  }
  if (!state.users.size) {
    recent.appendChild(el("div", { class: "empty", text: "Busque um jogador para editar." }));
  }
  let right;
  if (!view.id) {
    right = el("div", { class: "card empty" }, "Busque um jogador pelo nome para ver e editar cargos, tags e permissões.");
  } else if (!state.users.has(view.id)) {
    ensureLoaded(view.id);
    right = el("div", { class: "card empty" }, "Carregando jogador…");
  } else {
    const user = state.users.get(view.id).working;
    right = el("div", null,
      profileCard(user),
      groupsCard(user),
      tagsCard(user),
      nodeTable({
        key: "user:" + user.id,
        title: "Permissões do jogador",
        hint: "cargos e tags decorativas ficam nos quadros acima",
        nodes: () => user.nodes,
        update: (nodes) => {
          user.nodes = nodes;
        },
        exclude: (node) => isGroupNode(node) || isTagNode(node),
        emptyText: "Nenhuma permissão própria.",
      }));
  }
  return el("div", null,
    el("div", { class: "page-head" }, el("h1", { text: "Jogadores" })),
    searchBar(nav),
    el("div", { class: "spacer-8" }),
    el("div", { class: "split" }, el("div", { class: "card" }, el("h2", { text: "Abertos nesta sessão" }), recent), right));
}

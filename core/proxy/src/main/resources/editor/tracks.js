import { groupsByWeight, isChanged, notify, state, tracksById } from "./state.js";
import { ID_PATTERN, button, confirmDialog, el, field, modal, toast } from "./ui.js";

function label(groupId) {
  const group = state.working.groups.get(groupId);
  return group ? group.displayName : groupId;
}

async function createTrack(nav) {
  const id = el("input", { type: "text", class: "mono", placeholder: "ex.: staff" });
  const fill = el("label", { class: "row" }, el("input", { type: "checkbox" }), "Começar com todos os cargos, do menor para o maior peso");
  const created = await modal("Nova trilha", el("div", { class: "stack" }, field("Id", id, "Minúsculas, números, _ - +"), fill), [
    { label: "Cancelar", className: "ghost" },
    {
      label: "Criar",
      className: "primary",
      run: () => {
        const trackId = id.value.trim().toLowerCase();
        if (!ID_PATTERN.test(trackId)) {
          toast("Id inválido", "bad");
          return false;
        }
        if (state.working.tracks.has(trackId)) {
          toast("Já existe uma trilha com esse id", "bad");
          return false;
        }
        const all = fill.querySelector("input").checked;
        return { id: trackId, groups: all ? groupsByWeight().map((group) => group.id).reverse() : [] };
      },
    },
  ]);
  if (created) {
    state.working.tracks.set(created.id, created);
    notify();
    nav.go("tracks", created.id);
  }
}

function move(track, index, offset) {
  const target = index + offset;
  if (target < 0 || target >= track.groups.length) {
    return;
  }
  const groups = track.groups.slice();
  [groups[index], groups[target]] = [groups[target], groups[index]];
  track.groups = groups;
  notify();
}

function editor(track, nav) {
  const rows = track.groups.map((groupId, index) => el("tr", null,
    el("td", { class: "muted", text: String(index + 1) }),
    el("td", null, el("strong", { text: label(groupId) }), " ", el("span", { class: "muted mono", text: groupId })),
    el("td", { class: "actions" },
      button("↑", () => move(track, index, -1), "small ghost"),
      button("↓", () => move(track, index, 1), "small ghost"),
      button("Remover", () => {
        track.groups = track.groups.filter((id) => id !== groupId);
        notify();
      }, "small danger"))));
  const select = el("select", null, el("option", { value: "", text: "Adicionar cargo ao fim" }));
  for (const group of groupsByWeight().slice().reverse()) {
    if (!track.groups.includes(group.id)) {
      select.appendChild(el("option", { value: group.id, text: group.displayName + " (" + group.id + ")" }));
    }
  }
  const steps = el("div", { class: "track-steps" });
  track.groups.forEach((groupId, index) => {
    if (index > 0) {
      steps.appendChild(el("span", { class: "arrow", text: "→" }));
    }
    steps.appendChild(el("span", { class: "badge", text: label(groupId) }));
  });
  return el("div", { class: "card" },
    el("div", { class: "row" },
      el("h2", { text: "Trilha " + track.id }),
      el("div", { class: "grow" }),
      button("Excluir", async () => {
        if (await confirmDialog("Excluir trilha", "A trilha " + track.id + " será excluída ao salvar.", "Excluir", true)) {
          state.working.tracks.delete(track.id);
          notify();
          nav.go("tracks", null);
        }
      }, "small danger")),
    el("p", { class: "section-note", text: "Promover leva o jogador ao próximo cargo da lista; rebaixar leva ao anterior." }),
    track.groups.length ? steps : el("p", { class: "muted", text: "Trilha vazia." }),
    el("div", { class: "spacer-8" }),
    track.groups.length ? el("table", null, el("tbody", null, rows)) : null,
    el("div", { class: "spacer-8" }),
    el("div", { class: "row" }, el("div", { class: "grow" }, select), button("Adicionar", () => {
      if (select.value) {
        track.groups = track.groups.concat([select.value]);
        notify();
      }
    }, "primary")));
}

export function tracksPage(view, nav) {
  const tracks = tracksById();
  const selected = view.id && state.working.tracks.get(view.id) ? state.working.tracks.get(view.id) : null;
  const list = el("div", { class: "list" });
  for (const track of tracks) {
    list.appendChild(el("button", {
      type: "button",
      class: "list-item" + (selected && selected.id === track.id ? " active" : "") + (isChanged("track", track.id) ? " changed" : ""),
      onclick: () => nav.go("tracks", track.id),
    }, el("span", { class: "mono", text: track.id }), el("span", { class: "meta", text: track.groups.length + " cargos" })));
  }
  if (!tracks.length) {
    list.appendChild(el("div", { class: "empty", text: "Nenhuma trilha." }));
  }
  const left = el("div", { class: "card" },
    el("div", { class: "row" }, el("h2", { text: "Trilhas" }), el("div", { class: "grow" }), button("+ Nova", () => createTrack(nav), "small primary")),
    list);
  const right = selected ? editor(selected, nav) : el("div", { class: "card empty" }, "Trilhas definem a ordem de promoção dos cargos.");
  return el("div", null,
    el("div", { class: "page-head" }, el("h1", { text: "Trilhas" })),
    el("div", { class: "split" }, left, right));
}

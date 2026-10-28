import { api, forgetToken, takeToken } from "./api.js";
import { groupsPage } from "./groups.js";
import { forgetHistory, historyPage } from "./history.js";
import { changesPayload, loadData, markSaved, now, pendingChanges, revert, revertAll, state, subscribe } from "./state.js";
import { tagsPage } from "./tags.js";
import { tracksPage } from "./tracks.js";
import { usersPage } from "./users.js";
import { button, clear, confirmDialog, el, formatClock, toast } from "./ui.js";

const PAGES = [
  { id: "groups", label: "Cargos", icon: "◆", render: groupsPage, count: () => state.working.groups.size },
  { id: "tags", label: "Tags", icon: "✦", render: tagsPage, count: () => state.working.tags.size },
  { id: "tracks", label: "Trilhas", icon: "⇅", render: tracksPage, count: () => state.working.tracks.size },
  { id: "users", label: "Jogadores", icon: "☺", render: usersPage, count: () => state.users.size || "" },
  { id: "history", label: "Histórico", icon: "↺", render: historyPage, count: () => "" },
];

const view = { page: "groups", id: null };
const nav = {
  go(page, id) {
    view.page = page;
    view.id = id || null;
    window.scrollTo(0, 0);
    refresh();
  },
};

let saving = false;
let ended = false;

function screen(title, message) {
  ended = true;
  const root = clear(document.getElementById("root"));
  root.appendChild(el("div", { class: "screen" },
    el("div", { class: "card" },
      el("img", { class: "screen-logo", src: "logo.png", alt: "Muxe" }),
      el("h1", { text: title }),
      el("p", { class: "muted", text: message }))));
}

function shell() {
  const root = clear(document.getElementById("root"));
  root.appendChild(el("div", { class: "app" },
    el("header", { class: "navbar" },
      el("div", { class: "wrap navbar-inner" },
        el("div", { class: "brand" }, el("img", { class: "brand-logo", src: "logo.png", alt: "" }), el("span", { text: "MUXE" })),
        el("nav", { class: "sidebar", id: "sidebar", "aria-label": "Seções" }),
        el("div", { class: "topbar", id: "topbar" }))),
    el("section", { class: "hero" },
      el("div", { class: "wrap" },
        el("h1", { text: "Editor de permissões" }),
        el("p", { text: "Cargos, tags, trilhas e jogadores da rede Muxe." }))),
    el("main", { class: "main wrap", id: "main" }),
    el("footer", { class: "footer" },
      el("p", { text: "Muxe · Editor de permissões. As alterações valem em todos os servidores assim que você salvar." }),
      el("p", { text: "O Muxe não é afiliado à Mojang nem à Microsoft." }))));
  root.appendChild(el("aside", { class: "pending", id: "pending", hidden: true, "aria-label": "Alterações pendentes" }));
}

function renderSidebar() {
  const side = clear(document.getElementById("sidebar"));
  for (const page of PAGES) {
    side.appendChild(el("button", {
      type: "button",
      class: "nav-item" + (view.page === page.id ? " active" : ""),
      onclick: () => nav.go(page.id, null),
    }, el("span", { text: page.icon }), el("span", { text: page.label }), el("span", { class: "count", text: String(page.count()) })));
  }
}

function renderTopbar() {
  const top = clear(document.getElementById("topbar"));
  const pending = pendingChanges();
  const timer = el("span", { class: "timer", id: "timer" });
  top.appendChild(el("span", { class: "session-pill" }, el("strong", { text: state.session.actor }), el("span", { text: "sessão " }), timer));
  if (pending.length) {
    top.appendChild(el("span", { class: "badge warn", text: pending.length + " alteração(ões) pendente(s)" }));
    top.appendChild(button("Descartar", discardAll, "ghost"));
  }
  const save = button(saving ? "Salvando…" : "Salvar", saveAll, "save");
  save.disabled = saving || !pending.length;
  top.appendChild(save);
  top.appendChild(button("Sair", logout, "ghost"));
  tick();
}

function renderPending() {
  const panel = clear(document.getElementById("pending"));
  const pending = pendingChanges();
  panel.hidden = !pending.length;
  if (!pending.length) {
    return;
  }
  const names = { add: "NOVO", edit: "EDITADO", del: "EXCLUÍDO" };
  const pages = { group: "groups", tag: "tags", track: "tracks", user: "users" };
  panel.appendChild(el("header", null, el("strong", { text: "Alterações pendentes" }), el("span", { class: "badge", text: String(pending.length) })));
  panel.appendChild(el("ul", null, pending.map((item) => el("li", null,
    el("span", { class: "kind " + item.kind, text: names[item.kind] }),
    el("button", { type: "button", class: "btn ghost small what", onclick: () => item.kind !== "del" && nav.go(pages[item.type], item.id), text: item.label }),
    button("Desfazer", () => revert(item.type, item.id), "small ghost")))));
  panel.appendChild(el("footer", null, button("Descartar tudo", discardAll, "ghost"), button("Salvar", saveAll, "save")));
}

function renderMain() {
  const main = document.getElementById("main");
  const scroll = window.scrollY;
  const page = PAGES.find((candidate) => candidate.id === view.page) || PAGES[0];
  clear(main).appendChild(page.render(view, nav));
  window.scrollTo(0, scroll);
}

function refresh() {
  if (ended) {
    return;
  }
  renderSidebar();
  renderTopbar();
  renderMain();
  renderPending();
}

function tick() {
  const timer = document.getElementById("timer");
  if (!timer || !state.session) {
    return;
  }
  const left = state.session.expiresAt - now();
  timer.textContent = formatClock(left);
  timer.className = "timer" + (left < 5 * 60000 ? " low" : "");
  if (left <= 0 && !ended) {
    expire();
  }
}

function expire() {
  forgetToken();
  screen("Sessão expirada", "Use /editor no servidor para abrir uma nova sessão. Alterações não salvas foram perdidas.");
}

async function discardAll() {
  if (await confirmDialog("Descartar alterações", "Todas as alterações não salvas serão perdidas.", "Descartar", true)) {
    revertAll();
  }
}

async function saveAll() {
  const changes = changesPayload();
  if (!changes.length || saving) {
    return;
  }
  saving = true;
  refresh();
  try {
    const result = await api.save(changes);
    forgetHistory();
    markSaved(result.data);
    toast(result.saved + " alteração(ões) salva(s) e enviada(s) para a rede", "ok");
  } catch (error) {
    if (error.expired) {
      expire();
      return;
    }
    toast("Não foi possível salvar: " + error.message, "bad");
  } finally {
    saving = false;
    refresh();
  }
}

async function logout() {
  const pending = pendingChanges().length;
  const message = pending ? "Há " + pending + " alteração(ões) não salva(s), que serão perdidas." : "O link deixará de funcionar.";
  if (!await confirmDialog("Encerrar sessão", message, "Sair", pending > 0)) {
    return;
  }
  try {
    await api.logout();
  } catch (ignored) {
  }
  forgetToken();
  screen("Sessão encerrada", "Para editar de novo, use /editor no servidor.");
}

async function start() {
  if (!takeToken()) {
    screen("Abra o editor pelo jogo", "Use /editor no servidor para receber um link de acesso.");
    return;
  }
  let session;
  let data;
  try {
    session = await api.session();
    data = await api.data();
  } catch (error) {
    if (error.expired) {
      forgetToken();
    }
    screen(error.expired ? "Acesso negado" : "Não foi possível abrir o editor", error.friendly || error.message);
    return;
  }
  state.session = session;
  state.clockOffset = session.now - Date.now();
  loadData(data);
  shell();
  subscribe(refresh);
  refresh();
  setInterval(tick, 1000);
  window.addEventListener("beforeunload", (event) => {
    if (!ended && pendingChanges().length) {
      event.preventDefault();
      event.returnValue = "";
    }
  });
}

window.addEventListener("hashchange", () => {
  if (window.location.hash.length > 1) {
    window.location.reload();
  }
});

start();

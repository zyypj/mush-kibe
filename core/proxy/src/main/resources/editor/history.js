import { api } from "./api.js";
import { button, clear, el, formatDate, toast } from "./ui.js";

const cache = { actions: null, loading: false, filter: "" };

function load(render) {
  if (cache.loading) {
    return;
  }
  cache.loading = true;
  api.actions(300).then((actions) => {
    cache.actions = actions;
  }).catch((error) => toast(error.friendly || error.message, "bad")).finally(() => {
    cache.loading = false;
    render();
  });
}

export function historyPage() {
  const body = el("div");
  const search = el("input", { type: "search", placeholder: "Filtrar por autor ou texto…", value: cache.filter });
  const render = () => {
    clear(body);
    if (!cache.actions) {
      body.appendChild(el("div", { class: "empty", text: "Carregando histórico…" }));
      return;
    }
    const term = cache.filter.trim().toLowerCase();
    const rows = cache.actions
      .filter((action) => !term || action.actor.toLowerCase().includes(term) || action.description.toLowerCase().includes(term))
      .map((action) => el("tr", null,
        el("td", { class: "muted", text: formatDate(action.at) }),
        el("td", null, el("strong", { text: action.actor })),
        el("td", { class: "mono", text: action.description })));
    if (!rows.length) {
      body.appendChild(el("div", { class: "empty", text: "Nada encontrado." }));
      return;
    }
    body.appendChild(el("div", { class: "table-wrap" }, el("table", null,
      el("thead", null, el("tr", null, el("th", { text: "Quando" }), el("th", { text: "Quem" }), el("th", { text: "O que mudou" }))),
      el("tbody", null, rows))));
  };
  search.addEventListener("input", () => {
    cache.filter = search.value;
    render();
  });
  if (!cache.actions) {
    load(render);
  }
  render();
  return el("div", null,
    el("div", { class: "page-head" },
      el("h1", { text: "Histórico" }),
      el("span", { class: "muted", text: "últimas 300 alterações" }),
      el("div", { class: "spacer" }),
      button("Atualizar", () => {
        cache.actions = null;
        render();
        load(render);
      })),
    el("div", { class: "card" }, el("div", { class: "row" }, el("div", { class: "grow" }, search)), el("div", { class: "spacer-8" }), body));
}

export function forgetHistory() {
  cache.actions = null;
}

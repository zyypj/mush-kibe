import { chatLine, chatPrefix, renderMc, swatches, tagDisplay } from "./colors.js";
import { isChanged, notify, state, tagsByWeight } from "./state.js";
import { ID_PATTERN, button, confirmDialog, el, field, modal, toast } from "./ui.js";

const SAMPLE_PLAYER = "tadeu";
const SAMPLE_MESSAGE = "Olá!";

function previews(tag) {
  return el("div", { class: "stack" },
    field("Chat", renderMc(chatLine(tag, SAMPLE_PLAYER, SAMPLE_MESSAGE))),
    field("/tag", renderMc(tagDisplay(tag))),
    field("Nome acima da cabeça / tab", renderMc(chatPrefix(tag) + tag.color + SAMPLE_PLAYER)));
}

function validName(name) {
  return name.length >= 1 && name.length <= 24;
}

export async function createTagDialog(nav) {
  const draft = { id: "", name: "", color: "&a", weight: 0, prefix: null, decorative: false };
  const id = el("input", { type: "text", class: "mono", placeholder: "ex.: halloween" });
  const name = el("input", { type: "text", placeholder: "ex.: Halloween", maxlength: "24" });
  const weight = el("input", { type: "number", value: "0" });
  const decorative = el("input", { type: "checkbox" });
  const previewBox = el("div");
  const colorBox = el("div");
  const refresh = () => {
    draft.name = name.value.trim() || "Tag";
    previewBox.replaceChildren(previews(draft));
    colorBox.replaceChildren(swatches(draft.color, "Cor da tag", (color) => {
      draft.color = color;
      refresh();
    }));
  };
  name.addEventListener("input", refresh);
  refresh();
  const body = el("div", { class: "stack" },
    el("div", { class: "grid-2" }, field("Id", id, "Minúsculas, números, _ - +"), field("Nome", name)),
    field("Cor", colorBox),
    el("div", { class: "grid-2" }, field("Peso", weight, "Tags de maior peso aparecem primeiro"),
      field("Tipo", el("label", { class: "row" }, decorative, "Decorativa (dada a jogadores, sem cargo)"))),
    previewBox);
  const created = await modal("Nova tag", body, [
    { label: "Cancelar", className: "ghost" },
    {
      label: "Criar",
      className: "primary",
      run: () => {
        const tagId = id.value.trim().toLowerCase();
        if (!ID_PATTERN.test(tagId)) {
          toast("Id inválido", "bad");
          return false;
        }
        if (state.working.tags.has(tagId)) {
          toast("Já existe uma tag com esse id", "bad");
          return false;
        }
        if (!validName(name.value.trim())) {
          toast("O nome tem de 1 a 24 caracteres", "bad");
          return false;
        }
        const parsedWeight = Number.parseInt(weight.value, 10);
        if (Number.isNaN(parsedWeight)) {
          toast("Peso inválido", "bad");
          return false;
        }
        return { id: tagId, name: name.value.trim(), color: draft.color, weight: parsedWeight, prefix: null, decorative: decorative.checked };
      },
    },
  ]);
  if (created) {
    state.working.tags.set(created.id, created);
    notify();
    nav.go("tags", created.id);
  }
}

async function deleteTag(tag, nav) {
  const groups = Array.from(state.working.groups.values()).filter((group) => group.tag === tag.id);
  let message = "A tag " + tag.name + " será excluída ao salvar.";
  if (groups.length) {
    message += " Os cargos " + groups.map((group) => group.id).join(", ") + " ficam sem tag.";
  }
  if (!await confirmDialog("Excluir tag", message, "Excluir", true)) {
    return;
  }
  for (const group of groups) {
    group.tag = null;
  }
  state.working.tags.delete(tag.id);
  notify();
  nav.go("tags", null);
}

function editor(tag, nav) {
  const name = el("input", { type: "text", value: tag.name, maxlength: "24" });
  name.addEventListener("change", () => {
    if (!validName(name.value.trim())) {
      toast("O nome tem de 1 a 24 caracteres", "bad");
      name.value = tag.name;
      return;
    }
    tag.name = name.value.trim();
    notify();
  });
  const weight = el("input", { type: "number", value: String(tag.weight) });
  weight.addEventListener("change", () => {
    const value = Number.parseInt(weight.value, 10);
    if (Number.isNaN(value) || Math.abs(value) > 1000000) {
      toast("Peso inválido", "bad");
      weight.value = String(tag.weight);
      return;
    }
    tag.weight = value;
    notify();
  });
  const custom = el("input", { type: "checkbox", checked: tag.prefix !== null && tag.prefix !== undefined });
  const prefix = el("input", { type: "text", class: "mono", value: tag.prefix || "", placeholder: "&a&lVIP ", disabled: !custom.checked, maxlength: "64" });
  custom.addEventListener("change", () => {
    tag.prefix = custom.checked ? (prefix.value || chatPrefix({ ...tag, prefix: null })) : null;
    notify();
  });
  prefix.addEventListener("change", () => {
    tag.prefix = prefix.value;
    notify();
  });
  const decorative = el("input", { type: "checkbox", checked: Boolean(tag.decorative) });
  decorative.addEventListener("change", () => {
    tag.decorative = decorative.checked;
    notify();
  });
  const usedBy = Array.from(state.working.groups.values()).filter((group) => group.tag === tag.id).map((group) => group.id);
  return el("div", null,
    el("div", { class: "card" },
      el("div", { class: "row" },
        renderMc(tagDisplay(tag), true),
        el("span", { class: "badge mono", text: tag.id }),
        tag.decorative ? el("span", { class: "badge accent", text: "decorativa" }) : null,
        el("div", { class: "grow" }),
        button("Excluir", () => deleteTag(tag, nav), "small danger")),
      el("div", { class: "spacer-8" }),
      el("div", { class: "grid-2" }, field("Nome", name), field("Peso", weight)),
      el("div", { class: "spacer-8" }),
      field("Cor", swatches(tag.color, "Cor da tag", (color) => {
        tag.color = color;
        notify();
      })),
      el("div", { class: "spacer-8" }),
      el("label", { class: "row" }, decorative, "Decorativa: liberada para jogadores com a permissão muxe.tag." + tag.id + ", sem precisar de cargo"),
      el("div", { class: "spacer-8" }),
      el("label", { class: "row" }, custom, "Usar um prefixo de chat próprio"),
      field("Prefixo do chat", prefix, "Por padrão: cor + negrito + NOME + espaço. Deixe vazio para sem prefixo."),
      el("p", { class: "section-note", text: usedBy.length ? "Tag dos cargos: " + usedBy.join(", ") : "Nenhum cargo usa esta tag." })),
    el("div", { class: "card" }, el("h2", { text: "Prévia" }), previews(tag)));
}

export function tagsPage(view, nav) {
  const tags = tagsByWeight();
  const selected = view.id && state.working.tags.get(view.id) ? state.working.tags.get(view.id) : null;
  const list = el("div", { class: "list" });
  for (const tag of tags) {
    list.appendChild(el("button", {
      type: "button",
      class: "list-item" + (selected && selected.id === tag.id ? " active" : "") + (isChanged("tag", tag.id) ? " changed" : ""),
      onclick: () => nav.go("tags", tag.id),
    },
    renderMc(tagDisplay(tag), true),
    tag.decorative ? el("span", { class: "badge accent", text: "decorativa" }) : null,
    el("span", { class: "meta", text: "peso " + tag.weight })));
  }
  const left = el("div", { class: "card" },
    el("div", { class: "row" }, el("h2", { text: "Tags" }), el("div", { class: "grow" }), button("+ Nova", () => createTagDialog(nav), "small primary")),
    list);
  const right = selected ? editor(selected, nav) : el("div", { class: "card empty" }, "Escolha uma tag à esquerda ou crie uma nova.");
  return el("div", null,
    el("div", { class: "page-head" }, el("h1", { text: "Tags" }), el("span", { class: "muted", text: tags.length + " tags" })),
    el("div", { class: "split" }, left, right));
}

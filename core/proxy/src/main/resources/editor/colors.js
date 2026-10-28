import { el } from "./ui.js";

export const COLORS = [
  { code: "0", name: "Preto", hex: "#000000" },
  { code: "1", name: "Azul escuro", hex: "#0000AA" },
  { code: "2", name: "Verde escuro", hex: "#00AA00" },
  { code: "3", name: "Ciano escuro", hex: "#00AAAA" },
  { code: "4", name: "Vermelho escuro", hex: "#AA0000" },
  { code: "5", name: "Roxo", hex: "#AA00AA" },
  { code: "6", name: "Dourado", hex: "#FFAA00" },
  { code: "7", name: "Cinza", hex: "#AAAAAA" },
  { code: "8", name: "Cinza escuro", hex: "#555555" },
  { code: "9", name: "Azul", hex: "#5555FF" },
  { code: "a", name: "Verde", hex: "#55FF55" },
  { code: "b", name: "Ciano", hex: "#55FFFF" },
  { code: "c", name: "Vermelho", hex: "#FF5555" },
  { code: "d", name: "Rosa", hex: "#FF55FF" },
  { code: "e", name: "Amarelo", hex: "#FFFF55" },
  { code: "f", name: "Branco", hex: "#FFFFFF" },
];

const HEX = Object.fromEntries(COLORS.map((color) => [color.code, color.hex]));
const FORMATS = { l: "mc-bold", o: "mc-italic", n: "mc-underline", m: "mc-strike", k: "mc-magic" };
const DARK = new Set([HEX["0"], HEX["1"]]);

export function renderMc(text, inline) {
  const box = el("span", { class: "mc" + (inline ? " inline" : "") });
  let color = HEX.f;
  let formats = new Set();
  let buffer = "";
  const flush = () => {
    if (!buffer) {
      return;
    }
    const span = el("span", { text: buffer });
    span.style.color = color;
    span.className = Array.from(formats).concat(DARK.has(color) ? ["mc-dark"] : []).join(" ");
    box.appendChild(span);
    buffer = "";
  };
  const source = String(text || "");
  for (let index = 0; index < source.length; index++) {
    const char = source[index];
    const next = (source[index + 1] || "").toLowerCase();
    if ((char === "&" || char === "§") && (HEX[next] || FORMATS[next] || next === "r")) {
      flush();
      if (HEX[next]) {
        color = HEX[next];
        formats = new Set();
      } else if (next === "r") {
        color = HEX.f;
        formats = new Set();
      } else {
        formats.add(FORMATS[next]);
      }
      index++;
      continue;
    }
    buffer += char;
  }
  flush();
  if (!box.firstChild) {
    box.appendChild(el("span", { class: "muted", text: " " }));
  }
  return box;
}

export function chatPrefix(tag) {
  if (tag.prefix !== null && tag.prefix !== undefined) {
    return tag.prefix;
  }
  return tag.color + "&l" + tag.name.toUpperCase() + " ";
}

export function tagDisplay(tag) {
  return tag.color + tag.name;
}

export function chatLine(tag, player, message) {
  return chatPrefix(tag) + tag.color + player + "&f: " + message;
}

export function el(tag, props, ...children) {
  const node = document.createElement(tag);
  if (props) {
    for (const [key, value] of Object.entries(props)) {
      if (value === undefined || value === null || value === false) {
        continue;
      }
      if (key === "class") {
        node.className = value;
      } else if (key === "text") {
        node.textContent = value;
      } else if (key.startsWith("on") && typeof value === "function") {
        node.addEventListener(key.substring(2).toLowerCase(), value);
      } else if (key === "value") {
        node.value = value;
      } else if (key === "checked" || key === "disabled" || key === "selected") {
        node[key] = Boolean(value);
      } else {
        node.setAttribute(key, value === true ? "" : String(value));
      }
    }
  }
  append(node, children);
  return node;
}

function append(node, children) {
  for (const child of children) {
    if (child === undefined || child === null || child === false) {
      continue;
    }
    if (Array.isArray(child)) {
      append(node, child);
    } else if (child instanceof Node) {
      node.appendChild(child);
    } else {
      node.appendChild(document.createTextNode(String(child)));
    }
  }
}

export function clear(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
  return node;
}

export function button(label, onClick, className) {
  return el("button", { type: "button", class: "btn " + (className || ""), onclick: onClick, text: label });
}

export function field(label, input, hint) {
  return el("div", { class: "field" }, el("label", { text: label }), input, hint ? el("span", { class: "muted", text: hint }) : null);
}

export function toast(message, kind) {
  const box = document.getElementById("toasts");
  const item = el("div", { class: "toast " + (kind || ""), role: "status", text: message });
  box.appendChild(item);
  setTimeout(() => item.remove(), kind === "bad" ? 7000 : 3500);
}

export function modal(title, body, actions) {
  return new Promise((resolve) => {
    const overlay = el("div", { class: "overlay" });
    const close = (value) => {
      overlay.remove();
      document.removeEventListener("keydown", onKey);
      resolve(value);
    };
    const onKey = (event) => {
      if (event.key === "Escape") {
        close(null);
      }
    };
    const footer = el("div", { class: "row end" });
    for (const action of actions) {
      footer.appendChild(button(action.label, () => {
        if (action.run) {
          const result = action.run();
          if (result === false) {
            return;
          }
          close(result === undefined ? true : result);
        } else {
          close(null);
        }
      }, action.className));
    }
    overlay.appendChild(el("div", { class: "modal", role: "dialog", "aria-modal": "true" }, el("h3", { text: title }), body, footer));
    overlay.addEventListener("mousedown", (event) => {
      if (event.target === overlay) {
        close(null);
      }
    });
    document.addEventListener("keydown", onKey);
    document.body.appendChild(overlay);
    const first = overlay.querySelector("input, select, textarea");
    if (first) {
      first.focus();
    }
  });
}

export function confirmDialog(title, message, confirmLabel, danger) {
  return modal(title, el("p", { class: "muted", text: message }), [
    { label: "Cancelar", className: "ghost" },
    { label: confirmLabel || "Confirmar", className: danger ? "danger" : "primary", run: () => true },
  ]);
}

const UNITS = { s: 1000, m: 60000, h: 3600000, d: 86400000, w: 604800000, mo: 2592000000, y: 31536000000 };

export function parseDuration(text) {
  const clean = String(text || "").trim().toLowerCase().replace(/\s+/g, "");
  if (!clean) {
    return null;
  }
  const pattern = /(\d+)(mo|y|w|d|h|m|s)/g;
  let total = 0;
  let consumed = "";
  let match;
  while ((match = pattern.exec(clean)) !== null) {
    total += Number(match[1]) * UNITS[match[2]];
    consumed += match[0];
  }
  return consumed === clean && total > 0 ? total : null;
}

export function formatDate(millis) {
  if (millis === null || millis === undefined) {
    return "—";
  }
  return new Date(millis).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function formatRemaining(millis) {
  if (millis <= 0) {
    return "expirado";
  }
  const minutes = Math.floor(millis / 60000);
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const rest = minutes % 60;
  if (days > 0) {
    return days + "d " + hours + "h";
  }
  if (hours > 0) {
    return hours + "h " + rest + "m";
  }
  return Math.max(rest, 1) + "m";
}

export function formatClock(millis) {
  const seconds = Math.max(0, Math.floor(millis / 1000));
  const minutes = Math.floor(seconds / 60);
  return String(minutes).padStart(2, "0") + ":" + String(seconds % 60).padStart(2, "0");
}

export function toLocalInput(millis) {
  const date = new Date(millis);
  const pad = (value) => String(value).padStart(2, "0");
  return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate()) + "T" + pad(date.getHours()) + ":" + pad(date.getMinutes());
}

export const ID_PATTERN = /^[a-z0-9_+-]{1,32}$/;
export const NODE_PATTERN = /^[a-z0-9_.*+-]{1,128}$/;
export const CONTEXT_PATTERN = /^[^=;\s]+$/;

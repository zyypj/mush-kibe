const listeners = new Set();

export const state = {
  session: null,
  clockOffset: 0,
  defaultGroup: "membro",
  original: { groups: new Map(), tags: new Map(), tracks: new Map() },
  working: { groups: new Map(), tags: new Map(), tracks: new Map() },
  users: new Map(),
};

export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function notify() {
  for (const listener of listeners) {
    listener();
  }
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function toMap(list) {
  return new Map(list.map((item) => [item.id, item]));
}

export function loadData(data) {
  state.defaultGroup = data.defaultGroup;
  state.original = { groups: toMap(data.groups), tags: toMap(data.tags), tracks: toMap(data.tracks) };
  state.working = { groups: toMap(clone(data.groups)), tags: toMap(clone(data.tags)), tracks: toMap(clone(data.tracks)) };
}

export function loadUser(user) {
  const existing = state.users.get(user.id);
  if (existing && userChanged(existing)) {
    return existing;
  }
  const entry = { original: clone(user), working: clone(user) };
  state.users.set(user.id, entry);
  return entry;
}

export function now() {
  return Date.now() + state.clockOffset;
}

function nodeSignature(node) {
  const contexts = Object.keys(node.contexts || {}).sort().map((key) => key + "=" + node.contexts[key]).join(";");
  return [node.key, node.value, contexts, node.expiresAt || ""].join("|");
}

function nodesSignature(nodes) {
  return (nodes || []).map(nodeSignature).sort().join("\n");
}

function groupSignature(group) {
  return JSON.stringify([group.displayName, group.weight, group.tag || null, nodesSignature(group.nodes)]);
}

function tagSignature(tag) {
  return JSON.stringify([tag.name, tag.color, tag.weight, tag.prefix === undefined ? null : tag.prefix, Boolean(tag.decorative)]);
}

function trackSignature(track) {
  return JSON.stringify(track.groups);
}

function userChanged(entry) {
  return nodesSignature(entry.original.nodes) !== nodesSignature(entry.working.nodes)
    || (entry.original.selectedTag || null) !== (entry.working.selectedTag || null);
}

const KINDS = [
  { type: "tag", key: "tags", signature: tagSignature, label: "Tag" },
  { type: "group", key: "groups", signature: groupSignature, label: "Cargo" },
  { type: "track", key: "tracks", signature: trackSignature, label: "Trilha" },
];

export function isChanged(type, id) {
  if (type === "user") {
    const entry = state.users.get(id);
    return Boolean(entry && userChanged(entry));
  }
  const kind = KINDS.find((candidate) => candidate.type === type);
  const before = state.original[kind.key].get(id);
  const after = state.working[kind.key].get(id);
  if (!before || !after) {
    return before !== after;
  }
  return kind.signature(before) !== kind.signature(after);
}

export function pendingChanges() {
  const items = [];
  for (const kind of KINDS) {
    const before = state.original[kind.key];
    const after = state.working[kind.key];
    for (const [id, item] of after) {
      if (!before.has(id)) {
        items.push({ kind: "add", type: kind.type, id, label: kind.label + " " + id });
      } else if (kind.signature(before.get(id)) !== kind.signature(item)) {
        items.push({ kind: "edit", type: kind.type, id, label: kind.label + " " + id });
      }
    }
    for (const id of before.keys()) {
      if (!after.has(id)) {
        items.push({ kind: "del", type: kind.type, id, label: kind.label + " " + id });
      }
    }
  }
  for (const [id, entry] of state.users) {
    if (userChanged(entry)) {
      items.push({ kind: "edit", type: "user", id, label: "Jogador " + (entry.working.username || id) });
    }
  }
  return items;
}

export function changesPayload() {
  const pending = pendingChanges();
  const payload = [];
  const order = ["tag", "group", "track", "user"];
  for (const type of order) {
    for (const item of pending.filter((candidate) => candidate.type === type && candidate.kind !== "del")) {
      payload.push(put(item));
    }
  }
  for (const type of ["track", "group", "tag"]) {
    for (const item of pending.filter((candidate) => candidate.type === type && candidate.kind === "del")) {
      payload.push({ type: "delete-" + type, id: item.id });
    }
  }
  return payload;
}

function put(item) {
  if (item.type === "user") {
    const user = state.users.get(item.id).working;
    return { type: "put-user", user: { id: user.id, nodes: user.nodes, selectedTag: user.selectedTag || null } };
  }
  if (item.type === "group") {
    const group = state.working.groups.get(item.id);
    return { type: "put-group", group: { id: group.id, displayName: group.displayName, weight: group.weight, tag: group.tag || null, nodes: group.nodes } };
  }
  if (item.type === "tag") {
    const tag = state.working.tags.get(item.id);
    return { type: "put-tag", tag: { id: tag.id, name: tag.name, color: tag.color, weight: tag.weight, prefix: tag.prefix === undefined ? null : tag.prefix, decorative: Boolean(tag.decorative) } };
  }
  const track = state.working.tracks.get(item.id);
  return { type: "put-track", track: { id: track.id, groups: track.groups } };
}

export function revert(type, id) {
  if (type === "user") {
    const entry = state.users.get(id);
    entry.working = clone(entry.original);
  } else {
    const key = KINDS.find((candidate) => candidate.type === type).key;
    const original = state.original[key].get(id);
    if (original) {
      state.working[key].set(id, clone(original));
    } else {
      state.working[key].delete(id);
    }
  }
  notify();
}

export function revertAll() {
  for (const kind of KINDS) {
    state.working[kind.key] = new Map(Array.from(state.original[kind.key].entries()).map(([id, item]) => [id, clone(item)]));
  }
  for (const entry of state.users.values()) {
    entry.working = clone(entry.original);
  }
  notify();
}

export function markSaved(data) {
  loadData(data);
  for (const entry of state.users.values()) {
    entry.original = clone(entry.working);
  }
  notify();
}

export function groupsByWeight() {
  return Array.from(state.working.groups.values()).sort((a, b) => b.weight - a.weight || a.id.localeCompare(b.id));
}

export function tagsByWeight() {
  return Array.from(state.working.tags.values()).sort((a, b) => b.weight - a.weight || a.id.localeCompare(b.id));
}

export function tracksById() {
  return Array.from(state.working.tracks.values()).sort((a, b) => a.id.localeCompare(b.id));
}

export function parentsOf(group) {
  return group.nodes.filter((node) => node.value && node.key.startsWith("group.") && Object.keys(node.contexts || {}).length === 0 && !node.expiresAt)
    .map((node) => node.key.substring("group.".length));
}

export function inheritedChain(groupId) {
  const seen = new Set();
  const stack = [groupId];
  while (stack.length) {
    const current = stack.pop();
    const group = state.working.groups.get(current);
    if (!group || seen.has(current)) {
      continue;
    }
    seen.add(current);
    for (const node of group.nodes) {
      if (node.value && node.key.startsWith("group.")) {
        stack.push(node.key.substring("group.".length));
      }
    }
  }
  seen.delete(groupId);
  return seen;
}

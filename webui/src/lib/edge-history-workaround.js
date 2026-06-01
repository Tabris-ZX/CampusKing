function isAffectedEdge() {
  const userAgent = navigator.userAgent || "";
  return /Edg\//.test(userAgent);
}

function wrapHistoryMethod(methodName) {
  const original = window.history[methodName];
  if (typeof original !== "function") {
    return;
  }

  window.history[methodName] = function patchedHistoryMethod(state, title, url) {
    // Work around Edge bringing the tab back to foreground when vue-router
    // updates history state from its visibilitychange handler.
    if (document.visibilityState === "hidden") {
      return original.call(this, null, title ?? "", url);
    }
    return original.call(this, state, title, url);
  };
}

export function installEdgeHistoryWorkaround() {
  if (typeof window === "undefined" || !isAffectedEdge()) {
    return;
  }

  wrapHistoryMethod("replaceState");
  wrapHistoryMethod("pushState");
}

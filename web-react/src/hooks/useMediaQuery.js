import { useCallback, useSyncExternalStore } from "react";

/**
 * Subscribe to a CSS media query from JavaScript.
 *
 * Use this only when the breakpoint has to change WHAT is rendered, not how it
 * looks. CSS handles appearance far better and keeps layout out of React, but
 * it cannot turn one control into a different control: a <select> and a group
 * of buttons have different semantics, keyboard behaviour and screen reader
 * output, so choosing between them is a render decision.
 *
 * Built on useSyncExternalStore, which exists for exactly this shape of
 * problem: reading from a browser API that changes outside React. It avoids
 * the useState + useEffect version's window between first render and the
 * effect, where the component briefly renders against a stale match.
 *
 * Pass the exact complement of the stylesheet's breakpoint, e.g. the CSS
 * "(min-width: 600px)" tier is "(max-width: 599.98px)" here. The fractional
 * value avoids a dead zone at non-integer viewport widths, which happen on
 * zoomed and high-DPI displays.
 */
export function useMediaQuery(query) {
  const subscribe = useCallback(
    (onStoreChange) => {
      const mql = window.matchMedia(query);
      mql.addEventListener("change", onStoreChange);
      return () => mql.removeEventListener("change", onStoreChange);
    },
    [query]
  );

  const getSnapshot = useCallback(() => window.matchMedia(query).matches, [query]);

  return useSyncExternalStore(subscribe, getSnapshot);
}

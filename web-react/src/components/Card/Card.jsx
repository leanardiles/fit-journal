/**
 * Reusable container ("card") , a styled box that wraps arbitrary content.
 * No text of its own, so no i18n needed; it just holds other components.
 * Props:
 *   children - whatever you place inside <Card>...</Card>
 */
export function Card({ children }) {
  return (
    <div
      style={{
        background: "var(--bg)",
        border: "1px solid var(--border)",
        borderRadius: "var(--radius)",
        padding: "var(--space-lg)",
        maxWidth: 360,
        fontFamily: "var(--font-body)",
        color: "var(--text)",
      }}
    >
      {children}
    </div>
  );
}
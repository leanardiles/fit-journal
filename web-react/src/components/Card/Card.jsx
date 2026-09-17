/**
 * Reusable container ("card") — a styled box that wraps arbitrary content.
 * No text of its own, so no i18n needed; it just holds other components.
 * Props:
 *   children - whatever you place inside <Card>...</Card>
 */
export function Card({ children }) {
  return (
    <div
      style={{
        background: "#fff",
        border: "1px solid #e2e2e2",
        borderRadius: 12,
        padding: 24,
        boxShadow: "0 4px 14px rgba(0,0,0,0.06)",
        maxWidth: 360,
        fontFamily: "sans-serif",
      }}
    >
      {children}
    </div>
  );
}
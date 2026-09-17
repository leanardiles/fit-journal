import { Outlet } from "react-router-dom";

/**
 * Shared shell for the auth pages (login, register): a centered card area
 * on the app background. The specific page (login/register) renders via <Outlet />.
 */
export function AuthLayout() {
  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--bg)",
      }}
    >
      <Outlet />
    </div>
  );
}
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { logout } from "../../api/auth";
import logo from "../../assets/logo-and-name-dark.png";
import "./AppLayout.css";

export function AppLayout() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <img className="app-logo" src={logo} alt={t("appName")} />

        <nav className="app-nav">
          <NavLink to="/dashboard">{t("nav.dashboard")}</NavLink>
          <NavLink to="/routine">{t("nav.routine")}</NavLink>
          <NavLink to="/exercises">{t("nav.exercises")}</NavLink>
          <NavLink to="/calendar">{t("nav.calendar")}</NavLink>
          <NavLink to="/workout">{t("nav.workout")}</NavLink>
          <NavLink to="/profile">{t("nav.profile")}</NavLink>
        </nav>

        <button className="app-logout-btn" onClick={handleLogout}>
          {t("nav.logout")}
        </button>
      </header>

      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}
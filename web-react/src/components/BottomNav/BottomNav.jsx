import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  IconDashboard,
  IconProfile,
  IconRoutine,
  IconExercises,
  IconCalendar,
  IconWorkout,
} from "../icons/NavIcons";
import "./BottomNav.css";

// Narrow-screen primary navigation. Six equal destinations, no brand mark:
// the mark would eat a tab's worth of width without being a destination.
const ITEMS = [
  { to: "/dashboard", labelKey: "nav.dashboard", Icon: IconDashboard },
  { to: "/profile", labelKey: "nav.profile", Icon: IconProfile },
  { to: "/routine", labelKey: "nav.routine", Icon: IconRoutine },
  { to: "/exercises", labelKey: "nav.exercises", Icon: IconExercises },
  { to: "/calendar", labelKey: "nav.calendar", Icon: IconCalendar },
  { to: "/workout", labelKey: "nav.workout", Icon: IconWorkout },
];

export function BottomNav() {
  const { t } = useTranslation();

  return (
    <nav className="bottom-nav" aria-label={t("nav.primary")}>
      {ITEMS.map(({ to, labelKey, Icon }) => (
        // Icon-only links still need an accessible name, so the translated
        // label goes on aria-label and surfaces again as the hover tooltip.
        <NavLink
          key={to}
          to={to}
          className="bn-item"
          aria-label={t(labelKey)}
          title={t(labelKey)}
        >
          <Icon className="bn-icon" />
        </NavLink>
      ))}
    </nav>
  );
}

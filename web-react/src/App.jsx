import { useTranslation } from "react-i18next";
import { Button } from "./components/Button/Button";

function App() {
  const { t, i18n } = useTranslation();

  return (
    <div style={{ padding: 40, fontFamily: "sans-serif" }}>
      <h1>{t("appName")}</h1>
      <p>{t("login.title")}</p>

      {/* New reusable, i18n-aware Button component */}
      <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
        <Button labelKey="login.button" variant="primary" onClick={() => alert("clicked")} />
        <Button labelKey="nav.logout" variant="secondary" />
      </div>

      <div style={{ marginTop: 20 }}>
        <button onClick={() => i18n.changeLanguage("en")}>English</button>
        <button onClick={() => i18n.changeLanguage("es")}>Español (LatAm)</button>
        <button onClick={() => i18n.changeLanguage("es-AR")}>Español (Argentina)</button>
        <button onClick={() => i18n.changeLanguage("nl")}>Nederlands</button>
      </div>
    </div>
  );
}

export default App;
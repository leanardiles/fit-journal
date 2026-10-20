import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { RegisterPage } from "./pages/RegisterPage/RegisterPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage/VerifyEmailPage";
import { ForgotPasswordPage } from "./pages/ForgotPasswordPage/ForgotPasswordPage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage/ResetPasswordPage";
import { ToastProvider } from "./context/ToastContext";
import { Dashboard } from "./pages/Dashboard/Dashboard";
import { Routine } from "./pages/Routine/Routine";
import { Exercises } from "./pages/Exercises/Exercises";
import { Calendar } from "./pages/Calendar/Calendar";
import { Profile } from "./pages/Profile/Profile";
import { Workout } from "./pages/Workout/Workout";
import { AppLayout } from "./layouts/AppLayout/AppLayout";
import { AuthLayout } from "./layouts/AuthLayout/AuthLayout";   // ← ADD THIS
import { ProtectedRoute } from "./components/ProtectedRoute/ProtectedRoute";
import { UserProvider } from "./context/UserContext";
import { useEffect } from "react";
import i18n from "./i18n/config";
import { DevL10nPanel } from "./components/DevL10nPanel/DevL10nPanel";



function App() {
  // Keep <html lang> and dir in sync with the active locale; the dev RTL toggle
  // can then flip dir for mirroring tests.
  useEffect(() => {
    const RTL = ["ar", "he", "fa", "ur"];
    const apply = (lng) => {
      const el = document.documentElement;
      el.setAttribute("lang", lng || "en");
      el.setAttribute("dir", RTL.some((r) => (lng || "").startsWith(r)) ? "rtl" : "ltr");
    };
    apply(i18n.language);
    i18n.on("languageChanged", apply);
    return () => i18n.off("languageChanged", apply);
  }, []);

  return (
    <BrowserRouter>
      <ToastProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* Auth pages share AuthLayout (centered, no nav) */}
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
          </Route>

          {/* App pages share AppLayout + protection */}
          <Route element={<ProtectedRoute><UserProvider><AppLayout /></UserProvider></ProtectedRoute>}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/routine" element={<Routine />} />
            <Route path="/exercises" element={<Exercises />} />
            <Route path="/calendar" element={<Calendar />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/workout" element={<Workout />} />
          </Route>
        </Routes>
        {import.meta.env.DEV && <DevL10nPanel />}
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
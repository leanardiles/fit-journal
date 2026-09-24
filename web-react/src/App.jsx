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



function App() {
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
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ToastProvider } from "./context/ToastContext";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { Dashboard } from "./pages/Dashboard/Dashboard";
import { Routine } from "./pages/Routine/Routine";
import { Exercises } from "./pages/Exercises/Exercises";
import { Calendar } from "./pages/Calendar/Calendar";
import { Profile } from "./pages/Profile/Profile";
import { Workout } from "./pages/Workout/Workout";
import { AppLayout } from "./layouts/AppLayout/AppLayout";
import { AuthLayout } from "./layouts/AuthLayout/AuthLayout";   // ← ADD THIS
import { ProtectedRoute } from "./components/ProtectedRoute/ProtectedRoute";
import { RegisterPage } from "./pages/RegisterPage/RegisterPage";
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
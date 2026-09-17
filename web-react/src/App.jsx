import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage/LoginPage";
import { Dashboard } from "./pages/Dashboard/Dashboard";
import { Routine } from "./pages/Routine/Routine";
import { Exercises } from "./pages/Exercises/Exercises";
import { Calendar } from "./pages/Calendar/Calendar";
import { Profile } from "./pages/Profile/Profile";
import { Workout } from "./pages/Workout/Workout";
import { AppLayout } from "./layouts/AppLayout/AppLayout";
import { ProtectedRoute } from "./components/ProtectedRoute/ProtectedRoute";
import { UserProvider } from "./context/UserContext";


function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />

        {/* All app pages share AppLayout and are protected */}
        <Route
          element={
            <ProtectedRoute>
              <UserProvider>
                <AppLayout />
              </UserProvider>
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/routine" element={<Routine />} />
          <Route path="/exercises" element={<Exercises />} />
          <Route path="/calendar" element={<Calendar />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/workout" element={<Workout />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
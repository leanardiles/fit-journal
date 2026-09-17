import { useUser } from "../../context/UserContext";

export function Dashboard() {
  const { user, loading } = useUser();

  if (loading) return <div>Loading...</div>;
  if (!user) return <div>Could not load your profile.</div>;

  return (
    <div>
      <h1>Dashboard</h1>
      <p>Welcome, {user.user_first_name || user.user_email}!</p>
      <p>Locale: {user.user_locale || "not set"}</p>
    </div>
  );
}
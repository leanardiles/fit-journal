import { createContext, useContext, useEffect, useState } from "react";
import { apiGet } from "../api/client";

// The context object , holds the shared user data.
const UserContext = createContext(null);

/**
 * Provider , fetches the logged-in user's profile ONCE and shares it with
 * every component inside. Wrap the app (or the authenticated area) in this.
 */
export function UserProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    if (!userId) {
      setLoading(false);
      return;
    }
    apiGet(`/profile/${userId}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setUser(data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  return (
    <UserContext.Provider value={{ user, loading, setUser }}>
      {children}
    </UserContext.Provider>
  );
}

/** Hook , any component calls useUser() to read the shared user. */
export function useUser() {
  return useContext(UserContext);
}
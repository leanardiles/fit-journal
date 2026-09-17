// Central API base URL , comes from Vite env (.env.development / .env.production).
export const API_URL = import.meta.env.VITE_API_URL;

// Optional: a thin helper for JSON POST requests, so components don't repeat fetch boilerplate.
export async function apiPost(path, body) {
  const res = await fetch(`${API_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return res;
}
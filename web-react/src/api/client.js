import { getToken } from "./auth";

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

// Authenticated GET , sends the JWT so the backend returns the user's data.
export async function apiGet(path) {
  const res = await fetch(`${API_URL}${path}`, {
    headers: {
      "Authorization": `Bearer ${getToken()}`,
    },
  });
  return res;
}

// Authenticated PUT , update a resource (sends the JWT + JSON body).
export async function apiPut(path, body) {
  const res = await fetch(`${API_URL}${path}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${getToken()}`,
    },
    body: JSON.stringify(body),
  });
  return res;
}

export async function apiDelete(path, body) {
  const res = await fetch(`${API_URL}${path}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${getToken()}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  return res;
}
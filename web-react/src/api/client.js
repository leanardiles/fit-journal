import { getToken } from "./auth";
import i18n from "../i18n/config";

// Central API base URL , comes from Vite env (.env.development / .env.production).
export const API_URL = import.meta.env.VITE_API_URL;

// Which client this is, sent on every request as X-FitJournal-Client. The
// backend stores it on registration and on every logged workout, so the admin
// dashboard can tell web users from Android ones. It has to be on every verb
// rather than just register and log, because the write sites move.
const CLIENT = "web";

export async function apiPost(path, body) {
  const token = getToken();
  const res = await fetch(`${API_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Accept-Language": i18n.language || "en",
      "X-FitJournal-Client": CLIENT,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });
  return res;
}

// Authenticated GET , sends the JWT so the backend returns the user's data.
export async function apiGet(path) {
  const res = await fetch(`${API_URL}${path}`, {
    headers: {
      "Accept-Language": i18n.language || "en",
      "X-FitJournal-Client": CLIENT,
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
      "Accept-Language": i18n.language || "en",
      "X-FitJournal-Client": CLIENT,
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
      "Accept-Language": i18n.language || "en",
      "X-FitJournal-Client": CLIENT,
      "Authorization": `Bearer ${getToken()}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  return res;
}
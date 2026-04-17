const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const TOKEN_KEY = "task_manager_token";

export function getApiBaseUrl() {
  if (!API_BASE_URL) {
    throw new Error("VITE_API_BASE_URL is required.");
  }

  return API_BASE_URL.replace(/\/$/, "");
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function storeToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login(payload) {
  return request("/auth/login", { method: "POST", body: payload });
}

export async function register(payload) {
  return request("/auth/register", { method: "POST", body: payload });
}

export async function logout(token) {
  return request("/auth/logout", { method: "POST", token });
}

export async function fetchCurrentUser(token) {
  return request("/auth/me", { token });
}

export async function fetchTasks(token, status) {
  const query = status === "ALL" ? "" : `?status=${encodeURIComponent(status)}`;
  return request(`/tasks${query}`, { token });
}

export async function createTask(token, payload) {
  return request("/tasks", { method: "POST", token, body: payload });
}

export async function updateTask(token, id, payload) {
  return request(`/tasks/${id}`, { method: "PUT", token, body: payload });
}

export async function deleteTask(token, id) {
  return request(`/tasks/${id}`, { method: "DELETE", token });
}

export async function fetchBackendHealth() {
  return request("/health/dependencies");
}

async function request(path, options = {}) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    method: options.method ?? "GET",
    headers: {
      Accept: "application/json",
      ...jsonHeader(options.body),
      ...authHeader(options.token)
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function jsonHeader(body) {
  return body ? { "Content-Type": "application/json" } : {};
}

function authHeader(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function readError(response) {
  try {
    const data = await response.json();
    return data.message ?? `请求失败：HTTP ${response.status}`;
  } catch (error) {
    return `请求失败：HTTP ${response.status}`;
  }
}

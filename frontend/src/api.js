const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export function getApiBaseUrl() {
  if (!API_BASE_URL) {
    throw new Error("VITE_API_BASE_URL is required.");
  }

  return API_BASE_URL.replace(/\/$/, "");
}

export async function fetchBackendHealth() {
  const response = await fetch(`${getApiBaseUrl()}/health/dependencies`, {
    headers: {
      Accept: "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(`Health check failed with HTTP ${response.status}.`);
  }

  return response.json();
}

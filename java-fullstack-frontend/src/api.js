const defaultProductionApiUrl = "https://watermanagentadvace-production.up.railway.app";
const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD ? defaultProductionApiUrl : "")
).replace(/\/+$/, "");
export const API_UNAVAILABLE_CODE = "API_UNAVAILABLE";

export function isApiUnavailable(error) {
  return error?.code === API_UNAVAILABLE_CODE;
}

export async function api(path, options = {}) {
  if (import.meta.env.PROD && !apiBaseUrl) {
    const error = new Error("The account and shopping service is not connected right now. Please try again later.");
    error.code = API_UNAVAILABLE_CODE;
    throw error;
  }

  const url = /^https?:\/\//i.test(path) ? path : `${apiBaseUrl}${path}`;
  let response;
  try {
    response = await fetch(url, {
      credentials: "include",
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
      body: options.body && typeof options.body !== "string"
        ? JSON.stringify(options.body)
        : options.body,
    });
  } catch {
    const error = new Error("The account and shopping service could not be reached. Please try again later.");
    error.code = API_UNAVAILABLE_CODE;
    throw error;
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.message || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return data;
}

export const money = (amount) => `₹${Number(amount || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
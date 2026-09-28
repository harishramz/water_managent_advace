const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/+$/, "");

export async function api(path, options = {}) {
  if (import.meta.env.PROD && !apiBaseUrl) {
    throw new Error("The API server is not configured. Set VITE_API_BASE_URL in the Vercel project settings and redeploy.");
  }

  const url = /^https?:\/\//i.test(path) ? path : `${apiBaseUrl}${path}`;
  const response = await fetch(url, {
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
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.message || `Request failed (${response.status})`);
  }
  return data;
}

export const money = (amount) => `₹${Number(amount || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
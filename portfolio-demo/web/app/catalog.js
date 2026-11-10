export async function readCatalog(path, query = new URLSearchParams()) {
  const url = new URL("/api/" + path, process.env.CATALOG_API_ORIGIN ?? "http://127.0.0.1:8080");
  url.search = query.toString();
  const response = await fetch(url, { cache: "no-store", redirect: "error", signal: AbortSignal.timeout(5000) });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error("Catalog request failed");
  return response.json();
}

export function first(value) {
  return Array.isArray(value) ? value[0] : value;
}

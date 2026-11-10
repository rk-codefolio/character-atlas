const reads = /^(characters(?:\/[a-z0-9][a-z0-9-]{0,39}(?:\/(?:effects|progress))?)?|terms|search|search-contents|review-options|posts|admin\/(?:session|posts|logs|reports|discord\/(?:start|callback)))$/;
const postId = /^posts\/[a-f0-9-]{36}$/;
const reportSubmit = /^posts\/[a-f0-9-]{36}\/reports$/;
const reportResolve = /^admin\/reports\/[a-f0-9-]{36}$/;
const setting = /^admin\/search-contents\/[a-z0-9][a-z0-9-]{0,39}$/;
const sessionCookie = /(?:^|;\s*)(atlas_admin=[A-Za-z0-9._-]+)/;

const maxWriteBytes = 128 * 1024;

async function readWriteBody(request) {
  if (Number(request.headers.get("content-length")) > maxWriteBytes) return null;
  if (!request.body) return "";
  const reader = request.body.getReader();
  const decoder = new TextDecoder();
  let text = "";
  let bytes = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) return text + decoder.decode();
      bytes += value.byteLength;
      if (bytes > maxWriteBytes) {
        await reader.cancel().catch(() => {});
        return null;
      }
      text += decoder.decode(value, { stream: true });
    }
  } finally {
    reader.releaseLock();
  }
}

async function proxy(request, { params }) {
  const { path } = await params;
  const route = path.join("/");
  const { method } = request;
  const allowed = method === "GET" ? reads.test(route) :
    method === "POST" ? (["posts", "admin/logout"].includes(route) || reportSubmit.test(route)) :
      postId.test(route) || (method === "PUT" && (setting.test(route) || reportResolve.test(route)));
  if (!allowed) return new Response(null, { status: 404 });

  const headers = new Headers();
  const bodyRequired = (method === "POST" && (route === "posts" || reportSubmit.test(route))) || (method === "PUT" && !reportResolve.test(route));
  if (method !== "GET") {
    const origin = request.headers.get("origin");
    const host = request.headers.get("host");
    if (origin !== "http://" + host && origin !== "https://" + host) return new Response(null, { status: 403 });
    headers.set("Origin", origin);
    if (bodyRequired && !request.headers.get("content-type")?.startsWith("application/json")) {
      return new Response(null, { status: 415 });
    }
    if (bodyRequired) headers.set("Content-Type", "application/json");
  }
  const cookie = request.headers.get("cookie")?.match(sessionCookie)?.[1];
  if (cookie) headers.set("Cookie", cookie);
  const target = new URL("/api/" + route, process.env.CATALOG_API_ORIGIN ?? "http://127.0.0.1:8080");
  target.search = new URL(request.url).search;
  try {
    const body = bodyRequired ? await readWriteBody(request) : undefined;
    if (body === null) return new Response(null, { status: 413 });
    const upstream = await fetch(target, {
      method, cache: "no-store", redirect: "manual", headers,
      body,
    });
    const responseHeaders = new Headers({ "Cache-Control": "no-store", "Referrer-Policy": "no-referrer" });
    for (const name of ["Content-Type", "Location"]) {
      const value = upstream.headers.get(name);
      if (value) responseHeaders.set(name, value);
    }
    for (const value of upstream.headers.getSetCookie()) {
      if (value.startsWith("atlas_admin=")) responseHeaders.append("Set-Cookie", value);
    }
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch {
    return Response.json({ message: "APIに接続できません" }, { status: 503 });
  }
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE };

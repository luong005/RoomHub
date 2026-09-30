export function readJwtClaims(token) {
  try {
    const payload = String(token).split(".")[1];
    if (!payload || typeof globalThis.atob !== "function") return {};
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    return JSON.parse(globalThis.atob(padded));
  } catch {
    return {};
  }
}

export function isJwtExpired(token, leewaySeconds = 0) {
  const expiration = Number(readJwtClaims(token).exp);
  if (!Number.isFinite(expiration)) return false;
  return expiration <= Math.floor(Date.now() / 1000) + Math.max(0, leewaySeconds);
}

const LOGIN_LIMIT_CODES = new Set(["LOGIN_RATE_LIMITED", "LOGIN_IP_RATE_LIMITED"]);

function readSeconds(value, now) {
  if (typeof value !== "number" && typeof value !== "string") return null;
  if (typeof value === "string" && !/^\d+(?:\.\d+)?$/.test(value.trim())) return null;
  const seconds = Math.ceil(Number(value));
  return Number.isSafeInteger(seconds) && seconds >= 0 && Number.isSafeInteger(now + seconds * 1000)
    ? seconds : null;
}

function readRetryAfter(headers, now) {
  const value = typeof headers?.get === "function"
    ? headers.get("retry-after") : headers?.["retry-after"] ?? headers?.["Retry-After"];
  const seconds = readSeconds(value, now);
  if (seconds !== null) return seconds;
  if (typeof value !== "string" || !/,|\bGMT\b/i.test(value)) return null;
  const retryAt = Date.parse(value);
  return Number.isFinite(retryAt) ? Math.max(0, Math.ceil((retryAt - now) / 1000)) : null;
}

export function readLoginRateLimit(response, requestUrl, now = Date.now()) {
  if (response?.status !== 429) return null;
  const body = response.data;
  const seconds = readSeconds(body?.details?.retryAfterSeconds, now) ?? readRetryAfter(response.headers, now);
  const scope = body?.details?.scope;
  const rateLimitScope = scope === "IP" || scope === "IP_PHONE" ? scope
    : body?.code === "LOGIN_IP_RATE_LIMITED" ? "IP" : "IP_PHONE";
  const path = String(requestUrl || "").split(/[?#]/)[0].replace(/\/+$/, "");
  return {
    isLoginRateLimit: LOGIN_LIMIT_CODES.has(body?.code) || /(?:^|\/)login$/.test(path),
    retryAfterSeconds: seconds,
    retryAt: seconds === null ? null : now + seconds * 1000,
    rateLimitScope,
  };
}

function phoneKey(phone) {
  return String(phone || "").trim().toLowerCase();
}

export function createLoginCooldown(error, phone, now = Date.now()) {
  if (error?.status !== 429 || !error.isLoginRateLimit
      || !Number.isFinite(error.retryAt) || error.retryAt <= now) return null;
  return { scope: error.rateLimitScope === "IP" ? "IP" : "IP_PHONE", phone: phoneKey(phone), retryAt: error.retryAt };
}

export function rememberLoginCooldown(previous, next, now = Date.now()) {
  return [...previous.filter((limit) => limit.retryAt > now
    && !(limit.scope === next.scope && (next.scope === "IP" || limit.phone === next.phone))), next];
}

export function getActiveLoginCooldown(limits, phone, now = Date.now()) {
  const active = limits.filter((limit) => limit.retryAt > now
    && (limit.scope === "IP" || limit.phone === phoneKey(phone)))
    .reduce((latest, limit) => !latest || limit.retryAt > latest.retryAt ? limit : latest, null);
  return active ? { ...active, remainingSeconds: Math.ceil((active.retryAt - now) / 1000) } : null;
}

export function loginWaitMessage(seconds, registrationCompleted = false) {
  const remaining = Math.max(0, Math.ceil(seconds));
  const minutes = String(Math.floor(remaining / 60)).padStart(2, "0");
  const remainder = String(remaining % 60).padStart(2, "0");
  const prefix = registrationCompleted ? "Tài khoản đã được tạo. Vui lòng thử đăng nhập lại"
    : "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại";
  return `${prefix} sau ${minutes}:${remainder}.`;
}

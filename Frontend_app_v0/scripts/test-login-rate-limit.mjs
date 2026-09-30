import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import test from "node:test";

const root = process.cwd();
const dependencyRoot = process.env.ROOMMATE_DEPENDENCY_ROOT || root;
const requireDependency = createRequire(path.join(dependencyRoot, "package.json"));
const { transformSync } = requireDependency("@babel/core");
const commonJs = requireDependency.resolve("@babel/plugin-transform-modules-commonjs");
const axios = requireDependency("axios");
const START = Date.parse("2026-09-09T12:00:00Z");

function sourceLoader(mocks = {}, clock = { now: START }, extras = {}) {
  const cache = new Map();
  const context = vm.createContext({
    Date: class extends Date { static now() { return clock.now; } },
    process: { env: { EXPO_PUBLIC_API_URL: "http://test.invalid/api" } },
    __DEV__: true,
    setInterval, clearInterval,
    ...extras,
  });
  function load(relative) {
    const file = path.resolve(root, relative);
    if (cache.has(file)) return cache.get(file).exports;
    const module = { exports: {} };
    cache.set(file, module);
    const code = transformSync(fs.readFileSync(file, "utf8"), {
      filename: file, babelrc: false, configFile: false, plugins: [commonJs],
    }).code;
    const localRequire = (request) => {
      if (Object.hasOwn(mocks, request)) return mocks[request];
      if (!request.startsWith(".")) return requireDependency(request);
      let target = path.resolve(path.dirname(file), request);
      if (!fs.existsSync(target)) target += ".js";
      return load(target);
    };
    vm.runInContext(`(function(require, module, exports) {${code}\n})`, context, { filename: file })
      (localRequire, module, module.exports);
    return module.exports;
  }
  return load;
}

function apiHarness(respond, initial = {}) {
  const clock = { now: START };
  const calls = [];
  const state = { accessToken: "existing-access", refreshToken: "existing-refresh", clears: 0, ended: 0, ...initial };
  const storage = {
    getAccessToken: async () => state.accessToken,
    getRefreshToken: async () => state.refreshToken,
    setAccessToken: async (token) => { state.accessToken = token; },
    setRefreshToken: async (token) => { state.refreshToken = token; },
    setSessionTokens: async (tokens) => { Object.assign(state, tokens); },
    clearSessionTokens: async () => { state.accessToken = null; state.refreshToken = null; state.clears++; },
  };
  const load = sourceLoader({
    "../storage/secureTokenStore": storage,
    "../utils/jwt": { isJwtExpired: (token) => token === "expired", readJwtClaims: () => ({}) },
    "../utils/profileMapper": { mapProfile: (data) => data },
  }, clock);
  const apiModule = load("src/services/api.js");
  apiModule.setUnauthorizedHandler(() => { state.ended++; });
  apiModule.api.defaults.adapter = async (config) => {
    calls.push({ url: config.url, data: config.data && JSON.parse(config.data), authorization: config.headers.Authorization });
    const spec = await respond(config, calls.length);
    const response = { status: spec.status ?? 200, data: spec.data, headers: spec.headers || {}, config };
    if (response.status >= 400) {
      throw new axios.AxiosError("Test HTTP error", "ERR_BAD_RESPONSE", config, null, response);
    }
    return response;
  };
  return { ...apiModule, auth: load("src/services/auth.service.js"), limits: load("src/utils/loginRateLimit.js"), calls, state, clock };
}

const limited = (scope = "IP_PHONE", seconds = 300) => ({
  status: 429,
  data: { code: scope === "IP" ? "LOGIN_IP_RATE_LIMITED" : "LOGIN_RATE_LIMITED",
    message: "Vui lòng thử lại sau.", details: { retryAfterSeconds: seconds, scope } },
  headers: { "retry-after": String(seconds) },
});
const credentials = { phoneNumber: "0900000000", password: "test-only-password" };

test("login 429 preserves wait metadata without displaying raw details or touching the session", async () => {
  const h = apiHarness(() => limited());
  await assert.rejects(h.auth.login(credentials), (error) => {
    assert.equal(error.status, 429);
    assert.equal(error.message, "Vui lòng thử lại sau.");
    assert.equal(error.isLoginRateLimit, true);
    assert.equal(error.retryAfterSeconds, 300);
    assert.equal(error.retryAt, START + 300_000);
    assert.equal(error.rateLimitScope, "IP_PHONE");
    return true;
  });
  assert.deepEqual(h.calls.map((call) => call.url), ["/login"]);
  assert.equal(h.state.clears, 0);
  assert.equal(h.state.ended, 0);
  assert.equal(h.state.accessToken, "existing-access");
});

test("429 on a protected API neither refreshes tokens nor marks login as blocked", async () => {
  const h = apiHarness(() => ({ status: 429, data: { message: "Try later", details: { retryAfterSeconds: 30 } } }));
  await assert.rejects(h.api.get("/posts"), (error) => error.status === 429 && !error.isLoginRateLimit);
  assert.deepEqual(h.calls.map((call) => call.url), ["/posts"]);
  assert.equal(h.state.clears, 0);
  assert.equal(h.state.ended, 0);
});

test("wrong credentials remain 401 and never start token refresh", async () => {
  const h = apiHarness(() => ({ status: 401, data: { code: "LOGIN_FAILED", message: "Wrong phone number or password" } }));
  await assert.rejects(h.auth.login(credentials), (error) => error.status === 401 && error.code === "LOGIN_FAILED");
  assert.deepEqual(h.calls.map((call) => call.url), ["/login"]);
  assert.equal(h.state.clears, 0);
});

test("existing 401 refresh and one retry remain intact", async () => {
  const h = apiHarness((config, count) => config.url === "/refresh"
    ? { data: { accessToken: "refreshed-access" } }
    : count === 1 ? { status: 401, data: { message: "Expired" } } : { data: { ok: true } });
  assert.equal((await h.api.get("/posts")).data.ok, true);
  assert.deepEqual(h.calls.map((call) => call.url), ["/posts", "/refresh", "/posts"]);
  assert.equal(h.calls[2].authorization, "Bearer refreshed-access");
  assert.equal(h.state.clears, 0);
});

test("successful login still saves tokens and loads the profile", async () => {
  const profile = { id: 7, fullName: "Test profile" };
  const h = apiHarness((config) => ({ data: config.url === "/login"
    ? { accessToken: "new-access", refreshToken: "new-refresh" } : profile }));
  assert.equal(await h.auth.login(credentials), profile);
  assert.deepEqual(h.calls.map((call) => call.url), ["/login", "/profile"]);
  assert.equal(h.calls[0].data.phone_number, credentials.phoneNumber);
  assert.equal(h.state.accessToken, "new-access");
  assert.equal(h.state.refreshToken, "new-refresh");
});

test("validation and server errors retain their previous messages", async () => {
  const validation = apiHarness(() => ({ status: 400, data: { details: { phone: "Required", password: "Too short" } } }));
  await assert.rejects(validation.auth.login(credentials), (error) => error.message === "Required\nToo short");
  const server = apiHarness(() => ({ status: 500, data: { message: "Internal test details" } }));
  await assert.rejects(server.auth.login(credentials), (error) => error.message === "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.");
});

test("normal registration retains register, login and profile order", async () => {
  const h = apiHarness((config) => ({ data: config.url === "/login"
    ? { accessToken: "new-access", refreshToken: "new-refresh" } : { id: 8 } }));
  await h.auth.register(credentials);
  assert.deepEqual(h.calls.map((call) => call.url), ["/register", "/login", "/profile"]);
  assert.equal(h.auth.isRegistrationAwaitingLogin(credentials.phoneNumber), false);
});

test("registration followed by login 429 retries only login, without creating the account twice", async () => {
  let allowLogin = false;
  const h = apiHarness((config) => config.url === "/login"
    ? allowLogin ? { data: { accessToken: "new-access", refreshToken: "new-refresh" } } : limited("IP")
    : { data: { id: 8 } });
  await assert.rejects(h.auth.register(credentials), (error) => error.registrationCompleted && error.status === 429);
  assert.equal(h.auth.isRegistrationAwaitingLogin(credentials.phoneNumber), true);
  assert.equal(h.auth.isRegistrationAwaitingLogin("0911111111"), false);
  allowLogin = true;
  await h.auth.register(credentials);
  assert.deepEqual(h.calls.map((call) => call.url), ["/register", "/login", "/login", "/profile"]);
  assert.equal(h.auth.isRegistrationAwaitingLogin(credentials.phoneNumber), false);
});

test("an unsuccessful registration never marks the account as created", async () => {
  const h = apiHarness(() => ({ status: 403, data: { code: "ACCOUNT_EXISTS", message: "Already exists" } }));
  await assert.rejects(h.auth.register(credentials), (error) => error.status === 403 && !error.registrationCompleted);
  assert.equal(h.auth.isRegistrationAwaitingLogin(credentials.phoneNumber), false);
  assert.deepEqual(h.calls.map((call) => call.url), ["/register"]);
});

test("body wait time is preferred, fractional seconds round up and scope is recognized", () => {
  const limits = sourceLoader()("src/utils/loginRateLimit.js");
  const data = limits.readLoginRateLimit(limited("IP", 1.2), "/login", START);
  assert.equal(data.retryAfterSeconds, 2);
  assert.equal(data.rateLimitScope, "IP");
  assert.equal(data.retryAt, START + 2000);
});

test("Retry-After supports header objects and HTTP dates", () => {
  const limits = sourceLoader()("src/utils/loginRateLimit.js");
  const numeric = limits.readLoginRateLimit({ status: 429, headers: { get: () => "90" } }, "/login", START);
  assert.equal(numeric.retryAfterSeconds, 90);
  const dated = limits.readLoginRateLimit({ status: 429,
    headers: { "Retry-After": new Date(START + 120_000).toUTCString() } }, "/login", START);
  assert.equal(dated.retryAfterSeconds, 120);
});

test("invalid timing metadata falls back safely and never creates an indefinite block", () => {
  const limits = sourceLoader()("src/utils/loginRateLimit.js");
  for (const retryAfterSeconds of [-1, "", "NaN", Infinity, {}, true, 1e100]) {
    const parsed = limits.readLoginRateLimit({ status: 429, data: { details: { retryAfterSeconds } },
      headers: { "retry-after": "42" } }, "/login", START);
    assert.equal(parsed.retryAfterSeconds, 42);
  }
  const missing = limits.readLoginRateLimit({ status: 429, headers: { "retry-after": "-1" } }, "/login", START);
  assert.equal(missing.retryAt, null);
  assert.equal(limits.createLoginCooldown({ status: 429, ...missing }, "phone", START), null);
  assert.equal(limits.readLoginRateLimit({ status: 401 }, "/login", START), null);
});

test("changing phone preserves the original pair wait while an IP wait applies to every phone", () => {
  const limits = sourceLoader()("src/utils/loginRateLimit.js");
  const pair = { scope: "IP_PHONE", phone: "0900000000", retryAt: START + 300_000 };
  let saved = limits.rememberLoginCooldown([], pair, START);
  assert.equal(limits.getActiveLoginCooldown(saved, "0911111111", START), null);
  assert.equal(limits.getActiveLoginCooldown(saved, " 0900000000 ", START).remainingSeconds, 300);
  saved = limits.rememberLoginCooldown(saved, { scope: "IP", phone: "", retryAt: START + 60_000 }, START);
  assert.equal(limits.getActiveLoginCooldown(saved, "0911111111", START).remainingSeconds, 60);
  assert.equal(limits.getActiveLoginCooldown(saved, "0900000000", START).remainingSeconds, 300);
  assert.equal(limits.getActiveLoginCooldown(saved, "0911111111", START + 60_000), null);
});

test("countdown uses a deadline after time away, expires exactly, and does not create another request", () => {
  const limits = sourceLoader()("src/utils/loginRateLimit.js");
  const saved = [{ scope: "IP_PHONE", phone: "phone", retryAt: START + 300_000 }];
  assert.equal(limits.getActiveLoginCooldown(saved, "phone", START + 120_000).remainingSeconds, 180);
  assert.equal(limits.getActiveLoginCooldown(saved, "phone", START + 299_001).remainingSeconds, 1);
  assert.equal(limits.getActiveLoginCooldown(saved, "phone", START + 300_000), null);
  assert.match(limits.loginWaitMessage(180), /03:00/);
  assert.match(limits.loginWaitMessage(180, true), /Tài khoản đã được tạo/);
});

test("the hook refreshes on foreground, retains pair state across input changes, and cleans up subscriptions", () => {
  const clock = { now: START };
  const state = [];
  let cursor = 0, effect, previousDeps, cleanup, timer, listener, removed = 0, cleared = 0;
  const react = {
    useState(initial) {
      const index = cursor++;
      if (!(index in state)) state[index] = typeof initial === "function" ? initial() : initial;
      return [state[index], (next) => { state[index] = typeof next === "function" ? next(state[index]) : next; }];
    },
    useCallback: (callback) => callback,
    useEffect(callback, deps) {
      if (!previousDeps || deps.some((value, index) => !Object.is(value, previousDeps[index]))) {
        effect = callback; previousDeps = deps;
      }
    },
  };
  const load = sourceLoader({ react, "react-native": { AppState: {
    addEventListener(event, callback) {
      assert.equal(event, "change"); listener = callback;
      return { remove() { listener = null; removed++; } };
    },
  } } }, clock, {
    setInterval(callback) { timer = callback; return 1; },
    clearInterval() { timer = null; cleared++; },
  });
  const { useLoginRateLimit } = load("src/hooks/useLoginRateLimit.js");
  function render(phone) {
    cursor = 0;
    const result = useLoginRateLimit(phone);
    if (effect) { cleanup?.(); const pending = effect; effect = null; cleanup = pending(); }
    return result;
  }
  let result = render("phone");
  assert.equal(result.capture({ status: 429, isLoginRateLimit: true,
    rateLimitScope: "IP_PHONE", retryAt: START + 300_000 }, "phone"), true);
  result = render("phone");
  assert.equal(result.cooldown.remainingSeconds, 300);
  assert.equal(typeof timer, "function");
  assert.equal(render("other-phone").cooldown, null);
  clock.now += 120_000;
  listener("active");
  assert.equal(render("phone").cooldown.remainingSeconds, 180);
  clock.now += 180_000;
  timer();
  assert.equal(render("phone").cooldown, null);
  assert.equal(listener, null);
  assert.equal(timer, null);
  assert.equal(removed, 1);
  assert.equal(cleared, 1);
});

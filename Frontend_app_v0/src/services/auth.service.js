import { api, refreshAccessToken } from "./api";
import {
  clearSessionTokens,
  getAccessToken,
  getRefreshToken,
  setSessionTokens,
} from "../storage/secureTokenStore";
import { readJwtClaims } from "../utils/jwt";
import { mapProfile } from "../utils/profileMapper";

// Only remember successful registrations whose automatic login was rate limited.
// No passwords or tokens are retained here; this state lasts for the current app process.
const registrationsAwaitingLogin = new Set();
const registrationKey = (phoneNumber) => String(phoneNumber || "").trim().toLowerCase();

export function isRegistrationAwaitingLogin(phoneNumber) {
  return registrationsAwaitingLogin.has(registrationKey(phoneNumber));
}

export async function getProfile() {
  const response = await api.get("/profile");
  const token = await getAccessToken();
  return mapProfile(response.data, readJwtClaims(token));
}

export async function login({ phoneNumber, password }) {
  const response = await api.post(
    "/login",
    { phone_number: phoneNumber, password },
    { skipAuth: true, skipUnauthorized: true },
  );
  await setSessionTokens({
    accessToken: response.data?.accessToken,
    refreshToken: response.data?.refreshToken,
  });
  try {
    const profile = await getProfile();
    registrationsAwaitingLogin.delete(registrationKey(phoneNumber));
    return profile;
  } catch (error) {
    await clearSessionTokens();
    throw error;
  }
}

export async function register(credentials) {
  if (!isRegistrationAwaitingLogin(credentials.phoneNumber)) {
    await api.post(
      "/register",
      { phone_number: credentials.phoneNumber, password: credentials.password },
      { skipAuth: true, skipUnauthorized: true },
    );
  }
  try {
    return await login(credentials);
  } catch (error) {
    if (error.status === 429 && error.isLoginRateLimit) {
      registrationsAwaitingLogin.add(registrationKey(credentials.phoneNumber));
      error.registrationCompleted = true;
    }
    throw error;
  }
}

export async function restoreSession() {
  const [accessToken, refreshToken] = await Promise.all([getAccessToken(), getRefreshToken()]);
  if (!accessToken && !refreshToken) return null;
  try {
    if (!accessToken && refreshToken) await refreshAccessToken();
    return await getProfile();
  } catch {
    await clearSessionTokens();
    return null;
  }
}

export async function logout() {
  try {
    const refreshToken = await getRefreshToken();
    await api.post(
      "/logout",
      refreshToken ? { refreshToken } : null,
      { skipAuth: true, skipUnauthorized: true },
    );
  } catch {
    // Luôn xóa phiên cục bộ kể cả khi backend tạm thời không thể thu hồi token.
  } finally {
    await clearSessionTokens();
  }
}

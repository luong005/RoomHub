import * as SecureStore from "expo-secure-store";

const LEGACY_ACCESS_TOKEN_KEY = "roommate.access_token";
const REFRESH_TOKEN_KEY = "roommate.refresh_token";
const SECURE_STORE_OPTIONS = {
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
};

let memoryAccessToken = null;
let memoryRefreshToken = null;
let legacyAccessTokenPurged = false;

export async function getAccessToken() {
  await purgeLegacyAccessToken();
  return memoryAccessToken;
}

export async function setAccessToken(token) {
  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Access token không hợp lệ.");
  }
  memoryAccessToken = token.trim();
}

export async function getRefreshToken() {
  if (memoryRefreshToken) return memoryRefreshToken;
  memoryRefreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  return memoryRefreshToken;
}

export async function setRefreshToken(token) {
  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Refresh token không hợp lệ.");
  }
  memoryRefreshToken = token.trim();
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, memoryRefreshToken, SECURE_STORE_OPTIONS);
}

export async function setSessionTokens({ accessToken, refreshToken }) {
  if (typeof accessToken !== "string" || !accessToken.trim()) {
    throw new Error("Máy chủ không trả về access token hợp lệ.");
  }
  if (typeof refreshToken !== "string" || !refreshToken.trim()) {
    throw new Error("Máy chủ không trả về refresh token hợp lệ.");
  }
  try {
    await Promise.all([purgeLegacyAccessToken(), SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken.trim(), SECURE_STORE_OPTIONS)]);
    memoryAccessToken = accessToken.trim();
    memoryRefreshToken = refreshToken.trim();
  } catch (error) {
    await clearSessionTokens();
    throw error;
  }
}

export async function clearAccessToken() {
  memoryAccessToken = null;
  await purgeLegacyAccessToken();
}

export async function clearSessionTokens() {
  memoryAccessToken = null;
  memoryRefreshToken = null;
  await Promise.all([
    purgeLegacyAccessToken(),
    SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
  ]);
}

export function clearMemoryToken() {
  memoryAccessToken = null;
  memoryRefreshToken = null;
}

async function purgeLegacyAccessToken() {
  if (legacyAccessTokenPurged) return;
  await SecureStore.deleteItemAsync(LEGACY_ACCESS_TOKEN_KEY);
  legacyAccessTokenPurged = true;
}

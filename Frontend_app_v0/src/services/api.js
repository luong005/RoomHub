import axios from "axios";
import {
  clearSessionTokens,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from "../storage/secureTokenStore";
import { isJwtExpired } from "../utils/jwt";
import { readLoginRateLimit } from "../utils/loginRateLimit";

const REQUEST_TIMEOUT_MS = 15000;
const TOKEN_EXPIRY_LEEWAY_SECONDS = 30;
let unauthorizedHandler = null;
let refreshPromise = null;

export function getApiBaseUrl() {
  const value = process.env.EXPO_PUBLIC_API_URL?.trim().replace(/\/+$/, "");
  if (!value) {
    throw new Error("Thiếu EXPO_PUBLIC_API_URL. Hãy tạo file .env theo .env.example rồi khởi động lại Expo.");
  }
  if (!/^https?:\/\//i.test(value)) {
    throw new Error("EXPO_PUBLIC_API_URL phải bắt đầu bằng http:// hoặc https://.");
  }
  if (!__DEV__ && !/^https:\/\//i.test(value)) {
    throw new Error("Bản production chỉ cho phép API qua HTTPS.");
  }
  return value;
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === "function" ? handler : null;
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = null;
  };
}

export const api = axios.create({
  timeout: REQUEST_TIMEOUT_MS,
  headers: { Accept: "application/json" },
});

export async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = await getRefreshToken();
      if (!refreshToken) throw sessionExpiredError();

      const response = await api.post(
        "/refresh",
        { refreshToken },
        { skipAuth: true, skipUnauthorized: true },
      );
      const nextAccessToken = response.data?.accessToken;
      if (typeof nextAccessToken !== "string" || !nextAccessToken.trim()) {
        throw sessionExpiredError();
      }

      await setAccessToken(nextAccessToken);
      if (typeof response.data?.refreshToken === "string" && response.data.refreshToken.trim()) {
        await setRefreshToken(response.data.refreshToken);
      }
      return nextAccessToken.trim();
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

api.interceptors.request.use(async (config) => {
  config.baseURL = getApiBaseUrl();
  if (!config.skipAuth) {
    let token = await getAccessToken();
    const hasRefreshToken = token ? false : Boolean(await getRefreshToken());

    if (hasRefreshToken || isJwtExpired(token, TOKEN_EXPIRY_LEEWAY_SECONDS)) {
      try {
        token = await refreshAccessToken();
      } catch (error) {
        await clearSessionTokens();
        unauthorizedHandler?.();
        return Promise.reject(toApiError(error));
      }
    }

    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;
    const status = error.response?.status;
    const sentAccessToken = getBearerToken(config?.headers?.Authorization);
    const requiresRefresh = status === 401
      || (status === 403 && isJwtExpired(sentAccessToken));

    if (requiresRefresh && !config?.skipUnauthorized) {
      if (!config._retriedAfterRefresh) {
        config._retriedAfterRefresh = true;
        try {
          const accessToken = await refreshAccessToken();
          config.headers = config.headers || {};
          config.headers.Authorization = `Bearer ${accessToken}`;
          return api(config);
        } catch (refreshError) {
          await clearSessionTokens();
          unauthorizedHandler?.();
          return Promise.reject(toApiError(refreshError));
        }
      }
      await clearSessionTokens();
      unauthorizedHandler?.();
    }
    return Promise.reject(toApiError(error));
  },
);

function getBearerToken(authorization) {
  const value = String(authorization || "");
  return value.startsWith("Bearer ") ? value.slice(7).trim() : "";
}

function sessionExpiredError() {
  const error = new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  error.name = "ApiError";
  error.isApiError = true;
  error.status = 401;
  return error;
}

function toApiError(error) {
  if (error?.isApiError) return error;
  const status = error.response?.status;
  const body = error.response?.data;
  let message;

  if (!error.response && /EXPO_PUBLIC_API_URL|production chỉ cho phép API/.test(error.message || "")) {
    message = error.message;
  } else if (!error.response) {
    message = error.code === "ECONNABORTED"
      ? "Máy chủ phản hồi quá lâu. Vui lòng thử lại."
      : "Không thể kết nối máy chủ. Kiểm tra địa chỉ API, Wi-Fi và trạng thái backend.";
  } else if (status >= 500) {
    message = "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
  } else if (status === 429) {
    message = typeof body?.message === "string" && body.message.trim()
      ? body.message : "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.";
  } else if (body?.details && typeof body.details === "object") {
    message = Object.values(body.details).filter(Boolean).join("\n") || body.message;
  } else {
    message = body?.message || (typeof body === "string" ? body : null);
  }

  const apiError = new Error(message || "Yêu cầu không thành công.");
  apiError.name = "ApiError";
  apiError.isApiError = true;
  apiError.status = status;
  apiError.code = body?.code || error.code;
  if (status === 429) Object.assign(apiError, readLoginRateLimit(error.response, error.config?.url));
  return apiError;
}

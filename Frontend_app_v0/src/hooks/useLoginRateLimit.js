import { useCallback, useEffect, useState } from "react";
import { AppState } from "react-native";
import { createLoginCooldown, getActiveLoginCooldown, rememberLoginCooldown } from "../utils/loginRateLimit";

export function useLoginRateLimit(phone) {
  const [limits, setLimits] = useState([]);
  const [now, setNow] = useState(Date.now);
  const hasActiveLimits = limits.some((limit) => limit.retryAt > now);

  useEffect(() => {
    if (!hasActiveLimits) return undefined;
    const update = () => setNow(Date.now());
    const timer = setInterval(update, 1000);
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "active") update();
    });
    return () => { clearInterval(timer); subscription.remove(); };
  }, [hasActiveLimits]);

  const capture = useCallback((error, attemptedPhone) => {
    const receivedAt = Date.now();
    const next = createLoginCooldown(error, attemptedPhone, receivedAt);
    if (!next) return false;
    setNow(receivedAt);
    setLimits((previous) => rememberLoginCooldown(previous, next, receivedAt));
    return true;
  }, []);

  return { cooldown: getActiveLoginCooldown(limits, phone, now), capture };
}

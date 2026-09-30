import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import * as authService from "../services/auth.service";
import { setUnauthorizedHandler } from "../services/api";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isBooting, setIsBooting] = useState(true);

  const endSession = useCallback(() => setUser(null), []);
  const updateUser = useCallback((profile) => setUser(profile), []);

  useEffect(() => setUnauthorizedHandler(endSession), [endSession]);

  useEffect(() => {
    let active = true;
    authService.restoreSession()
      .then((profile) => active && setUser(profile))
      .finally(() => active && setIsBooting(false));
    return () => { active = false; };
  }, []);

  const value = useMemo(() => ({
    user,
    isBooting,
    isAuthenticated: Boolean(user),
    async login(credentials) {
      const profile = await authService.login(credentials);
      setUser(profile);
      return profile;
    },
    async register(credentials) {
      const profile = await authService.register(credentials);
      setUser(profile);
      return profile;
    },
    async logout() {
      await authService.logout();
      setUser(null);
    },
    updateUser,
  }), [isBooting, updateUser, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

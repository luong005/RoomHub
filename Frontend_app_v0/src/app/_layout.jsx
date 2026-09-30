import { Stack, router, useSegments } from "expo-router";
import { useEffect } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { AuthProvider } from "../context/AuthContext";
import { useAuth } from "../hooks/useAuth";
import { colors } from "../constants/theme";

const PUBLIC_ROUTES = new Set(["login"]);

function ProtectedNavigator() {
  const segments = useSegments();
  const { isAuthenticated, isBooting } = useAuth();
  const route = segments[0] || "index";
  const isPublic = PUBLIC_ROUTES.has(route);

  useEffect(() => {
    if (isBooting) return;
    if (!isAuthenticated && !isPublic) router.replace("/login");
    if (isAuthenticated && isPublic) router.replace("/");
  }, [isAuthenticated, isBooting, isPublic]);

  if (isBooting) {
    return <View style={styles.loading}><ActivityIndicator color={colors.primary} /><Text style={styles.loadingText}>Đang kiểm tra phiên đăng nhập...</Text></View>;
  }
  return <Stack screenOptions={{ headerShown: false, animation: "slide_from_right" }} />;
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      <AuthProvider><ProtectedNavigator /></AuthProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loading: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12, backgroundColor: colors.background },
  loadingText: { color: colors.muted, fontWeight: "600" },
});

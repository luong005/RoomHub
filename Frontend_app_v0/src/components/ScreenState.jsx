import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { colors } from "../constants/theme";

export function ScreenState({ loading, error, empty, onRetry }) {
  if (!loading && !error && !empty) return null;
  return (
    <View style={styles.box}>
      {loading ? <ActivityIndicator color={colors.primary} /> : null}
      <Text style={[styles.text, error && styles.error]}>
        {loading ? "Đang tải dữ liệu..." : error || empty}
      </Text>
      {!loading && error && onRetry ? (
        <Pressable onPress={onRetry} style={styles.retry}>
          <Text style={styles.retryText}>Thử lại</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  box: { padding: 28, borderRadius: 16, alignItems: "center", backgroundColor: "white", gap: 10 },
  text: { color: colors.muted, textAlign: "center", lineHeight: 20 },
  error: { color: colors.danger },
  retry: { minHeight: 42, paddingHorizontal: 18, borderRadius: 10, justifyContent: "center", backgroundColor: colors.softBlue },
  retryText: { color: colors.primary, fontWeight: "800" },
});

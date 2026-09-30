import Ionicons from "@expo/vector-icons/Ionicons";
import { useCallback, useEffect, useState } from "react";
import {
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { AppHeader } from "../components/AppHeader";
import { ScreenState } from "../components/ScreenState";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { getUsers } from "../services/admin.service";
import { isAdmin, normalizeRole } from "../utils/roles";

const PAGE_SIZE = 10;

export default function AdminScreen() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [result, setResult] = useState({
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(
    async (targetPage, refresh = false) => {
      if (!isAdmin(user)) {
        setLoading(false);
        setRefreshing(false);
        setError("Bạn không có quyền truy cập trang quản trị.");
        return;
      }
      try {
        refresh ? setRefreshing(true) : setLoading(true);
        setError("");
        setResult(await getUsers(targetPage, PAGE_SIZE));
      } catch (nextError) {
        setError(nextError.message);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [user],
  );

  useEffect(() => {
    load(page);
  }, [load, page]);

  const totalPages = Math.max(result.totalPages, 1);
  const currentPage = Math.min(result.number, totalPages - 1);
  return (
    <SafeAreaView style={styles.safe} edges={["top"]}>
      <AppHeader backTo="/" backLabel="Về bảng tin" />
      <FlatList
        data={loading || error ? [] : result.content}
        keyExtractor={(item, index) =>
          String(item.id ?? item.username ?? item.phoneNumber ?? index)
        }
        renderItem={({ item }) => <UserCard user={item} />}
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => load(page, true)}
            colors={[colors.primary]}
          />
        }
        ListHeaderComponent={
          <>
            <View style={styles.hero}>
              <View style={styles.heroIcon}>
                <Ionicons name="people" size={27} color="white" />
              </View>
              <View style={styles.grow}>
                <Text style={styles.eyebrow}>QUẢN TRỊ HỆ THỐNG</Text>
                <Text style={styles.title}>Quản lý người dùng</Text>
              
              </View>
            </View>
            <View style={styles.stat}>
              <Text style={styles.statLabel}>TỔNG NGƯỜI DÙNG</Text>
              <Text style={styles.statNumber}>{result.totalElements}</Text>
              <Text style={styles.statCaption}>tài khoản trong hệ thống</Text>
            </View>
            <View style={styles.sectionHead}>
              <Text style={styles.sectionEyebrow}>DANH SÁCH</Text>
              <Text style={styles.sectionTitle}>Người dùng</Text>
            </View>
            {loading || error ? (
              <ScreenState
                loading={loading}
                error={error}
                onRetry={() => load(page)}
              />
            ) : null}
          </>
        }
        ListEmptyComponent={
          !loading && !error ? (
            <ScreenState empty="Chưa có người dùng." />
          ) : null
        }
        ListFooterComponent={
          !loading && !error && result.content.length ? (
            <View style={styles.pagination}>
              <Pressable
                disabled={currentPage <= 0}
                onPress={() => setPage((value) => Math.max(0, value - 1))}
                style={[styles.pageButton, currentPage <= 0 && styles.disabled]}
              >
                <Ionicons
                  name="chevron-back"
                  size={16}
                  color={colors.primary}
                />
                <Text style={styles.pageButtonText}>Trước</Text>
              </Pressable>
              <Text style={styles.pageText}>
                Trang {currentPage + 1}/{totalPages}
              </Text>
              <Pressable
                disabled={currentPage >= totalPages - 1}
                onPress={() =>
                  setPage((value) => Math.min(totalPages - 1, value + 1))
                }
                style={[
                  styles.pageButton,
                  currentPage >= totalPages - 1 && styles.disabled,
                ]}
              >
                <Text style={styles.pageButtonText}>Sau</Text>
                <Ionicons
                  name="chevron-forward"
                  size={16}
                  color={colors.primary}
                />
              </Pressable>
            </View>
          ) : null
        }
      />
    </SafeAreaView>
  );
}

function UserCard({ user }) {
  const role = normalizeRole(user.role) || "USER";
  const active = user.active === true;
  return (
    <View style={styles.card}>
      <View style={styles.avatar}>
        <Ionicons name="person" size={21} color={colors.primary} />
      </View>
      <View style={styles.grow}>
        <View style={styles.userTop}>
          <Text numberOfLines={1} style={styles.username}>
            {user.username || "Chưa có username"}
          </Text>
          {/* <Text
            style={[
              styles.status,
              active ? styles.statusActive : styles.statusInactive,
            ]}
          >
            {active ? "Đang hoạt động" : "Đã khóa"}
          </Text> */}
        </View>
        <Text style={styles.phone}>
          {user.phoneNumber || "Chưa có số điện thoại"}
        </Text>
        <View style={styles.meta}>
          <Text style={styles.id}>ID #{user.id ?? "—"}</Text>
          <Text style={[styles.role, role === "ADMIN" && styles.adminRole]}>
            {role}
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, paddingBottom: 36, flexGrow: 1 },
  grow: { flex: 1, minWidth: 0 },
  hero: {
    backgroundColor: "#172B56",
    borderRadius: 19,
    padding: 17,
    flexDirection: "row",
    alignItems: "center",
    gap: 13,
    ...shadow,
  },
  heroIcon: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  eyebrow: {
    color: "#AFC8F6",
    fontSize: 10,
    fontWeight: "900",
    letterSpacing: 1.4,
  },
  title: { color: "white", fontSize: 23, fontWeight: "900", marginTop: 2 },
  copy: { color: "#C6D3E8", fontSize: 12, lineHeight: 17, marginTop: 4 },
  stat: {
    backgroundColor: "white",
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 17,
    padding: 17,
    marginTop: 14,
    ...shadow,
  },
  statLabel: {
    color: colors.muted,
    fontSize: 10,
    fontWeight: "900",
    letterSpacing: 1.2,
  },
  statNumber: {
    color: colors.primary,
    fontSize: 34,
    fontWeight: "900",
    marginTop: 3,
  },
  statCaption: { color: colors.muted, fontSize: 12 },
  sectionHead: { marginTop: 27, marginBottom: 11 },
  sectionEyebrow: {
    color: colors.primary,
    fontSize: 10,
    fontWeight: "900",
    letterSpacing: 1.3,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 22,
    fontWeight: "900",
    marginTop: 2,
  },
  card: {
    backgroundColor: "white",
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 15,
    padding: 13,
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 11,
    marginBottom: 10,
    ...shadow,
  },
  avatar: {
    width: 43,
    height: 43,
    borderRadius: 22,
    backgroundColor: colors.softBlue,
    alignItems: "center",
    justifyContent: "center",
  },
  userTop: { flexDirection: "row", alignItems: "flex-start", gap: 7 },
  username: { color: colors.text, fontSize: 14, fontWeight: "900", flex: 1 },
  status: {
    borderRadius: 7,
    paddingHorizontal: 7,
    paddingVertical: 4,
    fontSize: 9,
    fontWeight: "900",
    overflow: "hidden",
  },
  statusActive: { color: colors.success, backgroundColor: "#E4F8EC" },
  statusInactive: { color: colors.danger, backgroundColor: "#FBECEF" },
  phone: { color: "#536781", fontSize: 12, fontWeight: "700", marginTop: 4 },
  meta: { flexDirection: "row", alignItems: "center", gap: 7, marginTop: 8 },
  id: { color: colors.muted, fontSize: 10, fontWeight: "700" },
  role: {
    color: "#526174",
    backgroundColor: "#EDF1F6",
    borderRadius: 6,
    paddingHorizontal: 7,
    paddingVertical: 3,
    fontSize: 9,
    fontWeight: "900",
    overflow: "hidden",
  },
  adminRole: { color: colors.primary, backgroundColor: colors.softBlue },
  pagination: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
    marginTop: 8,
  },
  pageButton: {
    minHeight: 42,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "#CFDAED",
    backgroundColor: "white",
    paddingHorizontal: 11,
    flexDirection: "row",
    alignItems: "center",
    gap: 3,
  },
  pageButtonText: { color: colors.primary, fontSize: 12, fontWeight: "900" },
  pageText: { color: colors.muted, fontSize: 12, fontWeight: "800" },
  disabled: { opacity: 0.4 },
});

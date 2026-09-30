import Ionicons from "@expo/vector-icons/Ionicons";
import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { colors } from "../constants/theme";
import { updateJoinRequest } from "../services/notification.service";
import { getJoinedRoomMembers } from "../services/posts.service";
import { ModalSheet } from "./ModalSheet";
import { ScreenState } from "./ScreenState";

export function RoomMembersModal({ post, onClose, onChanged }) {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [removingId, setRemovingId] = useState(null);

  const loadMembers = useCallback(async () => {
    if (!post?.id) return;
    try {
      setLoading(true);
      setError("");
      const result = await getJoinedRoomMembers(post.id);
      setMembers(Array.isArray(result) ? result : []);
    } catch (nextError) {
      setError(nextError.message || "Không thể tải danh sách người ở.");
    } finally {
      setLoading(false);
    }
  }, [post?.id]);

  useEffect(() => {
    if (post?.id) loadMembers();
    else {
      setMembers([]);
      setError("");
    }
  }, [post?.id, loadMembers]);

  const openProfile = (member) => {
    onClose();
    router.push({ pathname: "/users/[userId]", params: { userId: String(member.userId), name: member.fullName || "Thành viên RoomMate" } });
  };

  const removeMember = (member) => {
    Alert.alert("Mời rời phòng?", `Bạn có chắc muốn mời ${member.fullName || "người này"} rời khỏi phòng?`, [
      { text: "Hủy", style: "cancel" },
      {
        text: "Mời rời phòng",
        style: "destructive",
        onPress: async () => {
          try {
            setRemovingId(member.roomConnectionId);
            setError("");
            await updateJoinRequest(member.roomConnectionId, "KICKED");
            setMembers((current) => current.filter((item) => item.roomConnectionId !== member.roomConnectionId));
            onChanged?.();
          } catch (nextError) {
            setError(nextError.message || "Không thể mời người ở rời phòng.");
          } finally {
            setRemovingId(null);
          }
        },
      },
    ]);
  };

  return <ModalSheet visible={Boolean(post)} onClose={onClose} eyebrow="QUẢN LÝ PHÒNG" title={post ? `Người ở · ${post.title}` : "Người ở"}><ScrollView contentContainerStyle={styles.content}><ScreenState loading={loading} error={error} empty={!loading && !error && !members.length ? "Chưa có người ở trong phòng." : ""} onRetry={loadMembers} />{!loading && members.map((member) => <View key={member.roomConnectionId} style={styles.member}><Pressable style={styles.memberInfo} onPress={() => openProfile(member)}><View style={styles.avatar}><Ionicons name="person" size={20} color={colors.primary} /></View><View style={styles.memberText}><Text style={styles.name}>{member.fullName || "Thành viên RoomMate"}</Text><Text style={styles.link}>Xem trang cá nhân</Text></View></Pressable><Pressable disabled={removingId === member.roomConnectionId} onPress={() => removeMember(member)} style={[styles.remove, removingId === member.roomConnectionId && styles.disabled]}><Text style={styles.removeText}>{removingId === member.roomConnectionId ? "Đang xử lý..." : "Mời rời"}</Text></Pressable></View>)}</ScrollView></ModalSheet>;
}

const styles = StyleSheet.create({ content: { paddingBottom: 12 }, member: { minHeight: 70, borderTopWidth: 1, borderColor: "#EEF1F5", flexDirection: "row", alignItems: "center", gap: 10 }, memberInfo: { flex: 1, minWidth: 0, flexDirection: "row", alignItems: "center", gap: 10, paddingVertical: 10 }, avatar: { width: 42, height: 42, borderRadius: 21, alignItems: "center", justifyContent: "center", backgroundColor: colors.softBlue }, memberText: { flex: 1 }, name: { color: colors.text, fontSize: 14, fontWeight: "800" }, link: { color: colors.primary, fontSize: 11, marginTop: 3, fontWeight: "700" }, remove: { minHeight: 36, paddingHorizontal: 10, borderRadius: 9, alignItems: "center", justifyContent: "center", backgroundColor: "#FFF0F0" }, removeText: { color: colors.danger, fontSize: 11, fontWeight: "800" }, disabled: { opacity: 0.55 } });
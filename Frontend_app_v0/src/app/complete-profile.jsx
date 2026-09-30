import Ionicons from "@expo/vector-icons/Ionicons";
import { router } from "expo-router";
import { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { ProfileForm } from "../components/ProfileForm";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { updateInfo } from "../services/user.service";
import { toUpdateInfoPayload } from "../utils/profileMapper";

export default function CompleteProfileScreen() {
  const { updateUser } = useAuth(); const [saving, setSaving] = useState(false); const [message, setMessage] = useState("");
  const submit = async (form, validationMessage) => { if (!form) return setMessage(validationMessage); try { setSaving(true); setMessage(""); const profile = await updateInfo(toUpdateInfoPayload(form)); updateUser(profile); router.replace("/"); } catch (error) { setMessage(error.message); } finally { setSaving(false); } };
  return <SafeAreaView style={styles.safe}><KeyboardAvoidingView style={styles.grow} behavior={Platform.OS === "ios" ? "padding" : "height"}><ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.content}><View style={styles.card}><View style={styles.head}><View style={styles.avatar}><Ionicons name="person" size={30} color={colors.primary} /></View><View style={styles.grow}><Text style={styles.eyebrow}>HOÀN TẤT HỒ SƠ</Text><Text style={styles.title}>Thêm thông tin cá nhân</Text><Text style={styles.copy}>Thông tin này giúp RoomMate gợi ý phòng và bạn ở phù hợp hơn.</Text></View></View><ProfileForm disabled={saving} submitLabel="Hoàn tất" onSubmit={submit} message={message} /></View></ScrollView></KeyboardAvoidingView></SafeAreaView>;
}
const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: colors.background }, grow: { flex: 1 }, content: { padding: 16, paddingBottom: 36 }, card: { backgroundColor: "white", borderRadius: 20, padding: 18, borderWidth: 1, borderColor: colors.border, ...shadow }, head: { flexDirection: "row", gap: 13, borderBottomWidth: 1, borderColor: colors.border, paddingBottom: 18, marginBottom: 20 }, avatar: { width: 60, height: 60, borderRadius: 30, alignItems: "center", justifyContent: "center", backgroundColor: colors.softBlue }, grow: { flex: 1 }, eyebrow: { color: colors.muted, fontSize: 10, fontWeight: "800", letterSpacing: 1.3 }, title: { color: colors.text, fontSize: 21, fontWeight: "900", marginTop: 2 }, copy: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: 4 } });

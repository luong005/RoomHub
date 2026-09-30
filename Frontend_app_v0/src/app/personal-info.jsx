import Ionicons from "@expo/vector-icons/Ionicons";
import { useCallback, useEffect, useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { AppHeader } from "../components/AppHeader";
import { ProfileForm, emptyProfile } from "../components/ProfileForm";
import { ScreenState } from "../components/ScreenState";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { getProfile, updateInfo } from "../services/user.service";
import { toUpdateInfoPayload } from "../utils/profileMapper";

export default function PersonalInfoScreen() {
  const { updateUser } = useAuth();
  const [initial, setInitial] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const load = useCallback(async () => {
    try { setLoading(true); setError(""); const profile = await getProfile(); setInitial(profile); updateUser(profile); }
    catch (nextError) { setError(nextError.message); } finally { setLoading(false); }
  }, [updateUser]);
  useEffect(() => { load(); }, [load]);
  const submit = async (form, validationMessage) => {
    if (!form) return setMessage(validationMessage);
    try { setSaving(true); setMessage(""); const profile = await updateInfo(toUpdateInfoPayload(form)); setInitial(profile); updateUser(profile); setMessage("✓ Thông tin cá nhân đã được lưu."); }
    catch (nextError) { setMessage(nextError.message); } finally { setSaving(false); }
  };
  return <SafeAreaView style={styles.safe} edges={["top"]}><AppHeader backTo="/profile" backLabel="Trang cá nhân" /><KeyboardAvoidingView style={styles.grow} behavior={Platform.OS === "ios" ? "padding" : "height"}><ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.content}><View style={styles.card}><View style={styles.head}><View style={styles.avatar}><Ionicons name="person" size={30} color={colors.primary} /></View><View style={styles.grow}><Text style={styles.eyebrow}>HỒ SƠ CỦA BẠN</Text><Text style={styles.title}>Thông tin cá nhân</Text><Text style={styles.copy}>Thông tin giúp RoomMate đề xuất phòng và bạn ở phù hợp hơn.</Text></View></View>{loading || error ? <ScreenState loading={loading} error={error} onRetry={load} /> : <ProfileForm initialValue={initial} disabled={saving} submitLabel="Lưu thay đổi" onSubmit={submit} message={message} success={message.startsWith("✓")} />}</View></ScrollView></KeyboardAvoidingView></SafeAreaView>;
}
const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: colors.background }, grow: { flex: 1 }, content: { padding: 16, paddingBottom: 36 }, card: { backgroundColor: "white", borderRadius: 20, padding: 18, borderWidth: 1, borderColor: colors.border, ...shadow }, head: { flexDirection: "row", gap: 13, borderBottomWidth: 1, borderColor: colors.border, paddingBottom: 18, marginBottom: 20 }, avatar: { width: 60, height: 60, borderRadius: 30, alignItems: "center", justifyContent: "center", backgroundColor: colors.softBlue }, grow: { flex: 1 }, eyebrow: { color: colors.muted, fontSize: 10, fontWeight: "800", letterSpacing: 1.3 }, title: { color: colors.text, fontSize: 21, fontWeight: "900", marginTop: 2 }, copy: { color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: 4 } });

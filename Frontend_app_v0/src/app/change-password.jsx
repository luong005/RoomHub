import { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { AppHeader } from "../components/AppHeader";
import { FieldLabel, Notice, PrimaryButton, TextField } from "../components/FormFields";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { updatePassword } from "../services/user.service";

export default function ChangePasswordScreen() {
  const { user } = useAuth();
  const [oldPassword, setOldPassword] = useState(""); const [newPassword, setNewPassword] = useState(""); const [confirm, setConfirm] = useState(""); const [saving, setSaving] = useState(false); const [result, setResult] = useState(null);
  const submit = async () => {
    if (!oldPassword || !newPassword || !confirm) return setResult({ text: "Vui lòng nhập đầy đủ các trường.", success: false });
    if (newPassword.length < 8) return setResult({ text: "Mật khẩu mới cần có ít nhất 8 ký tự.", success: false });
    if (newPassword !== confirm) return setResult({ text: "Mật khẩu mới và xác nhận chưa trùng khớp.", success: false });
    try { setSaving(true); setResult(null); await updatePassword({ phoneNumber: user.phoneNumber, oldPassword, newPassword }); setOldPassword(""); setNewPassword(""); setConfirm(""); setResult({ text: "✓ Mật khẩu đã được thay đổi thành công.", success: true }); }
    catch (error) { setResult({ text: error.message, success: false }); } finally { setSaving(false); }
  };
  return <SafeAreaView style={styles.safe} edges={["top"]}><AppHeader backTo="/profile" backLabel="Trang cá nhân" /><KeyboardAvoidingView style={styles.grow} behavior={Platform.OS === "ios" ? "padding" : "height"}><ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.content}><View style={styles.card}><View style={styles.head}><Text style={styles.eyebrow}>BẢO MẬT TÀI KHOẢN</Text><Text style={styles.title}>Đổi mật khẩu</Text><Text style={styles.copy}>Dùng mật khẩu mạnh gồm ít nhất 8 ký tự để bảo vệ tài khoản của bạn.</Text></View><Password label="Mật khẩu cũ" value={oldPassword} onChange={setOldPassword} autoComplete="current-password" /><Password label="Mật khẩu mới" value={newPassword} onChange={setNewPassword} autoComplete="new-password" /><Password label="Nhập lại mật khẩu mới" value={confirm} onChange={setConfirm} autoComplete="new-password" /><PrimaryButton title={saving ? "Đang cập nhật..." : "Cập nhật mật khẩu"} onPress={submit} disabled={saving} />{result ? <View style={styles.notice}><Notice success={result.success}>{result.text}</Notice></View> : null}</View></ScrollView></KeyboardAvoidingView></SafeAreaView>;
}
function Password({ label, value, onChange, autoComplete }) { return <View style={styles.field}><FieldLabel>{label}</FieldLabel><TextField value={value} onChangeText={onChange} secureTextEntry autoCapitalize="none" autoCorrect={false} autoComplete={autoComplete} maxLength={128} /></View>; }
const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: colors.background }, grow: { flex: 1 }, content: { padding: 16, paddingBottom: 36 }, card: { backgroundColor: "white", borderRadius: 20, padding: 18, borderWidth: 1, borderColor: colors.border, ...shadow }, head: { borderBottomWidth: 1, borderColor: colors.border, paddingBottom: 18, marginBottom: 20 }, eyebrow: { color: colors.muted, fontSize: 10, fontWeight: "800", letterSpacing: 1.3 }, title: { color: colors.text, fontSize: 23, fontWeight: "900", marginTop: 3 }, copy: { color: colors.muted, lineHeight: 20, marginTop: 6 }, field: { marginBottom: 16 }, notice: { marginTop: 13 } });

import Ionicons from "@expo/vector-icons/Ionicons";
import { router } from "expo-router";
import { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { FieldLabel, Notice, PrimaryButton, TextField } from "../components/FormFields";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { useLoginRateLimit } from "../hooks/useLoginRateLimit";
import { isRegistrationAwaitingLogin } from "../services/auth.service";
import { loginWaitMessage } from "../utils/loginRateLimit";

export default function LoginScreen() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState("login"); const [phone, setPhone] = useState(""); const [password, setPassword] = useState(""); const [confirm, setConfirm] = useState(""); const [hidden, setHidden] = useState(true); const [submitting, setSubmitting] = useState(false); const [message, setMessage] = useState("");
  const isRegister = mode === "register";
  const { cooldown, capture } = useLoginRateLimit(phone);
  const registrationPending = isRegister && isRegistrationAwaitingLogin(phone);
  const waiting = Boolean(cooldown) && (!isRegister || registrationPending);
  const displayMessage = waiting ? loginWaitMessage(cooldown.remainingSeconds, registrationPending) : message;
  const switchMode = (next) => { setMode(next); setPassword(""); setConfirm(""); setMessage(""); };
  const submit = async () => {
    if (submitting || waiting) return;
    const cleanPhone = phone.trim();
    if (!cleanPhone || !password) return setMessage("Vui lòng nhập số điện thoại và mật khẩu.");
    if (password.length < 6) return setMessage("Mật khẩu cần có ít nhất 6 ký tự.");
    if (isRegister && password !== confirm) return setMessage("Mật khẩu nhắc lại chưa trùng khớp.");
    try { setSubmitting(true); setMessage(""); await (isRegister ? register : login)({ phoneNumber: cleanPhone, password }); router.replace(isRegister ? "/complete-profile" : "/"); }
    catch (error) { const countingDown = capture(error, cleanPhone); setMessage(countingDown ? "" : `${error.registrationCompleted ? "Tài khoản đã được tạo. " : ""}${error.message || "Không thể xác thực tài khoản."}`); } finally { setSubmitting(false); }
  };
  return <SafeAreaView style={styles.safe}><KeyboardAvoidingView style={styles.grow} behavior={Platform.OS === "ios" ? "padding" : "height"}><ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled"><View style={styles.card}><View style={styles.brand}><View style={styles.logo}><Ionicons name="home" size={25} color="white" /></View><Text style={styles.brandText}>Retal Room</Text></View><Text style={styles.eyebrow}>NỀN TẢNG TÌM PHÒNG TRỌ</Text><Text style={styles.title}>{isRegister ? "Tạo tài khoản mới" : "Chào mừng bạn"}</Text><Text style={styles.copy}>{isRegister ? "Đăng ký để lưu phòng, nhắn tin và đăng bài cho thuê." : "Đăng nhập để quản lý hồ sơ, bài đăng và yêu cầu vào phòng."}</Text><View style={styles.tabs}><Tab title="Đăng nhập" active={!isRegister} onPress={() => switchMode("login")} /><Tab title="Đăng ký" active={isRegister} onPress={() => switchMode("register")} /></View><View style={styles.field}><FieldLabel>Số điện thoại</FieldLabel><TextField value={phone} onChangeText={setPhone} keyboardType="phone-pad" autoComplete="tel" placeholder="Ví dụ: 0901234567" maxLength={20} /></View><View style={styles.field}><FieldLabel>Mật khẩu</FieldLabel><View><TextField value={password} onChangeText={setPassword} secureTextEntry={hidden} autoCapitalize="none" autoCorrect={false} autoComplete={isRegister ? "new-password" : "current-password"} maxLength={128} style={styles.password} /><Pressable onPress={() => setHidden((value) => !value)} style={styles.eye} hitSlop={8}><Ionicons name={hidden ? "eye-outline" : "eye-off-outline"} size={21} color={colors.muted} /></Pressable></View></View>{isRegister ? <View style={styles.field}><FieldLabel>Nhắc lại mật khẩu</FieldLabel><TextField value={confirm} onChangeText={setConfirm} secureTextEntry autoCapitalize="none" autoCorrect={false} maxLength={128} /></View> : null}<PrimaryButton title={submitting ? "Đang xử lý..." : isRegister ? "Tạo tài khoản" : "Đăng nhập"} onPress={submit} disabled={submitting || waiting} />{displayMessage ? <View style={styles.message}><Notice>{displayMessage}</Notice></View> : null}</View></ScrollView></KeyboardAvoidingView></SafeAreaView>;
}
function Tab({ title, active, onPress }) { return <Pressable onPress={onPress} style={[styles.tab, active && styles.tabActive]}><Text style={[styles.tabText, active && styles.tabTextActive]}>{title}</Text></Pressable>; }
const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: colors.background }, grow: { flex: 1 }, content: { flexGrow: 1, justifyContent: "center", padding: 18 }, card: { backgroundColor: "white", borderRadius: 24, borderWidth: 1, borderColor: colors.border, padding: 22, ...shadow }, brand: { flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 28 }, logo: { width: 45, height: 45, borderRadius: 14, backgroundColor: colors.primary, alignItems: "center", justifyContent: "center" }, brandText: { color: colors.primary, fontSize: 25, fontWeight: "900", letterSpacing: -1 }, eyebrow: { color: "#758399", fontSize: 10, fontWeight: "800", letterSpacing: 1.3 }, title: { color: colors.text, fontSize: 28, fontWeight: "900", marginTop: 4 }, copy: { color: colors.muted, lineHeight: 20, marginTop: 7 }, tabs: { flexDirection: "row", backgroundColor: colors.softGray, borderRadius: 12, padding: 4, marginVertical: 24 }, tab: { flex: 1, minHeight: 43, justifyContent: "center", alignItems: "center", borderRadius: 9 }, tabActive: { backgroundColor: "white" }, tabText: { color: colors.muted, fontWeight: "800" }, tabTextActive: { color: colors.primary }, field: { marginBottom: 16 }, password: { paddingRight: 48 }, eye: { position: "absolute", right: 2, top: 2, width: 44, height: 43, justifyContent: "center", alignItems: "center" }, message: { marginTop: 12 } });

import Ionicons from "@expo/vector-icons/Ionicons";
import * as ImagePicker from "expo-image-picker";
import { useEffect, useState } from "react";
import { Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { ChoiceSheet, ModalSheet } from "./ModalSheet";
import { FieldLabel, Notice, PrimaryButton, SelectField, TextField } from "./FormFields";
import { colors } from "../constants/theme";

const emptyPost = { title: "", address: "", price: "", area: "", currentMembers: "0", maxMembers: "", gender: "", description: "", scheduleDate: "", scheduleTime: "" };

export function PostFormModal({ visible, post, onClose, onSave }) {
  const isEdit = Boolean(post?.id);
  const [form, setForm] = useState(emptyPost);
  const [images, setImages] = useState([]);
  const [choice, setChoice] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!visible) return;
    setForm(post ? { title: String(post.title || ""), address: String(post.address || ""), price: String(post.price ?? ""), area: String(post.area ?? ""), currentMembers: String(post.currentMembers ?? 0), maxMembers: String(post.maxMembers ?? ""), gender: String(post.gender || ""), description: String(post.description || ""), scheduleDate: "", scheduleTime: "" } : emptyPost);
    setImages([]); setMessage("");
  }, [post, visible]);

  const set = (key) => (value) => setForm((current) => ({ ...current, [key]: value }));
  const pickImages = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) return setMessage("Bạn cần cấp quyền thư viện ảnh để đăng ảnh phòng.");
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], allowsMultipleSelection: true, selectionLimit: 5, quality: 0.85 });
    if (result.canceled) return;
    const tooLarge = result.assets.find((asset) => asset.fileSize && asset.fileSize > 10 * 1024 * 1024);
    if (tooLarge) return setMessage("Mỗi ảnh phải nhỏ hơn hoặc bằng 10 MB.");
    setImages(result.assets.slice(0, 5)); setMessage("");
  };

  const submit = async () => {
    const payload = { title: form.title.trim(), address: form.address.trim(), price: Number(form.price), area: Number(form.area), currentMembers: Number(form.currentMembers), maxMembers: Number(form.maxMembers), gender: form.gender === "Không yêu cầu" ? "" : form.gender, description: form.description.trim() };
    if (!payload.title || !payload.address || !form.price || !form.area || !form.maxMembers) return setMessage("Vui lòng nhập đầy đủ thông tin bài đăng.");
    if (![payload.price, payload.area].every((value) => Number.isFinite(value) && value >= 0)) return setMessage("Giá và diện tích phải là số không âm.");
    if (!Number.isInteger(payload.currentMembers) || !Number.isInteger(payload.maxMembers) || payload.currentMembers < 0 || payload.maxMembers < 1) return setMessage("Số thành viên không hợp lệ.");
    if ((!isEdit && payload.currentMembers >= payload.maxMembers) || (isEdit && payload.currentMembers > payload.maxMembers)) return setMessage(isEdit ? "Số người hiện tại không được vượt quá số tối đa." : "Bài mới phải còn ít nhất một chỗ trống.");
    // if (!images.length) return setMessage(isEdit ? "Yêu cầu ít nhất một ảnh mới khi cập nhật bài." : "Cần chọn ít nhất một ảnh phòng.");
    if (!isEdit && (form.scheduleDate || form.scheduleTime)) {
      if (!form.scheduleDate || !form.scheduleTime || !/^\d{4}-\d{2}-\d{2}$/.test(form.scheduleDate) || !/^\d{2}:\d{2}$/.test(form.scheduleTime)) return setMessage("Lịch đăng phải có dạng ngày YYYY-MM-DD và giờ HH:mm.");
      payload.scheduledAt = `${form.scheduleDate}T${form.scheduleTime}`;
    }
    if (isEdit) payload.status = post.status === "FULL" ? "FULL" : "OPEN";
    try { setSaving(true); setMessage(""); await onSave(payload, images); onClose(); }
    catch (error) { setMessage(error.message || "Không thể lưu bài đăng."); } finally { setSaving(false); }
  };

  return <><ModalSheet expanded visible={visible} onClose={saving ? undefined : onClose} eyebrow={isEdit ? "CHỈNH SỬA BÀI ĐĂNG" : "ĐĂNG BÀI MỚI"} title={isEdit ? "Cập nhật thông tin phòng" : "Cho thuê phòng"}><KeyboardAvoidingView style={styles.formBody} behavior={Platform.OS === "ios" ? "padding" : "height"}><ScrollView style={styles.formScroll} contentContainerStyle={styles.formContent} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}><Field label="Tiêu đề bài đăng"><TextField value={form.title} onChangeText={set("title")} maxLength={200} /></Field><Field label="Địa chỉ phòng"><TextField value={form.address} onChangeText={set("address")} maxLength={300} /></Field><Field label="Giá thuê / tháng"><TextField value={form.price} onChangeText={set("price")} keyboardType="decimal-pad" /></Field><Field label="Diện tích (m²)"><TextField value={form.area} onChangeText={set("area")} keyboardType="decimal-pad" /></Field><View style={styles.two}><View style={styles.half}><Field label="Người đang ở"><TextField value={form.currentMembers} onChangeText={set("currentMembers")} keyboardType="number-pad" /></Field></View><View style={styles.half}><Field label="Người tối đa"><TextField value={form.maxMembers} onChangeText={set("maxMembers")} keyboardType="number-pad" /></Field></View></View><Field label="Giới tính phù hợp"><SelectField value={form.gender || "Không yêu cầu"} onPress={() => setChoice(true)} /></Field><Field label="Mô tả & chi phí khác"><TextField value={form.description} onChangeText={set("description")} multiline maxLength={2000} /></Field>{!isEdit ? <View style={styles.two}><View style={styles.half}><Field label="Ngày đăng (tùy chọn)"><TextField value={form.scheduleDate} onChangeText={set("scheduleDate")} placeholder="YYYY-MM-DD" maxLength={10} /></Field></View><View style={styles.half}><Field label="Giờ đăng"><TextField value={form.scheduleTime} onChangeText={set("scheduleTime")} placeholder="HH:mm" maxLength={5} /></Field></View></View> : null}<Pressable style={styles.upload} onPress={pickImages} disabled={saving}><Ionicons name="images-outline" size={23} color="#3869A9" /><Text style={styles.uploadText}>{images.length ? `Đã chọn ${images.length}/5 ảnh` : isEdit ? "Chọn ảnh mới" : "Tải ảnh phòng (tối đa 5 ảnh)"}</Text></Pressable>{images.length ? <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.previewRow}>{images.map((asset) => <Image key={asset.assetId || asset.uri} source={{ uri: asset.uri }} style={styles.preview} />)}</ScrollView> : null}{message ? <View style={styles.notice}><Notice>{message}</Notice></View> : null}<View style={styles.button}><PrimaryButton title={saving ? "Đang lưu..." : isEdit ? "Lưu thay đổi" : "Đăng bài"} onPress={submit} disabled={saving} /></View></ScrollView></KeyboardAvoidingView></ModalSheet><ChoiceSheet visible={choice} title="Giới tính phù hợp" value={form.gender || "Không yêu cầu"} options={["Không yêu cầu", "Nam", "Nữ"]} onClose={() => setChoice(false)} onChange={set("gender")} /></>;
}
function Field({ label, children }) { return <View style={styles.field}><FieldLabel>{label}</FieldLabel>{children}</View>; }
const styles = StyleSheet.create({ formBody: { flex: 1, minHeight: 0 }, formScroll: { flex: 1 }, formContent: { paddingBottom: 12 }, field: { marginBottom: 14 }, two: { flexDirection: "row", gap: 10 }, half: { flex: 1 }, upload: { minHeight: 64, flexDirection: "row", alignItems: "center", gap: 9, borderStyle: "dashed", borderWidth: 1, borderColor: "#AAC2ED", borderRadius: 12, padding: 13, backgroundColor: "#F4F8FF" }, uploadText: { flex: 1, color: "#3869A9", fontWeight: "800", fontSize: 13 }, previewRow: { marginTop: 10 }, preview: { width: 76, height: 76, borderRadius: 9, marginRight: 8 }, notice: { marginTop: 12 }, button: { marginTop: 16, paddingBottom: 12 } });

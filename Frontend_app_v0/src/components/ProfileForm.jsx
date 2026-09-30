import { useEffect, useState } from "react";
import { StyleSheet, View } from "react-native";
import { ChoiceSheet } from "./ModalSheet";
import { FieldLabel, Notice, PrimaryButton, SelectField, TextField } from "./FormFields";

export const emptyProfile = { fullName: "", gender: "", hometown: "", currentAddress: "", yearOfBirth: "", study: "", bio: "" };

export function ProfileForm({ initialValue = emptyProfile, disabled, submitLabel, onSubmit, message, success }) {
  const [form, setForm] = useState({ ...emptyProfile, ...initialValue });
  const [choice, setChoice] = useState(null);
  useEffect(() => setForm({ ...emptyProfile, ...initialValue }), [initialValue]);
  const set = (key) => (value) => setForm((current) => ({ ...current, [key]: value }));
  const submit = () => {
    const required = [form.fullName, form.gender, form.hometown, form.currentAddress, form.yearOfBirth, form.study];
    const year = Number(form.yearOfBirth);
    if (required.some((value) => !String(value).trim())) return onSubmit(null, "Vui lòng nhập đầy đủ các trường bắt buộc.");
    if (!Number.isInteger(year) || year < 1900 || year > new Date().getFullYear()) return onSubmit(null, "Năm sinh không hợp lệ.");
    return onSubmit(form);
  };
  return <><Field label="Họ và tên"><TextField value={form.fullName} onChangeText={set("fullName")} editable={!disabled} autoCapitalize="words" maxLength={120} /></Field><Field label="Giới tính"><SelectField value={form.gender} onPress={() => setChoice("gender")} disabled={disabled} /></Field><Field label="Quê quán"><TextField value={form.hometown} onChangeText={set("hometown")} editable={!disabled} maxLength={200} /></Field><Field label="Địa chỉ hiện tại"><TextField value={form.currentAddress} onChangeText={set("currentAddress")} editable={!disabled} maxLength={250} /></Field><Field label="Năm sinh"><TextField value={String(form.yearOfBirth || "")} onChangeText={set("yearOfBirth")} editable={!disabled} keyboardType="number-pad" maxLength={4} /></Field><Field label="Tình trạng hiện tại"><SelectField value={form.study} onPress={() => setChoice("study")} disabled={disabled} /></Field><Field label="Tiểu sử trang cá nhân"><TextField value={form.bio} onChangeText={set("bio")} editable={!disabled} multiline maxLength={1000} placeholder="Viết vài điều ngắn về bạn..." /></Field><Notice>🔒 Chỉ các thông tin cần thiết mới hiển thị với người dùng khác. Bạn có thể thay đổi nội dung này bất cứ lúc nào.</Notice><View style={styles.submit}><PrimaryButton title={disabled ? "Đang xử lý..." : submitLabel} onPress={submit} disabled={disabled} /></View>{message ? <Notice success={success}>{message}</Notice> : null}<ChoiceSheet visible={choice === "gender"} title="Giới tính" value={form.gender} options={["Nữ", "Nam"]} onClose={() => setChoice(null)} onChange={set("gender")} /><ChoiceSheet visible={choice === "study"} title="Tình trạng hiện tại" value={form.study} options={["Đang đi học", "Đã đi làm"]} onClose={() => setChoice(null)} onChange={set("study")} /></>;
}
function Field({ label, children }) { return <View style={styles.field}><FieldLabel>{label}</FieldLabel>{children}</View>; }
const styles = StyleSheet.create({ field: { marginBottom: 16 }, submit: { marginTop: 16, marginBottom: 12 } });

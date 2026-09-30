import Ionicons from "@expo/vector-icons/Ionicons";
import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Alert, FlatList, Image, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { AppHeader } from "../components/AppHeader";
import { FieldLabel, PrimaryButton, SecondaryButton, SelectField, TextField } from "../components/FormFields";
import { ChoiceSheet, ModalSheet } from "../components/ModalSheet";
import { PostCard } from "../components/PostCard";
import { PostFormModal } from "../components/PostFormModal";
import { ScreenState } from "../components/ScreenState";
import { colors, shadow } from "../constants/theme";
import { useAuth } from "../hooks/useAuth";
import { getChatRooms, getMessageRequests } from "../services/chat.service";
import { getReceivedNotifications, getSentNotifications, updateJoinRequest } from "../services/notification.service";
import { createPost, getPosts, requestJoin } from "../services/posts.service";
import { isAdmin } from "../utils/roles";

const emptyFilters = { address: "", area: "", priceFrom: "", priceTo: "", currentMembers: "", gender: "" };

export default function FeedScreen() {
  const { user, logout } = useAuth();
  const [posts, setPosts] = useState([]); const [loading, setLoading] = useState(true); const [refreshing, setRefreshing] = useState(false); const [error, setError] = useState("");
  const [filters, setFilters] = useState(emptyFilters); const [draftFilters, setDraftFilters] = useState(emptyFilters); const [sheet, setSheet] = useState(null); const [choice, setChoice] = useState(null); const [gallery, setGallery] = useState(null); const [joiningId, setJoiningId] = useState(null);
  const [notifications, setNotifications] = useState({ received: [], sent: [] }); const [notificationTab, setNotificationTab] = useState("received"); const [updatingRequestId, setUpdatingRequestId] = useState(null); const [respondedNotificationIds, setRespondedNotificationIds] = useState([]);
  const [chats, setChats] = useState([]); const [sheetLoading, setSheetLoading] = useState(false); const [sheetError, setSheetError] = useState("");

  const load = useCallback(async (nextFilters = filters, refresh = false) => {
    try { refresh ? setRefreshing(true) : setLoading(true); setError(""); setPosts(await getPosts(nextFilters)); }
    catch (nextError) { setError(nextError.message); } finally { setLoading(false); setRefreshing(false); }
  }, [filters]);
  useEffect(() => { load(emptyFilters); }, []);

  const loadNotifications = async () => {
    setSheetLoading(true); setSheetError("");
    try { const [received, sent] = await Promise.all([getReceivedNotifications(), getSentNotifications()]); setNotifications({ received: newest(received), sent: newest(sent) }); }
    catch (nextError) { setSheetError(nextError.message); } finally { setSheetLoading(false); }
  };
  const openNotifications = () => {
    setNotificationTab("received"); setSheet("notifications"); loadNotifications();
  };
  const openMessages = async () => {
    setSheet("messages"); setSheetLoading(true); setSheetError("");
    try { const [rooms, requests] = await Promise.all([getChatRooms(), getMessageRequests()]); setChats([...requests.map((room) => ({ ...room, isRequest: true })), ...rooms]); }
    catch (nextError) { setSheetError(nextError.message); } finally { setSheetLoading(false); }
  };
  const join = async (post) => {
    try { setJoiningId(post.id); await requestJoin(post.id); Alert.alert("Đã gửi yêu cầu", `Yêu cầu vào phòng “${post.title}” đã được gửi.`); }
    catch (nextError) { Alert.alert("Không thể gửi yêu cầu", nextError.message); } finally { setJoiningId(null); }
  };
  const respond = async (notification, status) => {
    try {
      setUpdatingRequestId(notification.id); setSheetError("");
      await updateJoinRequest(notification.roomConnectionId, status);
      setRespondedNotificationIds((ids) => ids.includes(notification.id) ? ids : [...ids, notification.id]);
      await loadNotifications();
    } catch (nextError) { setSheetError(nextError.message); }
    finally { setUpdatingRequestId(null); }
  };
  const performLogout = () => Alert.alert("Đăng xuất", "Bạn có chắc muốn đăng xuất?", [{ text: "Hủy", style: "cancel" }, { text: "Đăng xuất", style: "destructive", onPress: async () => { await logout(); router.replace("/login"); } }]);
  const openChat = (room) => { setSheet(null); router.push({ pathname: "/chat", params: { roomId: String(room.id), recipientId: String(room.otherUser?.id || ""), recipientName: room.otherUser?.fullName || "Thành viên RoomMate" } }); };

  return <SafeAreaView style={styles.safe} edges={["top"]}><AppHeader onInfo={() => setSheet("policy")} onMessages={openMessages} onNotifications={openNotifications} onAccount={() => setSheet("account")} /><FlatList data={posts} keyExtractor={(item) => String(item.id)} renderItem={({ item }) => <PostCard post={item} joining={joiningId === item.id} onJoin={() => join(item)} onGallery={() => { setGallery(item); setSheet("gallery"); }} onAuthor={() => router.push({ pathname: "/users/[userId]", params: { userId: String(item.userId), name: item.authorName || "Thành viên RoomMate" } })} onMessage={() => router.push({ pathname: "/chat", params: { recipientId: String(item.userId), recipientName: item.authorName || "Thành viên RoomMate" } })} />} contentContainerStyle={styles.list} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => load(filters, true)} colors={[colors.primary]} />} ListHeaderComponent={<><Pressable style={styles.composer} onPress={() => setSheet("create")}><View style={styles.composerAvatar}><Ionicons name="person" size={20} color={colors.primary} /></View><Text style={styles.composerText}>Bạn muốn đăng bài cho thuê phòng?</Text><Ionicons name="add-circle" size={23} color={colors.primary} /></Pressable><View style={styles.heading}><View style={styles.grow}><Text style={styles.eyebrow}>BẢNG TIN PHÒNG TRỌ</Text><Text style={styles.title}>Bài đăng mới nhất</Text></View><Pressable style={styles.filter} onPress={() => { setDraftFilters(filters); setSheet("filter"); }}><Ionicons name="options-outline" size={19} color="#416184" /><Text style={styles.filterText}>Bộ lọc</Text></Pressable></View></>} ListEmptyComponent={<ScreenState loading={loading} error={error} empty="Không tìm thấy bài đăng phù hợp." onRetry={() => load(filters)} />} />
    <Filter visible={sheet === "filter"} form={draftFilters} setForm={setDraftFilters} onChoice={setChoice} onClose={() => setSheet(null)} onApply={() => { setFilters(draftFilters); setSheet(null); load(draftFilters); }} onReset={() => setDraftFilters(emptyFilters)} />
    <PostFormModal visible={sheet === "create"} onClose={() => setSheet(null)} onSave={async (post, images) => { await createPost(post, images); await load(filters); Alert.alert("Đăng bài thành công", post.scheduledAt ? "Bài viết đã được lên lịch." : "Bài viết đang được hiển thị."); }} />
    <Gallery visible={sheet === "gallery"} post={gallery} onClose={() => setSheet(null)} />
    <Policy visible={sheet === "policy"} onClose={() => setSheet(null)} />
    <ModalSheet visible={sheet === "account"} onClose={() => setSheet(null)} title="Tài khoản">{isAdmin(user) ? <Menu icon="shield-checkmark-outline" label="Trang quản trị" onPress={() => { setSheet(null); router.push("/admin"); }} /> : null}<Menu icon="person-outline" label="Trang cá nhân" onPress={() => { setSheet(null); router.push("/profile"); }} /><Menu icon="settings-outline" label="Thông tin cá nhân" onPress={() => { setSheet(null); router.push("/personal-info"); }} /><Menu icon="lock-closed-outline" label="Đổi mật khẩu" onPress={() => { setSheet(null); router.push("/change-password"); }} /><Menu icon="log-out-outline" label="Đăng xuất" danger onPress={performLogout} /></ModalSheet>
    <ModalSheet visible={sheet === "messages"} onClose={() => setSheet(null)} title="Tin nhắn"><ScrollView><ScreenState loading={sheetLoading} error={sheetError} empty={!sheetLoading && !sheetError && !chats.length ? "Chưa có cuộc trò chuyện." : ""} onRetry={openMessages} />{!sheetLoading && chats.map((room) => <Pressable key={room.id} style={styles.social} onPress={() => openChat(room)}><View style={styles.socialAvatar}><Ionicons name="person" size={19} color={colors.primary} /></View><View style={styles.grow}><Text style={styles.name}>{room.otherUser?.fullName || "Thành viên RoomMate"}</Text><Text numberOfLines={1} style={styles.muted}>{room.isRequest ? "Tin nhắn đang chờ" : room.lastMessage?.messageText || "Mở cuộc trò chuyện"}</Text></View>{room.unreadCount ? <Text style={styles.badge}>{room.unreadCount}</Text> : null}</Pressable>)}</ScrollView></ModalSheet>
    <NotificationSheet visible={sheet === "notifications"} onClose={() => setSheet(null)} notifications={notifications} tab={notificationTab} onTabChange={setNotificationTab} loading={sheetLoading} error={sheetError} onRetry={loadNotifications} onRespond={respond} updatingRequestId={updatingRequestId} respondedNotificationIds={respondedNotificationIds} />
    <ChoiceSheet visible={choice === "gender"} title="Giới tính" value={draftFilters.gender || "Tất cả"} options={["Tất cả", "Nam", "Nữ"]} onClose={() => setChoice(null)} onChange={(value) => setDraftFilters((current) => ({ ...current, gender: value === "Tất cả" ? "" : value }))} />
    <AiAssistant visible={sheet === "ai"} onOpen={() => setSheet("ai")} onClose={() => setSheet(null)} />
  </SafeAreaView>;
}

function Filter({ visible, form, setForm, onChoice, onClose, onApply, onReset }) { const set = (key) => (value) => setForm((current) => ({ ...current, [key]: value })); return <ModalSheet visible={visible} onClose={onClose} eyebrow="TÌM KIẾM PHÒNG" title="Chọn tiêu chí của bạn"><ScrollView keyboardShouldPersistTaps="handled"><Field label="Khu vực / địa chỉ"><TextField value={form.address} onChangeText={set("address")} placeholder="Ví dụ: Quận 1" /></Field><Field label="Diện tích (m²)"><TextField value={form.area} onChangeText={set("area")} keyboardType="decimal-pad" /></Field><View style={styles.two}><View style={styles.half}><Field label="Giá từ"><TextField value={form.priceFrom} onChangeText={set("priceFrom")} keyboardType="decimal-pad" /></Field></View><View style={styles.half}><Field label="Giá đến"><TextField value={form.priceTo} onChangeText={set("priceTo")} keyboardType="decimal-pad" /></Field></View></View><Field label="Số người hiện tại"><TextField value={form.currentMembers} onChangeText={set("currentMembers")} keyboardType="number-pad" /></Field><Field label="Giới tính"><SelectField value={form.gender || "Tất cả"} onPress={() => onChoice("gender")} /></Field><View style={styles.buttons}><SecondaryButton title="Xóa điều kiện" onPress={onReset} /><View style={styles.grow}><PrimaryButton title="Tìm kiếm phòng" onPress={onApply} /></View></View></ScrollView></ModalSheet>; }
function Field({ label, children }) { return <View style={styles.field}><FieldLabel>{label}</FieldLabel>{children}</View>; }
function Gallery({ visible, post, onClose }) { const images = post?.images || []; return <ModalSheet visible={visible} onClose={onClose} eyebrow="ẢNH PHÒNG" title={post?.title || "Thư viện ảnh"}><ScrollView>{images.map((item, index) => <Image key={item.id || item.imageUrl} source={{ uri: item.imageUrl }} style={styles.galleryImage} accessibilityLabel={`Ảnh phòng ${index + 1}`} />)}{!images.length ? <Text style={styles.muted}>Bài đăng chưa có ảnh.</Text> : null}</ScrollView></ModalSheet>; }
function Policy({ visible, onClose }) { return <ModalSheet visible={visible} onClose={onClose} eyebrow="ROOMMATE · AN TOÀN CỘNG ĐỒNG" title="Quy định đăng bài"><Text style={styles.policy}>01  Thông tin chính xác{`\n`}Giá, địa chỉ, sức chứa và các khoản phí phải rõ ràng, đúng thực tế.</Text><Text style={styles.policy}>02  Ảnh của phòng thực{`\n`}Không dùng ảnh sao chép hoặc gây nhầm lẫn. Tối đa 5 ảnh cho mỗi bài.</Text><Text style={styles.policy}>03  Tôn trọng và an toàn{`\n`}Không đăng nội dung phân biệt hoặc yêu cầu chuyển khoản trước.</Text><PrimaryButton title="Đã hiểu" onPress={onClose} /></ModalSheet>; }
function Menu({ icon, label, onPress, danger }) { return <Pressable onPress={onPress} style={styles.menu}><Ionicons name={icon} size={20} color={danger ? colors.danger : "#34445C"} /><Text style={[styles.menuText, danger && { color: colors.danger }]}>{label}</Text></Pressable>; }
function newest(items) { return [...items].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)); }

const receivedNotificationActions = {
  JOIN_REQUEST_SENT: "đã gửi yêu cầu vào phòng của bạn.",
  JOIN_REQUEST_RECEIVED: "đã gửi yêu cầu vào phòng của bạn.",
  JOIN_REQUEST_ACCEPTED: "đã chấp nhận yêu cầu vào phòng.",
  USER_LEFT: "đã rời khỏi phòng.",
  JOIN_REQUEST_REJECTED: "đã từ chối yêu cầu vào phòng.",
  USER_KICKED: "đã bị mời rời khỏi phòng.",
  USER_CANCLLED: "đã hủy yêu cầu vào phòng.",
};

const sentNotificationActions = {
  JOIN_REQUEST_SENT: "đã gửi yêu cầu vào phòng.",
  JOIN_REQUEST_RECEIVED: "đã nhận một yêu cầu vào phòng.",
  JOIN_REQUEST_ACCEPTED: "đã chấp nhận yêu cầu vào phòng.",
  USER_LEFT: "đã rời khỏi phòng.",
  JOIN_REQUEST_REJECTED: "đã từ chối yêu cầu vào phòng.",
  USER_KICKED: "đã mời một thành viên rời khỏi phòng.",
  USER_CANCLLED: "đã hủy yêu cầu vào phòng.",
};

function NotificationSheet({ visible, onClose, notifications, tab, onTabChange, loading, error, onRetry, onRespond, updatingRequestId, respondedNotificationIds }) {
  const received = notifications.received || [];
  const sent = notifications.sent || [];
  const items = tab === "received" ? received : sent;
  const resolvedRoomConnectionIds = new Set(sent.filter((item) => ["JOIN_REQUEST_ACCEPTED", "JOIN_REQUEST_REJECTED"].includes(item.type) && item.roomConnectionId != null).map((item) => item.roomConnectionId));

  return <ModalSheet visible={visible} onClose={onClose} title="Thông báo"><View style={styles.notificationTabs}><NotificationTab label={`Đã nhận (${received.length})`} active={tab === "received"} onPress={() => onTabChange("received")} /><NotificationTab label={`Đã gửi (${sent.length})`} active={tab === "sent"} onPress={() => onTabChange("sent")} /></View><ScrollView style={styles.notificationList} contentContainerStyle={styles.notificationListContent}><ScreenState loading={loading} error={error} empty={!loading && !error && !items.length ? "Chưa có thông báo." : ""} onRetry={onRetry} />{!loading && !error && items.map((item) => <NotificationItem key={`${tab}-${item.id}`} item={item} received={tab === "received"} canRespond={tab === "received" && ["JOIN_REQUEST_SENT", "JOIN_REQUEST_RECEIVED"].includes(item.type) && item.roomConnectionId != null && !respondedNotificationIds.includes(item.id) && !resolvedRoomConnectionIds.has(item.roomConnectionId)} updating={updatingRequestId === item.id} onRespond={onRespond} onClose={onClose} />)}</ScrollView></ModalSheet>;
}

function NotificationTab({ label, active, onPress }) {
  return <Pressable onPress={onPress} style={[styles.notificationTab, active && styles.notificationTabActive]}><Text style={[styles.notificationTabText, active && styles.notificationTabTextActive]}>{label}</Text></Pressable>;
}

function NotificationItem({ item, received, canRespond, updating, onRespond, onClose }) {
  const personName = (received ? item.senderName : item.receiverName) || item.senderName || "Thành viên RoomMate";
  const personId = received ? item.senderId : (item.receiverId ?? item.senderId);
  const action = received ? receivedNotificationActions[item.type] : sentNotificationActions[item.type];
  const openPerson = personId == null ? undefined : () => {
    onClose();
    router.push({ pathname: "/users/[userId]", params: { userId: String(personId), name: personName } });
  };
  const showRequestSender = !received && ["JOIN_REQUEST_ACCEPTED", "JOIN_REQUEST_REJECTED"].includes(item.type);

  return <View style={styles.notification}><View style={styles.socialAvatar}><Ionicons name="person" size={19} color={colors.primary} /></View><View style={styles.grow}><Text style={styles.notificationCopy}>{received ? <><Text style={styles.notificationPerson} onPress={openPerson}>{personName}</Text>{` ${action || "đã cập nhật một yêu cầu vào phòng."}`}</> : <><Text style={styles.notificationPerson}>Bạn</Text>{` ${(action || "đã cập nhật một yêu cầu vào phòng.").replace(/\.$/, "")}`}{showRequestSender ? <> của <Text style={styles.notificationPerson} onPress={openPerson}>{personName}</Text>.</> : "."}</>}{item.postId != null ? ` · Bài đăng #${item.postId}` : ""}</Text>{formatNotificationTime(item.createdAt) ? <Text style={styles.notificationTime}>{formatNotificationTime(item.createdAt)}</Text> : null}{canRespond ? <View style={styles.responseButtons}><Pressable disabled={updating} style={[styles.accept, updating && styles.responseDisabled]} onPress={() => onRespond(item, "JOINED")}><Text style={styles.acceptText}>{updating ? "Đang xử lý..." : "Đồng ý"}</Text></Pressable><Pressable disabled={updating} style={[styles.reject, updating && styles.responseDisabled]} onPress={() => onRespond(item, "REJECTED")}><Text style={styles.rejectText}>Từ chối</Text></Pressable></View> : null}</View>{!item.read ? <View style={styles.notificationDot} /> : null}</View>;
}

function formatNotificationTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const now = new Date();
  const time = `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
  const isToday = date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate();
  return isToday ? `Hôm nay, ${time}` : `${time} ${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function AiAssistant({ visible, onOpen, onClose }) { return <><Pressable style={aiStyles.fab} onPress={onOpen} accessibilityLabel="Mở trợ lý AI"><Text style={aiStyles.star}>✦</Text></Pressable><ModalSheet visible={visible} onClose={onClose} title="RoomMate AI"><Text style={aiStyles.subtitle}>Mình có thể giúp bạn tìm phòng.</Text><View style={aiStyles.bubble}><Text style={aiStyles.bubbleText}>Chào bạn! Bạn muốn tìm phòng ở khu vực nào?</Text></View><TextInput style={aiStyles.input} placeholder="Nhập câu hỏi của bạn..." placeholderTextColor="#92A0B4" /></ModalSheet></>; }

const aiStyles = StyleSheet.create({ fab: { position: "absolute", right: 20, bottom: 22, width: 56, height: 56, borderRadius: 28, backgroundColor: "#172B56", alignItems: "center", justifyContent: "center", elevation: 7 }, star: { color: "#9FD6FF", fontSize: 25 }, subtitle: { color: colors.muted, fontSize: 13 }, bubble: { marginTop: 15, backgroundColor: "#EDF4FF", padding: 12, borderRadius: 12 }, bubbleText: { color: colors.text, lineHeight: 20 }, input: { marginTop: 14, minHeight: 47, borderWidth: 1, borderColor: "#D7E1F0", borderRadius: 10, paddingHorizontal: 12, color: colors.text } });

const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: colors.background }, list: { padding: 16, paddingBottom: 50, flexGrow: 1 }, grow: { flex: 1 }, composer: { backgroundColor: "white", borderWidth: 1, borderColor: colors.border, borderRadius: 18, padding: 13, flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 26, ...shadow }, composerAvatar: { width: 42, height: 42, borderRadius: 21, backgroundColor: colors.softBlue, alignItems: "center", justifyContent: "center" }, composerText: { flex: 1, backgroundColor: colors.softGray, color: "#758198", borderRadius: 22, paddingHorizontal: 15, paddingVertical: 12 }, heading: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-end", marginBottom: 13, gap: 10 }, eyebrow: { color: "#78869A", fontSize: 10, fontWeight: "800", letterSpacing: 1.4 }, title: { color: colors.text, fontSize: 23, fontWeight: "900" }, filter: { flexDirection: "row", alignItems: "center", gap: 5, borderWidth: 1, borderColor: "#D8E1EF", borderRadius: 10, minHeight: 42, paddingHorizontal: 10, backgroundColor: "white" }, filterText: { color: "#416184", fontWeight: "800", fontSize: 13 }, field: { marginBottom: 14 }, two: { flexDirection: "row", gap: 10 }, half: { flex: 1 }, buttons: { flexDirection: "row", gap: 7, alignItems: "center", marginTop: 7, paddingBottom: 8 }, galleryImage: { width: "100%", height: 220, borderRadius: 12, marginBottom: 10, resizeMode: "cover" }, policy: { borderTopWidth: 1, borderColor: "#EDF0F4", paddingVertical: 12, color: "#526174", lineHeight: 20 }, menu: { minHeight: 54, flexDirection: "row", alignItems: "center", gap: 11, borderTopWidth: 1, borderColor: "#EDF0F4" }, menuText: { fontSize: 15, fontWeight: "700", color: "#34445C" }, social: { minHeight: 68, flexDirection: "row", alignItems: "center", gap: 10, borderTopWidth: 1, borderColor: "#EDF0F4" }, socialAvatar: { width: 42, height: 42, borderRadius: 21, backgroundColor: colors.softBlue, alignItems: "center", justifyContent: "center" }, name: { fontWeight: "800", color: "#28364C" }, muted: { color: colors.muted, fontSize: 13, marginTop: 3, lineHeight: 18 }, badge: { minWidth: 24, padding: 5, borderRadius: 12, backgroundColor: colors.primary, color: "white", textAlign: "center", fontWeight: "800", fontSize: 11 }, notificationTabs: { flexDirection: "row", borderBottomWidth: 1, borderColor: "#E6EBF2", marginBottom: 2 }, notificationTab: { paddingHorizontal: 10, paddingVertical: 10, borderBottomWidth: 2, borderBottomColor: "transparent" }, notificationTabActive: { borderBottomColor: colors.primary }, notificationTabText: { color: "#718097", fontSize: 12, fontWeight: "800" }, notificationTabTextActive: { color: colors.primary }, notificationList: { maxHeight: 420 }, notificationListContent: { paddingBottom: 8 }, notification: { minHeight: 82, paddingVertical: 12, borderTopWidth: 1, borderColor: "#EDF0F4", flexDirection: "row", alignItems: "flex-start", gap: 10 }, notificationCopy: { color: "#516178", fontSize: 13, lineHeight: 20, paddingRight: 9 }, notificationPerson: { color: colors.primary, fontWeight: "900" }, notificationTime: { color: "#8A96A8", fontSize: 11, marginTop: 3 }, notificationDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: "#E54954", marginTop: 6 }, responseButtons: { flexDirection: "row", gap: 8, marginTop: 10 }, accept: { minHeight: 36, backgroundColor: colors.primary, borderRadius: 9, paddingHorizontal: 12, alignItems: "center", justifyContent: "center" }, acceptText: { color: "white", fontWeight: "800", fontSize: 12 }, reject: { minHeight: 36, borderWidth: 1, borderColor: "#D7E0EC", borderRadius: 9, paddingHorizontal: 12, alignItems: "center", justifyContent: "center" }, rejectText: { color: "#60718A", fontWeight: "800", fontSize: 12 }, responseDisabled: { opacity: 0.6 } });

import Ionicons from "@expo/vector-icons/Ionicons";
import {
  Image,
  Linking,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { colors, shadow } from "../constants/theme";

export function PostCard({
  post,
  onGallery,
  onJoin,
  onMessage,
  onAuthor,
  joining,
}) {
  const photos = (post.images || [])
    .map((item) => item.imageUrl)
    .filter(Boolean);
  const openMap = async () => {
    const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(post.address || "")}`;
    if (await Linking.canOpenURL(url)) await Linking.openURL(url);
  };
  return (
    <View style={styles.card}>
      <View style={styles.stripe} />
      <View style={styles.authorRow}>
        <Pressable style={styles.author} onPress={onAuthor}>
          <View style={styles.avatar}>
            <Ionicons name="person" size={19} color={colors.primary} />
          </View>
          <View style={styles.authorInfo}>
            <Text numberOfLines={1} style={styles.name}>
              {post.authorName || "Thành viên RoomMate"}
            </Text>
            <Text style={styles.time}>Bài đăng đang hiển thị</Text>
          </View>
        </Pressable>
      </View>
      <Text numberOfLines={3} style={styles.title}>
        {post.title}
      </Text>
      {photos.length ? (
        <Pressable style={styles.photos} onPress={onGallery}>
          <Image source={{ uri: photos[0] }} style={styles.photo} />
          {photos[1] ? (
            <Image source={{ uri: photos[1] }} style={styles.photo} />
          ) : null}
          {photos.length > 2 ? (
            <View style={styles.count}>
              <Text style={styles.countText}>+{photos.length - 2}</Text>
            </View>
          ) : null}
        </Pressable>
      ) : (
        <View style={styles.noPhoto}>
          <Ionicons name="image-outline" size={25} color="#91A0B5" />
          <Text style={styles.noPhotoText}>Chưa có ảnh</Text>
        </View>
      )}
      <Pressable style={styles.address} onPress={openMap}>
        <Ionicons name="location-outline" size={16} color={colors.primary} />
        <Text style={styles.addressText} numberOfLines={2}>
          {post.address}
        </Text>
        <Ionicons name="open-outline" size={14} color={colors.primary} />
      </Pressable>
      <Text style={styles.price}>{formatPrice(post.price)}</Text>
      <View style={styles.tags}>
        <Text style={styles.tag}>▧ {post.area || 0} m²</Text>
        <Text style={styles.tag}>
          ♙ {post.currentMembers || 0}/{post.maxMembers || 0}
        </Text>
        {post.gender ? <Text style={styles.tag}>◉ {post.gender}</Text> : null}
      </View>
      <Text style={styles.description}>
        {post.description || "Thông tin tiện ích đang được cập nhật."}
      </Text>
      <View style={styles.actions}>
        <Pressable
          disabled={joining}
          onPress={onJoin}
          style={({ pressed }) => [
            styles.request,
            pressed && styles.pressed,
            joining && styles.disabled,
          ]}
        >
          <Text style={styles.requestText}>
            {joining ? "Đang gửi..." : "Yêu cầu vào phòng"}
          </Text>
        </Pressable>
        <Pressable onPress={onMessage} style={styles.message}>
          <Ionicons name="chatbubble-outline" size={17} color="#3567AA" />
        </Pressable>
      </View>
    </View>
  );
}
export function formatPrice(value) {
  return `${new Intl.NumberFormat("vi-VN").format(Number(value) || 0)}đ/tháng`;
}
const styles = StyleSheet.create({
  card: {
    overflow: "hidden",
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 17,
    padding: 15,
    marginBottom: 12,
    ...shadow,
  },
  stripe: {
    position: "absolute",
    left: 0,
    top: 0,
    right: 0,
    height: 4,
    backgroundColor: colors.primary,
  },
  authorRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  author: {
    flex: 1,
    minWidth: 0,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
  },
  avatar: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: colors.softBlue,
    alignItems: "center",
    justifyContent: "center",
  },
  authorInfo: { flex: 1 },
  name: { color: "#1E2A40", fontSize: 13, fontWeight: "800" },
  time: { color: "#7A8799", fontSize: 11, marginTop: 2 },
  title: {
    marginTop: 14,
    color: "#1D293D",
    fontSize: 17,
    lineHeight: 22,
    fontWeight: "900",
    letterSpacing: -0.2,
  },
  photos: {
    height: 118,
    marginTop: 12,
    overflow: "hidden",
    borderRadius: 10,
    gap: 3,
    flexDirection: "row",
    backgroundColor: "#EAF0F8",
  },
  photo: { flex: 1, height: "100%", resizeMode: "cover" },
  count: {
    position: "absolute",
    right: 7,
    bottom: 7,
    borderRadius: 7,
    backgroundColor: "rgba(19,31,49,0.78)",
    paddingHorizontal: 7,
    paddingVertical: 3,
  },
  countText: { color: "white", fontWeight: "800", fontSize: 11 },
  noPhoto: {
    height: 90,
    borderRadius: 10,
    backgroundColor: "#EDF1F6",
    alignItems: "center",
    justifyContent: "center",
    marginTop: 12,
  },
  noPhotoText: { color: colors.muted, fontSize: 11, marginTop: 3 },
  address: {
    minHeight: 45,
    marginTop: 10,
    borderRadius: 9,
    backgroundColor: "#F3F6FA",
    paddingHorizontal: 9,
    paddingVertical: 7,
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
  },
  addressText: {
    color: "#4D5F78",
    flex: 1,
    fontSize: 12,
    lineHeight: 16,
    fontWeight: "700",
  },
  price: {
    color: "#125BE9",
    fontSize: 22,
    fontWeight: "900",
    letterSpacing: -1,
    marginTop: 12,
  },
  tags: { flexDirection: "row", flexWrap: "wrap", gap: 5, marginTop: 7 },
  tag: {
    backgroundColor: "#EDF3FF",
    borderRadius: 6,
    color: "#506784",
    paddingHorizontal: 7,
    paddingVertical: 5,
    fontSize: 11,
    fontWeight: "800",
  },
  description: {
    color: "#586C87",
    fontSize: 12,
    lineHeight: 18,
    fontWeight: "600",
    marginTop: 8,
    borderTopWidth: 1,
    borderColor: "#EDF0F4",
    paddingTop: 8,
  },
  actions: { flexDirection: "row", gap: 7, marginTop: 10 },
  request: {
    flex: 1,
    minHeight: 42,
    borderRadius: 9,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.primary,
  },
  requestText: { color: "white", fontSize: 12, fontWeight: "800" },
  message: {
    width: 44,
    minHeight: 42,
    borderRadius: 9,
    borderWidth: 1,
    borderColor: "#CFDAED",
    alignItems: "center",
    justifyContent: "center",
  },
  pressed: { opacity: 0.75 },
  disabled: { opacity: 0.5 },
});

import { api } from "./api";

export async function getChatRooms() {
  const response = await api.get("/chat-rooms");
  return Array.isArray(response.data) ? response.data : [];
}

export async function getMessageRequests() {
  const response = await api.get("/chat-rooms/requests");
  return Array.isArray(response.data) ? response.data : [];
}

export async function getMessages(roomId, page = 0, size = 50) {
  const response = await api.get(`/chat-rooms/${roomId}/messages`, { params: { page, size } });
  if (Array.isArray(response.data)) return response.data;
  return Array.isArray(response.data?.content) ? response.data.content : [];
}

export async function acceptChatRoom(roomId) {
  const response = await api.patch(`/chat-rooms/${roomId}/accept`);
  return response.data;
}

export async function blockChatRoom(roomId) {
  const response = await api.patch(`/chat-rooms/${roomId}/block`);
  return response.data;
}

export async function markAsRead(roomId, lastReadMessageId) {
  const response = await api.patch(`/chat-rooms/${roomId}/read`, { lastReadMessageId });
  return response.data;
}

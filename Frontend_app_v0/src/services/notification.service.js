import { api } from "./api";

export async function getReceivedNotifications() {
  const response = await api.get("/Received_notifications");
  return Array.isArray(response.data) ? response.data : [];
}

export async function getSentNotifications() {
  const response = await api.get("/Sent_notifications");
  return Array.isArray(response.data) ? response.data : [];
}

export async function updateJoinRequest(roomConnectionId, status) {
  const response = await api.patch("/request_join", { roomConnectionId: roomConnectionId, status });
  return response.data;
}

export async function cancelJoinRequest(requestId) {
  const response = await api.delete(`/request_join/${requestId}`);
  return response.data;
}

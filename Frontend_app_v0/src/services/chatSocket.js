import { getApiBaseUrl } from "./api";
import { getAccessToken } from "../storage/secureTokenStore";

function getWebSocketUrl() {
  return getApiBaseUrl().replace(/^http/i, "ws").replace(/\/api\/?$/i, "/ws");
}

function safeHeader(value) {
  return String(value).replace(/[\r\n]/g, "");
}

function frame(command, headers = {}, body = "") {
  const lines = Object.entries(headers).map(([key, value]) => `${key}:${safeHeader(value)}`);
  return `${command}\n${lines.join("\n")}\n\n${body}\0`;
}

function parseFrame(raw) {
  const value = raw.replace(/^\n+/, "");
  const splitAt = value.indexOf("\n\n");
  if (splitAt < 0) return null;
  const headerLines = value.slice(0, splitAt).split("\n");
  const command = headerLines.shift()?.trim();
  const headers = {};
  headerLines.forEach((line) => {
    const index = line.indexOf(":");
    if (index > -1) headers[line.slice(0, index)] = line.slice(index + 1);
  });
  return { command, headers, body: value.slice(splitAt + 2) };
}

export function createChatSocket({ onEvent, onStatus, onError }) {
  let socket = null;
  let connected = false;
  let buffer = "";
  let manuallyClosed = false;
  const subscriptions = new Set();
  const queuedSubscriptions = new Set();

  const sendFrame = (command, headers, body = "") => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return false;
    socket.send(frame(command, headers, body));
    return true;
  };

  const subscribe = (destination, id) => sendFrame("SUBSCRIBE", { id, destination, ack: "auto" });

  const subscribeToRoom = (roomId) => {
    if (!roomId || subscriptions.has(roomId)) return;
    if (!connected) {
      queuedSubscriptions.add(roomId);
      return;
    }
    if (subscribe(`/topic/chat-rooms/${roomId}`, `room-${roomId}`)) subscriptions.add(roomId);
  };

  const handle = (parsed) => {
    if (parsed.command === "CONNECTED") {
      connected = true;
      onStatus?.("connected");
      subscribe("/user/queue/chat-rooms", "user-chat-events");
      [...queuedSubscriptions].forEach(subscribeToRoom);
      queuedSubscriptions.clear();
      return;
    }
    if (parsed.command === "ERROR") {
      onStatus?.("error");
      onError?.("Kết nối trò chuyện bị máy chủ từ chối.");
      return;
    }
    if (parsed.command !== "MESSAGE") return;
    try {
      onEvent?.(JSON.parse(parsed.body));
    } catch {
      // Bỏ qua frame broker không phải JSON; không ghi nội dung tin nhắn ra log.
    }
  };

  return {
    async connect() {
      const token = await getAccessToken();
      if (!token) {
        onError?.("Phiên đăng nhập không hợp lệ.");
        return;
      }
      manuallyClosed = false;
      onStatus?.("connecting");
      socket = new WebSocket(getWebSocketUrl(), ["v12.stomp"]);
      socket.onopen = () => sendFrame("CONNECT", {
        "accept-version": "1.2",
        "heart-beat": "0,0",
        Authorization: `Bearer ${token}`,
      });
      socket.onmessage = (event) => {
        buffer += String(event.data);
        const frames = buffer.split("\0");
        buffer = frames.pop() || "";
        frames.map(parseFrame).filter(Boolean).forEach(handle);
      };
      socket.onerror = () => {
        onStatus?.("error");
        onError?.("Không thể kết nối trò chuyện.");
      };
      socket.onclose = () => {
        connected = false;
        if (!manuallyClosed) onStatus?.("disconnected");
      };
    },
    disconnect() {
      manuallyClosed = true;
      if (socket) {
        sendFrame("DISCONNECT", { receipt: `close-${Date.now()}` });
        socket.close();
      }
      socket = null;
      connected = false;
    },
    subscribeToRoom,
    sendDirectMessage(receiverId, messageText) {
      return sendFrame("SEND", { destination: "/app/chat.direct", "content-type": "application/json" }, JSON.stringify({ receiverId, messageText, messageType: "TEXT" }));
    },
    sendRoomMessage(roomId, messageText) {
      return sendFrame("SEND", { destination: "/app/chat.send", "content-type": "application/json" }, JSON.stringify({ roomId, messageText, messageType: "TEXT" }));
    },
  };
}

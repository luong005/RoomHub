import { api } from "./api";

function cleanParams(filters = {}) {
  const params = { address: filters.address, authorId: filters.authorId, priceFrom: filters.priceFrom, priceTo: filters.priceTo, currentMember: filters.currentMembers, area: filters.area, gender: filters.gender };
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined));
}

function nativeImage(asset, index) {
  const extension = asset.fileName?.split(".").pop()?.toLowerCase();
  const type = asset.mimeType?.startsWith("image/") ? asset.mimeType : extension === "png" ? "image/png" : "image/jpeg";
  return { uri: asset.uri, name: (asset.fileName || `room-${Date.now()}-${index}.${extension || "jpg"}`).replace(/[^a-zA-Z0-9._-]/g, "_"), type };
}

function createPostForm(post, images = []) {
  const form = new FormData();
  form.append("post", JSON.stringify(post));
  images.forEach((asset, index) => form.append("images", nativeImage(asset, index)));
  return form;
}

export async function getPosts(filters = {}) { const response = await api.get("/posts", { params: cleanParams(filters) }); return Array.isArray(response.data) ? response.data : []; }
export async function getPost(postId) { const response = await api.get(`/posts/${postId}`); return response.data; }
export async function getMyPosts(status) { const response = await api.get("/my-posts", { params: { status } }); return Array.isArray(response.data) ? response.data : []; }
export async function getJoinedPosts() { const response = await api.get("/user_joined"); return Array.isArray(response.data) ? response.data : []; }
export async function getJoinedRoomMembers(postId) { const response = await api.get(`/posts/${postId}/members`); return Array.isArray(response.data) ? response.data : []; }
export async function createPost(post, images) { const response = await api.post("/posts", createPostForm(post, images)); return response.data; }
export async function updatePost(postId, post, images = []) { const response = await api.patch(`/posts/${postId}`, createPostForm(post, images)); return response.data; }
export async function deletePost(postId) { const response = await api.delete(`/post/${postId}`); return response.data; }
export async function requestJoin(postId, message = "") { const response = await api.post("/request_join", { postId, message }); return response.data; }

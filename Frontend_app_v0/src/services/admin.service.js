import { api } from "./api";

export async function getUsers(page = 0, size = 10) {
  const response = await api.get("/admin/users", { params: { page, size } });
  const data = response.data || {};
  const content = Array.isArray(data.content) ? data.content : [];
  return {
    content,
    totalElements: Number.isFinite(Number(data.totalElements)) ? Number(data.totalElements) : content.length,
    totalPages: Number.isFinite(Number(data.totalPages)) ? Number(data.totalPages) : 0,
    number: Number.isFinite(Number(data.number)) ? Number(data.number) : page,
  };
}

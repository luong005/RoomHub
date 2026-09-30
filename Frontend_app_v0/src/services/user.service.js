import { api } from "./api";
import { getProfile } from "./auth.service";

export { getProfile };

export async function updateInfo(profile) {
  await api.patch("/update-info", profile);
  return getProfile();
}

export async function updatePassword({ phoneNumber, oldPassword, newPassword }) {
  const response = await api.post("/update-password", {
    phone_number: phoneNumber,
    oldPassword,
    newPassword,
  });
  return response.data;
}

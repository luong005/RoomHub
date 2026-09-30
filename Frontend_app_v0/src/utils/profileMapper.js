export function mapProfile(profile = {}, tokenClaims = {}) {
  return {
    ...profile,
    id: profile.id ?? tokenClaims.id ?? null,
    fullName: profile.fullName ?? profile.fullname ?? "",
    phoneNumber: profile.phoneNumber ?? profile.phone_number ?? "",
    yearOfBirth: profile.yearOfBirth ?? "",
    currentAddress: profile.currentAddress ?? "",
    role: profile.role
      ?? profile.roleName
      ?? tokenClaims.role
      ?? (Array.isArray(tokenClaims.roles) ? tokenClaims.roles[0] : tokenClaims.roles)
      ?? (Array.isArray(tokenClaims.authorities) ? tokenClaims.authorities[0] : tokenClaims.authorities)
      ?? "",
  };
}

export function toUpdateInfoPayload(form) {
  return {
    fullName: form.fullName.trim(),
    hometown: form.hometown.trim(),
    currentAddress: form.currentAddress.trim(),
    yearOfBirth: Number(form.yearOfBirth),
    gender: form.gender,
    study: form.study,
    bio: form.bio.trim(),
  };
}

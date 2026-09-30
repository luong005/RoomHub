export function normalizeRole(value) {
  const role = value && typeof value === "object"
    ? value.name ?? value.authority ?? value.role ?? value.roleName
    : value;
  return String(role || "").trim().replace(/^ROLE_/i, "").toUpperCase();
}

export function isAdmin(user) {
  const roleId = user?.role_id ?? user?.roleId;
  if (Number(roleId) === 1) return true;

  const roles = [
    user?.role,
    user?.roleName,
    ...(Array.isArray(user?.roles) ? user.roles : []),
    ...(Array.isArray(user?.authorities) ? user.authorities : []),
  ];
  return roles.some((role) => normalizeRole(role) === "ADMIN");
}

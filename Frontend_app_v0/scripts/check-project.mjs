import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const src = path.join(root, "src");
const failures = [];
const sourceFiles = walk(src).filter((file) => /\.(js|jsx|ts|tsx)$/.test(file));
const forbidden = [
  [/<\/?(?:div|main|section|button|input|textarea|img|span|p|h[1-6])\b/i, "thẻ HTML"],
  [/\b(?:window|document|localStorage)\b/, "API trình duyệt"],
  [/react-native-web|react-router(?:-dom)?/, "thư viện web"],
];

for (const file of sourceFiles) {
  const relative = path.relative(root, file);
  if (/\.(ts|tsx)$/.test(file)) failures.push(`${relative}: dự án chỉ dùng JavaScript/JSX`);
  const content = fs.readFileSync(file, "utf8");
  for (const [pattern, label] of forbidden) if (pattern.test(content)) failures.push(`${relative}: còn ${label}`);
  if (path.basename(file) !== "api.js" && /from\s+["']axios["']/.test(content)) failures.push(`${relative}: Axios phải đi qua services/api.js`);
  for (const match of content.matchAll(/from\s+["'](\.[^"']+)["']/g)) {
    if (!resolveImport(path.dirname(file), match[1])) failures.push(`${relative}: không tìm thấy import ${match[1]}`);
  }
}

JSON.parse(fs.readFileSync(path.join(root, "app.json"), "utf8"));
JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log(`Kiểm tra đạt: ${sourceFiles.length} file JS/JSX, import và quy tắc React Native hợp lệ.`);

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(full) : [full];
  });
}

function resolveImport(directory, request) {
  const target = path.resolve(directory, request);
  return [target, `${target}.js`, `${target}.jsx`, `${target}.json`, path.join(target, "index.js"), path.join(target, "index.jsx")].some(fs.existsSync);
}

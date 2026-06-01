import { existsSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webuiRoot = path.resolve(__dirname, "..");
const textureRoot = path.join(webuiRoot, "public", "images", "texture");
const limitBytes = 1024 * 1024;
const imageExtensions = new Set([".png", ".jpg", ".jpeg", ".webp", ".gif"]);
const legacyOversizedImages = new Map([
  ["public/images/texture/characters/dragon.png", 2984495],
  ["public/images/texture/characters/meal.png", 2393752],
  ["public/images/texture/characters/robin.png", 2821346],
  ["public/images/texture/characters/sim.png", 2495096],
  ["public/images/texture/characters/sniper.png", 2913747],
  ["public/images/texture/skills/chips.png", 2944410],
  ["public/images/texture/skills/elf.png", 2421500],
  ["public/images/texture/skills/hands.png", 2986919],
  ["public/images/texture/skills/light.png", 2584155],
  ["public/images/texture/skills/sanctuary.png", 2141861],
  ["public/images/texture/skills/soda.png", 2266015],
  ["public/images/texture/skills/umbrella.png", 2395244]
]);

function listAllImages(dir) {
  if (!existsSync(dir)) {
    return [];
  }

  const results = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...listAllImages(fullPath));
      continue;
    }
    if (imageExtensions.has(path.extname(entry.name).toLowerCase())) {
      results.push(fullPath);
    }
  }
  return results;
}

const imagesToCheck = listAllImages(textureRoot);
const legacyWarnings = [];
const violations = imagesToCheck
  .map(filePath => {
    const size = statSync(filePath).size;
    const relativePath = path.relative(webuiRoot, filePath).replaceAll(path.sep, "/");
    return {
      filePath,
      relativePath,
      size
    };
  })
  .filter(item => {
    if (item.size <= limitBytes) {
      return false;
    }

    const legacySize = legacyOversizedImages.get(item.relativePath);
    if (legacySize === item.size) {
      legacyWarnings.push(item);
      return false;
    }

    return true;
  });

if (violations.length > 0) {
  console.error("卡牌图片大小超出 1MB，请压缩后再启动前端或提交代码：");
  for (const item of violations) {
    const sizeInMb = (item.size / 1024 / 1024).toFixed(2);
    console.error(`- ${item.relativePath} (${sizeInMb} MB)`);
  }
  process.exit(1);
}

console.log(`卡牌图片大小校验通过，共检查 ${imagesToCheck.length} 个文件。`);

if (legacyWarnings.length > 0) {
  console.warn(`当前仍有 ${legacyWarnings.length} 张历史卡图超过 1MB，但未改动时允许保留；一旦替换，请压缩到 1MB 内。`);
}

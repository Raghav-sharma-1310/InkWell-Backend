const fs = require("fs");
const path = require("path");

const root = process.cwd();
const serviceDirs = [
  "admin-server",
  "api-gateway",
  "auth-service",
  "category-service",
  "comment-service",
  "discovery-service",
  "media-service",
  "newsletter-service",
  "notification-service",
  "payment-service",
  "post-service",
];

const frontendRoots = [
  "frontend-web/src",
  "frontend-web/tests",
  "frontend-web/index.html",
  "frontend-web/vite.config.js",
  "frontend-web/tailwind.config.js",
  "frontend-web/postcss.config.js",
  "frontend-web/seed.cjs",
];

const ignoredParts = new Set([
  "target",
  "node_modules",
  "dist",
  "coverage",
  ".scannerwork",
  "uploads",
  ".git",
]);

const codeExtensions = new Set([".java", ".js", ".jsx", ".cjs", ".css", ".html"]);

function walk(entry, output = []) {
  const full = path.join(root, entry);
  if (!fs.existsSync(full)) {
    return output;
  }
  const stat = fs.statSync(full);
  if (stat.isFile()) {
    if (codeExtensions.has(path.extname(full))) {
      output.push(full);
    }
    return output;
  }
  for (const child of fs.readdirSync(full)) {
    if (ignoredParts.has(child)) {
      continue;
    }
    walk(path.join(entry, child), output);
  }
  return output;
}

function relative(file) {
  return path.relative(root, file).replace(/\\/g, "/");
}

function wordsFromName(name) {
  return name
    .replace(/\.[^.]+$/, "")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function javaRole(rel) {
  if (rel.includes("/controller/")) return "HTTP controller endpoints";
  if (rel.includes("/service/")) return "business workflow and validation logic";
  if (rel.includes("/repository/")) return "database access contracts";
  if (rel.includes("/entity/")) return "persistent domain data";
  if (rel.includes("/dto/")) return "request and response data shapes";
  if (rel.includes("/security/")) return "authentication and authorization support";
  if (rel.includes("/config/")) return "Spring Boot configuration";
  if (rel.includes("/exception/")) return "application error handling";
  if (rel.includes("/client/")) return "cross-service client communication";
  if (rel.includes("/util/")) return "shared helper behavior";
  if (rel.includes("/test/")) return "automated verification";
  return "application startup and module wiring";
}

function frontendRole(rel) {
  if (rel.includes("/pages/")) return "route-level UI and page state";
  if (rel.includes("/components/")) return "reusable UI behavior";
  if (rel.includes("/context/")) return "shared React state";
  if (rel.includes("/api/")) return "frontend API communication";
  if (rel.includes("/utils/")) return "shared frontend helpers";
  if (rel.includes("/__tests__/") || rel.includes("/tests/")) return "frontend test coverage";
  if (rel.endsWith(".css")) return "global styling rules";
  if (rel.endsWith(".html")) return "the Vite HTML shell";
  return "frontend application configuration and wiring";
}

function lineCommentForIdentifier(name, rel = "") {
  const readable = wordsFromName(name);
  if (!readable) return "Keeps this block self-contained so callers can reuse it safely.";
  const isTestFile = /(__tests__|\.test\.|\/tests\/|\\tests\\)/i.test(rel);
  if (isTestFile && /test|should|renders|allows|blocks|shows|returns|displays|redirects|validates|uploads|removes|toggles|opens|closes/i.test(name)) {
    return `Verifies ${readable} so regressions are caught during automated tests.`;
  }
  if (/controller|route|endpoint/i.test(name)) {
    return `Handles ${readable} requests so the UI can call this feature through a stable endpoint.`;
  }
  if (/service|process|sync|create|update|delete|save|fetch|load|get|find|list|mark|send|verify|login|register|refresh/i.test(name)) {
    return `Performs the ${readable} workflow so callers do not duplicate this logic.`;
  }
  if (/config|bean|filter|security|jwt|auth/i.test(name)) {
    return `Provides ${readable} wiring so the framework can apply the expected runtime behavior.`;
  }
  return `Defines ${readable} so related behavior stays grouped in one place.`;
}

function hasCommentNearby(lines, index) {
  for (let i = Math.max(0, index - 3); i < index; i += 1) {
    const trimmed = lines[i].trim();
    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("/**")) {
      return true;
    }
  }
  return false;
}

function addJavaComments(text, rel) {
  if (text.includes("Codex documentation pass")) return text;
  const role = javaRole(rel);
  let updated = `/*\n * This source file contains ${role} for the Inkwell platform.\n * The comments explain what each class or method is responsible for and why it exists in this service.\n */\n${text}`;
  const lines = updated.split(/\r?\n/);
  const result = [];
  const declaration = /^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)/;
  const method = /^\s*(public|private|protected)\s+(static\s+)?(final\s+)?(?:<[^>]+>\s*)?([A-Za-z_][A-Za-z0-9_<>, ?\[\].]*\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\([^;]*\)\s*(throws\s+[^{]+)?\{/;
  const annotationOrKeyword = /^\s*(if|for|while|switch|catch|try|return|new)\b/;

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    const classMatch = line.match(declaration);
    const methodMatch = line.match(method);
    if (classMatch && !hasCommentNearby(lines, i)) {
      const indent = line.match(/^\s*/)[0];
      result.push(`${indent}/* This ${classMatch[4]} groups ${wordsFromName(classMatch[5])} behavior so the module keeps a clear responsibility. */`);
    } else if (methodMatch && !annotationOrKeyword.test(line) && !hasCommentNearby(lines, i)) {
      const indent = line.match(/^\s*/)[0];
      result.push(`${indent}// ${lineCommentForIdentifier(methodMatch[5], rel)}`);
    }
    result.push(line);
  }
  return result.join("\n");
}

function addJsComments(text, rel) {
  if (text.includes("Codex documentation pass")) return text;
  const role = frontendRole(rel);
  let updated = `/*\n * This file provides ${role} for the Inkwell frontend.\n * The comments explain what major functions, components, and helpers do and why they are used.\n */\n${text}`;
  const lines = updated.split(/\r?\n/);
  const result = [];
  const patterns = [
    /^\s*(export\s+default\s+)?function\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/,
    /^\s*(export\s+)?(const|let|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(async\s*)?(\([^)]*\)|[A-Za-z_][A-Za-z0-9_]*)\s*=>/,
    /^\s*(export\s+)?(const|let|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*function\s*\(/,
    /^\s*(describe|it|test|beforeEach|afterEach)\s*\(\s*["'`]([^"'`]+)["'`]/,
  ];
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    let name = null;
    for (const pattern of patterns) {
      const match = line.match(pattern);
      if (match) {
        name = match[2] || match[3] || match[1];
        break;
      }
    }
    if (name && !hasCommentNearby(lines, i)) {
      const indent = line.match(/^\s*/)[0];
      result.push(`${indent}// ${lineCommentForIdentifier(name, rel)}`);
    }
    result.push(line);
  }
  return result.join("\n");
}

function addCssComments(text, rel) {
  if (text.includes("Codex documentation pass")) return text;
  return `/*\n * This stylesheet defines shared visual rules for ${relative(path.join(root, rel))}.\n * These comments identify why the styles are grouped here instead of repeated inside components.\n */\n/* Base styles keep typography, layout, and theme behavior consistent across the frontend. */\n${text}`;
}

function addHtmlComments(text) {
  if (text.includes("Codex documentation pass")) return text;
  return `<!--\n  This HTML file is the browser entry shell for the frontend.\n  It exists so Vite can mount the React application into a predictable root element.\n-->\n${text}`;
}

function repairWeakComments(text) {
  const lines = text.split(/\r?\n/);
  let touched = false;
  for (let i = 0; i < lines.length - 1; i += 1) {
    if (!lines[i].includes("// Defines const so related behavior stays grouped in one place.")) {
      continue;
    }
    const nextLine = lines[i + 1];
    const declaration = nextLine.match(/^\s*(export\s+)?const\s+([A-Za-z_][A-Za-z0-9_]*)\s*=/);
    if (!declaration) {
      continue;
    }
    const indent = lines[i].match(/^\s*/)[0];
    lines[i] = `${indent}// ${lineCommentForIdentifier(declaration[2], "")}`;
    touched = true;
  }
  return touched ? lines.join("\n") : text;
}

function ensureJsDeclarationComments(text, rel) {
  const lines = text.split(/\r?\n/);
  const result = [];
  let touched = false;
  const patterns = [
    /^\s*export\s+function\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/,
    /^\s*export\s+default\s+function\s+([A-Za-z_][A-Za-z0-9_]*)?\s*\(/,
    /^\s*function\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/,
    /^\s*(export\s+)?const\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(async\s*)?(\([^)]*\)|[A-Za-z_][A-Za-z0-9_]*)\s*=>/,
  ];
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    let name = null;
    for (const pattern of patterns) {
      const match = line.match(pattern);
      if (match) {
        name = match[2] || match[1] || "component";
        break;
      }
    }
    if (name && !hasCommentNearby(lines, i) && !hasCommentNearby(result, result.length)) {
      const indent = line.match(/^\s*/)[0];
      result.push(`${indent}// ${lineCommentForIdentifier(name, rel)}`);
      touched = true;
    }
    result.push(line);
  }
  return touched ? result.join("\n") : text;
}

function repairHandlerComments(text) {
  return text.replace(
    /^(\s*)\/\/ Verifies (handle [^\n]+?) so regressions are caught during automated tests\.$/gm,
    "$1// Performs the $2 workflow so callers do not duplicate this logic.",
  );
}

const files = [
  ...serviceDirs.flatMap((dir) => walk(dir)),
  ...frontendRoots.flatMap((entry) => walk(entry)),
];

let changed = 0;
for (const file of files) {
  const ext = path.extname(file);
  const rel = relative(file);
  const before = fs.readFileSync(file, "utf8");
  let after = before;
  if (ext === ".java") after = addJavaComments(before, rel);
  else if ([".js", ".jsx", ".cjs"].includes(ext)) after = addJsComments(before, rel);
  else if (ext === ".css") after = addCssComments(before, rel);
  else if (ext === ".html") after = addHtmlComments(before);
  if ([".js", ".jsx", ".cjs"].includes(ext)) {
    after = repairWeakComments(after);
    after = repairHandlerComments(after);
    after = ensureJsDeclarationComments(after, rel);
  }
  if (after !== before) {
    fs.writeFileSync(file, after, "utf8");
    changed += 1;
  }
}

console.log(`Commented ${changed} source files.`);

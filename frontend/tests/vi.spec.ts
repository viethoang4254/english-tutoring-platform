import { test } from "node:test";
import assert from "node:assert/strict";
import { formatDate, roleLabels, statusLabels } from "../src/lib/vi.ts";

test("Vietnamese role and status labels do not change API enums", () => {
  assert.deepEqual(Object.keys(roleLabels), ["STUDENT", "TEACHER", "ADMIN"]);
  assert.deepEqual(Object.values(statusLabels), ["Chờ xét duyệt", "Đã phê duyệt", "Bị từ chối"]);
});

test("dates use Vietnamese formatting in the runtime timezone with a safe invalid fallback", () => {
  const value = "2026-10-13T12:34:56Z";
  assert.equal(formatDate(value), new Date(value).toLocaleString("vi-VN", {
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", timeZoneName: "short",
  }));
  assert.equal(formatDate("invalid"), "Không xác định");
});

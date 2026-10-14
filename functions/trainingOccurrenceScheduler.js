
"use strict";

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const TIME_ZONE = "Asia/Jerusalem";

const WEEKDAYS = {
  SUNDAY: 0,
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
};

const dateFormatter = new Intl.DateTimeFormat("en-US", {
  timeZone: TIME_ZONE,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

const dateTimeFormatter = new Intl.DateTimeFormat("en-US", {
  timeZone: TIME_ZONE,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hourCycle: "h23",
});

function dateParts(formatter, millis) {
  const result = {};

  formatter.formatToParts(new Date(millis))
    .forEach(({ type, value }) => {
      if (type !== "literal") {
        result[type] = Number(value);
      }
    });

  return result;
}

function israelMillis(year, month, day, hour, minute) {
  const target = Date.UTC(
    year, month - 1, day, hour, minute
  );

  let result = target;

  for (let index = 0; index < 4; index++) {
    const local = dateParts(dateTimeFormatter, result);

    const observed = Date.UTC(
      local.year,
      local.month - 1,
      local.day,
      local.hour,
      local.minute,
      local.second
    );

    result -= observed - target;
  }

  const verified = dateParts(dateTimeFormatter, result);

  if (
    verified.year !== year ||
    verified.month !== month ||
    verified.day !== day ||
    verified.hour !== hour ||
    verified.minute !== minute
  ) {
    throw new Error("Invalid local training date/time");
  }

  return result;
}

function normalizeIdentityPart(value) {
  return String(value || "")
    .trim()
    .replace(/[־–—]/g, "-")
    .replace(/\s+/g, " ")
    .toLocaleLowerCase("he-IL");
}

function occurrenceKeyFor(item) {
  return [
    item.branch,
    item.group,
    item.place,
    item.address,
    item.coachName,
    String(item.originalStartMillis),
    String(item.originalEndMillis),
  ].map(normalizeIdentityPart).join("|");
}

function occurrenceIdFor(key) {
  return crypto
    .createHash("sha256")
    .update(key, "utf8")
    .digest("hex");
}

function readSchedule() {
  const filePath = path.join(
    __dirname, "kmi_data", "branches.json"
  );

  const content = JSON.parse(
    fs.readFileSync(filePath, "utf8")
  );

  if (!Array.isArray(content.branches)) {
    throw new Error("Missing branches array");
  }

  return content;
}

function generateOccurrences(daysAhead = 14) {
  const schedule = readSchedule();
  const today = dateParts(dateFormatter, Date.now());

  const todayUtc = Date.UTC(
    today.year,
    today.month - 1,
    today.day
  );

  const results = [];
  const seen = new Set();

  for (let offset = 0; offset < daysAhead; offset++) {
    const date = new Date(
      todayUtc + offset * 86400000
    );

    const year = date.getUTCFullYear();
    const month = date.getUTCMonth() + 1;
    const day = date.getUTCDate();
    const weekday = date.getUTCDay();

    for (const branch of schedule.branches) {
      if (branch.active !== true) continue;

      for (const slot of branch.trainingDays || []) {
        if (WEEKDAYS[slot.dayOfWeek] !== weekday) {
          continue;
        }

        const startParts = String(slot.startTime || "")
          .split(":")
          .map(Number);

        const endParts = String(slot.endTime || "")
          .split(":")
          .map(Number);

        if (
          startParts.length !== 2 ||
          endParts.length !== 2 ||
          !startParts.every(Number.isInteger) ||
          !endParts.every(Number.isInteger)
        ) {
          throw new Error("Invalid training time");
        }

        const start = israelMillis(
          year, month, day,
          startParts[0], startParts[1]
        );

        let end = israelMillis(
          year, month, day,
          endParts[0], endParts[1]
        );

        if (end <= start) {
          const nextDay = new Date(
            Date.UTC(year, month - 1, day) + 86400000
          );

          end = israelMillis(
            nextDay.getUTCFullYear(),
            nextDay.getUTCMonth() + 1,
            nextDay.getUTCDate(),
            endParts[0],
            endParts[1]
          );
        }

        const item = {
          branch: branch.nameHe,
          group: slot.groupHe,
          place: branch.placeHe,
          address: branch.addressHe,
          coachName: slot.coachNameHe,
          originalStartMillis: start,
          originalEndMillis: end,
          sessionDate:
            `${year}-${String(month).padStart(2, "0")}` +
            `-${String(day).padStart(2, "0")}`,
        };

        const occurrenceKey = occurrenceKeyFor(item);
        const occurrenceId = occurrenceIdFor(occurrenceKey);

        if (seen.has(occurrenceId)) continue;
        seen.add(occurrenceId);

        results.push({
          ...item,
          occurrenceKey,
          occurrenceId,
        });
      }
    }
  }

  return results;
}

if (require.main === module) {
  const occurrences = generateOccurrences(14);

  console.log("KMI_OCCURRENCE_DRY_RUN");
  console.log("Generated:", occurrences.length);

  const sample = occurrences.find((item) =>
    item.branch.includes("מרכז קהילתי אופק") &&
    item.group === "נוער + בוגרים" &&
    item.sessionDate === "2026-10-15"
  );

  console.log(
    "REFERENCE_2026_10_15:",
    sample ? JSON.stringify(sample, null, 2) : "NOT_FOUND"
  );

  console.log("Firestore writes: 0");
}

module.exports = {
  generateOccurrences,
  occurrenceKeyFor,
  occurrenceIdFor,
};

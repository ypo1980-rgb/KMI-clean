
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

 const occurrenceIds =
   occurrences.map((item) => item.occurrenceId);

 const uniqueIds = new Set(occurrenceIds);

 const invalidOccurrences =
   occurrences.filter((item) =>
     !item.branch ||
     !item.group ||
     !item.occurrenceId ||
     !item.sessionDate ||
     item.originalEndMillis <= item.originalStartMillis
   );

 console.log("KMI_OCCURRENCE_VALIDATION");
 console.log("Total:", occurrences.length);
 console.log("Unique IDs:", uniqueIds.size);
 console.log("Duplicates:", occurrences.length - uniqueIds.size);
 console.log("Invalid:", invalidOccurrences.length);


console.log("KMI_OCCURRENCE_DRY_RUN");
console.log("Generated:", occurrences.length);

/*
 * מציג כל סוג אימון פעם אחת בלבד.
 * המפתח כולל סניף, קבוצה, יום ושעות.
 *
 * הבדיקה מקומית בלבד ואינה כותבת ל-Firestore.
 */
const scheduleRows = new Map();

occurrences.forEach((item) => {
  const date = new Date(item.originalStartMillis);

  const dayOfWeek = new Intl.DateTimeFormat(
    "en-US",
    {
      timeZone: "Asia/Jerusalem",
      weekday: "long",
    }
  ).format(date);

  const time = new Intl.DateTimeFormat(
    "en-GB",
    {
      timeZone: "Asia/Jerusalem",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }
  ).format(date);

  const durationMinutes =
    (item.originalEndMillis -
      item.originalStartMillis) / 60000;

  const key = JSON.stringify([
    item.branch,
    item.group,
    dayOfWeek,
    time,
    durationMinutes,
  ]);

  if (!scheduleRows.has(key)) {
    scheduleRows.set(key, {
      branch: item.branch,
      group: item.group,
      day: dayOfWeek,
      time,
      durationMinutes,
    });
  }
});

const sortedRows = Array.from(
  scheduleRows.values()
).sort((a, b) =>
  a.branch.localeCompare(b.branch, "he") ||
  a.group.localeCompare(b.group, "he") ||
  a.day.localeCompare(b.day, "en") ||
  a.time.localeCompare(b.time)
);

console.log("KMI_UNIQUE_WEEKLY_SCHEDULE");
console.log(JSON.stringify(sortedRows, null, 2));
console.log("Unique weekly slots:", sortedRows.length);

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


/**
 * Creates missing training occurrences only.
 *
 * Existing documents are never overwritten.
 *
 * This function does not run automatically.
 * It must be called explicitly by an authorized
 * server-side process in a future step.
 */
async function createMissingOccurrences(db, occurrences) {
  if (!db || typeof db.collection !== "function") {
    throw new Error("Firestore Admin instance is required");
  }

  if (!Array.isArray(occurrences)) {
    throw new Error("Occurrences must be an array");
  }

 const stats = {
   total: occurrences.length,
   created: 0,
   alreadyExists: 0,
   skippedOverrides: 0,
   failed: 0,
 };

  for (const item of occurrences) {
    if (
      !item ||
      !item.occurrenceKey ||
      !item.occurrenceId ||
      !item.sessionDate ||
      !Number.isSafeInteger(item.originalStartMillis) ||
      !Number.isSafeInteger(item.originalEndMillis) ||
      item.originalEndMillis <= item.originalStartMillis
    ) {
      throw new Error("Invalid training occurrence data");
    }

    const expectedId =
      occurrenceIdFor(item.occurrenceKey);

    const expectedKey =
      occurrenceKeyFor(item);

    if (
      item.occurrenceId !== expectedId ||
      item.occurrenceKey !== expectedKey
    ) {
      throw new Error("Training occurrence identity mismatch");
    }



const occurrenceRef = db
  .collection("trainingOccurrences")
  .doc(item.occurrenceId);

const overrideRef = db
  .collection("trainingOverrides")
  .doc(item.occurrenceId);

try {
  const result = await db.runTransaction(
    async (transaction) => {
      /*
       * כל הקריאות מתבצעות לפני הכתיבה.
       * Firestore ינסה שוב את ה-Transaction
       * אם מסמך שנקרא השתנה במקביל.
       */
      const overrideSnapshot =
        await transaction.get(overrideRef);

      const occurrenceSnapshot =
        await transaction.get(occurrenceRef);

      if (occurrenceSnapshot.exists) {
        return "alreadyExists";
      }

      let effectiveStartMillis =
        item.originalStartMillis;

      let effectiveEndMillis =
        item.originalEndMillis;

      let isCancelled = false;

      if (overrideSnapshot.exists) {
        const override =
          overrideSnapshot.data() || {};

        if (override.isActive === true) {
          if (
            override.occurrenceKey !==
            item.occurrenceKey
          ) {
            throw new Error(
              "Training override identity mismatch"
            );
          }

          /*
           * לא יוצרים מופע רגיל עבור אימון
           * עם ביטול או שינוי שעה פעיל.
           */
          return "skippedOverride";
        }
      }

      const documentData = {
        occurrenceId: item.occurrenceId,
        occurrenceKey: item.occurrenceKey,

        branch: item.branch,
        group: item.group,
        place: item.place,
        address: item.address,
        coachName: item.coachName,

        originalStartMillis:
          item.originalStartMillis,

        originalEndMillis:
          item.originalEndMillis,

        sessionDate: item.sessionDate,

        effectiveStartMillis,
        effectiveEndMillis,
        isCancelled,

        source:
          "server_training_occurrence_scheduler",

        updatedByUid: "system",

        updatedAt:
          new Date(),
      };

      transaction.create(
        occurrenceRef,
        documentData
      );

      return "created";
    }
  );

  if (result === "created") {
    stats.created++;
  } else if (result === "alreadyExists") {
    stats.alreadyExists++;
  } else if (result === "skippedOverride") {
    stats.skippedOverrides++;

    console.log(
      "KMI_OCCURRENCE_SKIPPED_ACTIVE_OVERRIDE",
      {
        occurrenceId: item.occurrenceId,
        sessionDate: item.sessionDate,
      }
    );
  }

} catch (error) {
  stats.failed++;

  console.error(
    "KMI_OCCURRENCE_CREATE_FAILED",
    {
      occurrenceId: item.occurrenceId,
      sessionDate: item.sessionDate,
      errorCode: String(
        (error && error.code) || "unknown"
      ),
    }
  );
}

  }

  return stats;
}


/**
 * Read-only audit of generated training occurrences.
 *
 * No Firestore writes.
 * Existing documents are never modified.
 */
async function auditTrainingOccurrences(db, occurrences) {
  if (!db || typeof db.collection !== "function") {
    throw new Error("Firestore Admin instance is required");
  }

  if (!Array.isArray(occurrences)) {
    throw new Error("Occurrences must be an array");
  }

 const stats = {
   total: occurrences.length,
   existing: 0,
   missing: 0,
   readyToCreate: 0,
   missingWithActiveOverride: 0,
   mismatched: 0,
   errors: 0,
 };

  const mismatches = [];

  // Small batches to avoid excessive concurrent reads.
  for (let start = 0; start < occurrences.length; start += 10) {
    const batch = occurrences.slice(start, start + 10);

    const results = await Promise.allSettled(
      batch.map(async (item) => {
        if (
          !item ||
          !item.occurrenceKey ||
          !item.occurrenceId ||
          occurrenceKeyFor(item) !== item.occurrenceKey ||
          occurrenceIdFor(item.occurrenceKey) !== item.occurrenceId
        ) {
          throw new Error("Invalid occurrence identity");
        }

      const snapshot = await db
        .collection("trainingOccurrences")
        .doc(item.occurrenceId)
        .get();

      let overrideSnapshot = null;

      if (!snapshot.exists) {
        overrideSnapshot = await db
          .collection("trainingOverrides")
          .doc(item.occurrenceId)
          .get();
      }

      return {
        item,
        snapshot,
        overrideSnapshot,
      };
      })
    );

    results.forEach((result, index) => {
      if (result.status === "rejected") {
        stats.errors++;

        console.error("KMI_OCCURRENCE_AUDIT_ERROR", {
          occurrenceId: batch[index]?.occurrenceId || "",
          errorCode: String(
            result.reason?.code || "unknown"
          ),
        });

        return;
      }

   const {
     item,
     snapshot,
     overrideSnapshot,
   } = result.value;

   if (!snapshot.exists) {
     stats.missing++;

     if (overrideSnapshot?.exists) {
       const override = overrideSnapshot.data() || {};

       if (override.occurrenceKey !== item.occurrenceKey) {
         stats.mismatched++;
         return;
       }

       if (override.isActive === true) {
         stats.missingWithActiveOverride++;
         return;
       }
     }

     stats.readyToCreate++;
     return;
   }

      const stored = snapshot.data() || {};

      const identityMatches =
        stored.occurrenceId === item.occurrenceId &&
        stored.occurrenceKey === item.occurrenceKey &&
        stored.branch === item.branch &&
        stored.group === item.group &&
        stored.sessionDate === item.sessionDate &&
        stored.originalStartMillis === item.originalStartMillis &&
        stored.originalEndMillis === item.originalEndMillis;

      if (identityMatches) {
        stats.existing++;
      } else {
        stats.mismatched++;

        mismatches.push({
          occurrenceId: item.occurrenceId,
          sessionDate: item.sessionDate,
          branch: item.branch,
          group: item.group,
        });
      }
    });
  }

  console.log("KMI_OCCURRENCE_FIRESTORE_AUDIT", stats);

  if (mismatches.length > 0) {
    console.warn(
      "KMI_OCCURRENCE_IDENTITY_MISMATCHES",
      mismatches.slice(0, 20)
    );
  }

  return stats;
}

module.exports = {
  generateOccurrences,
  occurrenceKeyFor,
  occurrenceIdFor,
  createMissingOccurrences,
  auditTrainingOccurrences,
};



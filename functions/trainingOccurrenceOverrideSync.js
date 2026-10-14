
"use strict";

const crypto = require("crypto");

const TIME_ZONE = "Asia/Jerusalem";

function occurrenceIdForKey(key) {
  return crypto
    .createHash("sha256")
    .update(key, "utf8")
    .digest("hex");
}

function sessionDateForMillis(millis) {
  const formatter = new Intl.DateTimeFormat("en-US", {
    timeZone: TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  const parts = {};

  formatter.formatToParts(new Date(millis))
    .forEach(({ type, value }) => {
      if (type !== "literal") {
        parts[type] = value;
      }
    });

  return (
    `${parts.year}-${parts.month}-${parts.day}`
  );
}

/**
 * Synchronizes one training override to its occurrence.
 *
 * Reads the latest override inside a transaction.
 * Never trusts an old trigger event payload.
 *
 * Does not modify trainingOverrides.
 */
async function syncOverrideToOccurrence(
  db,
  occurrenceId
) {
  if (
    !db ||
    typeof db.runTransaction !== "function"
  ) {
    throw new Error("Firestore instance is required");
  }

  const cleanId = String(occurrenceId || "").trim();

  if (!/^[a-f0-9]{64}$/.test(cleanId)) {
    throw new Error("Invalid occurrence ID");
  }

  const overrideRef = db
    .collection("trainingOverrides")
    .doc(cleanId);

  const occurrenceRef = db
    .collection("trainingOccurrences")
    .doc(cleanId);

  return db.runTransaction(async (transaction) => {
    /*
     * Always read both documents before writing.
     */
    const overrideSnapshot =
      await transaction.get(overrideRef);

    const occurrenceSnapshot =
      await transaction.get(occurrenceRef);

    if (!overrideSnapshot.exists) {
      return {
        status: "override_missing",
      };
    }

    const override = overrideSnapshot.data() || {};

    const occurrenceKey =
      String(override.occurrenceKey || "").trim();

    if (
      !occurrenceKey ||
      occurrenceIdForKey(occurrenceKey) !== cleanId
    ) {
      throw new Error(
        "Override occurrence identity mismatch"
      );
    }

    const originalStartMillis =
      Number(override.originalStartMillis);

    const originalEndMillis =
      Number(override.originalEndMillis);

    if (
      !Number.isSafeInteger(originalStartMillis) ||
      !Number.isSafeInteger(originalEndMillis) ||
      originalStartMillis <= 0 ||
      originalEndMillis <= originalStartMillis
    ) {
      throw new Error("Invalid original training time");
    }

    const type =
      String(override.type || "")
        .trim()
        .toLowerCase();

    const isActive =
      override.isActive === true;

    const isCancelled =
      isActive && type === "cancelled";

    const hasChangedTime =
      isActive && type === "time_changed";

    if (
      isActive &&
      !isCancelled &&
      !hasChangedTime
    ) {
      throw new Error("Unsupported override type");
    }

    let effectiveStartMillis =
      originalStartMillis;

    let effectiveEndMillis =
      originalEndMillis;

    if (hasChangedTime) {
      effectiveStartMillis =
        Number(override.newStartMillis);

      effectiveEndMillis =
        Number(override.newEndMillis);

      if (
        !Number.isSafeInteger(effectiveStartMillis) ||
        !Number.isSafeInteger(effectiveEndMillis) ||
        effectiveStartMillis <= 0 ||
        effectiveEndMillis <= effectiveStartMillis
      ) {
        throw new Error("Invalid changed training time");
      }
    }

    const sessionDate =
      sessionDateForMillis(originalStartMillis);

    const existing =
      occurrenceSnapshot.exists
        ? occurrenceSnapshot.data() || {}
        : null;

    /*
     * Never alter an occurrence whose identity
     * contradicts the original override.
     */
    if (existing) {
      if (
        existing.occurrenceId !== cleanId ||
        existing.occurrenceKey !== occurrenceKey ||
        existing.originalStartMillis !==
          originalStartMillis ||
        existing.originalEndMillis !==
          originalEndMillis ||
        existing.sessionDate !== sessionDate
      ) {
        throw new Error(
          "Existing occurrence identity mismatch"
        );
      }

      const alreadySynchronized =
        existing.isCancelled === isCancelled &&
        existing.effectiveStartMillis ===
          effectiveStartMillis &&
        existing.effectiveEndMillis ===
          effectiveEndMillis;

      if (alreadySynchronized) {
        return {
          status: "unchanged",
        };
      }

      transaction.update(occurrenceRef, {
        isCancelled,
        effectiveStartMillis,
        effectiveEndMillis,
        updatedAt: new Date(),
        overrideSyncedAt: new Date(),
        overrideSyncSource:
          "server_training_override_sync",
      });

      return {
        status: "updated",
      };
    }

    /*
     * No occurrence exists yet.
     * Build one from the verified override.
     */
    const branch =
      String(override.branch || "").trim();

    const group =
      String(override.group || "").trim();

    const place =
      String(override.place || "").trim();

    const address =
      String(override.address || "").trim();

    const coachName =
      String(override.coachName || "").trim();

    if (!branch || !group) {
      throw new Error("Missing training branch/group");
    }

    transaction.create(occurrenceRef, {
      occurrenceId: cleanId,
      occurrenceKey,

      branch,
      group,
      place,
      address,
      coachName,

      originalStartMillis,
      originalEndMillis,

      sessionDate,

      effectiveStartMillis,
      effectiveEndMillis,

      isCancelled,

      updatedByUid:
        String(
          override.changedByUid ||
          override.restoredByUid ||
          "system"
        ).trim(),

      source:
        "server_training_occurrence_override",

      updatedAt: new Date(),

      overrideSyncedAt: new Date(),
      overrideSyncSource:
        "server_training_override_sync",
    });

    return {
      status: "created",
    };
  });
}

module.exports = {
  syncOverrideToOccurrence,
};

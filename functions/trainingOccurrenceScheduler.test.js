
"use strict";

const assert = require("node:assert/strict");

const {
  generateOccurrences,
  createMissingOccurrences,
} = require("./trainingOccurrenceScheduler");

const {
  syncOverrideToOccurrence,
} = require("./trainingOccurrenceOverrideSync");

/*
 * Firestore Mock with optimistic transaction retries.
 *
 * All data stays in memory.
 * No real Firebase connection.
 */
function createMockFirestore(options = {}) {
  const documents = new Map();
  const overrides = new Map();
  const versions = new Map();

  let createCalls = 0;
  let transactionAttempts = 0;
  let concurrentChangeApplied = false;

  function readData(key) {
    return key.startsWith("trainingOverrides/")
      ? overrides.get(key)
      : documents.get(key);
  }

  function snapshotFor(key) {
    const data = readData(key);

    return {
      exists: data !== undefined,
      data: () => data,
    };
  }

  const firestore = {
    collection(name) {
      return {
        doc(id) {
          return {
            key: `${name}/${id}`,
          };
        },
      };
    },

    async runTransaction(callback) {
      for (let attempt = 0; attempt < 5; attempt++) {
        transactionAttempts++;

        const reads = new Map();
        const writes = [];

        const transaction = {
          async get(ref) {
            const key = ref.key;

            if (
              options.failOverrideRead &&
              key.startsWith("trainingOverrides/")
            ) {
              throw new Error(
                "Simulated Firestore read failure"
              );
            }

            if (writes.length > 0) {
              throw new Error(
                "Transaction reads must precede writes"
              );
            }

            reads.set(
              key,
              versions.get(key) || 0
            );

            return snapshotFor(key);
          },

        create(ref, data) {
          writes.push({
            operation: "create",
            key: ref.key,
            data,
          });
        },

        update(ref, data) {
          writes.push({
            operation: "update",
            key: ref.key,
            data,
          });
        },
        };

        const result = await callback(transaction);

        /*
         * Simulate a coach changing an override
         * after the transaction reads but
         * before Firestore commits.
         */
        if (
          options.concurrentOverride &&
          !concurrentChangeApplied
        ) {
          const change = options.concurrentOverride;
          const key =
            `trainingOverrides/${change.occurrenceId}`;

          overrides.set(key, change.data);

          versions.set(
            key,
            (versions.get(key) || 0) + 1
          );

          concurrentChangeApplied = true;
        }

        const conflict = Array.from(reads)
          .some(([key, version]) =>
            (versions.get(key) || 0) !== version
          );

        if (conflict) {
          continue;
        }

     for (const write of writes) {
       const exists = documents.has(write.key);

       if (
         write.operation === "create" &&
         exists
       ) {
         const error = new Error(
           "Document already exists"
         );

         error.code = 6;
         throw error;
       }

       if (
         write.operation === "update" &&
         !exists
       ) {
         throw new Error(
           "Cannot update missing document"
         );
       }
     }

     for (const write of writes) {
       if (write.operation === "create") {
         createCalls++;

         documents.set(
           write.key,
           write.data
         );
       } else if (write.operation === "update") {
         documents.set(
           write.key,
           {
             ...documents.get(write.key),
             ...write.data,
           }
         );
       }

       versions.set(
         write.key,
         (versions.get(write.key) || 0) + 1
       );
     }

        return result;
      }

      throw new Error(
        "Transaction failed after retries"
      );
    },
  };

  return {
    firestore,
    documents,
    overrides,
    getCreateCalls: () => createCalls,
    getTransactionAttempts: () => transactionAttempts,
  };
}

async function runTests() {
  const occurrences = generateOccurrences(14);

  const sample = occurrences.find((item) =>
    item.branch.includes("מרכז קהילתי אופק") &&
    item.group === "נוער + בוגרים"
  );

  assert.ok(
    sample,
    "Reference training must exist"
  );

  // Test 1: Regular training.
  {
    const mock = createMockFirestore();

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 1);
    assert.equal(result.failed, 0);
    assert.equal(mock.getCreateCalls(), 1);

    console.log("PASS: Regular training");
  }

  // Test 2: Cancelled training.
  {
    const mock = createMockFirestore();

    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        isActive: true,
        occurrenceKey: sample.occurrenceKey,
        type: "cancelled",
      }
    );

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 0);
    assert.equal(result.skippedOverrides, 1);
    assert.equal(mock.getCreateCalls(), 0);

    console.log("PASS: Cancelled training");
  }

  // Test 3: Changed training time.
  {
    const mock = createMockFirestore();

    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        isActive: true,
        occurrenceKey: sample.occurrenceKey,
        type: "time_changed",
      }
    );

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 0);
    assert.equal(result.skippedOverrides, 1);
    assert.equal(mock.getCreateCalls(), 0);

    console.log("PASS: Time-changed training");
  }

  // Test 4: Existing document preserved.
  {
    const mock = createMockFirestore();

    const key =
      `trainingOccurrences/${sample.occurrenceId}`;

    const existingData = {
      isCancelled: true,
      effectiveStartMillis: 123456,
    };

    mock.documents.set(key, existingData);

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 0);
    assert.equal(result.alreadyExists, 1);
    assert.equal(mock.getCreateCalls(), 0);

    assert.deepEqual(
      mock.documents.get(key),
      existingData
    );

    console.log(
      "PASS: Existing document preserved"
    );
  }

  // Test 5: Firestore read failure.
  {
    const mock = createMockFirestore({
      failOverrideRead: true,
    });

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 0);
    assert.equal(result.failed, 1);
    assert.equal(mock.getCreateCalls(), 0);

    console.log(
      "PASS: Firestore read failure"
    );
  }

  // Test 6: Concurrent cancellation.
  {
    const mock = createMockFirestore({
      concurrentOverride: {
        occurrenceId: sample.occurrenceId,
        data: {
          isActive: true,
          occurrenceKey: sample.occurrenceKey,
          type: "cancelled",
        },
      },
    });

    const result = await createMissingOccurrences(
      mock.firestore,
      [sample]
    );

    assert.equal(result.created, 0);
    assert.equal(result.skippedOverrides, 1);
    assert.equal(result.failed, 0);

    assert.equal(mock.getCreateCalls(), 0);

    assert.ok(
      mock.getTransactionAttempts() >= 2,
      "Concurrent change should retry transaction"
    );

    console.log(
      "PASS: Concurrent cancellation protected"
    );
  }


  // Test 7: Synchronize cancellation of existing training.
  {
    const mock = createMockFirestore();

    const occurrenceKey =
      `trainingOccurrences/${sample.occurrenceId}`;

    mock.documents.set(occurrenceKey, {
      occurrenceId: sample.occurrenceId,
      occurrenceKey: sample.occurrenceKey,
      branch: sample.branch,
      group: sample.group,
      originalStartMillis: sample.originalStartMillis,
      originalEndMillis: sample.originalEndMillis,
      sessionDate: sample.sessionDate,
      effectiveStartMillis: sample.originalStartMillis,
      effectiveEndMillis: sample.originalEndMillis,
      isCancelled: false,
    });

    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        occurrenceKey: sample.occurrenceKey,
        originalStartMillis: sample.originalStartMillis,
        originalEndMillis: sample.originalEndMillis,
        isActive: true,
        type: "cancelled",
      }
    );

    const result = await syncOverrideToOccurrence(
      mock.firestore,
      sample.occurrenceId
    );

    assert.equal(result.status, "updated");

    const updated =
      mock.documents.get(occurrenceKey);

    assert.equal(updated.isCancelled, true);
    assert.equal(
      updated.effectiveStartMillis,
      sample.originalStartMillis
    );
    assert.equal(
      updated.effectiveEndMillis,
      sample.originalEndMillis
    );

    console.log(
      "PASS: Existing training cancellation synchronized"
    );
  }


  // Test 8: Synchronize a changed training time.
  {
    const mock = createMockFirestore();

    const occurrencePath =
      `trainingOccurrences/${sample.occurrenceId}`;

    mock.documents.set(occurrencePath, {
      occurrenceId: sample.occurrenceId,
      occurrenceKey: sample.occurrenceKey,
      branch: sample.branch,
      group: sample.group,
      originalStartMillis: sample.originalStartMillis,
      originalEndMillis: sample.originalEndMillis,
      sessionDate: sample.sessionDate,
      effectiveStartMillis: sample.originalStartMillis,
      effectiveEndMillis: sample.originalEndMillis,
      isCancelled: false,
    });

    const newStartMillis =
      sample.originalStartMillis + 30 * 60 * 1000;

    const newEndMillis =
      sample.originalEndMillis + 30 * 60 * 1000;

    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        occurrenceKey: sample.occurrenceKey,
        originalStartMillis: sample.originalStartMillis,
        originalEndMillis: sample.originalEndMillis,
        isActive: true,
        type: "time_changed",
        newStartMillis,
        newEndMillis,
      }
    );

    const result = await syncOverrideToOccurrence(
      mock.firestore,
      sample.occurrenceId
    );

    assert.equal(result.status, "updated");

    const updated =
      mock.documents.get(occurrencePath);

    assert.equal(updated.isCancelled, false);
    assert.equal(
      updated.effectiveStartMillis,
      newStartMillis
    );
    assert.equal(
      updated.effectiveEndMillis,
      newEndMillis
    );

    assert.equal(
      updated.originalStartMillis,
      sample.originalStartMillis
    );

    assert.equal(
      updated.occurrenceKey,
      sample.occurrenceKey
    );

    console.log(
      "PASS: Existing training time change synchronized"
    );
  }


  // Test 9: Restore original training.
  {
    const mock = createMockFirestore();

    const occurrencePath =
      `trainingOccurrences/${sample.occurrenceId}`;

    mock.documents.set(occurrencePath, {
      occurrenceId: sample.occurrenceId,
      occurrenceKey: sample.occurrenceKey,
      branch: sample.branch,
      group: sample.group,
      originalStartMillis: sample.originalStartMillis,
      originalEndMillis: sample.originalEndMillis,
      sessionDate: sample.sessionDate,

      effectiveStartMillis:
        sample.originalStartMillis + 30 * 60 * 1000,

      effectiveEndMillis:
        sample.originalEndMillis + 30 * 60 * 1000,

      isCancelled: false,
    });

    /*
     * שחזור אימון אינו מוחק את מסמך השינוי.
     * הוא רק מעדכן isActive=false.
     */
    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        occurrenceKey: sample.occurrenceKey,
        originalStartMillis: sample.originalStartMillis,
        originalEndMillis: sample.originalEndMillis,
        isActive: false,
        type: "time_changed",
        newStartMillis:
          sample.originalStartMillis + 30 * 60 * 1000,
        newEndMillis:
          sample.originalEndMillis + 30 * 60 * 1000,
      }
    );

    const result = await syncOverrideToOccurrence(
      mock.firestore,
      sample.occurrenceId
    );

    assert.equal(result.status, "updated");

    const restored =
      mock.documents.get(occurrencePath);

    assert.equal(restored.isCancelled, false);
    assert.equal(
      restored.effectiveStartMillis,
      sample.originalStartMillis
    );
    assert.equal(
      restored.effectiveEndMillis,
      sample.originalEndMillis
    );

    assert.equal(
      restored.occurrenceKey,
      sample.occurrenceKey
    );

    console.log(
      "PASS: Original training restored"
    );
  }


  // Test 10: Concurrent override change during synchronization.
  {
    const newStartMillis =
      sample.originalStartMillis + 60 * 60 * 1000;

    const newEndMillis =
      sample.originalEndMillis + 60 * 60 * 1000;

    const mock = createMockFirestore({
      concurrentOverride: {
        occurrenceId: sample.occurrenceId,
        data: {
          occurrenceKey: sample.occurrenceKey,
          originalStartMillis: sample.originalStartMillis,
          originalEndMillis: sample.originalEndMillis,
          isActive: true,
          type: "time_changed",
          newStartMillis,
          newEndMillis,
        },
      },
    });

    const occurrencePath =
      `trainingOccurrences/${sample.occurrenceId}`;

    mock.documents.set(occurrencePath, {
      occurrenceId: sample.occurrenceId,
      occurrenceKey: sample.occurrenceKey,
      branch: sample.branch,
      group: sample.group,
      originalStartMillis: sample.originalStartMillis,
      originalEndMillis: sample.originalEndMillis,
      sessionDate: sample.sessionDate,
      effectiveStartMillis: sample.originalStartMillis,
      effectiveEndMillis: sample.originalEndMillis,
      isCancelled: false,
    });

    mock.overrides.set(
      `trainingOverrides/${sample.occurrenceId}`,
      {
        occurrenceKey: sample.occurrenceKey,
        originalStartMillis: sample.originalStartMillis,
        originalEndMillis: sample.originalEndMillis,
        isActive: true,
        type: "cancelled",
      }
    );

    const result = await syncOverrideToOccurrence(
      mock.firestore,
      sample.occurrenceId
    );

    assert.equal(result.status, "updated");

    const updated =
      mock.documents.get(occurrencePath);

    assert.equal(updated.isCancelled, false);

    assert.equal(
      updated.effectiveStartMillis,
      newStartMillis
    );

    assert.equal(
      updated.effectiveEndMillis,
      newEndMillis
    );

    assert.ok(
      mock.getTransactionAttempts() >= 2,
      "Concurrent override must retry the transaction"
    );

    console.log(
      "PASS: Concurrent override synchronization"
    );
  }

  console.log(
    "KMI_OCCURRENCE_TESTS_PASSED: 10/10"
  );

  console.log("Real Firestore writes: 0");
}

runTests().catch((error) => {
  console.error(
    "KMI_OCCURRENCE_TEST_FAILED",
    error
  );

  process.exitCode = 1;
});

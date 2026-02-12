// שימוש ב־v1 compat של Firebase Functions (Node 20)
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

// אתחול Firebase Admin פעם אחת
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

// 🎙️ Google Cloud Text-to-Speech – קול גברי Neural
const textToSpeech = require("@google-cloud/text-to-speech");
const ttsClient = new textToSpeech.TextToSpeechClient();

/**
 * פונקציית עזר לפיצול מערכים למקטעים (כרגע לא נשתמש בה, אבל נשאיר אם תרצה בעתיד)
 */
function chunkArray(arr, size) {
  const chunks = [];
  for (let i = 0; i < arr.length; i += size) {
    chunks.push(arr.slice(i, i + size));
  }
  return chunks;
}

/**
 * ====================================================
 * 1. טריגר לפורום – הודעה חדשה בקבוצת הפורום
 *    branches/{branchId}/messages/{messageId}
 * ====================================================
 */
exports.onForumMessageCreated = functions.firestore
  .document("branches/{branchId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const data = snap.data() || {};
    const branchId = context.params.branchId;
    const groupKey = data.groupKey || "";

    console.log("New forum message created:", {
      branchId,
      groupKey,
      text: (data.text || "").toString().slice(0, 80),
    });

    // אם אין groupKey – אין למי לשלוח
    if (!groupKey) {
      console.log("No groupKey on message, skipping push");
      return null;
    }

    // ===== 1. שליפת כל המשתמשים בקבוצה הזו =====
    let usersSnap;
    try {
      usersSnap = await db
        .collection("users")
        // עובדים לפי השדה groups (מערך)
        .where("groups", "array-contains", groupKey)
        .get();
    } catch (e) {
      console.error("Failed to query users:", e);
      return null;
    }

    if (usersSnap.empty) {
      console.log("No users found for group:", groupKey);
      return null;
    }

    const tokens = [];
    usersSnap.forEach((doc) => {
      const t = doc.get("fcmToken");
      if (t) tokens.push(t);
    });

    if (tokens.length === 0) {
      console.log("No FCM tokens for users in group:", groupKey);
      return null;
    }

    // ===== 2. בניית הודעת ה-Push =====
    const title = "הודעה חדשה בפורום";
    const body = `נוספה הודעה חדשה בקבוצה "${groupKey}"`;

    const payload = {
      notification: {
        title,
        body,
      },
      data: {
        type: "forum_message",
        branchId,
        groupKey,
        messageId: snap.id,
      },
    };

    // ===== 3. שליחה לכל ה-tokens =====
    try {
      const res = await admin.messaging().sendEachForMulticast({
        tokens,
        ...payload,
      });

      console.log(
        `Push sent to ${tokens.length} tokens. ` +
          `success=${res.successCount}, failure=${res.failureCount}`
      );

      return null;
    } catch (e) {
      console.error("Failed to send FCM multicast:", e);
      return null;
    }
  });

/**
 * ====================================================
 * 2. טריגר להודעת מאמן – coachBroadcasts/{broadcastId}
 *    עובד לפי groupKey + השדה groups במשתמשים
 * ====================================================
 */
exports.onCoachBroadcastCreated = functions.firestore
  .document("coachBroadcasts/{broadcastId}")
  .onCreate(async (snap, context) => {
    const data = snap.data() || {};
    const broadcastId = context.params.broadcastId;

    const text = (data.text || "").toString();
    const region = data.region || "";
    const branch = data.branch || "";
    const groupKey = data.groupKey || "";

    console.log("New coach broadcast created:", {
      broadcastId,
      region,
      branch,
      groupKey,
      textPreview: text.slice(0, 80),
    });

    if (!groupKey) {
      console.log("No groupKey on coach broadcast, skipping push");
      return null;
    }

    // ===== 1. שליפת המשתמשים בקבוצה הזו =====
    let usersSnap;
    try {
      // ניסיון ראשון: לפי שם הקבוצה בשדה groups
      usersSnap = await db
        .collection("users")
        .where("groups", "array-contains", groupKey)
        .get();

      // אם אין אף משתמש – ננסה לפי branches (שם סניף / סניף+קבוצה)
      if (usersSnap.empty) {
        console.log(
          "No users found by groups for coach broadcast, trying branches with groupKey:",
          groupKey
        );

        usersSnap = await db
          .collection("users")
          .where("branches", "array-contains", groupKey)
          .get();
      }
    } catch (e) {
      console.error("Failed to query users for coach broadcast:", e);
      return null;
    }

    if (usersSnap.empty) {
      console.log("No users found for coach broadcast group:", groupKey);
      return null;
    }

    const tokens = [];
    usersSnap.forEach((doc) => {
      const t = doc.get("fcmToken");
      if (t) tokens.push(t);
    });

    if (tokens.length === 0) {
      console.log("No FCM tokens for users in coach broadcast");
      return null;
    }

    // ===== 2. בניית הודעת ה-Push =====
    const title = "הודעה חדשה מהמאמן";
    const body = text.slice(0, 100); // מקצרים אם צריך

    const payload = {
      notification: {
        title,
        body,
      },
      data: {
        type: "coach_broadcast",
        broadcastId,
        region,
        branch,
        groupKey,
      },
    };

    // ===== 3. שליחה לכל ה-tokens =====
    try {
      const res = await admin.messaging().sendEachForMulticast({
        tokens,
        ...payload,
      });

      console.log(
        `Coach broadcast push sent to ${tokens.length} tokens. ` +
          `success=${res.successCount}, failure=${res.failureCount}`
      );

      return null;
    } catch (e) {
      console.error("Failed to send coach broadcast FCM:", e);
      return null;
    }
  });

/**
 * ====================================================
 * 3. פונקציית HTTP ל-TTS – kmiTts
 *    מקבלת JSON ומחזירה bytes של MP3 (audio/mpeg)
 * ====================================================
 */
exports.kmiTts = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(204).send("");
  if (req.method !== "POST") return res.status(405).json({ error: "Use POST with JSON body" });

  try {
    const { text, languageCode, pitch, voice } = req.body || {};
    if (!text || typeof text !== "string" || !text.trim()) {
      return res.status(400).json({ error: 'Missing "text" field in body' });
    }

    const lang = (languageCode || "he-IL").trim();
    const wantMale = ((voice || "male") + "").toLowerCase().trim() === "male";

    // ✅ FIX מוחלט: ל-he-IL נועלים Wavenet קיים (ולא Neural2)
    // male  -> he-IL-Wavenet-B
    // female-> he-IL-Wavenet-A
    const voiceName =
      lang === "he-IL"
        ? (wantMale ? "he-IL-Wavenet-B" : "he-IL-Wavenet-A")
        : null;

    console.log("kmiTts voice selection:", { lang, wantMale, voiceName });

    // ✅ FIX: ברירת מחדל "אנושית" = 1.0 (לא 0.45!)
    // וגם טווח הגיוני שלא יגרום לקול "רובוטי"
    const rawRateAny = (req.body || {}).speakingRate;
    const rawRateNum =
      typeof rawRateAny === "number"
        ? rawRateAny
        : (typeof rawRateAny === "string" ? Number(rawRateAny) : NaN);

 const rate =
   Number.isFinite(rawRateNum)
     ? Math.min(1.6, Math.max(1.2, rawRateNum))
     : 1.35;

    console.log("kmiTts speakingRate:", {
      rawRateAny,
      rawType: typeof rawRateAny,
      rawRateNum,
      rate,
    });

    // ✅ להחזיר headers כדי שנראה באנדרואיד מה באמת שימש
    res.set("X-KMI-Rate", String(rate));

    // ✅ FIX: לא מערבבים SSML prosody + audioConfig.speakingRate
    // שולחים text רגיל ומכוונים את המהירות רק דרך audioConfig
    const request = {
      input: { text: text.trim() },
      voice: voiceName
        ? { languageCode: lang, name: voiceName }
        : { languageCode: lang, ssmlGender: wantMale ? "MALE" : "FEMALE" },
      audioConfig: {
        audioEncoding: "MP3",
        speakingRate: rate,
        pitch:
          typeof pitch === "number"
            ? Math.min(6.0, Math.max(-6.0, pitch))
            : (wantMale ? -0.2 : 0.2),
      },
    };

    const [response] = await ttsClient.synthesizeSpeech(request);

    res.set("Content-Type", "audio/mpeg");
    return res.status(200).send(response.audioContent);
  } catch (err) {
    console.error("kmiTts error:", err);
    return res.status(500).json({ error: "TTS failed", details: String(err) });
  }
});

function escapeXml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

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

// 🔥 Generative TTS (קול אנושי הרבה יותר)
const { v1beta1: ttsGen } = require("@google-cloud/text-to-speech");
const genClient = new ttsGen.TextToSpeechClient();

const KMI_TTS_VERSION = "tts-human-v4";

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

function detectTtsStyle(text) {
  const t = String(text || "").trim().toLowerCase();

  if (!t) return "default";

  if (
    t.includes("שים לב") ||
    t.includes("זהירות") ||
    t.includes("אסור") ||
    t.includes("danger") ||
    t.includes("warning")
  ) {
    return "warning";
  }

  if (
    t.includes("שלב") ||
    t.includes("עמידת מוצא") ||
    t.includes("בצע") ||
    t.includes("הרם") ||
    t.includes("סובב") ||
    t.includes("step") ||
    t.includes("start position")
  ) {
    return "instruction";
  }

  if (
    t.includes("מעולה") ||
    t.includes("יפה") ||
    t.includes("בהצלחה") ||
    t.includes("כל הכבוד") ||
    t.includes("great") ||
    t.includes("excellent") ||
    t.includes("well done")
  ) {
    return "friendly";
  }

  return "default";
}

function buildExpressiveSsml(text, style) {

  const normalizedText = String(text || "")
    .trim()
    .replace(/\n+/g, ". ")
    .replace(/•/g, ". ")
    .replace(/\s+/g, " ");

  const rate =
    style === "instruction" ? "95%" :
    style === "warning" ? "90%" :
    style === "friendly" ? "100%" :
    "96%";

  return `
<speak>
  <prosody rate="${rate}" pitch="+0%">
    ${escapeXml(normalizedText)}
  </prosody>
</speak>`;
}

async function synthesizeHumanVoice({ text, lang, preferredHumanVoice }) {
  try {
    console.log("Trying human voice path:", {
      engine: "genClient.synthesizeSpeech",
      lang,
      preferredHumanVoice,
    });

 const ssml = buildExpressiveSsml(text, "default");

 const request = {
   input: { ssml },
         voice: {
        languageCode: lang,
        name: preferredHumanVoice,
      },
      audioConfig: {
        audioEncoding: "MP3",
        speakingRate: 1.10,
        pitch: 0.0,
      }
    };

    const [response] = await genClient.synthesizeSpeech(request);

    if (!response || !response.audioContent) {
      throw new Error("Human voice path returned empty audio");
    }

    return {
      audioContent: response.audioContent,
      usedVoiceName: preferredHumanVoice,
      usedEngine: "human-path",
    };

  } catch (err) {
    console.log("Human voice path failed, fallback to classic:", {
      preferredHumanVoice,
      message: String(err),
    });

    return null;
  }
}

async function synthesizeWithVoiceFallback({
  text,
  lang,
  voiceKey,
  rate,
  pitch,
  style,
}) {
  const wantFemale = voiceKey === "female";
  const wantMale = voiceKey === "male";

  const preferredVoices =
    lang === "he-IL"
      ? [
          // קודם Chirp 3: HD - הכי טבעי כרגע בגוגל לעברית
          wantFemale
            ? "he-IL-Chirp3-HD-Aoede"
            : "he-IL-Chirp3-HD-Charon",

          // fallback נוסף בתוך Chirp 3: HD
          wantFemale
            ? "he-IL-Chirp3-HD-Kore"
            : "he-IL-Chirp3-HD-Schedar",

          // fallback ישן יותר
          wantFemale
            ? "he-IL-Neural2-A"
            : "he-IL-Neural2-B",

          wantFemale
            ? "he-IL-Wavenet-A"
            : "he-IL-Wavenet-B",
        ]
      : [null];

  const resolvedPitch =
    typeof pitch === "number"
      ? Math.min(2.0, Math.max(-2.0, pitch))
      : 0.0;

  const plainText = buildExpressiveSsml(text, style);

  let lastError = null;

  for (const voiceName of preferredVoices) {
    try {
console.log("kmiTts trying voice:", {
  lang,
  voiceKey,
  voiceName,
  rate,
  style,
  resolvedPitch,
  engine: "classic"
});

    const request = {
      input: { ssml: plainText },
              voice: voiceName
          ? { languageCode: lang, name: voiceName }
          : {
              languageCode: lang,
              ssmlGender: wantFemale ? "FEMALE" : "MALE",
            },
        audioConfig: {
          audioEncoding: "MP3",
          speakingRate: rate,
          pitch: resolvedPitch,
        },
      };

      const [response] = await ttsClient.synthesizeSpeech(request);

      if (!response || !response.audioContent) {
        throw new Error("Empty audioContent from TTS");
      }

      return {
        audioContent: response.audioContent,
        usedVoiceName: voiceName || (wantFemale ? "FEMALE" : "MALE"),
        usedEngine: "classic-fallback",
      };
    } catch (err) {
      lastError = err;
      console.error("kmiTts voice failed, trying next fallback:", {
        attemptedVoice: voiceName,
        message: String(err),
      });
    }
  }

  throw lastError || new Error("All TTS voice fallbacks failed");
}

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
 const { text, languageCode, pitch, voice, style } = req.body || {};
 if (!text || typeof text !== "string" || !text.trim()) {
   return res.status(400).json({ error: 'Missing "text" field in body' });
 }

 const lang = (languageCode || "he-IL").trim();
 const voiceKey = ((voice || "human") + "").toLowerCase().trim();

 const wantMale = voiceKey === "male";
 const wantFemale = voiceKey === "female";
 const wantHuman = voiceKey === "human";

 const preferredHumanVoice =
   lang === "he-IL"
     ? (wantFemale ? "he-IL-Chirp3-HD-Aoede" : "he-IL-Chirp3-HD-Charon")
     : null;

 console.log("kmiTts voice selection:", {
   lang,
   voiceKey,
   wantMale,
   wantFemale,
   wantHuman,
   preferredHumanVoice,
   preferred: lang === "he-IL"
     ? (
         wantFemale
           ? "he-IL-Chirp3-HD-Aoede -> he-IL-Chirp3-HD-Kore -> he-IL-Neural2-A -> he-IL-Wavenet-A"
           : "he-IL-Chirp3-HD-Charon -> he-IL-Chirp3-HD-Schedar -> he-IL-Neural2-B -> he-IL-Wavenet-B"
       )
     : "default",
   style: style || "default",
 });

    // ✅ FIX: ברירת מחדל "אנושית" = 1.0 (לא 0.45!)
    // וגם טווח הגיוני שלא יגרום לקול "רובוטי"
 const rawRateAny = (req.body || {}).speakingRate;
 const rawRateNum =
   typeof rawRateAny === "number"
     ? rawRateAny
     : (typeof rawRateAny === "string" ? Number(rawRateAny) : NaN);

 const baseRate =
   Number.isFinite(rawRateNum)
     ? Math.min(1.18, Math.max(0.96, rawRateNum))
     : 1.08;

 const resolvedStyle = ((style || detectTtsStyle(text) || "default") + "")
   .toLowerCase()
   .trim();

 const rate =
   resolvedStyle === "instruction" ? Math.min(1.18, baseRate + 0.04) :
   resolvedStyle === "warning" ? Math.min(1.12, baseRate) :
   resolvedStyle === "friendly" ? Math.max(0.96, baseRate - 0.03) :
   baseRate;

 console.log("kmiTts speakingRate:", {
   rawRateAny,
   rawType: typeof rawRateAny,
   rawRateNum,
   baseRate,
   resolvedStyle,
   rate,
 });

    // ✅ להחזיר headers כדי שנראה באנדרואיד מה באמת שימש
    res.set("X-KMI-Version", KMI_TTS_VERSION);
    res.set("X-KMI-Rate", String(rate));
    res.set("X-KMI-Style", String(resolvedStyle));

// 🔥 ניסיון ראשון – קול אנושי, רק אם נבחר human
if (wantHuman && preferredHumanVoice) {
  const humanResult = await synthesizeHumanVoice({
    text,
    lang,
    preferredHumanVoice,
  });

   if (humanResult) {
     console.log("kmiTts final response:", {
       version: KMI_TTS_VERSION,
       voice: humanResult.usedVoiceName,
       engine: humanResult.usedEngine,
       style: resolvedStyle,
       rate,
     });

     res.set("X-KMI-Voice", String(humanResult.usedVoiceName));
     res.set("X-KMI-Engine", String(humanResult.usedEngine));
     res.set("Content-Type", "audio/mpeg");

     return res.status(200).send(humanResult.audioContent);
   }
   }

// fallback רגיל
const { audioContent, usedVoiceName, usedEngine } = await synthesizeWithVoiceFallback({
  text,
  lang,
  voiceKey,
  rate,
  pitch,
  style: resolvedStyle,
});

  console.log("kmiTts final voice:", {
    voice: usedVoiceName,
    engine: usedEngine || "classic-fallback",
    style: resolvedStyle,
    rate,
  });

  console.log("kmiTts final response:", {
    version: KMI_TTS_VERSION,
    voice: usedVoiceName,
    engine: usedEngine || "classic-fallback",
    style: resolvedStyle,
    rate,
  });

  res.set("X-KMI-Voice", String(usedVoiceName));
  res.set("X-KMI-Engine", String(usedEngine || "classic-fallback"));
  res.set("Content-Type", "audio/mpeg");
  return res.status(200).send(audioContent);
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

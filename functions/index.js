/**
 * 예약 FCM 토픽 푸시 — Firestore `push_schedule` 폴링 (1분마다).
 * DB: named database `sapiens` (앱과 동일).
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const { getFirestore } = require("firebase-admin/firestore");

admin.initializeApp();

const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || "sapiens";

function db() {
  try {
    return getFirestore(admin.app(), DATABASE_ID);
  } catch (e) {
    functions.logger.warn("getFirestore(databaseId) 실패, 기본 DB 사용", e);
    return admin.firestore();
  }
}

exports.sendScheduledPush = functions
  .runWith({ timeoutSeconds: 60 })
  .pubsub.schedule("every 1 minutes")
  .onRun(async () => {
    const now = new Date();
    const nowMs = now.getTime();
    const snap = await db().collection("push_schedule").where("status", "==", "pending").get();

    for (const doc of snap.docs) {
      const data = doc.data() || {};
      const raw = data.scheduled_at;
      let whenMs = NaN;
      if (raw && typeof raw.toDate === "function") {
        whenMs = raw.toDate().getTime();
      } else if (typeof raw === "string") {
        whenMs = new Date(raw).getTime();
      }
      if (Number.isNaN(whenMs) || whenMs > nowMs) {
        continue;
      }

      const topic = (data.topic || "").trim();
      if (!topic) {
        functions.logger.warn("push_schedule 문서 topic 비어 있음", doc.id);
        continue;
      }

      try {
        await admin.messaging().send({
          topic,
          notification: {
            title: String(data.title || "Sapiens"),
            body: String(data.body || ""),
          },
          data: {
            section: String(data.section || ""),
          },
          android: {
            priority: "high",
            notification: {
              sound: "default",
              channelId: "sapiens_news",
            },
          },
        });
        await doc.ref.update({
          status: "sent",
          sent_at: now.toISOString(),
        });
        functions.logger.info("FCM 전송 완료", { doc: doc.id, topic });
      } catch (e) {
        functions.logger.error("FCM 전송 실패", doc.id, e);
      }
    }
    return null;
  });

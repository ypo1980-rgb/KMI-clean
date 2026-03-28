package il.kmi.app

import il.kmi.shared.domain.Belt
import java.net.URLEncoder

// קידוד בטוח ל-URL (לסגמנטי נתיב). URLEncoder מחליף רווחים ל-+,
// לכן מחליפים ל-%20 כדי ש-Navigation יפענח נכון.
private fun enc(value: String?): String =
    if (value.isNullOrEmpty()) ""
    else URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

sealed class Route(val route: String) {

    object Intro : Route("intro")
    object Home : Route("home")

    // ▼▼▼ חדש: מסך נחיתה של הרישום (התמונה עם "משתמש חדש / משתמש קיים")
    data object RegistrationLanding : Route("registration_landing")
    data object NewUserTrainee      : Route("new_user_trainee")
    data object NewUserCoach        : Route("new_user_coach")
    data object ExistingUserTrainee : Route("existing_user_trainee")
    data object ExistingUserCoach   : Route("existing_user_coach")

    // ✅ יישור קו עם שאר הראוטים
    data object MonthlyCalendar : Route("calendar_monthly")
    data object PhoneGate : Route("phone_auth_gate")   // ⬅️ חדש
    data object MyProfile : Route("my_profile")

    object Legal : Route("legal")
    object Splash : Route("splash")
    object CalendarMonthly : Route("calendar/monthly")

    object SmsEntry : Route("smsEntry/{openPrefix}") {
        fun make(openPrefix: Boolean) = "smsEntry/$openPrefix"
    }

    object RateUs : Route("rate_us")
    object Subscriptions : Route("subscriptions")
    object VoiceSettings : Route(route = "voice_settings")

    // ✅ NEW: מסך עוזר קולי (AiAssistantDialog כ-screen)
    object VoiceAssistant : Route(route = "voice_assistant")

    object BeltQ : Route("beltQ")
    object BeltQByTopic : Route("beltQ_by_topic")   // ✅ NEW: מסך תרגילים לפי נושא
    object Topics : Route("topics")
    object WeakPoints : Route("weak_points") // ✅ חדש

    // 🔐 חדש: אזור מנהל – ניהול משתמשים
    object AdminUsers : Route("admin_users")

    object Exam : Route("exam/{beltId}") {
        fun make(belt: Belt) = "exam/${belt.id}"
        fun makeId(beltId: String) = "exam/$beltId"
    }

    object RoleSelect : Route("roleSelect")
    object CoachBroadcast : Route("coachBroadcast")

    object Materials : Route("materials/{beltId}/{topic}?coach={coach}") {
        fun make(belt: Belt, topic: String, coach: Boolean = false) =
            "materials/${belt.id}/${enc(topic)}?coach=$coach"
        fun makeId(beltId: String, topic: String, coach: Boolean = false) =
            "materials/$beltId/${enc(topic)}?coach=$coach"
    }

    // גרסת Materials עם תת-נושא (תאימות לאחור נשמרת)
    object MaterialsSub : Route("materials/{beltId}/{topic}/{subTopic}") {

        fun make(belt: Belt, topic: String, subTopic: String?): String {
            val st = subTopic?.takeIf { it.isNotBlank() }
                ?: return Materials.make(belt, topic) // fallback בטוח
            return "materials/${belt.id}/${enc(topic)}/${enc(st)}"
        }

        fun makeId(beltId: String, topic: String, subTopic: String?): String {
            val st = subTopic?.takeIf { it.isNotBlank() }
                ?: return Materials.makeId(beltId, topic)
            return "materials/$beltId/${enc(topic)}/${enc(st)}"
        }
    }

    object AttendanceGroupStats : Route("attendance_group_stats/{branch}/{groupKey}") {

        fun make(branch: String, groupKey: String): String =
            "attendance_group_stats/${enc(branch)}/${enc(groupKey)}"
    }


    // ✅ מסך תרגילים לפי חגורה+נושא, ותת־נושא אופציונלי ב-query
    object TopicExercises : Route("topic_ex/{beltId}/{topic}?sub={sub}") {

        fun make(belt: Belt, topic: String, sub: String? = null): String {
            val base = "topic_ex/${belt.id}/${enc(topic)}"
            return if (sub.isNullOrBlank()) base else "$base?sub=${enc(sub)}"
        }

        fun makeId(beltId: String, topic: String, sub: String? = null): String {
            val base = "topic_ex/$beltId/${enc(topic)}"
            return if (sub.isNullOrBlank()) base else "$base?sub=${enc(sub)}"
        }
    }

    object Summary : Route("summary/{beltId}?topic={topic}&subTopic={subTopic}") {

        // ✅ חדש: מסך סיכום לפי חגורה+נושא (+ אופציונלי תת־נושא)
        fun make(belt: Belt, topic: String, subTopic: String? = null): String =
            "summary/${belt.id}?topic=${enc(topic)}&subTopic=${enc(subTopic)}"

        fun makeId(beltId: String, topic: String, subTopic: String? = null): String =
            "summary/$beltId?topic=${enc(topic)}&subTopic=${enc(subTopic)}"

        // ✅ תאימות לאחור (ישן): אם קראו make(belt) נשלח עם topic ריק
        fun make(belt: Belt): String =
            "summary/${belt.id}?topic=&subTopic="

        fun makeId(beltId: String): String =
            "summary/$beltId?topic=&subTopic="
    }

    // ✅ NEW: Training Summary (סיכום אימון)
    object TrainingSummary : Route("training_summary?date={date}") {

        fun make(dateIso: String? = null): String {
            val clean = dateIso?.trim().orEmpty()
            return if (clean.isBlank()) {
                "training_summary"
            } else {
                "training_summary?date=${enc(clean)}"
            }
        }
    }

    // ↓ הוסף לצד שאר המסלולים
    object AboutAvi : Route("about_avi")

    object Practice : Route("practice/{beltId}?topic={topic}") {
        fun make(belt: Belt, topic: String? = null): String =
            if (topic.isNullOrEmpty()) {
                "practice/${belt.id}"
            } else {
                "practice/${belt.id}?topic=${enc(topic)}"
            }

        fun makeId(beltId: String, topic: String? = null): String =
            if (topic.isNullOrEmpty()) {
                "practice/$beltId"
            } else {
                "practice/$beltId?topic=${enc(topic)}"
            }
    }

    object Settings : Route("settings")
    object Progress : Route("progress")

    // 🧪 Debug: בדיקת KMP Catalog + HTML (לעזור ל-iOS)
    object DebugCatalog : Route("debug_catalog")

    object Registration : Route("registration")
    object Favorites : Route("favorites")
    // ▼▼▼ מבחן פנימי – מסך המאמן
    object InternalExam : Route("internal_exam")

    object AboutMethod : Route("about_method")

    // ▼ מסך תרגיל בודד (ניווט עם /{id})
    object Exercise : Route("exercise/{id}") {
        fun make(id: String) = "exercise/$id"
    }

    // 👇 כאן התיקון – זה המסך הראשון של "ניהול מנוי"
    object Subscription : Route("subscription")

    object Forum : Route("forum")

    // 👇 זה נשאר כמו שהיה – זה המסך עם הכרטיסיות
    object SubscriptionPlans : Route("subscriptionPlans")

    object AboutNetwork : Route("about_network")

    // ▼▼▼ חדש: מסך "אודות איציק ביטון" (לשימוש מהמגירה)
    object AboutItzik : Route("about_itzik")

    // ▼▼▼ NEW: נוכחות – סימון יומי
    object AttendanceMark : Route("attendance/mark/{branch}/{groupKey}") {
        fun make(branch: String, groupKey: String) =
            "attendance/mark/${enc(branch)}/${enc(groupKey)}"
    }

    // ▼▼▼ NEW: נוכחות – סטטיסטיקות
    object AttendanceStats :
        Route("attendance/stats/{branch}/{groupKey}?memberId={memberId}&memberName={memberName}") {

        fun make(branch: String, groupKey: String, memberId: Long? = null, memberName: String? = null): String {
            val base = "attendance/stats/${enc(branch)}/${enc(groupKey)}"
            val qs = buildList {
                if (memberId != null) add("memberId=$memberId")
                if (!memberName.isNullOrBlank()) add("memberName=${enc(memberName)}")
            }
            return if (qs.isEmpty()) base else "$base?${qs.joinToString("&")}"
        }

        fun makeId(branch: String, groupKey: String, memberId: String? = null, memberName: String? = null): String {
            val base = "attendance/stats/${enc(branch)}/${enc(groupKey)}"
            val qs = buildList {
                if (!memberId.isNullOrBlank()) add("memberId=${enc(memberId)}")
                if (!memberName.isNullOrBlank()) add("memberName=${enc(memberName)}")
            }
            return if (qs.isEmpty()) base else "$base?${qs.joinToString("&")}"
        }
    }
}

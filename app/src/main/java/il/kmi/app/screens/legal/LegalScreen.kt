package il.kmi.app.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import il.kmi.app.localization.rememberIsEnglish
import il.kmi.app.ui.KmiTypography


//================================================================================

private data class LegalDocumentContent(
    val title: String,
    val sections: List<LegalSectionContent>
)

private data class LegalSectionContent(
    val title: String,
    val paragraphs: List<String>
)

private fun legalDocumentForTab(
    selectedTab: Int,
    isEnglish: Boolean
): LegalDocumentContent {

    fun section(
        titleHe: String,
        titleEn: String,
        paragraphsHe: List<String>,
        paragraphsEn: List<String>
    ): LegalSectionContent {
        return LegalSectionContent(
            title =
                if (isEnglish) {
                    titleEn
                } else {
                    titleHe
                },
            paragraphs =
                if (isEnglish) {
                    paragraphsEn
                } else {
                    paragraphsHe
                }
        )
    }

    return when (selectedTab) {

        /*
         * =====================================================
         * מדיניות פרטיות
         * =====================================================
         */
        1 -> {
            LegalDocumentContent(
                title =
                    if (isEnglish) {
                        "Privacy Policy"
                    } else {
                        "מדיניות פרטיות"
                    },
                sections =
                    listOf(
                        section(
                            titleHe = "1. כללי",
                            titleEn = "1. General",
                            paragraphsHe =
                                listOf(
                                    "1.1. מדיניות פרטיות זו (\"המדיניות\") מסבירה כיצד נאסף, נשמר, מעובד ומשמש מידע באפליקציית ק.מ.י – קרב מגן ישראלי, וכיצד נשמרות זכויותיך לפי הדין, לרבות לפי חוק הגנת הפרטיות, התשמ\"א–1981, והוראות כל דין רלוונטי אחר.",
                                    "1.2. השימוש באפליקציה מהווה הסכמה למדיניות זו, לרבות לאיסוף, עיבוד ושימוש במידע אודותיך כפי שמפורט בה. אם אינך מסכים למדיניות, עליך להפסיק להשתמש באפליקציה ולמחוק אותה ממכשירך.",
                                    "1.3. המדיניות מנוסחת בלשון זכר לצורך נוחות בלבד, אך פונה לכל המגדרים באופן שווה."
                                ),
                            paragraphsEn =
                                listOf(
                                    "1.1. This Privacy Policy (the \"Policy\") explains how information is collected, stored, processed and used in the KAMI – Israeli Krav Magen application, and how your rights are protected under applicable law, including the Israeli Protection of Privacy Law, 1981, and any other applicable legal provisions.",
                                    "1.2. Use of the application constitutes your consent to this Policy, including the collection, processing and use of information about you as described herein. If you do not agree to this Policy, you must stop using the application and remove it from your device.",
                                    "1.3. The Hebrew version of this Policy may use masculine wording for convenience only and is intended to apply equally to all genders."
                                )
                        ),

                        section(
                            titleHe = "2. סוגי מידע שנאספים",
                            titleEn = "2. Types of Information Collected",
                            paragraphsHe =
                                listOf(
                                    "2.1. מידע שנמסר על ידך ישירות: בעת הרשמה לאפליקציה, מילוי טפסים, יצירת פרופיל, עדכון פרטים, פנייה למפעיל או השתתפות בפורומים/צ'אטים, ייתכן שתתבקש למסור פרטים כגון: שם מלא, מספר טלפון, כתובת דוא\"ל, גיל/שנת לידה, חגורה/דרגה, שיוך לקבוצה/סניף, פרטי קשר נוספים, והערות שתבחר להזין.",
                                    "2.2. מידע על שימוש באפליקציה: נתונים סטטיסטיים ותפעוליים על אופן השימוש באפליקציה, כגון: מספר כניסות, זמני שימוש, מסכים שנצפו, כפתורים שנלחצו, סוגי תכנים שנצרכו, פעילות באימונים ובתרגולים, היסטוריית אימונים, נוכחות (במידה והפונקציה קיימת), מענה על שאלונים ועוד.",
                                    "2.3. מידע טכני מהמכשיר: כתובת IP (ככל שנאספת), סוג מכשיר, מערכת הפעלה, גרסת אפליקציה, מזהה ייחודי של המכשיר או מזהה פרסום (IDFA/GAID), שפה, הגדרות איזור וכדומה, ככל שהדבר דרוש לתפעול ושיפור השירות.",
                                    "2.4. מידע שמוזן על ידי מדריכים/מועדון: במידה והאפליקציה משמשת גם מדריכים ומועדונים, ייתכן שיירשם על שמך מידע כגון השתתפות באימונים, הישגים, חגורה, הערות מקצועיות, השתתפות באירועים, משובים ועוד.",
                                    "2.5. מידע שנאסף באמצעים אוטומטיים: ייתכן שימוש בכלים אנליטיים (למשל לצורך סטטיסטיקה, שיפור ביצועים, איתור תקלות) האוספים מידע אנונימי/מזוהה למחצה על אופן השימוש באפליקציה."
                                ),
                            paragraphsEn =
                                listOf(
                                    "2.1. Information provided directly by you: when registering for the application, completing forms, creating a profile, updating details, contacting the operator or participating in forums or chats, you may be asked to provide information such as your full name, telephone number, email address, age or year of birth, belt or rank, branch or group affiliation, additional contact details and comments you choose to enter.",
                                    "2.2. Application usage information: statistical and operational information about how the application is used, such as number of launches, usage times, screens viewed, buttons pressed, types of content consumed, activity in training and exercises, training history, attendance where applicable, questionnaire responses and similar information.",
                                    "2.3. Technical device information: IP address where collected, device type, operating system, application version, unique device identifier or advertising identifier such as IDFA or GAID, language, region settings and similar information, where required for operation and improvement of the service.",
                                    "2.4. Information entered by instructors or clubs: where the application is also used by instructors and clubs, information relating to you may be recorded, including training participation, achievements, belt, professional notes, event participation and feedback.",
                                    "2.5. Information collected automatically: analytical tools may be used for purposes such as statistics, performance improvement and fault detection, and may collect anonymous or partially identifiable information about use of the application."
                                )
                        ),

                        section(
                            titleHe = "3. מטרות השימוש במידע",
                            titleEn = "3. Purposes of Processing",
                            paragraphsHe =
                                listOf(
                                    "3.1. הפעלת האפליקציה ומתן השירותים, כולל יצירת חשבון משתמש, ניהול פרופיל, הצגת תכנים מותאמים, חישוב התקדמות, ניהול אימונים, הצגת היסטוריית פעילות ונוכחות ועוד.",
                                    "3.2. שיפור, תחזוקה ופיתוח של האפליקציה, לרבות ניתוח נתונים סטטיסטיים לשם הבנת דפוסי שימוש, איתור תקלות, התאמת חוויית המשתמש ושיפור ביצועים.",
                                    "3.3. יצירת קשר איתך במידת הצורך, למשל לצורך תמיכה, מענה לפניות, שליחת הודעות שירות, עדכונים טכניים ושינויים בתנאי השימוש ובמדיניות הפרטיות.",
                                    "3.4. שליחת הודעות תזכורת, עדכונים על אימונים, הודעות ממדריכים/מועדון, תכנים מקצועיים או הודעות שיווקיות מתונות – בכפוף להסכמתך ובהתאם לדין החל לגבי דיוור אלקטרוני.",
                                    "3.5. הגנה על זכויות, טיפול במחלוקות, בירור תלונות, אכיפת תנאי השימוש, מניעת שימוש לרעה, מניעת הונאות או פעילויות בלתי חוקיות.",
                                    "3.6. עמידה בהוראות כל דין, צווים שיפוטיים, דרישות של רשויות מוסמכות, וביצוע חובות חוקיות אחרות, ככל שהן חלות על המפעיל."
                                ),
                            paragraphsEn =
                                listOf(
                                    "3.1. Operating the application and providing its services, including creating user accounts, managing profiles, displaying personalized content, calculating progress, managing training sessions and displaying activity and attendance history.",
                                    "3.2. Improving, maintaining and developing the application, including analysis of statistical information to understand usage patterns, detect faults, adapt the user experience and improve performance.",
                                    "3.3. Contacting you when necessary, including for support, responding to inquiries, sending service notices, technical updates and changes to the Terms of Use or Privacy Policy.",
                                    "3.4. Sending reminders, training updates, instructor or club messages, professional content or limited marketing communications, subject to your consent and applicable electronic communications law.",
                                    "3.5. Protecting rights, handling disputes and complaints, enforcing the Terms of Use and preventing misuse, fraud or unlawful activity.",
                                    "3.6. Complying with applicable law, judicial orders, requests by competent authorities and other legal obligations applicable to the operator."
                                )
                        ),

                        section(
                            titleHe = "4. בסיס משפטי לעיבוד מידע",
                            titleEn = "4. Legal Basis for Processing",
                            paragraphsHe =
                                listOf(
                                    "4.1. עיבוד המידע נעשה, בין היתר, על בסיס אחד או יותר מהבאים: (א) הסכמתך למדיניות זו ולשימוש באפליקציה; (ב) הצורך לספק לך את השירותים שביקשת במסגרת האפליקציה; (ג) אינטרס לגיטימי של המפעיל בשיפור השירותים, הגנה על זכויותיו ומניעת שימוש לרעה; (ד) עמידה בהוראות כל דין."
                                ),
                            paragraphsEn =
                                listOf(
                                    "4.1. Information may be processed on one or more of the following bases: (a) your consent to this Policy and use of the application; (b) the need to provide services requested by you through the application; (c) the operator's legitimate interests in improving services, protecting rights and preventing misuse; and (d) compliance with applicable law."
                                )
                        ),

                        section(
                            titleHe = "5. שמירת מידע ואבטחתו",
                            titleEn = "5. Data Retention and Security",
                            paragraphsHe =
                                listOf(
                                    "5.1. המידע עשוי להישמר במאגרי מידע המנוהלים עבור המפעיל או על ידי ספקי שירות מטעמו, לרבות בשרתי ענן, בהתאם להוראות הדין.",
                                    "5.2. המפעיל ואו ספקי השירותים מטעמו נוקטים באמצעי אבטחה סבירים ומקובלים, שנועדו לצמצם את הסיכון לדליפת מידע, גישה בלתי מורשית, שימוש לרעה או שינוי בלתי מורשה במידע. יחד עם זאת, אין באפשרותם להבטיח אבטחה מוחלטת, והשימוש באפליקציה נעשה על אחריותך.",
                                    "5.3. המידע יישמר למשך התקופה הנדרשת לשם הגשמת המטרות שתוארו במדיניות זו, או לתקופה ארוכה יותר ככל שנדרש על פי דין או לצורך הגנה על זכויות המפעיל במקרה של סכסוך/הליך משפטי פוטנציאלי."
                                ),
                            paragraphsEn =
                                listOf(
                                    "5.1. Information may be stored in databases managed for the operator or by service providers acting on its behalf, including cloud servers, in accordance with applicable law.",
                                    "5.2. The operator and its service providers use reasonable and commonly accepted security measures intended to reduce the risk of information leakage, unauthorized access, misuse or unauthorized alteration. Nevertheless, absolute security cannot be guaranteed and use of the application is at your own risk.",
                                    "5.3. Information will be retained for as long as reasonably necessary to fulfil the purposes described in this Policy, or for a longer period where required by law or necessary to protect the operator's rights in connection with a dispute or potential legal proceeding."
                                )
                        ),

                        section(
                            titleHe = "6. מסירת מידע לצדדים שלישיים",
                            titleEn = "6. Disclosure to Third Parties",
                            paragraphsHe =
                                listOf(
                                    "6.1. המפעיל אינו מוכר את המידע האישי שלך לצדדים שלישיים. עם זאת, המידע עשוי להימסר, כולו או חלקו, לגורמים הבאים, ככל שנדרש ולפי העניין:",
                                    "6.1.1. ספקי שירות טכנולוגיים/ענן/אכסון/גיבוי/אבטחה/אנליטיקה המאפשרים את הפעלת האפליקציה ומתן השירותים (למשל שירותי ענן, שירותי הודעות, שירותי ניתוח נתונים).",
                                    "6.1.2. מדריכים, מועדונים או גופים הקשורים לפעילות ק.מ.י, ככל שהדבר נדרש לצורך ניהול האימונים, נוכחות, הדרגות והקשר השוטף בינך לבין המועדון/המדריך.",
                                    "6.1.3. יועצים מקצועיים (כגון עורכי דין, רואי חשבון) ככל שהדבר נדרש לשם הגנה על זכויות המפעיל, ניהול הליכים או עמידה בחובות חוקיות.",
                                    "6.1.4. רשויות מוסמכות, רגולטורים, בתי משפט וכל גורם אחר ככל שנדרש על פי דין, צו שיפוטי או בקשה חוקית מחייבת.",
                                    "6.2. ככל שהמידע יועבר אל מחוץ לגבולות ישראל, יעשה הדבר בהתאם לדין החל ובכפוף לנקיטת אמצעים סבירים לשמירה על פרטיותך."
                                ),
                            paragraphsEn =
                                listOf(
                                    "6.1. The operator does not sell your personal information to third parties. However, information may be disclosed, in whole or in part and where necessary, to the following parties:",
                                    "6.1.1. Technology, cloud, hosting, backup, security or analytics providers that enable operation of the application and provision of services.",
                                    "6.1.2. Instructors, clubs or organizations connected with KAMI activities where required for management of training, attendance, ranks and ongoing communication between you and your club or instructor.",
                                    "6.1.3. Professional advisers, including lawyers and accountants, where required to protect the operator's rights, manage proceedings or comply with legal obligations.",
                                    "6.1.4. Competent authorities, regulators, courts or other parties where required by law, judicial order or other binding lawful request.",
                                    "6.2. Where information is transferred outside Israel, such transfer will be carried out in accordance with applicable law and subject to reasonable measures intended to protect your privacy."
                                )
                        ),

                        section(
                            titleHe = "7. עוגיות, טכנולוגיות מעקב וניתוח שימוש",
                            titleEn = "7. Tracking Technologies and Analytics",
                            paragraphsHe =
                                listOf(
                                    "7.1. האפליקציה עשויה לעשות שימוש בטכנולוגיות מעקב וניתוח (דוגמת ספריות אנליטיקה, SDKs או טכנולוגיות דומות) לצורך איסוף מידע על השימוש באפליקציה, לשיפור השירותים, התאמתם ולצורכי אבטחה.",
                                    "7.2. שימוש כזה יתבצע בכפוף לדין החל ויכוון, ככל האפשר, למידע סטטיסטי ומצרפי, שאינו מזהה אותך באופן אישי, אלא אם יש צורך בזיהוי לשם תפעול או אבטחה.",
                                    "7.3. במידת האפשר, ניתן יהיה להגביל או לשנות חלק מההגדרות באמצעות הגדרות המכשיר שלך או הגדרות האפליקציה, אולם ייתכן שהגבלות מסוימות ישפיעו על חוויית השימוש."
                                ),
                            paragraphsEn =
                                listOf(
                                    "7.1. The application may use tracking and analytics technologies, such as analytics libraries, SDKs or similar technologies, to collect information about use of the application for service improvement, customization and security.",
                                    "7.2. Such use will be subject to applicable law and, where possible, directed toward statistical and aggregated information that does not personally identify you, unless identification is required for operation or security.",
                                    "7.3. Where technically available, some settings may be limited or changed through your device or application settings, although certain restrictions may affect the user experience."
                                )
                        ),

                        section(
                            titleHe = "8. זכויות המשתמש במידע האישי",
                            titleEn = "8. Your Rights",
                            paragraphsHe =
                                listOf(
                                    "8.1. בכפוף לדין החל, אתה רשאי לעיין במידע האישי שנשמר אודותיך במאגרי המפעיל, ולבקש את תיקונו אם הוא שגוי, לא מעודכן או לא שלם.",
                                    "8.2. במקרים מסוימים, ובהתאם להוראות הדין, אתה רשאי לבקש את מחיקת המידע האישי שנשמר אודותיך, או הגבלת עיבודו. המפעיל יבחן כל בקשה כזו בהתאם לחובותיו החוקיות והחוזיות.",
                                    "8.3. לצורך מימוש זכויות אלה, ניתן לפנות אל המפעיל בכתובת הדוא\"ל או באמצעי התקשרות אחרים המופיעים באפליקציה. המפעיל רשאי לבקש פרטים מזהים נוספים כדי לוודא את זהותך."
                                ),
                            paragraphsEn =
                                listOf(
                                    "8.1. Subject to applicable law, you may request access to personal information maintained about you and request correction where such information is inaccurate, outdated or incomplete.",
                                    "8.2. In certain circumstances and subject to applicable law, you may request deletion of your personal information or restriction of its processing. Each request will be reviewed in accordance with the operator's legal and contractual obligations.",
                                    "8.3. To exercise these rights, you may contact the operator using the email address or other contact method shown in the application. Additional identifying information may be requested in order to verify your identity."
                                )
                        ),

                        section(
                            titleHe = "9. קטינים ופרטיותם",
                            titleEn = "9. Minors and Privacy",
                            paragraphsHe =
                                listOf(
                                    "9.1. כאשר מדובר בקטינים, איסוף ושימוש במידע יתבצע בכפוף להסכמת האפוטרופוס החוקי ולפי הדין החל.",
                                    "9.2. אפוטרופוס יכול לבקש לעיין במידע שנאסף על הקטין שבאחריותו, לבקש את תיקונו או מחיקתו, הכל בהתאם לדין ולנסיבות.",
                                    "9.3. המפעיל אינו פונה במודע לקטינים מתחת לגיל שהחוק מתייחס אליו ללא הסכמת אפוטרופוס, ואינו מבקש מהם באופן ישיר מסירת מידע מעבר לנדרש להפעלת השירות."
                                ),
                            paragraphsEn =
                                listOf(
                                    "9.1. Where a user is a minor, collection and use of information will be subject to the consent of a legal guardian and applicable law.",
                                    "9.2. A guardian may request access to information collected regarding a minor under the guardian's responsibility and may request its correction or deletion, subject to applicable law and the circumstances.",
                                    "9.3. The operator does not knowingly target minors below the applicable legal age without guardian consent and does not intentionally request information from them beyond that required to operate the service."
                                )
                        ),

                        section(
                            titleHe = "10. שמירת נתוני גיבוי ותיעוד",
                            titleEn = "10. Backup and Archival Data",
                            paragraphsHe =
                                listOf(
                                    "10.1. ייתכן שהמידע יישמר גם במערכות גיבוי ואחסון לטווח ארוך יותר לצרכי המשכיות עסקית, התאוששות מאסון, הגנה משפטית או תיעוד לצורך ניהול טענות ומחלוקות.",
                                    "10.2. מידע שנמחק או תוקן במערכות הפעילות עשוי להישאר לתקופה מסוימת בגיבויים עד למחיקה/החלפה תקופתית שלהם, בהתאם למדיניות הגיבוי של המפעיל."
                                ),
                            paragraphsEn =
                                listOf(
                                    "10.1. Information may also be retained in backup and longer-term storage systems for business continuity, disaster recovery, legal protection or documentation required for managing claims and disputes.",
                                    "10.2. Information deleted or corrected in active systems may remain in backup systems for a limited period until those backups are periodically deleted or replaced in accordance with the operator's backup practices."
                                )
                        ),

                        section(
                            titleHe = "11. שינויים במדיניות הפרטיות",
                            titleEn = "11. Changes to this Privacy Policy",
                            paragraphsHe =
                                listOf(
                                    "11.1. המפעיל רשאי לעדכן את המדיניות מעת לעת. המדיניות המעודכנת תפורסם באפליקציה ותחול מרגע פרסומה, אלא אם צוין אחרת.",
                                    "11.2. אם יבוצעו שינויים מהותיים באופן השימוש במידע האישי, המפעיל עשוי להודיע לך על כך באמצעי סביר (למשל באמצעות הודעה באפליקציה). המשך השימוש באפליקציה לאחר עדכון המדיניות מהווה הסכמה למדיניות המעודכנת."
                                ),
                            paragraphsEn =
                                listOf(
                                    "11.1. The operator may update this Policy from time to time. The updated version will be published in the application and will apply from the time of publication unless stated otherwise.",
                                    "11.2. If material changes are made to the way personal information is used, the operator may provide notice through a reasonable method, such as an in-app notification. Continued use of the application following an update constitutes acceptance of the updated Policy."
                                )
                        ),

                        section(
                            titleHe = "12. יצירת קשר בנושא פרטיות",
                            titleEn = "12. Privacy Contact",
                            paragraphsHe =
                                listOf(
                                    "12.1. לכל שאלה, בקשה או תלונה הנוגעת למדיניות פרטיות זו ו/או לאופן שבו מטופל המידע האישי שלך, ניתן לפנות למפעיל באמצעי ההתקשרות המופיעים באפליקציה. המפעיל ישתדל לטפל בפניות בתוך זמן סביר ובהתאם להוראות הדין."
                                ),
                            paragraphsEn =
                                listOf(
                                    "12.1. For any question, request or complaint relating to this Privacy Policy or the handling of your personal information, you may contact the operator through the contact methods displayed in the application. The operator will make reasonable efforts to respond within a reasonable period and in accordance with applicable law."
                                )
                        )
                    )
            )
        }

        /*
         * =====================================================
         * הצהרת נגישות
         * =====================================================
         */
        2 -> {
            LegalDocumentContent(
                title =
                    if (isEnglish) {
                        "Accessibility Statement"
                    } else {
                        "הצהרת נגישות"
                    },
                sections =
                    listOf(
                        section(
                            "1. כללי",
                            "1. General",
                            listOf(
                                "1.1. אפליקציית ק.מ.י – קרב מגן ישראלי (להלן: \"האפליקציה\") שואפת להיות נגישה ושוויונית עבור כלל המשתמשים, ובכלל זה אנשים עם מוגבלויות שונות.",
                                "1.2. המפעיל רואה חשיבות רבה בהנגשת האפליקציה ובהתאמתה לצרכיהם של אנשים עם מוגבלות, מתוך תפיסה כי לכל אדם זכות לשוויון, כבוד, עצמאות ושימוש נוח בשירותים דיגיטליים.",
                                "1.3. ההנגשה מבוצעת, ככל הניתן, בהתאם לעקרונות התקן הישראלי ת\"י 5568 (הנגשת אתרי אינטרנט) ועקרונות הנגישות של WCAG 2.1 ברמת AA, בשינויים המחויבים לסביבת אפליקציה סלולרית.",
                                "1.4. יחד עם זאת, ייתכן שחלק מהתכנים או הפונקציות טרם הונגשו במלואם, או שאינם מותאמים לכל סוגי המוגבלויות והמסכים. אנו פועלים באופן מתמשך לשיפור ולמענה על פערים שיידעו לנו."
                            ),
                            listOf(
                                "1.1. The KAMI – Israeli Krav Magen application (the \"Application\") aims to provide an accessible and equitable experience for all users, including people with various disabilities.",
                                "1.2. The operator places significant importance on accessibility and on adapting the Application to the needs of people with disabilities, based on the principle that every person is entitled to equality, dignity, independence and convenient access to digital services.",
                                "1.3. Accessibility efforts are implemented, where reasonably possible, in accordance with the principles of Israeli Standard 5568 and the WCAG 2.1 Level AA accessibility principles, as adapted to the mobile application environment.",
                                "1.4. Nevertheless, certain content or functions may not yet be fully accessible or suitable for every type of disability or device. Accessibility improvements are an ongoing process."
                            )
                        ),

                        section(
                            "2. התאמות נגישות עיקריות באפליקציה",
                            "2. Main Accessibility Features",
                            listOf(
                                "2.1. ניגודיות וצבעים – נעשה ניסיון לשמור על ניגודיות מספקת בין טקסט לרקע, שימוש בצבעים מובחנים, והימנעות מהסתמכות בלעדית על צבע לצורך הבנת המידע.",
                                "2.2. גודל טקסט – האפליקציה מאפשרת למשתמש לבחור במסך ההגדרות בין שלושה גדלי תצוגה: קטן, בינוני וגדול. הבחירה מוחלת באופן אחיד על הטקסטים והרכיבים הרלוונטיים באפליקציה, במטרה לשפר את הקריאות ולהתאים את התצוגה לצורכי המשתמש.",
                                "2.3. מצב כהה (Dark Mode) – האפליקציה מאפשרת מעבר למצב כהה/בהיר או שימוש במצב מערכת, על מנת לשפר את הנוחות וקריאות התכנים למשתמשים הרגישים לאור.",
                                "2.4. ניווט ברור – המסכים מסודרים במבנה עקבי, עם כותרת ברורה, תפריט תחתון קבוע ופעולות מרכזיות הנגישות מהאזור התחתון של המסך.",
                                "2.5. כפתורים ולחצנים – מרבית האזורים הלחיצים הם בעלי שטח מגע מוגדל, טקסט מלווה ואייקונים ברורים, במטרה להקל על משתמשים המתקשים בתנועה עדינה.",
                                "2.6. טקסט אלטרנטיבי – באזורים רלוונטיים נעשה ניסיון לספק תיאורים טקסטואליים לאלמנטים איקוניים, כך שקוראי מסך יוכלו למסור מידע משמעותי על הפעולות.",
                                "2.7. נגישות קולית – חלק מהתרגילים וההסברים באפליקציה עשויים להיות מלווים בהקראה קולית או בתוכן קולי משלים (ככל שהפיצ'ר פעיל במכשיר ובאפליקציה)."
                            ),
                            listOf(
                                "2.1. Contrast and colors – efforts are made to maintain sufficient contrast between text and backgrounds, use distinguishable colors and avoid relying solely on color to communicate information.",
                                "2.2. Text size – the application allows users to select Small, Medium or Large display size in the Settings screen. The selected size is applied consistently to relevant text and interface elements in order to improve readability and adapt the display to the user's needs.",
                                "2.3. Dark Mode – the application supports light, dark or system-based appearance in order to improve comfort and readability.",
                                "2.4. Clear navigation – application screens use a consistent structure, clear titles and accessible primary actions.",
                                "2.5. Buttons and controls – most interactive areas are designed with sufficiently large touch targets, supporting text and recognizable icons.",
                                "2.6. Alternative text – where relevant, efforts are made to provide meaningful textual descriptions for icon-based elements so that screen readers can communicate their purpose.",
                                "2.7. Audio accessibility – certain exercises and explanations may include spoken or supplementary audio content where the relevant feature is available and enabled."
                            )
                        ),

                        section(
                            "3. שימוש בקורא מסך ואמצעי נגישות מובנים במכשיר",
                            "3. Screen Readers and Device Accessibility",
                            listOf(
                                "3.1. האפליקציה פועלת בסביבת מערכת ההפעלה של המכשיר (Android), ולכן היא נשענת גם על מנגנוני הנגישות שמציעה מערכת ההפעלה, כגון קורא מסך (TalkBack), הגדלת תצוגה, ניגודיות גבוהה, סינון צבעים ועוד.",
                                "3.2. לצורך חוויית שימוש מיטבית, מומלץ להפעיל את הגדרות הנגישות המתאימות עבורך בהגדרות המכשיר, ולוודא שהן מעודכנות ופעילות.",
                                "3.3. ייתכן שבגרסאות שונות של מערכת ההפעלה או מכשירים שונים יהיו הבדלים באופן שבו קוראי המסך מציגים את התכנים. אנו פועלים להפחתת תקלות שנובעות מהבדלים אלה ככל שניתן."
                            ),
                            listOf(
                                "3.1. The Application operates within the device operating system environment and therefore also relies on built-in accessibility features such as TalkBack, display magnification, high contrast and color filtering.",
                                "3.2. For the best possible experience, users are encouraged to enable the accessibility features appropriate to their needs in the device settings and ensure that those features are active and up to date.",
                                "3.3. Different operating system versions and devices may cause differences in the way screen readers present content. Reasonable efforts are made to reduce issues resulting from such differences."
                            )
                        ),

                        section(
                            "4. מגבלות נוכחיות בהנגשה",
                            "4. Current Accessibility Limitations",
                            listOf(
                                "4.1. תכני וידאו, תמונות והדגמות תנועה – חלק מההדגמות באפליקציה מבוססות על וידאו או גרפיקה תנועתית. ייתכן שלא לכל סרטון קיימים כתוביות מלאות, תיאורי אודיו או תיאור טקסטואלי מפורט.",
                                "4.2. תכני צד שלישי – באזורים בהם מוטמעים תכנים או שירותים חיצוניים (כגון קישורים לאתרים חיצוניים, מדיה מוטמעת או מערכות של ספקים חיצוניים), ייתכן שרמת הנגישות תלויה בצד השלישי ואינה בשליטה מלאה של המפעיל.",
                                "4.3. ממשקים חדשים – רכיבים ופונקציות חדשים המתווספים לאפליקציה מעת לעת עשויים להופיע בשלב ראשון לפני השלמת כל בדיקות הנגישות. אנו שואפים להשלים התאמות בהקדם האפשרי.",
                                "4.4. שפות – עיקר התוכן באפליקציה מיועד בשלב זה בעברית. בעת שימוש בשפות אחרות ייתכנו חוסרים בניקוד, יישור טקסט, כיווניות (RTL/LTR) ותרגום חלקי בלבד."
                            ),
                            listOf(
                                "4.1. Video, images and motion demonstrations – some demonstrations rely on video or animated graphics and may not yet include complete captions, audio descriptions or detailed textual alternatives.",
                                "4.2. Third-party content – where external content or services are embedded or linked, accessibility may depend on the relevant third party and may not be fully controlled by the operator.",
                                "4.3. New interfaces – newly introduced components or functions may occasionally be released before every accessibility test or adjustment has been completed. The goal is to complete necessary adaptations as soon as reasonably possible.",
                                "4.4. Languages – certain accessibility characteristics, layout direction and translations may vary between Hebrew and other supported languages."
                            )
                        ),

                        section(
                            "5. אחריות ושיפור מתמיד",
                            "5. Ongoing Improvement",
                            listOf(
                                "5.1. המפעיל רואה בתהליך הנגישות תהליך מתמשך. אנו פועלים לשפר את חוויית השימוש, לתקן תקלות שנתגלו, ולהוסיף התאמות נוספות ככל שהדבר מתאפשר מבחינה טכנולוגית ותפעולית.",
                                "5.2. למרות המאמצים להשגת רמת נגישות גבוהה, ייתכן ותיתקלו בקשיים בשימוש באפליקציה, לרבות עקב שילוב בין מגבלות טכנולוגיות לבין מגבלות מכשיר/מערכת הפעלה.",
                                "5.3. איננו מתחייבים כי כל חלקי האפליקציה יהיו נגישים בכל זמן ולכל סוגי המוגבלויות, אך אנו מתחייבים לבחון כל פנייה עניינית בנושא ולנסות לספק פתרון הולם במידת האפשר."
                            ),
                            listOf(
                                "5.1. Accessibility is regarded as an ongoing process. Efforts are made to improve the user experience, correct identified issues and introduce additional adaptations where technologically and operationally feasible.",
                                "5.2. Despite efforts to achieve a high level of accessibility, difficulties may still occur due to technological limitations or differences between devices and operating systems.",
                                "5.3. Full accessibility of every part of the Application at all times and for every disability cannot be guaranteed, but reasonable accessibility-related inquiries will be reviewed and appropriate solutions will be considered where possible."
                            )
                        ),

                        section(
                            "6. יצירת קשר בנושא נגישות",
                            "6. Accessibility Contact",
                            listOf(
                                "6.1. אם נתקלת בקושי נגישות, תקלה בחוויית שימוש, בעיה בקריאת טקסט, הפעלת כפתור או שימוש בעזרי נגישות – נשמח מאוד לדעת על כך.",
                                "6.2. ניתן לפנות אלינו בנושא נגישות באמצעי ההתקשרות המופיעים באפליקציה (למשל טופס יצירת קשר, דוא\"ל או טלפון). בפנייה חשוב לציין, ככל שניתן:",
                                "6.2.1. תיאור קצר של הבעיה (מה ניסית לעשות ומה לא עבד).",
                                "6.2.2. סוג המכשיר ומערכת ההפעלה (למשל: Android 14, מכשיר מסוג X).",
                                "6.2.3. גרסת האפליקציה (ככל שניתן לראות במסך \"אודות\" או בחנות).",
                                "6.2.4. כלי נגישות פעילים במכשיר (למשל TalkBack, הגדלת טקסט, ניגודיות גבוהה וכו').",
                                "6.3. המפעיל יעשה מאמץ לטפל בפנייתך בנושא נגישות בהקדם האפשרי, ולתת מענה ענייני ומכבד.",
                                "6.4. ניתן לראות בפנייה מצדך תרומה משמעותית לשיפור השירות ולהנגשת האפליקציה עבור כלל המשתמשים, ואנו מודים מראש על שיתוף הפעולה."
                            ),
                            listOf(
                                "6.1. If you encounter an accessibility difficulty, usability issue, problem reading text, activating a control or using an accessibility aid, we encourage you to report it.",
                                "6.2. Accessibility inquiries may be submitted using the contact methods displayed in the Application, such as a contact form, email or telephone number. Where possible, please include:",
                                "6.2.1. A brief description of the problem and what you attempted to do.",
                                "6.2.2. Your device type and operating system version.",
                                "6.2.3. The Application version, where available.",
                                "6.2.4. Any active accessibility tools, such as TalkBack, display magnification or high-contrast settings.",
                                "6.3. Reasonable efforts will be made to review accessibility inquiries promptly and respond appropriately.",
                                "6.4. Accessibility feedback contributes to improvement of the service for all users and is appreciated."
                            )
                        ),

                        section(
                            "7. עדכון הצהרת הנגישות",
                            "7. Updates to this Accessibility Statement",
                            listOf(
                                "7.1. הצהרת נגישות זו עודכנה לאחרונה במועד הקרוב למועד שחרור הגרסה העדכנית של האפליקציה.",
                                "7.2. ייתכנו מעת לעת שינויים בתכני ההצהרה, בין אם עקב שינויי רגולציה ובין אם עקב שיפורים באפליקציה ובמנגנוני הנגישות שלה.",
                                "7.3. נוסח ההצהרה העדכני תמיד יוצג בתוך האפליקציה עצמה, בטאב \"הצהרת נגישות\" זה."
                            ),
                            listOf(
                                "7.1. This Accessibility Statement is updated periodically in connection with releases and improvements to the Application.",
                                "7.2. The Statement may change from time to time due to regulatory developments or improvements to the Application and its accessibility mechanisms.",
                                "7.3. The current version of the Statement will be displayed within the Application in the Accessibility tab."
                            )
                        )
                    )
            )
        }

        /*
         * =====================================================
         * תנאי שימוש
         * =====================================================
         */
        else -> {
            LegalDocumentContent(
                title =
                    if (isEnglish) {
                        "Terms of Use"
                    } else {
                        "תנאי שימוש"
                    },
                sections =
                    listOf(
                        section(
                            "1. כללי והסכמה לתנאים",
                            "1. General and Acceptance",
                            listOf(
                                "1.1. אפליקציית ק.מ.י – קרב מגן ישראלי (להלן: \"האפליקציה\") מופעלת ומנוהלת על ידי יובל פולק (להלן: \"המפעיל\").",
                                "1.2. תנאי שימוש אלה (להלן: \"התנאים\") מסדירים את מכלול השימוש באפליקציה, בתכנים ובשירותים המוצעים בה. השימוש באפליקציה מכל סוג שהוא מהווה את הסכמתך המלאה, הבלתי חוזרת והבלתי מסויגת לתנאים אלה, לרבות כל שינוי שייערך בהם מעת לעת.",
                                "1.3. אם אינך מסכים לאיזה מתנאי השימוש, הנך נדרש להפסיק מיד את השימוש באפליקציה, למחוק אותה מן המכשיר ולא לעשות בה כל שימוש נוסף.",
                                "1.4. האפליקציה מיועדת למשתמשים מעל גיל 18. שימוש על ידי קטינים יתאפשר רק באישור ובפיקוח אפוטרופוס חוקי ו/או במסגרת פעילות מועדון/מדריך מוסמך, והאפוטרופוס הוא הנושא במלוא האחריות לשימוש הקטין באפליקציה.",
                                "1.5. המפעיל רשאי לעדכן את התנאים בכל עת, לפי שיקול דעתו הבלעדי. מועד העדכון האחרון עשוי להיות מוצג בתוך האפליקציה. המשך שימוש באפליקציה לאחר עדכון התנאים מהווה הסכמה שלך לתנאים המעודכנים."
                            ),
                            listOf(
                                "1.1. The KAMI – Israeli Krav Magen application (the \"Application\") is operated and managed by Yuval Pollak (the \"Operator\").",
                                "1.2. These Terms of Use (the \"Terms\") govern all use of the Application, its content and the services offered through it. Any use of the Application constitutes your full and unconditional acceptance of these Terms, including any amendments made from time to time.",
                                "1.3. If you do not agree to any provision of these Terms, you must immediately stop using the Application, remove it from your device and refrain from further use.",
                                "1.4. The Application is intended for users over the age of 18. Use by minors is permitted only with the approval and supervision of a legal guardian and/or within the framework of activities managed by a club or qualified instructor. Responsibility for a minor's use rests with the relevant guardian.",
                                "1.5. The Operator may update these Terms at any time. The most recent update date may be displayed in the Application. Continued use after an update constitutes acceptance of the updated Terms."
                            )
                        ),

                        section(
                            "2. מהות האפליקציה והשירותים",
                            "2. Nature of the Application and Services",
                            listOf(
                                "2.1. האפליקציה נועדה לספק למשתמשים מידע, תכנים, אימונים, הסברים, חומרי עזר, תיעוד השתתפות ואפשרויות תקשורת בתחום קרב מגן ישראלי (ק.מ.י), לרבות תרגילים, הסברים טכניים, חומרי לימוד, משובים ותיעוד התקדמות.",
                                "2.2. כל המידע והתכנים הניתנים באפליקציה הם מידע כללי וחינוכי בלבד ואינם מהווים ייעוץ מקצועי, הדרכה מוסמכת, המלצה בריאותית, ייעוץ רפואי, ייעוץ משפטי, פסיכולוגי או כל ייעוץ אחר.",
                                "2.3. האפליקציה אינה מחליפה אימון עם מדריך מוסמך ו/או השתתפות בשיעורים מסודרים, ואינה מבטיחה תוצאות, שיפור כושר, קבלת דרגה/חגורה או הצלחה בבחינות כלשהן.",
                                "2.4. המפעיל רשאי להוסיף, להסיר, לשנות או להפסיק כל שירות, פונקציה או תוכן באפליקציה, כולם או חלקם, בכל עת וללא הודעה מוקדמת."
                            ),
                            listOf(
                                "2.1. The Application is intended to provide users with information, content, training materials, explanations, learning resources, participation records and communication features relating to KAMI, including exercises, technical explanations, educational materials, feedback and progress records.",
                                "2.2. All information and content provided through the Application is for general educational purposes only and does not constitute professional advice, certified instruction, health advice, medical advice, legal advice, psychological advice or any other professional advice.",
                                "2.3. The Application does not replace training with a qualified instructor or participation in organized classes and does not guarantee results, improved fitness, award of any belt or rank or success in examinations.",
                                "2.4. The Operator may add, remove, change or discontinue any service, function or content, in whole or in part, at any time and without prior notice."
                            )
                        ),

                        section(
                            "3. רישיון שימוש מוגבל",
                            "3. Limited License",
                            listOf(
                                "3.1. בכפוף לעמידתך המלאה בתנאים אלה, מעניק לך המפעיל רישיון שימוש אישי, מוגבל, הדיר, לא בלעדי, בלתי ניתן להעברה ובלתי ניתן לרישוי-משנה, לשימוש באפליקציה למטרות פרטיות ולימודיות בלבד, על גבי מכשיר קצה שבבעלותך או בשליטתך.",
                                "3.2. אין להשתמש באפליקציה לכל מטרה מסחרית, עסקית, מוסדית או ציבורית ללא קבלת רישיון מפורש, מראש ובכתב מהמפעיל.",
                                "3.3. המפעיל רשאי לבטל את רישיון השימוש שלך באפליקציה בכל עת, לפי שיקול דעתו הבלעדי, לרבות עקב הפרת תנאי שימוש אלה, וזאת ללא צורך במתן הודעה מוקדמת וללא שנדרש לפצות אותך."
                            ),
                            listOf(
                                "3.1. Subject to full compliance with these Terms, the Operator grants you a personal, limited, revocable, non-exclusive, non-transferable and non-sublicensable license to use the Application for private and educational purposes only on a device owned or controlled by you.",
                                "3.2. The Application may not be used for commercial, business, institutional or public purposes without the Operator's prior express written permission.",
                                "3.3. The Operator may revoke your license to use the Application at any time, including in the event of a breach of these Terms, without prior notice and without any obligation to compensate you."
                            )
                        ),

                        section(
                            "4. אחריות המשתמש והצהרותיו",
                            "4. User Responsibility",
                            listOf(
                                "4.1. השימוש באפליקציה, בתכניה ובשירותים הנלווים הוא על אחריותך הבלעדית והמלאה. כל פעולה שתבצע בהסתמך על תכני האפליקציה – לרבות ביצוע תרגילים פיזיים – נעשית לפי שיקול דעתך בלבד ועל אחריותך המלאה.",
                                "4.2. ביצוע תרגילי קרב מגן ופעילות גופנית כרוך מטבעו בסיכון לפציעות ו/או נזקים פיזיים. בכניסתך לאפליקציה אתה מצהיר כי מצבך הרפואי מאפשר השתתפות בפעילות גופנית וכי נועצת, במידת הצורך, ברופא/גורם רפואי מתאים.",
                                "4.3. אתה מתחייב לבצע כל תרגיל אך ורק בסביבה בטוחה, מתאימה וללא סיכון מיותר לעצמך ולזולת, ובכפוף לכל הדרכה והנחיה של מדריך מוסמך, ככל שישנו.",
                                "4.4. חל איסור מוחלט לעשות שימוש בתכני האפליקציה לצורך אלימות, תקיפה, פגיעה בגוף או ברכוש, איום, הטרדה, או לכל מטרה בלתי חוקית אחרת.",
                                "4.5. אתה מתחייב שלא לבצע באפליקציה כל פעולה המהווה הפרת דין, לרבות אך לא רק: חדירה לא מורשית למערכות, ניסיון לעקוף מנגנוני אבטחה, שליחת קוד זדוני, שימוש אוטומטי (\"בוטים\"), או ניסיון לשבש את פעילות האפליקציה."
                            ),
                            listOf(
                                "4.1. Use of the Application, its content and related services is entirely at your own responsibility. Any action taken in reliance on Application content, including physical exercises, is performed at your sole discretion and risk.",
                                "4.2. Martial arts training and physical activity inherently involve a risk of injury or physical harm. By using the Application you represent that your medical condition permits participation in physical activity and that you have consulted an appropriate medical professional where necessary.",
                                "4.3. You agree to perform exercises only in a safe and appropriate environment, without creating unnecessary risk to yourself or others, and subject to the instructions of a qualified instructor where applicable.",
                                "4.4. Use of Application content for violence, assault, bodily or property damage, threats, harassment or any unlawful purpose is strictly prohibited.",
                                "4.5. You must not engage in unlawful conduct through the Application, including unauthorized access to systems, attempts to bypass security mechanisms, distribution of malicious code, automated bot activity or attempts to interfere with Application operation."
                            )
                        ),

                        section(
                            "5. שימוש בקטינים, הורים ומדריכים",
                            "5. Minors, Guardians and Instructors",
                            listOf(
                                "5.1. במקרה של שימוש קטין באפליקציה, האחריות המלאה לשימוש הקטין חלה על האפוטרופוס החוקי ו/או על המועדון/המדריך המנהל את המשתמש עבורו.",
                                "5.2. האפוטרופוס מצהיר כי הוא מודע לתכני האפליקציה, וכי הוא אחראי לפקח על השימוש של הקטין ולוודא שהשימוש נעשה באופן בטוח, זהיר ובהתאם לכל דין.",
                                "5.3. המפעיל לא יישא בכל אחריות לכל נזק שייגרם לקטין או לצד שלישי עקב שימוש הקטין באפליקציה שלא בהתאם להוראות אלה."
                            ),
                            listOf(
                                "5.1. Where a minor uses the Application, responsibility for that use rests with the legal guardian and/or the club or instructor managing the minor's account.",
                                "5.2. The guardian acknowledges the nature of the Application content and is responsible for supervising the minor's use and ensuring that such use is safe, careful and lawful.",
                                "5.3. The Operator will not be responsible for harm caused to a minor or third party as a result of use by a minor contrary to these provisions."
                            )
                        ),

                        section(
                            "6. תכנים, זכויות יוצרים וקניין רוחני",
                            "6. Content and Intellectual Property",
                            listOf(
                                "6.1. כל זכויות הקניין הרוחני באפליקציה – לרבות אך לא רק: קוד מקור, עיצוב, טקסטים, תכני וידאו ואודיו, תמונות, לוגו, סימני מסחר, תרשימים, מסדי נתונים וכל תוכן אחר – שייכות למפעיל ו/או לצדדים שלישיים שהעניקו למפעיל זכויות שימוש בהם.",
                                "6.2. אין להעתיק, לשכפל, לתרגם, להפיץ, לשדר, להעמיד לרשות הציבור, לבצע, להציג, למכור, להשכיר, לערוך, לעבד, להנדס לאחור, לפרק, או לעשות כל שימוש אחר בתכני האפליקציה, כולם או חלקם, אלא אם ניתנה לכך הסכמה מפורשת, מראש ובכתב מהמפעיל.",
                                "6.3. סימני המסחר והלוגואים המופיעים באפליקציה הם קניינם של בעליהם החוקיים, ואין לעשות בהם שימוש ללא הרשאה.",
                                "6.4. משתמש שמעלה לאפליקציה כל תוכן (למשל בפורום, בהודעות, בשיתוף חומרים) מצהיר כי הוא בעל כל הזכויות בתוכן וכי אין בהעלאתו כדי להפר זכויות צד ג'. המשתמש מעניק למפעיל רישיון שימוש בלתי בלעדי, עולמי וללא תמורה בתוכן זה, לצורך הפעלת האפליקציה, שיפור השירותים והצגתם למשתמשים אחרים, בהתאם לדין."
                            ),
                            listOf(
                                "6.1. All intellectual property rights in the Application, including source code, design, text, video and audio content, images, logos, trademarks, diagrams, databases and other content, belong to the Operator and/or third parties that have granted the Operator rights to use them.",
                                "6.2. Application content may not be copied, reproduced, translated, distributed, transmitted, made available to the public, performed, displayed, sold, rented, edited, adapted, reverse engineered, decompiled or otherwise used without the Operator's prior express written consent.",
                                "6.3. Trademarks and logos appearing in the Application belong to their respective lawful owners and may not be used without authorization.",
                                "6.4. A user who uploads content to the Application represents that the user holds the necessary rights and that the upload does not infringe third-party rights. The user grants the Operator a non-exclusive, worldwide, royalty-free license to use such content for operation and improvement of the Application and display to other users, subject to applicable law."
                            )
                        ),

                        section(
                            "7. פורומים, שיתופים ותוכן משתמשים",
                            "7. Forums and User Content",
                            listOf(
                                "7.1. האפליקציה עשויה לכלול אזורים בהם ניתן לפרסם הודעות, תגובות, שאלות, תמונות או חומרים אחרים (למשל פורום, שידורי מאמן, צ'אטים). כל תוכן כזה הינו באחריותו הבלעדית של המשתמש שהעלה אותו.",
                                "7.2. המפעיל אינו מתחייב לפקח מראש על תוכן שמעלים משתמשים, אך רשאי, על פי שיקול דעתו, להסיר כל תוכן פוגעני, בלתי חוקי, מפר זכויות, או כל תוכן אחר שלדעתו אינו מתאים, ללא צורך בהודעה מראש וללא כל חובה לנמק.",
                                "7.3. חל איסור לפרסם תכנים משמיצים, גזעניים, מאיימים, פוגעניים, פורנוגרפיים, העשויים לפגוע בפרטיות הזולת, תכנים המהווים לשון הרע, הסתה, הטרדה, או כל תוכן המפר דין.",
                                "7.4. אין לפרסם פרטים אישיים של צדדים שלישיים ללא הסכמתם המפורשת."
                            ),
                            listOf(
                                "7.1. The Application may include areas where users can post messages, comments, questions, images or other material, including forums, instructor broadcasts and chats. Such content is the sole responsibility of the user who uploads it.",
                                "7.2. The Operator does not undertake to monitor user content in advance but may remove offensive, unlawful, infringing or otherwise inappropriate content at its discretion without prior notice.",
                                "7.3. Users must not publish defamatory, racist, threatening, offensive or pornographic material, content that violates another person's privacy, harassment, incitement or any other unlawful content.",
                                "7.4. Personal information concerning third parties may not be published without their express consent."
                            )
                        ),

                        section(
                            "8. שירותים ותכנים של צדדים שלישיים",
                            "8. Third-Party Services and Content",
                            listOf(
                                "8.1. האפליקציה עשויה לעשות שימוש בשירותים חיצוניים, כגון שירותי ענן, אחסון, אבטחה, ניתוח סטטיסטי, מפות, תשלומים או שירותי צד שלישי אחרים, המופעלים על ידי גורמים חיצוניים למפעיל.",
                                "8.2. ייתכן שיוצגו באפליקציה קישורים לאתרים או שירותים של צדדים שלישיים. אין לראות בקישורים אלה משום המלצה, אישור, או אחריות כלשהי של המפעיל לגבי צדדים שלישיים אלה, תכניהם או שירותיהם.",
                                "8.3. השימוש בשירותים ובאתרים של צדדים שלישיים כפוף לתנאי השימוש ולמדיניות הפרטיות של אותם צדדים שלישיים בלבד, והמפעיל אינו אחראי להם ולא יהיה אחראי לכל נזק שייגרם עקב שימוש כאמור."
                            ),
                            listOf(
                                "8.1. The Application may use external services such as cloud services, storage, security, analytics, maps, payment services or other services operated by third parties.",
                                "8.2. Links to third-party websites or services may be displayed. Such links do not constitute a recommendation, endorsement or assumption of responsibility by the Operator for those parties, their content or their services.",
                                "8.3. Use of third-party services and websites is subject solely to the terms and privacy policies of those third parties. The Operator is not responsible for such services or for damage arising from their use."
                            )
                        ),

                        section(
                            "9. הגבלת אחריות",
                            "9. Limitation of Liability",
                            listOf(
                                "9.1. האפליקציה, לרבות כל התכנים, הפונקציות והשירותים שבה, ניתנים לשימוש במתכונתם כפי שהם (AS IS) וכפי שהם זמינים (AS AVAILABLE), ללא כל מצג או אחריות, מפורשת או משתמעת, מכל סוג שהוא.",
                                "9.2. המפעיל אינו מתחייב כי האפליקציה תפעל ללא תקלות, ללא הפרעות, ללא שגיאות או ללא נפילות, ואינו מתחייב כי תקלות יתוקנו, וכי האפליקציה או השרתים שעליהם היא פועלת יהיו חפים מווירוסים, באגים, או רכיבים מזיקים אחרים.",
                                "9.3. המפעיל אינו אחראי לכל נזק, הפסד, אובדן, פגיעה בגוף, ברכוש או במוניטין, ישירים או עקיפים, תוצאתיים, עונשיים או מיוחדים, שייגרמו לך או לצד שלישי כלשהו בעקבות או בקשר עם השימוש באפליקציה, באי היכולת להשתמש בה, בהסתמכות על תכניה, בביצוע תרגילים או פעילות גופנית לפי התכנים, או עקב כל תקלה/ליקוי/עיכוב בשירות.",
                                "9.4. מבלי לגרוע מן האמור לעיל, ככל שחלה על המפעיל אחריות על פי דין שאינה ניתנת להתניה, אחריותו הכוללת של המפעיל לכל נזק שייגרם לך בקשר עם האפליקציה – אם תיקבע – לא תעלה בשום מקרה על הסכום הכולל ששילמת למפעיל, אם בכלל, בעבור השימוש באפליקציה במהלך 12 החודשים שקדמו לאירוע הנזק."
                            ),
                            listOf(
                                "9.1. The Application, including all content, functions and services, is provided on an \"AS IS\" and \"AS AVAILABLE\" basis without representations or warranties of any kind, express or implied.",
                                "9.2. The Operator does not guarantee uninterrupted, error-free or failure-free operation, that defects will be corrected, or that the Application or its servers will be free from viruses, bugs or other harmful components.",
                                "9.3. The Operator will not be responsible for direct, indirect, consequential, punitive or special damage or loss, bodily injury, property damage or reputational harm arising from or connected with use of the Application, inability to use it, reliance on its content, performance of exercises or physical activity based on its content, or faults, defects or delays in the service.",
                                "9.4. To the extent liability exists under mandatory law and cannot lawfully be excluded, the Operator's total liability in connection with the Application will not exceed the total amount paid by you to the Operator, if any, for use of the Application during the 12 months preceding the relevant event."
                            )
                        ),

                        section(
                            "10. שיפוי",
                            "10. Indemnification",
                            listOf(
                                "10.1. אתה מתחייב לשפות ולפצות את המפעיל, עובדיו, מנהליו, בעלי מניותיו וכל מי מטעמו, בגין כל נזק, הפסד, אובדן, הוצאה (לרבות הוצאות משפט ושכר טרחת עורכי דין), או חיוב שייגרמו להם עקב: (א) הפרת תנאי שימוש אלה על ידך; (ב) שימוש שלא כדין באפליקציה; (ג) הפרת זכויות צד שלישי (לרבות זכויות קניין רוחני ופרטיות); (ד) כל טענה או תביעה שתועלה נגד המפעיל על ידי צד שלישי בקשר עם פעולותיך באפליקציה.",
                                "10.2. חובת השיפוי תחול אף לאחר הפסקת השימוש שלך באפליקציה."
                            ),
                            listOf(
                                "10.1. You agree to indemnify and compensate the Operator and those acting on its behalf for damage, loss, expense, legal costs, attorneys' fees or liabilities resulting from your breach of these Terms, unlawful use of the Application, infringement of third-party rights or claims made by third parties in connection with your activities through the Application.",
                                "10.2. This indemnification obligation will survive termination of your use of the Application."
                            )
                        ),

                        section(
                            "11. שינויים, השהיה והפסקת השירות",
                            "11. Changes, Suspension and Termination",
                            listOf(
                                "11.1. המפעיל רשאי, בכל עת, להפסיק, להשעות, להגביל או לשנות את האפליקציה, את תכניה או את שירותיה, כולם או חלקם, באופן זמני או קבוע, וזאת לפי שיקול דעתו הבלעדי, וללא מתן הודעה מוקדמת.",
                                "11.2. המפעיל רשאי לחסום את גישתך לאפליקציה, כולה או חלקה, ולמחוק חשבון משתמש, בין היתר עקב הפרת תנאי שימוש אלה, ביצוע מעשים המנוגדים לדין או העלולים לפגוע במפעיל, בשאר המשתמשים או בצדדים שלישיים.",
                                "11.3. לא תהיה לך כל טענה, דרישה או תביעה כלפי המפעיל בקשר עם ביצוע שינויים/השעיה/הפסקת השירות כאמור."
                            ),
                            listOf(
                                "11.1. The Operator may discontinue, suspend, restrict or modify the Application, its content or services, in whole or in part, temporarily or permanently and without prior notice.",
                                "11.2. The Operator may block access to all or part of the Application and may delete a user account, including due to breach of these Terms, unlawful conduct or conduct that may harm the Operator, other users or third parties.",
                                "11.3. You will have no claim against the Operator solely as a result of such changes, suspension or termination, subject to rights that cannot lawfully be excluded."
                            )
                        ),

                        section(
                            "12. הודעות ויצירת קשר",
                            "12. Notices and Contact",
                            listOf(
                                "12.1. המפעיל רשאי לשלוח אליך מעת לעת הודעות, עדכונים, התראות, הודעות שירות ושינויים בתנאים באמצעות האפליקציה, בדוא\"ל, במסרונים (SMS) או באמצעי תקשורת אחרים שסיפקת.",
                                "12.2. בכל שאלה, פנייה או בקשה הקשורה לאפליקציה, לתנאי השימוש או למדיניות הפרטיות, ניתן לפנות למפעיל באמצעי ההתקשרות המופיעים באפליקציה."
                            ),
                            listOf(
                                "12.1. The Operator may send notices, updates, alerts, service communications and changes to these Terms through the Application, email, SMS or other communication methods provided by you.",
                                "12.2. Questions or requests concerning the Application, these Terms or the Privacy Policy may be submitted using the contact methods displayed in the Application."
                            )
                        ),

                        section(
                            "13. הדין החל וסמכות השיפוט",
                            "13. Governing Law and Jurisdiction",
                            listOf(
                                "13.1. על תנאי שימוש אלה ועל כל שימוש באפליקציה יחולו דיני מדינת ישראל בלבד, מבלי לתת תוקף לכללי ברירת הדין הבינלאומיים.",
                                "13.2. סמכות השיפוט הבלעדית בכל מחלוקת או עניין הנובעים מהאפליקציה ו/או מתנאי שימוש אלה תהיה נתונה לבתי המשפט המוסמכים במחוז מרכז (לרבות בתי המשפט בנתניה), ובלבד שהדבר לא יגביל זכות קוגנטית שלך לפי דין."
                            ),
                            listOf(
                                "13.1. These Terms and all use of the Application are governed exclusively by the laws of the State of Israel, without giving effect to conflict-of-law principles.",
                                "13.2. Exclusive jurisdiction over disputes arising from the Application or these Terms will lie with the competent courts in Israel's Central District, including the courts in Netanya, provided that this provision does not restrict any mandatory right available to you under applicable law."
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    onBack: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val isEnglish = rememberIsEnglish()

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) {
            en
        } else {
            he
        }
    }

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val screenBackground =
        if (isDarkMode) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surface
        }

    val secondaryTextColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    var selectedTab by remember {
        mutableIntStateOf(
            initialTab.coerceIn(0, 2)
        )
    }

    val selectedDocument =
        legalDocumentForTab(
            selectedTab = selectedTab,
            isEnglish = isEnglish
        )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title =
                        tr(
                            "תנאי שימוש ומדיניות",
                            "Terms & Policies"
                        ),
                    onBack = onBack,
                    showTopHome = false,
                    lockSearch = true,
                    showBottomActions = true,
                    showBottomShare = true,
                    onShare = {
                        shareLegalPdf(
                            context = context,
                            documentContent = selectedDocument,
                            isEnglish = isEnglish
                        )
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        screenBackground
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
            ) {

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor =
                        MaterialTheme.colorScheme.surface,
                    contentColor =
                        MaterialTheme.colorScheme.primary
                ) {

                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                        },
                        text = {
                            Text(
                                text =
                                    tr(
                                        "תנאי שימוש",
                                        "Terms"
                                    ),
                                style = KmiTypography.action,
                                color =
                                    if (selectedTab == 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        secondaryTextColor
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                        },
                        text = {
                            Text(
                                text =
                                    tr(
                                        "מדיניות פרטיות",
                                        "Privacy"
                                    ),
                                style = KmiTypography.action,
                                color =
                                    if (selectedTab == 1) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        secondaryTextColor
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    )

                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                        },
                        text = {
                            Text(
                                text =
                                    tr(
                                        "הצהרת נגישות",
                                        "Accessibility"
                                    ),
                                style = KmiTypography.action,
                                color =
                                    if (selectedTab == 2) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        secondaryTextColor
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Column(
                    modifier = Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    selectedDocument
                        .sections
                        .forEach { section ->

                            Text(
                                text = section.title,
                                style =
                                    KmiTypography.sectionTitle,
                                color =
                                    MaterialTheme.colorScheme.primary,
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Start
                                    } else {
                                        TextAlign.Right
                                    },
                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            section.paragraphs
                                .forEach { paragraph ->

                                    Text(
                                        text = paragraph,
                                        style =
                                            KmiTypography.body,
                                        color =
                                            MaterialTheme.colorScheme.onBackground,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Start
                                            } else {
                                                TextAlign.Right
                                            },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )
                        }
                }
            }
        }
    }
}

private fun shareLegalPdf(
    context: Context,
    documentContent: LegalDocumentContent,
    isEnglish: Boolean
) {
    val pdfFile =
        createLegalPdf(
            context = context,
            documentContent =
                documentContent,
            isEnglish = isEnglish
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                documentContent.title
            )

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) {
                "Share ${documentContent.title}"
            } else {
                "שיתוף ${documentContent.title}"
            }
        )
    )
}

private fun createLegalPdf(
    context: Context,
    documentContent: LegalDocumentContent,
    isEnglish: Boolean
): File {

    val document = PdfDocument()

    val pageWidth = 595
    val pageHeight = 842

    val margin = 36f
    val contentBottom = 786f
    val pdfTextAlign =
        if (isEnglish) {
            Paint.Align.LEFT
        } else {
            Paint.Align.RIGHT
        }

    val pdfTextX =
        if (isEnglish) {
            margin
        } else {
            pageWidth - margin
        }

    val navy =
        android.graphics.Color.rgb(
            2,
            43,
            74
        )

    val darkText =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val mutedText =
        android.graphics.Color.rgb(
            100,
            116,
            139
        )

    val regular =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val bold =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    val titlePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.WHITE

            textSize = 24f
            typeface = bold
            textAlign = Paint.Align.RIGHT
        }

    val subTitlePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.WHITE

            textSize = 10f
            typeface = regular
            textAlign = Paint.Align.RIGHT
        }

    val sectionPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            textSize = 15f
            typeface = bold
            textAlign = pdfTextAlign
        }

    val bodyPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = darkText
            textSize = 10.5f
            typeface = regular
            textAlign = pdfTextAlign
        }

    val footerPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mutedText
            textSize = 8.5f
            typeface = regular
            textAlign = Paint.Align.CENTER
        }

    var pageNumber = 0

    lateinit var page:
            PdfDocument.Page

    lateinit var canvas:
            android.graphics.Canvas

    var y = 0f

    fun splitLines(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        val words =
            text
                .trim()
                .split(
                    Regex("""\s+""")
                )

        if (words.isEmpty()) {
            return emptyList()
        }

        val lines =
            mutableListOf<String>()

        var currentLine = ""

        words.forEach { word ->

            val candidate =
                if (currentLine.isBlank()) {
                    word
                } else {
                    "$currentLine $word"
                }

            if (
                paint.measureText(
                    candidate
                ) <= maxWidth
            ) {
                currentLine =
                    candidate
            } else {
                if (
                    currentLine.isNotBlank()
                ) {
                    lines +=
                        currentLine
                }

                currentLine =
                    word
            }
        }

        if (
            currentLine.isNotBlank()
        ) {
            lines += currentLine
        }

        return lines
    }

    fun drawFooter() {

        canvas.drawLine(
            margin,
            pageHeight - 42f,
            pageWidth - margin,
            pageHeight - 42f,
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.LTGRAY

                strokeWidth = 1f
            }
        )

        canvas.drawText(
            if (isEnglish) {
                "Page $pageNumber · KAMI"
            } else {
                "עמוד $pageNumber · KAMI"
            },
            pageWidth / 2f,
            pageHeight - 24f,
            footerPaint
        )
    }

    fun drawKmiLogo(
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val outer =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
            }

        val inner =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
            }

        val logoText =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
                typeface = bold
                textSize =
                    radius * 0.62f
                textAlign =
                    Paint.Align.CENTER
            }

        canvas.drawCircle(
            cx,
            cy,
            radius,
            outer
        )

        canvas.drawCircle(
            cx,
            cy,
            radius - 4f,
            inner
        )

        canvas.drawText(
            "KAMI",
            cx,
            cy + radius * 0.22f,
            logoText
        )
    }

    fun startPage() {

        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(page)
        }

        pageNumber++

        page =
            document.startPage(
                PdfDocument.PageInfo
                    .Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    )
                    .create()
            )

        canvas = page.canvas

        canvas.drawColor(
            android.graphics.Color.WHITE
        )

        /*
         * =====================================================
         * Header אחיד — זהה ל-PDF של מסך הבית
         * =====================================================
         */

        val diagonal =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
            }

        val accent1 =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.rgb(
                        36,
                        103,
                        158
                    )
            }

        val accent2 =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.rgb(
                        128,
                        183,
                        220
                    )
            }

        /*
         * המשטח הכחול הראשי.
         */
        canvas.drawPath(
            Path().apply {
                moveTo(
                    pageWidth.toFloat(),
                    0f
                )

                lineTo(
                    pageWidth.toFloat(),
                    122f
                )

                lineTo(
                    178f,
                    122f
                )

                lineTo(
                    238f,
                    0f
                )

                close()
            },
            diagonal
        )

        /*
         * פס כחול ראשון.
         */
        canvas.drawPath(
            Path().apply {
                moveTo(
                    208f,
                    122f
                )

                lineTo(
                    224f,
                    122f
                )

                lineTo(
                    284f,
                    0f
                )

                lineTo(
                    268f,
                    0f
                )

                close()
            },
            accent1
        )

        /*
         * פס כחול שני.
         */
        canvas.drawPath(
            Path().apply {
                moveTo(
                    230f,
                    122f
                )

                lineTo(
                    238f,
                    122f
                )

                lineTo(
                    298f,
                    0f
                )

                lineTo(
                    290f,
                    0f
                )

                close()
            },
            accent2
        )

        /*
         * לוגו KAMI בצד שמאל.
         */
        drawKmiLogo(
            cx = 78f,
            cy = 58f,
            radius = 42f
        )

        /*
         * הכותרת נשארת בצד ימין של ה-Header,
         * בדיוק כמו במסך הבית.
         */
        canvas.drawText(
            documentContent.title,
            pageWidth - 34f,
            52f,
            titlePaint
        )

        canvas.drawText(
            if (isEnglish) {
                "KAMI · Terms, Privacy & Accessibility"
            } else {
                "ק.מ.י · תנאי שימוש, פרטיות ונגישות"
            },
            pageWidth - 34f,
            78f,
            subTitlePaint
        )

        y = 154f
    }

    fun ensureSpace(
        requiredHeight: Float
    ) {
        if (
            y + requiredHeight >
            contentBottom
        ) {
            startPage()
        }
    }

    startPage()

    documentContent
        .sections
        .forEach { section ->

            ensureSpace(
                34f
            )

            canvas.drawText(
                section.title,
                pdfTextX,
                y,
                sectionPaint
            )

            y += 24f

            section.paragraphs
                .forEach { paragraph ->

                    val lines =
                        splitLines(
                            text = paragraph,
                            paint = bodyPaint,
                            maxWidth =
                                pageWidth -
                                        margin * 2f
                        )

                    ensureSpace(
                        lines.size * 15f +
                                12f
                    )

                    lines.forEach { line ->

                        canvas.drawText(
                            line,
                            pdfTextX,
                            y,
                            bodyPaint
                        )

                        y += 15f
                    }

                    y += 9f
                }

            y += 6f
        }

    drawFooter()

    document.finishPage(
        page
    )

    val directory =
        File(
            context.cacheDir,
            "legal_pdfs"
        ).apply {
            mkdirs()
        }

    val safeFileName =
        documentContent
            .title
            .replace(
                Regex(
                    """[\\/:*?"<>|]"""
                ),
                "-"
            )

    val file =
        File(
            directory,
            "$safeFileName.pdf"
        )

    /*
     * כל יצירה חדשה מחליפה
     * את הקובץ הקודם.
     */
    if (file.exists()) {
        file.delete()
    }

    try {
        FileOutputStream(
            file
        ).use { output ->

            document.writeTo(
                output
            )
        }
    } finally {
        document.close()
    }

    return file
}
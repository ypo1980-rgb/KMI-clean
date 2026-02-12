package il.kmi.shared.report

import kotlin.math.roundToInt

/** נתון להצגה/ייצוא של התקדמות חגורה. */
data class BeltProgress(
    val title: String,        // לדוגמה: "חגורה: ירוקה"
    val percent: Int,         // 0..100
    val colorHex: String,     // לדוגמה "#43A047"
    val lightColorHex: String // לדוגמה "#C8E6C9"
)

/**
 * חישוב מד־התקדמות אמיתי לכל חגורה.
 * משתמשים בזה כדי לייצר BeltProgress ל־UI ולדו״ח HTML.
 */
object ProgressCalc {

    /** מונים לכל חגורה. */
    data class Counts(val done: Int, val total: Int)

    /** מטא־דאטה קבועה לכל חגורה (כותרת + צבעים). */
    private data class Meta(
        val title: String,
        val color: String,  // כהה
        val light: String   // בהיר
    )

    // סדר קבוע של חגורות + צבעים (התאם אם מוסיפים/משנים חגורות).
    private val BELTS: LinkedHashMap<String, Meta> = linkedMapOf(
        "yellow" to Meta(title = "חגורה: צהובה",  color = "#FBC02D", light = "#FFF59D"),
        "orange" to Meta(title = "חגורה: כתומה",  color = "#FB8C00", light = "#FFE0B2"),
        "green"  to Meta(title = "חגורה: ירוקה",  color = "#43A047", light = "#C8E6C9"),
        "blue"   to Meta(title = "חגורה: כחולה",  color = "#1E88E5", light = "#BBDEFB"),
    )

    /** עזר: הופך רשימת בוליאנים (true=סומן) למונים. */
    fun countsOf(checks: List<Boolean>): Counts =
        Counts(done = checks.count { it }, total = checks.size)

    /**
     * מקבל מונים לכל חגורה ומחזיר BeltProgress ל־UI/HTML.
     * אם total==0 האחוז יהיה 0.
     */
    fun fromCounts(
        yellow: Counts,
        orange: Counts,
        green: Counts,
        blue: Counts,
    ): List<BeltProgress> {

        val ordered: List<Pair<String, Counts>> = listOf(
            "yellow" to yellow,
            "orange" to orange,
            "green"  to green,
            "blue"   to blue
        )

        return ordered.map { (key: String, c: Counts) ->
            val m: Meta = BELTS.getValue(key)
            val percent: Int =
                if (c.total <= 0) 0
                else ((c.done.toDouble() / c.total.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 100)

            BeltProgress(
                title = m.title,
                percent = percent,
                colorHex = m.color,
                lightColorHex = m.light
            )
        }
    }
}

/**
 * יוצר HTML מלא (inline CSS) להצגת דו״ח ההתקדמות.
 */
object ProgressReport {
    fun buildHtml(items: List<BeltProgress>): String {
        val rows: String = items.joinToString("\n") { item ->
            """
            <div class="card" style="border-color:${item.colorHex}; background:${item.lightColorHex}22;">
              <div class="row">
                <span class="pill" style="background:${item.colorHex};">${item.percent}%</span>
                <span class="title" style="color:${item.colorHex};">${item.title}</span>
                <span class="dot" style="background:${item.colorHex};"></span>
              </div>
              <div class="bar">
                <div class="fill" style="background:${item.colorHex}; width:${item.percent}%;"></div>
              </div>
              <div class="status">${item.percent}% (חישוב לפי פריטים שסומנו באפליקציה)</div>
            </div>
            """.trimIndent()
        }

        return """
        <!doctype html>
        <html lang="he" dir="rtl">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>דו״ח מד התקדמות</title>
          <style>
            body { font-family: -apple-system, Roboto, "Segoe UI", Arial, sans-serif; margin: 24px; background:#FAFAFA; }
            h1 { font-size: 20px; margin-bottom: 16px; }
            .card { border:2px solid; border-radius:16px; padding:14px; margin-bottom:14px; background:#FFFFFF; }
            .row { display:flex; align-items:center; gap:8px; }
            .pill { color:white; font-weight:700; border-radius:999px; padding:6px 12px; display:inline-block; }
            .title { font-weight:700; margin-inline-start:auto; }
            .dot { width:14px; height:14px; border-radius:50%; display:inline-block; }
            .bar { width:100%; height:12px; border-radius:999px; background:rgba(0,0,0,0.08); margin:10px 0 6px; overflow:hidden; }
            .fill { height:100%; border-radius:999px; }
            .status { color:#666; font-size:14px; }
          </style>
        </head>
        <body>
          <h1>דו״ח מד התקדמות</h1>
          $rows
        </body>
        </html>
        """.trimIndent()
    }
}

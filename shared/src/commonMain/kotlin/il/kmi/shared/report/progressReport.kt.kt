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
            <section class="belt-card" style="--belt-color:${item.colorHex}; --belt-light:${item.lightColorHex};">
              <div class="belt-header">
                <div class="belt-percent">${item.percent}%</div>
                <div class="belt-title">${item.title}</div>
                <div class="belt-dot"></div>
              </div>

              <div class="progress-track">
                <div class="progress-fill" style="width:${item.percent}%;"></div>
              </div>

              <div class="belt-status">
                ${item.percent}% התקדמות לפי פריטים שסומנו באפליקציה
              </div>
            </section>
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
            @page {
              size: A4;
              margin: 18mm 14mm;
            }

            * {
              box-sizing: border-box;
            }

            body {
              margin: 0;
              direction: rtl;
              background: #F4F1EA;
              color: #182536;
              font-family: Arial, "Segoe UI", sans-serif;
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }

            .page {
              width: 100%;
              min-height: 100vh;
              padding: 0;
            }

            .report-shell {
              width: 100%;
              max-width: 760px;
              margin: 0 auto;
              background: #FFFDF8;
              border-radius: 28px;
              overflow: hidden;
              border: 1px solid #E5DED2;
              box-shadow: 0 10px 28px rgba(24, 37, 54, 0.10);
            }

            .hero {
              background: linear-gradient(135deg, #182536 0%, #24364D 100%);
              padding: 28px 30px 24px;
              color: #FFFFFF;
              text-align: center;
            }

            .hero-kicker {
              display: inline-block;
              margin-bottom: 10px;
              padding: 5px 14px;
              border-radius: 999px;
              background: rgba(255,255,255,0.14);
              font-size: 13px;
              font-weight: 700;
              letter-spacing: 0.2px;
            }

            h1 {
              margin: 0;
              font-size: 30px;
              line-height: 1.25;
              font-weight: 900;
            }

            .subtitle {
              margin-top: 10px;
              font-size: 15px;
              line-height: 1.6;
              color: rgba(255,255,255,0.82);
              font-weight: 600;
            }

            .content {
              padding: 26px 26px 30px;
            }

            .belt-card {
              --belt-color: #1E88E5;
              --belt-light: #BBDEFB;

              position: relative;
              margin-bottom: 18px;
              padding: 20px 20px 18px;
              border-radius: 22px;
              border: 2px solid var(--belt-color);
              background:
                linear-gradient(135deg, color-mix(in srgb, var(--belt-light) 34%, #FFFFFF 66%), #FFFFFF 78%);
              overflow: hidden;
              page-break-inside: avoid;
            }

            .belt-card::before {
              content: "";
              position: absolute;
              inset: 0;
              background:
                radial-gradient(circle at 8% 18%, color-mix(in srgb, var(--belt-color) 18%, transparent), transparent 28%),
                linear-gradient(90deg, color-mix(in srgb, var(--belt-color) 10%, transparent), transparent 42%);
              pointer-events: none;
            }

            .belt-header {
              position: relative;
              display: flex;
              align-items: center;
              gap: 12px;
              z-index: 1;
            }

            .belt-percent {
              min-width: 64px;
              height: 64px;
              padding: 0 12px;
              border-radius: 999px;
              display: flex;
              align-items: center;
              justify-content: center;
              background: var(--belt-color);
              color: #FFFFFF;
              font-size: 23px;
              font-weight: 900;
              box-shadow: 0 7px 16px color-mix(in srgb, var(--belt-color) 30%, transparent);
            }

            .belt-title {
              flex: 1;
              text-align: center;
              color: var(--belt-color);
              font-size: 25px;
              line-height: 1.25;
              font-weight: 900;
            }

            .belt-dot {
              width: 16px;
              height: 16px;
              border-radius: 50%;
              background: var(--belt-color);
              box-shadow: 0 0 0 6px color-mix(in srgb, var(--belt-color) 14%, transparent);
            }

            .progress-track {
              position: relative;
              z-index: 1;
              height: 15px;
              margin: 18px 0 12px;
              border-radius: 999px;
              background: rgba(24, 37, 54, 0.10);
              overflow: hidden;
            }

            .progress-fill {
              height: 100%;
              min-width: 0;
              border-radius: 999px;
              background: linear-gradient(90deg, var(--belt-color), color-mix(in srgb, var(--belt-color) 72%, #FFFFFF 28%));
            }

            .belt-status {
              position: relative;
              z-index: 1;
              text-align: left;
              color: #45414A;
              font-size: 16px;
              line-height: 1.5;
              font-weight: 800;
            }

            .footer {
              margin-top: 22px;
              padding-top: 16px;
              border-top: 1px solid #E5DED2;
              text-align: center;
              color: #6D6673;
              font-size: 13px;
              font-weight: 700;
            }
          </style>
        </head>
        <body>
          <main class="page">
            <section class="report-shell">
              <header class="hero">
                <div class="hero-kicker">KAMI</div>
                <h1>דו״ח מד התקדמות</h1>
                <div class="subtitle">סיכום התקדמות לפי חגורות ופריטים שסומנו באפליקציה</div>
              </header>

              <section class="content">
                $rows

                <div class="footer">
                  הופק מתוך אפליקציית KAMI
                </div>
              </section>
            </section>
          </main>
        </body>
        </html>
        """.trimIndent()
    }
}

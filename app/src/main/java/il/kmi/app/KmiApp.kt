package il.kmi.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.lang.ref.WeakReference

class KmiApp : Application() {

    // מצביע חלש ל-Activity העליון כדי להימנע מנזילות זיכרון
    // ✅ נשמור מראש FragmentActivity בלבד (כי AppLock צריך FragmentManager)
    private var topActivityRef: WeakReference<FragmentActivity>? = null

    override fun onCreate() {
        super.onCreate()

        // ✅ חובה: אתחול ה-ContentRepo (Source of Truth)
        il.kmi.app.domain.ContentRepo.initIfNeeded()

        // ✅ DEBUG – מדפיס פעם אחת מה באמת יש ב-Repo
        il.kmi.app.search.KmiSearchBridge.debugLogRepoOnce()

        // ✅ Shared platform & prefs
        runCatching { il.kmi.shared.Platform.init(appContext = this) }
        runCatching { il.kmi.shared.prefs.SharedSettingsFactoryProvider.init(context = this) }

        // ⭐ Favorites - source of truth אחד לכל האפליקציה
        runCatching {
            il.kmi.app.favorites.FavoritesStore.init(this)
        }

        runCatching {
            il.kmi.app.catalog.ExerciseHtmlProvider.setResolver { r ->
                val q = "תן בבקשה הסבר על ${r.itemTitle}"
                val ans = il.kmi.app.ui.assistant.AssistantExerciseExplanationKnowledge.answer(
                    question = q,
                    preferredBelt = r.belt
                ) ?: return@setResolver null

                buildString {
                    append("<html><body dir='rtl' style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial; line-height:1.5;\">")
                    append("<h2>")
                    append(escapeHtmlLocal(r.itemTitle))
                    append("</h2>")
                    append("<pre style=\"white-space:pre-wrap; font-size:16px;\">")
                    append(escapeHtmlLocal(ans))
                    append("</pre>")
                    append("</body></html>")
                }
            }
        }

        // ✅ Bootstrap אמיתי מה-ContentRepo אל shared.InMemoryCatalog
        runCatching {
            il.kmi.app.catalog.CatalogBootstrapper.bootstrapFromContentRepo()
        }

        // ✅ Demo fallback ONLY אם ה-bootstrap לא מילא כלום
        runCatching {
            val hasBelts = il.kmi.shared.catalog.KmiCatalogFacade.listBelts().isNotEmpty()
            if (!hasBelts) {
                il.kmi.shared.catalog.InMemoryCatalog.setBelts(
                    listOf(
                        il.kmi.shared.catalog.BeltDto("yellow", "חגורה צהובה", 1),
                        il.kmi.shared.catalog.BeltDto("orange", "חגורה כתומה", 2),
                        il.kmi.shared.catalog.BeltDto("green", "חגורה ירוקה", 3)
                    )
                )
            }
        }

        // ✅ Activity lifecycle – מי העליון כרגע
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            private fun updateTop(activity: Activity) {
                val fa = activity as? FragmentActivity ?: return
                topActivityRef = WeakReference(fa)
            }

            override fun onActivityStarted(activity: Activity) {
                updateTop(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                updateTop(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                val held = topActivityRef?.get()
                if (held != null && held === activity) {
                    topActivityRef?.clear()
                    topActivityRef = null
                }
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })

        // ✅ AppLock כשהאפליקציה חוזרת לפרונט
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val sp = getSharedPreferences("kmi_settings", MODE_PRIVATE)
                val mode = sp.getString("app_lock_mode", "none") ?: "none"
                if (mode == "none") return

                val fa = topActivityRef?.get() ?: return
                if (fa.isFinishing || fa.isDestroyed) return

                runCatching {
                    il.kmi.app.security.AppLock.requireIfNeeded(fa, false)
                }
            }
        })
    }
}

private fun escapeHtmlLocal(s: String): String =
    s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

package il.kmi.app.ui.assistant

import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations

object ExerciseExplanationProvider {

    fun get(belt: Belt?, item: String): String {
        if (belt != null) return Explanations.get(belt, item)

        val beltsToTry = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
        val fallbackPrefix = "הסבר מפורט על: "

        for (b in beltsToTry) {
            val ans = Explanations.get(b, item)
            if (!ans.startsWith(fallbackPrefix)) return ans
        }
        return Explanations.get(Belt.YELLOW, item) // fallback סופי
    }
}

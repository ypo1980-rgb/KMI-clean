package il.kmi.app.ui.assistant.core

import android.content.SharedPreferences

class AssistantMemory(
    private val prefs: SharedPreferences
) {

    fun saveLastQuestion(question: String) {
        prefs.edit()
            .putString(KEY_LAST_QUESTION, question.trim())
            .apply()
    }

    fun getLastQuestion(): String? {
        return prefs.getString(KEY_LAST_QUESTION, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun saveLastAnswer(answer: String) {
        prefs.edit()
            .putString(KEY_LAST_ANSWER, answer.trim())
            .apply()
    }

    fun getLastAnswer(): String? {
        return prefs.getString(KEY_LAST_ANSWER, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun saveLastTopic(topic: String) {
        prefs.edit()
            .putString(KEY_LAST_TOPIC, topic.trim())
            .apply()
    }

    fun getLastTopic(): String? {
        return prefs.getString(KEY_LAST_TOPIC, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun saveLastExercise(exercise: String, belt: String? = null) {
        prefs.edit()
            .putString(KEY_LAST_EXERCISE, exercise.trim())
            .putString(KEY_LAST_BELT, belt)
            .apply()
    }

    fun getLastBelt(): String? {
        return prefs.getString(KEY_LAST_BELT, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun getLastExercise(): String? {
        return prefs.getString(KEY_LAST_EXERCISE, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_LAST_QUESTION)
            .remove(KEY_LAST_ANSWER)
            .remove(KEY_LAST_TOPIC)
            .remove(KEY_LAST_EXERCISE)
            .apply()
    }

    private companion object {
        const val KEY_LAST_QUESTION = "last_question"
        const val KEY_LAST_ANSWER = "last_answer"
        const val KEY_LAST_TOPIC = "last_topic"
        const val KEY_LAST_BELT = "last_belt"
        const val KEY_LAST_EXERCISE = "last_exercise"
    }
}
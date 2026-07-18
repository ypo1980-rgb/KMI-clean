package il.kmi.app.ui.assistant.core

/**
 * כל סוגי הבקשות של העוזר.
 *
 * EXERCISE, MATERIAL ו-TRAININGS נשמרים לתאימות
 * עם AssistantBrain ו-AssistantIntentDetector הוותיקים.
 *
 * שאר הערכים משמשים את מנהל השיחה החדש.
 */
enum class AssistantIntent {

    // תאימות למנוע הוותיק
    EXERCISE,
    MATERIAL,
    TRAININGS,

    // כוונות מפורטות של מנהל השיחה החדש
    EXPLAIN_EXERCISE,
    SEARCH_EXERCISE,
    LIST_EXERCISES,

    SEARCH_MATERIAL,
    LIST_TOPICS,

    NEXT_TRAINING,
    LIST_TRAININGS,
    USER_TRAINING_DETAILS,

    NAVIGATION,
    UNKNOWN
}
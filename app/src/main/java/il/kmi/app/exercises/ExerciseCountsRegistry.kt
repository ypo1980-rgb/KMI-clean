package il.kmi.app.exercises

object ExerciseCountsRegistry {

    /**
     * Counts exercises per topic automatically from the catalog
     */
    fun countExercises(topicMap: Map<String, List<String>>): Map<String, Int> {
        return topicMap.mapValues { (_, exercises) ->
            exercises.size
        }
    }

    /**
     * Count exercises inside sub topics
     */
    fun countSubTopics(topicMap: Map<String, Map<String, List<String>>>): Map<String, Int> {
        return topicMap.mapValues { (_, subTopics) ->
            subTopics.values.sumOf { it.size }
        }
    }

    /**
     * Total exercises
     */
    fun totalExercises(topicMap: Map<String, List<String>>): Int {
        return topicMap.values.sumOf { it.size }
    }

}
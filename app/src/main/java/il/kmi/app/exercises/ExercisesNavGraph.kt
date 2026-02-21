package il.kmi.app.exercises

import android.net.Uri
import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.exercisesNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(
        route = Route.TopicExercises.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic") { type = NavType.StringType },
            navArgument("sub") { type = NavType.StringType; defaultValue = "" }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString("beltId").orEmpty()
        val topicEnc = entry.arguments?.getString("topic").orEmpty()
        val subEnc = entry.arguments?.getString("sub").orEmpty()

        val beltId = runCatching { Uri.decode(beltIdEnc) }.getOrDefault(beltIdEnc)
        val topic = runCatching { Uri.decode(topicEnc) }.getOrDefault(topicEnc)
        val sub = runCatching { Uri.decode(subEnc) }.getOrDefault(subEnc)

        val belt = Belt.fromId(beltId) ?: Belt.GREEN

        TopicRepoExercisesScreen(
            belt = belt,
            topicId = topic,
            subTopicId = sub,
            onBack = { nav.popBackStack() },
            onOpenExercise = { picked ->
                val id = Uri.encode(picked)
                nav.navigate(Route.Exercise.make(id))
            }
        )
    }
}

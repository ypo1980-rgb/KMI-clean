package il.kmi.app.stretching.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.app.stretching.ui.StretchingCategoryScreen
import il.kmi.app.stretching.ui.StretchingExerciseScreen
import il.kmi.app.stretching.ui.StretchingScreen
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.stretching.StretchingCatalog
import il.kmi.shared.stretching.StretchingCategory

object StretchingRoute {

    const val ROOT =
        "stretching"

    private const val CATEGORY_ARGUMENT =
        "categoryId"

    private const val EXERCISE_ARGUMENT =
        "exerciseId"

    private const val AUTO_START_ARGUMENT =
        "autoStart"

    const val CATEGORY =
        "$ROOT/category/{$CATEGORY_ARGUMENT}"

    const val EXERCISE =
        "$ROOT/exercise/{$EXERCISE_ARGUMENT}" +
                "?$AUTO_START_ARGUMENT=" +
                "{$AUTO_START_ARGUMENT}"

    fun category(
        categoryId: String
    ): String =
        "$ROOT/category/${Uri.encode(categoryId)}"

    fun exercise(
        exerciseId: String,
        autoStart: Boolean = false
    ): String =
        "$ROOT/exercise/${Uri.encode(exerciseId)}" +
                "?$AUTO_START_ARGUMENT=$autoStart"
}

fun NavGraphBuilder.stretchingNavGraph(
    nav: NavHostController
) {
    composable(
        route = StretchingRoute.ROOT
    ) {
        val isEnglish =
            currentStretchingLanguageIsEnglish()

        StretchingScreen(
            isEnglish = isEnglish,
            onBack = {
                nav.popBackStack()
            },
            onHome = {
                nav.navigateToStretchingHome()
            },
            onOpenCategory = { category ->
                nav.navigate(
                    StretchingRoute.category(
                        categoryId = category.id
                    )
                ) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(
        route = StretchingRoute.CATEGORY,
        arguments =
            listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                }
            )
    ) { backStackEntry ->
        val categoryId =
            Uri.decode(
                backStackEntry
                    .arguments
                    ?.getString("categoryId")
                    .orEmpty()
            )

        val category =
            StretchingCategory.fromId(categoryId)
                ?: StretchingCategory.NECK_AND_HEAD

        val isEnglish =
            currentStretchingLanguageIsEnglish()

        StretchingCategoryScreen(
            category = category,
            isEnglish = isEnglish,
            onBack = {
                nav.popBackStack()
            },
            onHome = {
                nav.navigateToStretchingHome()
            },
            onOpenExercise = { exerciseId ->
                nav.navigate(
                    StretchingRoute.exercise(
                        exerciseId = exerciseId
                    )
                ) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(
        route = StretchingRoute.EXERCISE,
        arguments =
            listOf(
                navArgument("exerciseId") {
                    type = NavType.StringType
                },
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
    ) { backStackEntry ->
        val exerciseId =
            Uri.decode(
                backStackEntry
                    .arguments
                    ?.getString("exerciseId")
                    .orEmpty()
            )

        val currentExercise =
            StretchingCatalog.exerciseById(
                exerciseId = exerciseId
            )

        val categoryExercises =
            currentExercise
                ?.let { exercise ->
                    StretchingCatalog.exercisesFor(
                        category = exercise.category
                    )
                }
                .orEmpty()

        val currentExerciseIndex =
            categoryExercises.indexOfFirst { exercise ->
                exercise.id == exerciseId
            }

        val nextExercise =
            categoryExercises.getOrNull(
                currentExerciseIndex + 1
            )

        val autoStart =
            backStackEntry
                .arguments
                ?.getBoolean("autoStart")
                ?: false

        val isEnglish =
            currentStretchingLanguageIsEnglish()

        StretchingExerciseScreen(
            exerciseId = exerciseId,
            isEnglish = isEnglish,
            onBack = {
                nav.popBackStack()
            },
            onHome = {
                nav.navigateToStretchingHome()
            },
            startAutomatically = autoStart,
            onNavigateToExercise = {
                    targetExerciseId ->

                nav.navigate(
                    StretchingRoute.exercise(
                        exerciseId =
                            targetExerciseId,
                        autoStart = true
                    )
                ) {
                    launchSingleTop = true

                    popUpTo(
                        backStackEntry.destination.id
                    ) {
                        inclusive = true
                    }
                }
            },
            onExerciseCompleted = {
                if (nextExercise != null) {
                    nav.navigate(
                        StretchingRoute.exercise(
                            exerciseId =
                                nextExercise.id,
                            autoStart = true
                        )
                    ) {
                        launchSingleTop = true

                        popUpTo(
                            backStackEntry.destination.id
                        ) {
                            inclusive = true
                        }
                    }
                } else {
                    nav.popBackStack()
                }
            }
        )
    }
}

@Composable
private fun currentStretchingLanguageIsEnglish(): Boolean {
    val context =
        LocalContext.current

    val languageManager =
        remember(context) {
            AppLanguageManager(context)
        }

    return languageManager.getCurrentLanguage() ==
            AppLanguage.ENGLISH
}

private fun NavHostController.navigateToStretchingHome() {
    navigate(Route.Home.route) {
        launchSingleTop = true
        restoreState = true

        popUpTo(Route.Home.route) {
            inclusive = false
            saveState = false
        }
    }
}
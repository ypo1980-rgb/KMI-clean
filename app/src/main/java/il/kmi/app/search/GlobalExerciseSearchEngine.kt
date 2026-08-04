package il.kmi.app.search

import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.content.HardSectionsResolver

/**
 * מנוע החיפוש הגלובלי של תרגילי ק.מ.י.
 *
 * הקובץ אינו תלוי ב-Compose או במסך מסוים, ולכן ניתן להשתמש בו
 * מחלון החיפוש, מהמיקרופון, מהעוזר האישי וממסכים נוספים.
 */
object GlobalExerciseSearchEngine {

    data class Result(
        val id: String,
        val title: String,
        val subtitle: String?
    )

    fun search(
        query: String,
        isEnglish: Boolean
    ): List<Result> {
        val cleanQuery = cleanVisibleText(query)

        if (cleanQuery.length < 2) {
            return emptyList()
        }

        val variants = searchVariants(
            query = cleanQuery,
            isEnglish = isEnglish
        )

        val directExplanationResults =
            directSideKickDefenseResults(
                query = cleanQuery,
                isEnglish = isEnglish
            )

        val hardSectionResults =
            hardSectionResults(
                query = cleanQuery,
                variants = variants,
                isEnglish = isEnglish
            )

        val regularResults =
            variants
                .flatMap { variant ->
                    runCatching {
                        KmiSearchBridge.searchExercises(variant)
                    }.getOrElse {
                        emptyList()
                    }
                }
                .map { hit ->
                    val rawKey = hit.id ?: hit.title

                    Result(
                        id = rawKey,
                        title = hit.title,
                        subtitle = hit.subtitle
                            ?: subtitleFromResolvedKey(
                                rawKey = rawKey,
                                fallbackBeltName = "",
                                isEnglish = isEnglish
                            )
                    )
                }

        return (
                directExplanationResults +
                        hardSectionResults +
                        regularResults
                )
            .distinctBy { result ->
                normalizeForMatch(result.title)
            }
    }

    fun normalizeSpokenQuery(raw: String): String {
        return raw
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(
                Regex(
                    pattern =
                        """(?<![\p{L}])(?:קבלה|קבלר|קוואל|קוואלרר|קאוולר|קוואולר)(?![\p{L}])""",
                    option = RegexOption.IGNORE_CASE
                ),
                "קוואלר"
            )
            .trim()
    }

    private fun searchVariants(
        query: String,
        isEnglish: Boolean
    ): List<String> {
        val variants = linkedSetOf<String>()

        fun add(value: String) {
            val clean = cleanVisibleText(value)

            if (clean.length >= 2) {
                variants += clean
            }
        }

        add(query)
        add(query.removePrefix("ה "))
        add(query.removePrefix("ה"))
        add(query.replace("הגנה נגד", "נגד"))
        add(query.replace("הגנה", ""))
        add(query.replace("ה ", ""))

        val normalizedQuery = normalizeForMatch(query)

        val isSideKickDefenseSearch =
            listOf(
                "הגנה נגד בעיטה לצד",
                "הגנה נגד בעיטות לצד",
                "נגד בעיטה לצד",
                "נגד בעיטות לצד",
                "הגנה חיצונית באמת",
                "הגנה חיצונית באמה",
                "באמת ימין",
                "באמת שמאל",
                "באמה ימין",
                "באמה שמאל"
            ).any { phrase ->
                phrase in normalizedQuery
            }

        if (isSideKickDefenseSearch) {
            add("הגנה חיצונית באמת ימין נגד בעיטה לצד")
            add("הגנה חיצונית באמת שמאל נגד בעיטה לצד")
            add("הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית")
            add("הגנות נגד בעיטות נגד בעיטות לצד")
            add("הגנות נגד בעיטות")
            add("נגד בעיטה לצד")
            add("נגד בעיטות לצד")
        }

        if (isEnglish) {
            addEnglishHebrewVariants(
                normalizedQuery = normalizedQuery,
                add = ::add
            )
        }

        return variants.toList()
    }

    private fun addEnglishHebrewVariants(
        normalizedQuery: String,
        add: (String) -> Unit
    ) {
        when {
            normalizedQuery.contains("body") ||
                    normalizedQuery.contains("hug") -> {
                add("חביקות גוף")
                add("שחרור מחביקות")
                add("שחרורים")
            }

            normalizedQuery.contains("knife") -> {
                add("סכין")
                add("הגנות מסכין")
                add("הגנות")
            }

            normalizedQuery.contains("kick") -> {
                add("בעיטה")
                add("בעיטות")
                add("הגנות נגד בעיטות")
            }

            normalizedQuery.contains("punch") -> {
                add("אגרוף")
                add("אגרופים")
                add("הגנות פנימיות")
                add("הגנות חיצוניות")
            }

            normalizedQuery.contains("release") ||
                    normalizedQuery.contains("choke") ||
                    normalizedQuery.contains("grab") -> {
                add("שחרור")
                add("שחרורים")
                add("שחרור מחניקות")
                add("שחרור מתפיסות ידיים / שיער / חולצה")
            }

            normalizedQuery.contains("elbow") -> {
                add("מרפק")
                add("מכות מרפק")
            }

            normalizedQuery.contains("stick") ||
                    normalizedQuery.contains("rifle") -> {
                add("מקל")
                add("רובה")
            }

            normalizedQuery.contains("roll") ||
                    normalizedQuery.contains("fall") -> {
                add("גלגול")
                add("בלימות וגלגולים")
            }
        }
    }

    private fun subtitleFromResolvedKey(
        rawKey: String,
        fallbackBeltName: String,
        isEnglish: Boolean
    ): String {
        val resolved = ContentRepo.resolveItemKey(rawKey)

        return if (resolved != null) {
            val beltLabel = beltLabel(
                belt = resolved.belt,
                isEnglish = isEnglish
            )

            if (resolved.topicTitle.isNotBlank()) {
                "$beltLabel • ${resolved.topicTitle}"
            } else {
                beltLabel
            }
        } else {
            beltLabel(
                beltName = fallbackBeltName,
                isEnglish = isEnglish
            )
        }
    }

    private fun hardSectionResults(
        query: String,
        variants: List<String>,
        isEnglish: Boolean
    ): List<Result> {
        val normalizedQuery = normalizeForMatch(query)

        val shouldSearchKnife =
            normalizedQuery.contains("סכין") ||
                    normalizedQuery.contains("knife") ||
                    variants.any { variant ->
                        val normalizedVariant =
                            normalizeForMatch(variant)

                        normalizedVariant.contains("סכין") ||
                                normalizedVariant.contains("knife")
                    }

        if (!shouldSearchKnife) {
            return emptyList()
        }

        val resolved = runCatching {
            HardSectionsResolver.resolve(
                subjectId = "knife_defense",
                sectionId = null
            )
        }.getOrNull()

        fun itemsForGroup(
            group: HardSectionsResolver.BeltItems
        ): List<String> {
            val rawItems: Any? = group.items

            return when (rawItems) {
                is Iterable<*> ->
                    rawItems.mapNotNull { item ->
                        item?.toString()?.trim()
                    }

                is Array<*> ->
                    rawItems.mapNotNull { item ->
                        item?.toString()?.trim()
                    }

                else ->
                    listOfNotNull(
                        rawItems?.toString()?.trim()
                    )
            }
                .filter { item -> item.isNotBlank() }
                .distinct()
        }

        fun resultsForGroup(
            group: HardSectionsResolver.BeltItems
        ): List<Result> {
            val beltText = beltLabel(
                belt = group.belt,
                isEnglish = isEnglish
            )

            val topicText =
                if (isEnglish) {
                    "Knife defenses"
                } else {
                    "הגנות מסכין"
                }

            return itemsForGroup(group)
                .filter { itemTitle ->
                    matchesTitle(
                        title = itemTitle,
                        variants = variants
                    ) ||
                            normalizeForMatch(itemTitle)
                                .contains("סכין") ||
                            normalizeForMatch(itemTitle)
                                .contains("knife")
                }
                .map { itemTitle ->
                    Result(
                        id =
                            "${group.belt.id}::הגנות מסכין::$itemTitle",
                        title = itemTitle,
                        subtitle = "$beltText • $topicText"
                    )
                }
        }

        fun flattenSections(
            subjectId: String,
            entries: List<HardSectionsResolver.SectionEntry>
        ): List<Result> {
            return entries.flatMap { entry ->
                when (
                    val nested = runCatching {
                        HardSectionsResolver.resolve(
                            subjectId = subjectId,
                            sectionId = entry.id
                        )
                    }.getOrNull()
                ) {
                    is HardSectionsResolver.NodeResult.BeltGroups ->
                        nested.groups.flatMap(::resultsForGroup)

                    is HardSectionsResolver.NodeResult.Sections ->
                        flattenSections(
                            subjectId = subjectId,
                            entries = nested.entries
                        )

                    null -> emptyList()
                }
            }
        }

        return when (resolved) {
            is HardSectionsResolver.NodeResult.BeltGroups ->
                resolved.groups.flatMap(::resultsForGroup)

            is HardSectionsResolver.NodeResult.Sections ->
                flattenSections(
                    subjectId = "knife_defense",
                    entries = resolved.entries
                )

            null -> emptyList()
        }
    }

    private fun directSideKickDefenseResults(
        query: String,
        isEnglish: Boolean
    ): List<Result> {
        val normalizedQuery = normalizeForMatch(query)

        val shouldUseDirectResults =
            listOf(
                "הגנה נגד בעיטה לצד",
                "הגנה נגד בעיטות לצד",
                "נגד בעיטה לצד",
                "נגד בעיטות לצד"
            ).any { phrase ->
                phrase in normalizedQuery
            }

        if (!shouldUseDirectResults) {
            return emptyList()
        }

        val directTitles = listOf(
            "הגנה חיצונית באמת ימין נגד בעיטה לצד",
            "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
            "הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית"
        )

        return directTitles.mapNotNull { title ->
            val explanation = Explanations.get(
                belt = Belt.GREEN,
                item = title,
                exerciseId = null
            ).trim()

            val hasExplanation =
                explanation.isNotBlank() &&
                        !explanation.startsWith(
                            "הסבר מפורט על:"
                        )

            if (hasExplanation) {
                Result(
                    id = "green::הגנות::$title",
                    title = title,
                    subtitle = if (isEnglish) {
                        "Green belt • Defenses"
                    } else {
                        "חגורה ירוקה • הגנות"
                    }
                )
            } else {
                null
            }
        }
    }

    private fun matchesTitle(
        title: String,
        variants: List<String>
    ): Boolean {
        val normalizedTitle = normalizeForMatch(title)

        return variants.any { variant ->
            val normalizedVariant =
                normalizeForMatch(variant)

            normalizedVariant.length >= 2 &&
                    (
                            normalizedTitle.contains(
                                normalizedVariant
                            ) ||
                                    normalizedVariant
                                        .split(" ")
                                        .filter { word ->
                                            word.length >= 2
                                        }
                                        .all { word ->
                                            normalizedTitle.contains(word)
                                        }
                            )
        }
    }

    private fun beltLabel(
        belt: Belt,
        isEnglish: Boolean
    ): String {
        return when (belt) {
            Belt.YELLOW ->
                if (isEnglish) "Yellow belt" else "חגורה צהובה"

            Belt.ORANGE ->
                if (isEnglish) "Orange belt" else "חגורה כתומה"

            Belt.GREEN ->
                if (isEnglish) "Green belt" else "חגורה ירוקה"

            Belt.BLUE ->
                if (isEnglish) "Blue belt" else "חגורה כחולה"

            Belt.BROWN ->
                if (isEnglish) "Brown belt" else "חגורה חומה"

            Belt.BLACK ->
                if (isEnglish) "Black belt" else "חגורה שחורה"

            else -> belt.name
        }
    }

    private fun beltLabel(
        beltName: String,
        isEnglish: Boolean
    ): String {
        return when (beltName.uppercase()) {
            "YELLOW" ->
                if (isEnglish) "Yellow belt" else "חגורה צהובה"

            "ORANGE" ->
                if (isEnglish) "Orange belt" else "חגורה כתומה"

            "GREEN" ->
                if (isEnglish) "Green belt" else "חגורה ירוקה"

            "BLUE" ->
                if (isEnglish) "Blue belt" else "חגורה כחולה"

            "BROWN" ->
                if (isEnglish) "Brown belt" else "חגורה חומה"

            "BLACK" ->
                if (isEnglish) "Black belt" else "חגורה שחורה"

            else -> beltName
        }
    }

    private fun cleanVisibleText(value: String): String {
        return value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeForMatch(value: String): String {
        return cleanVisibleText(value)
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .lowercase()
    }
}
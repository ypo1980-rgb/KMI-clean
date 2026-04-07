object AppStrings {

    enum class Lang { HE, EN }

    var currentLang: Lang = Lang.HE

    private val strings = mapOf(

        "branch_netanya_ofek" to mapOf(
            Lang.HE to "נתניה – מרכז קהילתי אופק",
            Lang.EN to "Netanya – Ofek Community Center"
        ),

        "branch_netanya_sokolov" to mapOf(
            Lang.HE to "נתניה – מרכז קהילתי סוקולוב",
            Lang.EN to "Netanya – Sokolov Community Center"
        ),

        "branch_netanya_nordau" to mapOf(
            Lang.HE to "נתניה – נורדאו",
            Lang.EN to "Netanya – Nordau"
        ),

        "branch_azriel" to mapOf(
            Lang.HE to "עזריאל – מושב עזריאל",
            Lang.EN to "Azriel – Moshav Azriel"
        ),

        "group_adults" to mapOf(
            Lang.HE to "בוגרים",
            Lang.EN to "Adults"
        ),

        "group_kids" to mapOf(
            Lang.HE to "ילדים",
            Lang.EN to "Kids"
        ),

        "group_youth_adults" to mapOf(
            Lang.HE to "נוער + בוגרים",
            Lang.EN to "Youth + Adults"
        )

    )

    fun t(key: String): String {
        return strings[key]?.get(currentLang) ?: key
    }
}
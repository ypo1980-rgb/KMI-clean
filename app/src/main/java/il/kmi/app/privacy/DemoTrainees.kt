package il.kmi.app.privacy

import il.kmi.shared.domain.Belt

data class DemoTrainee(
    val id: String,
    val name: String,
    val nameEn: String,
    val age: Int,
    val belt: Belt,
    val branch: String,
    val yearsTraining: Int,
    val beltProgressPercent: Int,
    val attendancePercent: Int
) {
    fun displayName(isEnglish: Boolean): String =
        if (isEnglish) nameEn else name
}

object DemoTrainees {

    val trainees = listOf(

        DemoTrainee(
            id = "t1",
            name = "איתי כהן",
            nameEn = "Itay Cohen",
            age = 14,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 42,
            attendancePercent = 88
        ),

        DemoTrainee(
            id = "t2",
            name = "נועם לוי",
            nameEn = "Noam Levi",
            age = 16,
            belt = Belt.ORANGE,
            branch = "אופק",
            yearsTraining = 2,
            beltProgressPercent = 67,
            attendancePercent = 91
        ),

        DemoTrainee(
            id = "t3",
            name = "אורי מזרחי",
            nameEn = "Ori Mizrahi",
            age = 18,
            belt = Belt.GREEN,
            branch = "סוקולוב",
            yearsTraining = 3,
            beltProgressPercent = 58,
            attendancePercent = 84
        ),

        DemoTrainee(
            id = "t4",
            name = "דניאל פרץ",
            nameEn = "Daniel Peretz",
            age = 22,
            belt = Belt.BLUE,
            branch = "סוקולוב",
            yearsTraining = 4,
            beltProgressPercent = 73,
            attendancePercent = 93
        ),

        DemoTrainee(
            id = "t5",
            name = "עידו אברהם",
            nameEn = "Ido Avraham",
            age = 19,
            belt = Belt.ORANGE,
            branch = "אופק",
            yearsTraining = 2,
            beltProgressPercent = 35,
            attendancePercent = 79
        ),

        DemoTrainee(
            id = "t6",
            name = "רועי בן דוד",
            nameEn = "Roy Ben David",
            age = 24,
            belt = Belt.GREEN,
            branch = "סוקולוב",
            yearsTraining = 5,
            beltProgressPercent = 81,
            attendancePercent = 95
        ),

        DemoTrainee(
            id = "t7",
            name = "יונתן ביטון",
            nameEn = "Yonatan Biton",
            age = 17,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 54,
            attendancePercent = 86
        ),

        DemoTrainee(
            id = "t8",
            name = "עומר מלכה",
            nameEn = "Omer Malka",
            age = 21,
            belt = Belt.BLUE,
            branch = "סוקולוב",
            yearsTraining = 4,
            beltProgressPercent = 62,
            attendancePercent = 82
        ),

        DemoTrainee(
            id = "t9",
            name = "אדם ישראלי",
            nameEn = "Adam Israeli",
            age = 15,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 48,
            attendancePercent = 90
        ),

        DemoTrainee(
            id = "t10",
            name = "אלון גולן",
            nameEn = "Alon Golan",
            age = 26,
            belt = Belt.BROWN,
            branch = "סוקולוב",
            yearsTraining = 6,
            beltProgressPercent = 89,
            attendancePercent = 97
        ),

        DemoTrainee(
            id = "t11",
            name = "תומר אזולאי",
            nameEn = "Tomer Azulay",
            age = 20,
            belt = Belt.GREEN,
            branch = "אופק",
            yearsTraining = 3,
            beltProgressPercent = 71,
            attendancePercent = 85
        ),

        DemoTrainee(
            id = "t12",
            name = "אריאל הרשקו",
            nameEn = "Ariel Hershko",
            age = 28,
            belt = Belt.BLACK,
            branch = "סוקולוב",
            yearsTraining = 8,
            beltProgressPercent = 96,
            attendancePercent = 98
        )
    )
}
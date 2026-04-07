package il.kmi.app.privacy

import il.kmi.shared.domain.Belt

data class DemoTrainee(
    val id: String,
    val name: String,
    val age: Int,
    val belt: Belt,
    val branch: String,
    val yearsTraining: Int,
    val beltProgressPercent: Int,
    val attendancePercent: Int
)

object DemoTrainees {

    val trainees = listOf(

        DemoTrainee(
            id = "t1",
            name = "מתאמן 1",
            age = 14,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 42,
            attendancePercent = 88
        ),

        DemoTrainee(
            id = "t2",
            name = "מתאמן 2",
            age = 16,
            belt = Belt.ORANGE,
            branch = "אופק",
            yearsTraining = 2,
            beltProgressPercent = 67,
            attendancePercent = 91
        ),

        DemoTrainee(
            id = "t3",
            name = "מתאמן 3",
            age = 18,
            belt = Belt.GREEN,
            branch = "סוקולוב",
            yearsTraining = 3,
            beltProgressPercent = 58,
            attendancePercent = 84
        ),

        DemoTrainee(
            id = "t4",
            name = "מתאמן 4",
            age = 22,
            belt = Belt.BLUE,
            branch = "סוקולוב",
            yearsTraining = 4,
            beltProgressPercent = 73,
            attendancePercent = 93
        ),

        DemoTrainee(
            id = "t5",
            name = "מתאמן 5",
            age = 19,
            belt = Belt.ORANGE,
            branch = "אופק",
            yearsTraining = 2,
            beltProgressPercent = 35,
            attendancePercent = 79
        ),

        DemoTrainee(
            id = "t6",
            name = "מתאמן 6",
            age = 24,
            belt = Belt.GREEN,
            branch = "סוקולוב",
            yearsTraining = 5,
            beltProgressPercent = 81,
            attendancePercent = 95
        ),

        DemoTrainee(
            id = "t7",
            name = "מתאמן 7",
            age = 17,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 54,
            attendancePercent = 86
        ),

        DemoTrainee(
            id = "t8",
            name = "מתאמן 8",
            age = 21,
            belt = Belt.BLUE,
            branch = "סוקולוב",
            yearsTraining = 4,
            beltProgressPercent = 62,
            attendancePercent = 82
        ),

        DemoTrainee(
            id = "t9",
            name = "מתאמן 9",
            age = 15,
            belt = Belt.YELLOW,
            branch = "אופק",
            yearsTraining = 1,
            beltProgressPercent = 48,
            attendancePercent = 90
        ),

        DemoTrainee(
            id = "t10",
            name = "מתאמן 10",
            age = 26,
            belt = Belt.BROWN,
            branch = "סוקולוב",
            yearsTraining = 6,
            beltProgressPercent = 89,
            attendancePercent = 97
        ),

        DemoTrainee(
            id = "t11",
            name = "מתאמן 11",
            age = 20,
            belt = Belt.GREEN,
            branch = "אופק",
            yearsTraining = 3,
            beltProgressPercent = 71,
            attendancePercent = 85
        ),

        DemoTrainee(
            id = "t12",
            name = "מתאמן 12",
            age = 28,
            belt = Belt.BLACK,
            branch = "סוקולוב",
            yearsTraining = 8,
            beltProgressPercent = 96,
            attendancePercent = 98
        )
    )
}
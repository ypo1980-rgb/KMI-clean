package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt

object ExerciseExplanationsEn {

    private const val FALLBACK_PREFIX = "Detailed explanation for:"

    fun get(belt: Belt, item: String): String {
        return when (belt) {

            Belt.YELLOW -> yellow(item)
            Belt.ORANGE -> orange(item)
            Belt.GREEN  -> green(item)
            Belt.BLUE   -> blue(item)
            Belt.BROWN  -> brown(item)
            Belt.BLACK  -> black(item)

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun yellow(item: String): String {
        return when (item) {
// ───── Yellow Belt ─────
// ───── General ─────

            "בלימת רכה לפנים" ->
                "Light stance. Fall forward onto the palms while absorbing the impact with bent elbows. Key points: head turned to the side, body straight, knees locked, heels stretched backward. Do not throw the legs backward."

            "בלימה לאחור" ->
                "Step backward. Chin to chest. Break the fall with the palms facing the ground and fingers together. Continue the fall onto the back. Shoulders, head and elbows slightly raised. Hands return above the chest ready to defend. The stepping foot stays on the floor while lifting the hips."

            "תזוזות" ->
                "Movement drills: forward, backward and side steps. Forward – step forward with the left foot and bring the right foot back to the starting stance. Backward – step back with the right foot and bring the left foot back to stance. Left – step left then close the stance with the right foot. Right – step right then close with the left foot."

            "גלגול לפנים – צד ימין" ->
                "Forward roll diagonally over the right shoulder with a rounded back and arms close to the body. Finish standing or in a defensive position."

            "הוצאות אגן, הרמת אגן והפניית גוף למעלה " ->
                "Hip mobility drill: lift the hips while lying on the back and rotate the body upward to build defensive movement and power from the hips."

            "צל בוקס" ->
                "Shadow boxing against an imaginary opponent. Practice punches, kicks and movement in the air to improve coordination and fighting rhythm."

            "סגירת אגרוף" ->
                "Closing the fist in three stages: first fold the top finger joints, then close the fist, and finally place the thumb across the fingers from below. The fist must remain fully closed."

            "אצבעות לפנים" ->
                "Light stance. Step backward while turning the body sideways to the attacker and extend the nearest hand with straight fingers toward the attacker’s throat hollow. The other hand protects the face."

            "מכת קשת האצבע והאגודל" ->
                "Light stance. Step backward and turn the body sideways while striking the attacker’s windpipe with the thumb-finger arch. The other hand protects the face."

// ───── Starting Stances ─────

            "עמידת מוצא רגילה" ->
                "Standard fighting stance. Right foot back, right heel slightly raised, left foot angled slightly inward. Knees relaxed and weight centered."

            "עמידת מוצא להגנות פנימיות" ->
                "Starting stance for internal defenses. Elbows bent downward and palms facing inward."

            "עמידת מוצא להגנות חיצוניות" ->
                "Arms extended forward with bent elbows, palms facing the attacker, leaving a small opening that encourages a direct attack."

            "עמידת מוצא צידית" ->
                "Left hand extended forward with a bent elbow and palm inward. Right hand crosses the line of the left shoulder."

            "עמידת מוצא כללית מספר 1" ->
                "Left shoulder slightly turned inward to reduce the target area. Left hand crosses the right thigh while the right hand crosses the left shoulder."

            "עמידת מוצא כללית מספר 2" ->
                "Left hand extended forward with the palm toward the attacker. Right hand near the left hip. Knees bent in a lower stance to reduce the target area."

// ───── Elbow Strikes ─────

            "מכת מרפק אופקית לפנים" ->
                "Horizontal elbow strike forward toward the opponent’s chest or face."

            "מכת מרפק אנכי למטה" ->
                "Sharp downward elbow strike aimed at the opponent’s head or shoulder."

            "מכת מרפק אנכי למעלה" ->
                "Upward elbow strike toward the opponent’s chin."

            "מכת מרפק לאחור" ->
                "Drive the elbow backward toward the abdomen or chest of an attacker behind you."

            "מכת מרפק לאחור למעלה" ->
                "Backward upward elbow strike aimed at the face or chin of an attacker behind you."

            "מכת מרפק אופקית לאחור" ->
                "Look over the shoulder, raise the elbow sideways with the forearm horizontal, then rotate the heel and deliver a horizontal elbow strike backward."

            "מכת מרפק אופקית לצד" ->
                "Raise the elbow sideways and strike horizontally while stepping to the side and closing the stance."

// ───── Palm Strikes ─────

            "מכת פיסת יד שמאל לפנים" ->
                "Left palm strike forward. Fingers straight and thumb closed. Strike with the heel of the palm while transferring body weight forward."

            "מכת פיסת יד ימין לפנים" ->
                "Right palm strike forward with heel and hip rotation for extra power."

            "מכת פיסת יד שמאל-ימין לפנים" ->
                "Left palm strike forward followed by a right palm strike."

            "מכת פיסת יד שמאל-ימין-שמאל לפנים" ->
                "Left palm strike, then right palm strike, followed by another left palm strike while advancing."

            "מכת פיסת יד מהצד" ->
                "Side palm strike with fingers toward the body and the elbow slightly outward."

// ───── Straight Punches ─────

            "אגרוף שמאל לפנים" ->
                "Extend the left fist forward while shifting body weight forward."

            "אגרוף ימין לפנים" ->
                "Extend the right fist forward while rotating the right heel and hip."

            "אגרוף שמאל-ימין לפנים" ->
                "Left punch followed by a right punch."

            "אגרוף שמאל בהתקדמות" ->
                "Step forward with the left foot and deliver a left punch while transferring weight forward."

            "אגרוף ימין בהתקדמות" ->
                "Step forward with the left foot and deliver a right punch while rotating the right heel and hip."

            "אגרוף שמאל-ימין בהתקדמות" ->
                "In one motion: step forward with the left foot, punch with the left, then punch with the right."

            "אגרוף שמאל-ימין ושמאל בהתקדמות" ->
                "Left punch, right punch, then another left punch while advancing."

            "אגרוף שמאל בנסיגה" ->
                "Step backward with the right foot while delivering a left punch."

            "אגרוף שמאל למטה בהתקפה" ->
                "Bend the knees and punch downward toward the opponent’s lower body."

            "אגרוף ימין למטה בהתקפה" ->
                "Lower the stance and deliver a right downward punch while rotating the hip."

            "אגרוף שמאל למטה בהגנה" ->
                "Defensive downward left punch while shifting the body sideways."

            "אגרוף ימין למטה בהגנה" ->
                "Defensive downward right punch while rotating the heel and hip."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun orange(item: String): String {
        return when (item) {
// ───── General ─────

            "בלימה לצד ימין" ->
                "Light stance. Fall to the right side while tucking the opposite knee toward the chest. Break the fall with the hand closest to the mat, fingers together, elbow lifted and head upright."

            "בלימה לצד שמאל" ->
                "Light stance. Fall to the left side while tucking the opposite knee toward the chest. Break the fall with the hand closest to the mat."

            "גלגול לפנים צד שמאל" ->
                "Left foot forward. Hands forward with the left palm turned inward. Roll diagonally over the left shoulder and finish rising through the right knee back into a fighting stance."

            "גלגול לאחור צד ימין" ->
                "Lower backward onto the right knee, roll over the shoulder with bent knees and return to the starting stance."

            "גלגול לאחור צד שמאל" ->
                "Lower backward and roll over the shoulder with bent knees, finishing back in the starting stance."

            "שילובי ידיים רגליים" ->
                "Continuous combinations of punches and kicks to improve coordination and flow."

// ───── Hand Strikes ─────

            "מכת גב יד בהצלפה" ->
                "A fast snapping back-hand strike aimed at the opponent’s face."

            "מכת גב יד בהצלפה בסיבוב" ->
                "A spinning version of the back-hand strike using body rotation for extra power."

            "מכת פטיש" ->
                "A hammer-fist strike delivered downward with the bottom part of the fist."

            "מכת פטיש מהצד" ->
                "A sideways hammer-fist strike aimed at the opponent’s head or shoulder."

// ───── Kicks ─────

            "בעיטה רגילה בעקב לסנטר" ->
                "Lift the knee high and strike the chin using the heel."

            "בעיטת הגנה לפנים" ->
                "Front defensive kick delivered with the ball of the foot, foot or heel toward the attacker’s center."

            "בעיטת סנוקרת לאחור" ->
                "Backwards heel strike while looking over the shoulder."

            "בעיטה לצד בשיכול" ->
                "Side kick after crossing the supporting leg behind the kicking leg."

            "בעיטה רגילה לאחור" ->
                "Back kick delivered between the opponent’s legs."

            "בעיטה לצד בנסיגה" ->
                "Step backward and deliver a low defensive side kick."

            "בעיטת הגנה לאחור" ->
                "Raise the knee forward then kick backward while bending the upper body forward."

            "בעיטת סטירה פנימית" ->
                "Inside slap kick delivered with the inner side of the foot."

            "בעיטת עצירה בכף הרגל האחורית" ->
                "Stopping kick with the rear foot timed to intercept the opponent’s kick."

            "בעיטת עצירה בכף הרגל הקדמית" ->
                "Stopping kick with the front foot timed to intercept the opponent’s kick."

            "בעיטה רגילה ובעיטת מגל ברגל השנייה" ->
                "Front kick followed by a roundhouse kick with the opposite leg."

            "שילובי בעיטות" ->
                "Practice correct combinations of kicks ensuring the landing leg is ready for the next kick."

            "ניתור ברגל ימין ובעיטה רגילה ברגל ימין" ->
                "Jump from the right leg, lift the left knee and deliver a right-leg kick while landing on the left."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun green(item: String): String {
        return when (item) {
            "מכת מרפק נגד קבוצה" ->
                "Step backward with the right foot while turning the body. Rotate back toward the attackers through the left shoulder and burst forward with a right elbow strike followed by a left punch."

            "בעיטת רגילה ובעיטת מגל באותה רגל" ->
                "Front kick with the rear leg followed immediately by a roundhouse kick with the same leg."

            "בעיטת סטירה חיצונית" ->
                "Rotate inward and deliver an outside slap kick while looking over the shoulder."

            "בעיטת מגל לאחור בשיכול אחורי" ->
                "Turn 180 degrees, cross the rear leg behind the front leg and deliver a backward roundhouse kick."

            "בעיטה לצד בסיבוב" ->
                "Rotate inward and deliver a spinning side kick with the rear leg."

            "בלימה לאחור מגובה" ->
                "Backward fall from height with chin tucked to chest, rounded back and limbs lifted."

            "בלימה לצד כהכנה לגזיזות" ->
                "Side fall preparation: lie on the right side supporting the head with the right hand while lifting the legs."

            "גלגול לפנים ובלימה לאחור – ימין" ->
                "Forward roll over the right shoulder finishing with a backward break fall."

            "גלגול לפנים ובלימה לאחור – שמאל" ->
                "Forward roll followed by a backward break fall."

            "גלגול לפנים ולאחור – ימין" ->
                "Forward roll followed immediately by a backward roll."

            "גלגול לפנים ולאחור – שמאל" ->
                "Forward roll followed by a backward roll."

            "גלגול ביד אחת – ימין" ->
                "Forward roll using the right hand as support."

            "גלגול ביד אחת – שמאל" ->
                "Forward roll using the left hand as support."

            "גלגול לפנים – קימה לפנים" ->
                "Forward roll finishing with a forward rise into a standing position."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun blue(item: String): String {
        return when (item) {
            "מניעת נפילה מחביקת שוקיים מלפנים להפלה" ->
                "Prevent a takedown from a front leg grab by throwing the legs backward into a wide stance while leaning onto the attacker. Control the attacker’s neck with the near hand and strike forward with the other hand."

            "גלגול לצד – ימין/שמאל" ->
                "Side roll to the right or left using one arm for support. Chin tucked to the chest while rolling across the shoulders."

            "גלגול ברחיפה - ימין/שמאל" ->
                "Run forward, leap into the air, absorb the landing with the hands and continue into a forward roll."

            "גלגול לגובה - ימין/שמאל" ->
                "Run and jump upward, break the fall with the hands and continue into a forward roll."

            "גלגול ללא ידיים - ימין/שמאל" ->
                "Right foot forward, head down and to the left. Roll over the right shoulder without using the hands and rise back to a fighting stance."
            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun brown(item: String): String {
        return when (item) {
            "בעיטה רגילה ובעיטת מגל בניתור" ->
                "Front kick followed by a jumping roundhouse kick."

            "בעיטת מגל בניתור" ->
                "Jump using the base leg and deliver a roundhouse kick with the other leg."

            "בעיטת מגל כפולה בניתור" ->
                "Horizontal roundhouse kick followed by a jump and a second roundhouse kick using the rotational momentum."

            "גלגול עם רובה" ->
                "Place the rifle butt on the ground at a sharp angle and roll forward over the shoulder. Finish in prone, kneeling or standing position."
            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun black(item: String): String {
        return when (item) {
            "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין" ->
                "Jump from the left leg and deliver a front kick with the right leg while the left knee bends and the heel moves toward the glutes."

            "ניתור ברגל שמאל ובעיטה לצד ברגל ימין" ->
                "Jump from the left leg, rotate the body sideways and deliver a side kick with the right leg."

            "בעיטת לצד בסיבוב מלא בניתור" ->
                "A spinning jump followed by a side kick with the opposite leg."

            "בעיטת מגל לאחור בסיבוב בניתור" ->
                "A spinning jump followed by a backward roundhouse kick."

            "בעיטת הגנה לאחור בניתור" ->
                "A jumping backward defensive kick."
            else -> "$FALLBACK_PREFIX $item"
        }
    }
}
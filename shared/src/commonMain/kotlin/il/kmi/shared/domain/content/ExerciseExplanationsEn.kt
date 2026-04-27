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
                "Elbows bent downward, palms toward the attacker."

            "עמידת מוצא להגנות פנימיות" ->
                "Hands extended forward, elbows bent and farther away from the body, palms toward the attacker and widened to draw in (lure in) a direct attack."

            "עמידת מוצא להגנות חיצוניות" ->
                "Left hand passes the right hip while turning the left shoulder inward. Right hand passes the left shoulder."

            "עמידת מוצא צידית" ->
                "Start from a simple stance with legs spread. Turn the body, hands and the foot closer to the attacker to the side, where the attacker is positioned. Correct the position of the rear foot for comfortability."

            "עמידת מוצא כללית מספר 1" ->
                "Left palm is extended forward, elbow bent dowward, palm facing the attacker. Right hand passes the left hip. Knees are bent to make the stance lower (to make \"you\" as target smaller and the area of defence larger)."

            "עמידת מוצא כללית מספר 2" ->
                "Left hand is extended forward, elbow is bent, and the palm is facing inward. The right hand is positioned over the left shoulder. This stance is for bursting toward the attacker."

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
    // ✔️ תקין
    private fun black(item: String): String {
        return when (item) {
            "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין" -> "Leap with the left foot. Perform a regular kick with the right foot while bending the left knee and bringing the left heel toward the buttocks. Land with the left foot and the right foot joins the left on the ground."

            "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין" -> "Leap with the left foot. Perform a regular kick with the right foot while bending the left knee and bringing the left heel toward the buttocks. Land with the left foot and the right foot joins the left on the ground."

            "ניתור ברגל שמאל ובעיטה לצד ברגל ימין" -> "Leap with the left foot. Turn the toes of the left foot to the left. Turn the right side of the body to face the attacker and perforn a side-kick with the right foot while bending the left knee and bringing the left heel toward the buttocks."

            "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל" -> "Leap with the left foot. Turn the toes of the left foot to the right. Turn the left side of the body to face the attacker. Perform a leg swich (scissors) and perforn a side-kick with the left foot while bending the right knee and bringing the right heel t"

            "בעיטה לצד בסיבוב מלא בניתור" -> "Leaping with a spin and performing a side-kick with the rear foot."

            "בעיטת מגל לאחור בסיבוב בניתור" -> "Leaping with a spin and performing a backward Magal (Circular) kick with the rear foot."

            "בעיטת הגנה לאחור בניתור" -> "Leaping with a spin and performing a backward defensive kick with the rear foot."

            "**הגנה פנימית נגד אגרוף שמאל - בעיטת הגנה**" -> "Perform an Internal Defence with your right hand and a defensive kick to the center of the attacker's body."

            "**הגנה פנימית נגד אגרוף שמאל - בעיטה לצד**" -> "Perform an Internal Defence with your right hand, cross your legs and perform a side-kick with your right foot toward the attacker's knee."
            "**הגנה פנימית נגד אגרוף שמאל - בעיטה רגילה לאחור**" -> "Perform an Internal Defence with your right hand, cross your legs, turn your back toward the attacker and perform a backward regular kick with your right foot."

            "**הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לאחור**" -> "Perform an Internal Defence with your right hand and a backward Magal (Circular) kick with your right foot."

            "**הגנה פנימית נגד אגרוף שמאל - בעיטת סטירה חיצונית**" -> "Perform an Internal Defence with your right hand and an external slapping kick with your right foot."

            "**הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לפנים**" -> "Perform an Internal Defence with your right hand and a front Magal (Circular) kick with your right foot."

            "**הגנה פנימית נגד אגרוף שמאל - גזיזה קדמית**" -> "Perform an Internal Defence with your right hand, cross your legs while rotating your body and perform a forward cutting kick."

            "**הגנה נגד בעיטה רגילה - התחמקות בסיבוב**" -> "Stand either in a neutral stance or a general stance number 2. Perform an Internal Defence with your left hand and continue the movement to lift the kicking leg while evading to the Blind Side with half a spin. Step backward while lifting your left hand t"

            "**הגנה נגד בעיטת מגל לפנים לראש - הדיפה באמת שמאל**" -> "From an External Defences stance perform a 360 degree defence number one with your left forearm under the attacker's leg while advancing and thrusting the leg upward to drop the attacker."

            "**הגנה נגד בעיטת מגל לפנים לראש - רגל עברה מעל הראש**" -> "From an External Defences stance perform a 360 degree defence number one with your left forearm under the attacker's leg while performing an uppercut strike to the groin."

            "**הגנה נגד מגל לפנים  לראש - התחמקות גוף בסיבוב בגזיזה**" -> "Evade the kick by kneeling on the left knee with a spin, place both your hands on the ground and perform a backward low Magal (Circular) kick to drop the attacker."

            "**הגנה נגד בעיטת סטירה - גזיזה**" -> "Stand in a reversed general stance number 2. Cross your legs and perforn a backward cutting kick with your right leg while thrusting the attacker's kicking leg upward with your right hand."

            "**שחרור מתפיסת נלסון**" -> "Lower your elbows as low as possible to grab the attacker hands with yours. Shift your hip to the side and place one foot behind the attackers foot. Push with your knee to the back of the knee of the attacker, fall backward on top of the attacker while ho"

            "**שחרור מחביקת צואר מהצד - משיכה לאחור**" -> "On the sense of the pull backward one hand goes to a vital point on the attacker's head while the leg closer to the attacker goes behind his legs. Fall with the attacker to the ground, strike and get up."

            "**שחרור מחביקת צואר מאחור - משיכה לאחור**" -> "לא מזהה הסבר לתרגיל"

            "**שחרור מחביקת צואר מהצד - יד תפוסה**" -> "The attacker's foot is in front of the deffender's feet and the deffender's hand is in front of the attacker's legs. The attcker wears long pants: Grab with both hands in the upper shin area and lift. Fall backward with the attacker, strike and get up. Th"

            "**שחרור מחביקת צואר מהצד - זריקת רגל**" -> "On the sense of falling forward send your leg between the attacker's legs while lying on your back. Strike to the groin with your free hand to throw the attacker on the back."

            "**שחרור מחביקת צואר מהצד - ירידה לברך**" -> "Strike the attacker's groin with the front hand while grabbing a vital point on the attacker's head with the rear hand and pull backward to drop the attacker."

            "**שחרור מחביקת צואר מהצד - מהברך**" -> "לא מזהה הסבר לתרגיל"

            "**שחרור מחביקה פתוחה מהצד**" -> "Perform a knee strike to the groin, elbow strike or a head butt."

            "**שחרור מחביקה סגורה מהצד - היד הרחוקה משוחררת**" -> "The head of the attacker is in front: Punch the head with the rear hand. The head of the attacker is in behind: With the hand closer to the attacker strike the groin and continue the movement toward a high elbow strike. It is possible to stomp the back of"

            "**שחרור מחביקה סגורה מהצד**" -> "With the hand closer to the attacker strike the groin and continue the movement toward a high elbow strike."

            "**שחרור מחביקה פתוחה מאחור - הטלה**" -> "לא מזהה הסבר לתרגיל"

            "**שחרור מחביקה סגורה מאחור - הטלה**" -> "לא מזהה הסבר לתרגיל"

            "**הגנה נגד מקל ארוך-התקפה לצד ימין מגן**" -> "From a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Pull down the attacker's hand with two hooked hands while taking your right foot backward and attack with a right punch."

            "**הגנה נגד מקל ארוך-התקפה לצד שמאל מגן**" -> "From a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Step forward with your right foot placing your body at the attacker's side striking with a right punch or elbow according to range."
            "הגנה נגד מקל ארוך מצד ימין" -> "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Wrap your right hand around the arms of the attacker, punch with your left hand (Some situations require a horizontal elbow strike) while spin"
            "הגנה נגד מקל ארוך מצד שמאל" -> "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Wrap your left hand around the arms of the atacker, step with your right foot and perform a horizontal elbow strike with your right hand while"

            "הגנה נגד מקל ארוך דקירה - צד חי" -> "Stand in a neutral stance. Deflect the rifle with your left palm. Step forward with your left while rotating the body and defend with both your forearms. Grab the rifle with both hands. Pull the rifle upward while kicking to the groin (Create leverage on"

            "הגנה נגד מקל ארוך דקירה - צד מת" -> "Stand in a neutral stance. Deflect the rifle with your right palm and grab it with your left while stepping forward with your right foot and performing a kick to the groin with your left foot (Punch the back of the head of the attacker with your right han"

            "הגנה נגד דקירה - צד חי ימין" -> "Perform an Internal Defence with the right forearm and kick to the attacker's knee while leaning the opposide direction."

            "הגנה נגד דקירה - צד חי שמאל" -> "Perform an External Defence with the left forearm and kick to the attacker's knee while leaning the opposide direction."

            "הגנה נגד דקירה - צד מת ימין" -> "Perform an External Defence with the right forearm and kick to the attacker's knee while leaning the opposide direction."

            "הגנה נגד דקירה - צד מת שמאל" -> "Perform an Internal Defence with the left forearm and kick to the attacker's knee while leaning the opposide direction."

            "מקל נגד סכין - דקירה רגילה" -> "Step with your right foot to the live Side of the attacker if necessary and stab with your stick to the attacker's throat."

            "מקל נגד סכין - דקירה מזרחית" -> "Defend with the stick on the attacker's wrist and strike the attacker's head."

            "מקל נגד סכין - דקירה ישרה" -> "Rotate your body to perform a body defence while deflecting the attack with the stick on the attacker's wrist and continue attacking to the attacker's head."

            "מקל נגד סכין - דקירה מעל מצד ימין" -> "Perform 360 defence with the stick to the attacker's wrist. Step with your left foot and punch the attacker with your left."

            "מקל נגד סכין - דקירה מעל מצד שמאל" -> "Perform an External Defence with your left forearm, step with your right foot and attack with the stick."

            "מקל נגד סכין - דקירה מזרחית מצד ימין" -> "Defend with the stick to the attacker's wrist and strike with the stick to the attacker's throat."

            "מקל נגד סכין - דקירה מזרחית מצד שמאל" -> "Perform an External Defence with your left forearm, step with your right foot and attack with the stick."

            "מקל נגד סכין - דקירה ישירה מצד ימין (פנימית)" -> "Perform an Internal Defence with the stick to the attacker's wrist, step with your right to the live Side and attack with the stick."

            "מקל נגד סכין - דקירה ישירה מצד ימין (חיצונית)" -> "Perform 360 defence with the stick to the attacker's wrist. Step with your left foot and punch the attacker with your left."

            "מקל נגד סכין - דקירה ישירה מצד שמאל (פנימית)" -> "Deflect the attack with your left forearm. Step with your left foot to the Blind Side of the attacker and attack with the stick."

            "מקל נגד סכין - דקירה ישירה מצד שמאל (חיצונית)" -> "Perform an External Defence with your left forearm. Step with your right foot and attack with the stick."

            "מקל אחד וסכין אחת - המקל בצד חי" -> "Defending against the stick with a spin while establishing eye contact with the second attacker. Disarm the attacker if possible and defend against the stab of the second attacker."

            "מקל אחד וסכין אחת - המקל בצד מת" -> "1. Defending against the stick with a cavalier while establishing eye contact with the second attacker. Disarm the attacker and use the stick against the second attacker. 2. Defend the stick and grab a vital spot on the attacker's head with your left hand"

            "מקל אחד וסכין אחת - הסכין קרוב" -> "Defending against the knife while establishing eye contact with the second attacker and then defend against the stick."

            "הגנה נגד איום אקדח לראש מלפנים" -> "Raise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker's thigh. (You should grab the barrel and the trigger guard with your hand). Step with your left foot forward to the Blind Side of the attacker and punch"

            "הגנה נגד איום אקדח צמוד לראש מלפנים" -> "Raise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker's thigh. (You should grab the barrel and the trigger guard with your hand). Step with your left foot forward to the Blind Side of the attacker and punch"

            "הגנה נגד איום אקדח מלפנים - קנה קצר" -> "Grab the wrist of the attacker's hand with your left hand so the back of the hand faces upward. Grab the fist holding the gun with your right hand. Twist to point the gun at the attacker and kick the groin while performing a cavalier to drop the attacker,"

            "הגנה נגד איום אקדח לראש - צד ימין" -> "1. The gun is in front of the defenders arm: Raise your left hand upward and grab the gun, deflecting it sideways and downward. Step with your left foot to the Blind Side of the attacker and punch with your right hand to the attacker's face. 2. The gun is"

            "הגנה נגד איום אקדח לראש - צד שמאל" -> "1. The gun is in front of the defenders arm: Raise your left hand upward and grab the gun, deflecting it sideways and downward. Step with your left foot to the Blind Side of the attacker and punch with your right hand to the attacker's face. 2. The gun is"

            "הגנה נגד איום אקדח מאחור באלכסון - צד שמאל" -> "Glance to the side. Reach your left hand away from your body while rotating counterclockwise and stepping with your right foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height. S"

            "הגנה נגד איום אקדח לראש מאחור" -> "Reach your left hand away from your body while rotating counterclockwise and stepping with your left foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height. Simultaneously, perfor"

            "הגנה נגד איום אקדח מאחור בידיים מורמות" -> "Hands raised. Glance backward. Turn to the live Side. Wrap the hand with the gun close to the wrist with your left. Drop the attacker with a right horizontal elbow while stepping with your right foot to the live Side."

            "הגנה נגד איום אקדח בהובלה" -> "Step with your right foot and turn left to the attacker's Blind Side. Raise your left hand to bring the gun closer to the attacker's body. Put your left ear to the attacker's back and send your right hand between the attacker's legs, raise your right hand"

            "הגנה מאיום אקדח מאחור דחיפה" -> ""

            "1 מקל 1 סכין – מקל בצד חי" -> "Defending against the stick with a spin while establishing eye contact with the second attacker. Disarm the attacker if possible and defend against the stab of the second attacker"
            "1 מקל 1 סכין – מקל בצד מת" -> "Defending against the stick with a cavalier while establishing eye contact with the second attacker. Disarm the attacker and use the stick against the second attacker. 2. Defend the stick and grab a vital spot on the attacker's head with your left hand"
            "1 מקל 1 סכין – במקרה והסכין קרוב" -> "Defending against the knife while establishing eye contact with the second attacker and then defend against the stick"
            "הדמיה כנגד 2 תוקפים" -> "The student should assemble two simulations against two attackers (One is armed and the other is not)"

            "מכת מקל לראש" -> "חסר הסבר בחוברת"
            "מכת מקל לרקה" -> "חסר הסבר בחוברת"
            "מכת מקל ללסת / צוואר" -> "חסר הסבר בחוברת"
            "מכת מקל לפרקי האצבעות" -> "חסר הסבר בחוברת"
            "מכת מקל לעצם הבריח" -> "חסר הסבר בחוברת"
            "מכת מקל למרפק" -> "חסר הסבר בחוברת"
            "מכת מקל לשורש כף היד" -> "חסר הסבר בחוברת"
            "מכת מקל לברך" -> "חסר הסבר בחוברת"
            "מכת מקל למפסעה" -> "חסר הסבר בחוברת"
            "הצלפת מקל לצלעות" -> ""
            "דקירת מקל חיצונית לצלעות" -> "חסר הסבר בחוברת"
            "דקירת מקל ישרה לבטן / לגרון" -> "חסר הסבר בחוברת"
            "דקירת מקל הפוכה" -> "חסר הסבר בחוברת"
            "מכה אופקית לצוואר" -> "From a regular stance, step forward with your left foot and strike the neck or face using the magazine or the handle or the weapon's body."
            "דקירה" -> "From a regular stance, stab forward with the bayonet or the front side of the stick (The body movement is similar to the one used in a straight stab with a knife)."
            "שיסוף" -> "From a regular stance, slash with the bayonet or the front side of the stick while rotating the left heel."
            "מכה לצד" -> "From a neutral stance, strike to the side with the butt of the weapon or the rear of the stick."
            "מכה לאחור" -> "From a regular stance, stab backward with the butt of the weapon or the rear of the stick."
            "מכה למפשעה" -> "From a regular stance, strike to the groin with the butt of the weapon or the rear of the stick, rotate right heel and hip."
            "מכת סנוקרת" -> "From a regular stance, perform an uppercut strike to the attacker's chin with the butt of the weapon or the rear of the stick, rotate right heel and hip."
            "מכה אופקית לאחור" -> "From a regular stance, strike horizontally backward with the butt of the weapon or the rear of the stick while rotating the left heel."
            "מכה אופקית ובעיטה למפשעה" -> "From a regular stance, perform a front horizontal strike and a regular kick to the groin with the rear leg."
            "מכה אופקית ובעיטת הגנה" -> "From a regular stance, perform a front horizontal strike and a defensive kick."
            "מכה לצד ובעיטה לצד" -> "From a neutral stance, perform a side strike. Cross your legs and perform a side-kick to the knee."

            else -> "$FALLBACK_PREFIX $item"
        }
    }
}
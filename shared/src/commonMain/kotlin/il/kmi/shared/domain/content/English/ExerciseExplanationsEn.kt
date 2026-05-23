package il.kmi.shared.domain.content.English

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

            "בלימה רכה לפנים" ->
                "Light stance. Fall forward onto the palms while absorbing the impact with bent elbows. Key points: head turned to the side, body straight, knees locked, heels stretched backward. Do not throw the legs backward."

            "בלימה לאחור" ->
                "Step backward. Chin to chest. Break the fall with the palms facing the ground and fingers together. Continue the fall onto the back. Shoulders, head and elbows slightly raised. Hands return above the chest ready to defend. The stepping foot stays on the floor while lifting the hips."

            "תזוזות" ->
                "Movement drills: forward, backward and side steps. Forward – step forward with the left foot and bring the right foot back to the starting stance. Backward – step back with the right foot and bring the left foot back to stance. Left – step left then close the stance with the right foot. Right – step right then close with the left foot."

            "גלגול לפנים - ימין" ->
                "Forward roll diagonally over the right shoulder with a rounded back and arms close to the body. Finish standing or in a defensive position."

            "הוצאת אגן" ->
                "Hip escape drill. Move the hips away from the attacker while lying on the back to create distance and improve ground movement."

            "הרמת אגן והפניית גוף לכיון ההפלה" ->
                "Lift the hips while lying on the back and rotate the body toward the sweep direction to build defensive movement and power from the hips."

            "צל בוקס" ->
                "Shadow boxing against an imaginary opponent. Practice punches, kicks and movement in the air to improve coordination and fighting rhythm."

            "סגירת אגרוף" ->
                "Closing the fist in three stages: first fold the top finger joints, then close the fist, and finally place the thumb across the fingers from below. The fist must remain fully closed."

            "אצבעות לפנים" ->
                "Light stance. Step backward while turning the body sideways to the attacker and extend the nearest hand with straight fingers toward the attacker’s throat hollow. The other hand protects the face."

            "מכת קשת האגודל והאצבע לקנה הנשימה" ->
                "Light stance. Step backward and turn the body sideways while striking the attacker’s windpipe with the thumb-finger arch. The other hand protects the face."

            "מכת קשת האגודל והאצבע" ->
                "Thumb-finger arch strike delivered forward toward a vulnerable target while keeping the other hand ready to protect."

// ───── Starting Stances ─────

            "עמידת מוצא רגילה" ->
                "Elbows bent downward, palms toward the attacker."

            "עמידת מוצא להגנות פנימיות" ->
                "Hands extended forward, elbows bent and farther away from the body, palms toward the attacker and widened to draw in (lure in) a direct attack."

            "עמידת מוצא להגנות חיצוניות" ->
                "Left hand passes the right hip while turning the left shoulder inward. Right hand passes the left shoulder."

            "עמידת מוצא צידית" ->
                "Start from a simple stance with legs spread. Turn the body, hands and the foot closer to the attacker to the side, where the attacker is positioned. Correct the position of the rear foot for comfortability."

            "עמידת מוצא כללית מס' 1" ->
                "Left palm is extended forward, elbow bent dowward, palm facing the attacker. Right hand passes the left hip. Knees are bent to make the stance lower (to make \"you\" as target smaller and the area of defence larger)."

            "עמידת מוצא כללית מס' 2" ->
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

// ───── Magal Punches and Uppercuts ─────

            "מכת מגל שמאל" ->
                "Left circular punch delivered in a rounded path toward the opponent while keeping the other hand ready to protect."

            "מכת מגל ימין" ->
                "Right circular punch delivered in a rounded path with hip rotation for power and the other hand protecting the face."

            "מכת מגל למטה ולמעלה בהתחלפות" ->
                "Alternating circular punches low and high. Keep continuous rhythm, switch hands smoothly, and maintain balance and guard."

            "מכת סנוקרת שמאל" ->
                "Left uppercut delivered upward toward the opponent’s chin or body while bending the knees and driving from the legs."

            "מכת סנוקרת ימין" ->
                "Right uppercut delivered upward with hip rotation and a short powerful motion toward the opponent’s chin or body."

// ───── Kicks ─────

            "בעיטה רגילה למפסעה" ->
                "Regular kick to the groin. Lift the knee, extend the kicking leg forward, strike with the foot, and return quickly to stance."

            "בעיטה רגילה לסנטר" ->
                "Regular kick to the chin. Lift the knee high, extend the leg upward and forward, and return immediately to a balanced stance."

            "בעיטת ברך נמוכה למפסעה" ->
                "Low knee strike to the groin. Drive the knee forward from close range while keeping the body stable and hands protecting."

            "בעיטת ברך גבוהה" ->
                "High knee strike toward the opponent’s body or head. Pull the knee upward powerfully while maintaining balance."

            "בעיטת ברך מהצד" ->
                "Side knee strike delivered from close range by turning the hips and driving the knee from the side."

            "בעיטת מגל אופקית" ->
                "Horizontal circular kick. Rotate the hip and deliver the kick in a horizontal arc toward the target."

            "בעיטת מגל אלכסונית" ->
                "Diagonal circular kick delivered in an angled path with hip rotation and quick recovery to stance."

            "בעיטת מגל בהטעיה" ->
                "Circular kick with diversion. Use a deceptive movement before delivering the circular kick toward the target."

            "בעיטת מגל נמוכה" ->
                "Low circular kick aimed at the lower body or leg area. Rotate the hip and return quickly to a stable stance."

            "בעיטה לצד מעמידת פיסוק" ->
                "Side kick from a neutral stance. Lift the knee, turn the hip, extend the leg sideways, and return to stance."

// ───── Defences ─────

            "הגנה חיצונית רפלקסיבית 360 מעלות" ->
                "Reflexive 360-degree external defence against strikes from different directions. Move the arm outward, protect the head and body, and continue with counterattack readiness."

            "הגנה פנימית רפלקסיבית" ->
                "Reflexive internal defence against a direct hand strike. Redirect the attack inward while keeping the body protected and ready to counter."

            "הגנה פנימית נגד ימין בכף יד שמאל" ->
                "Internal defence against a right punch using the left palm. Redirect the punch inward while moving the body off the attack line."

            "הגנה פנימית נגד שמאל בכף יד ימין" ->
                "Internal defence against a left punch using the right palm. Redirect the punch inward while maintaining balance and guard."

            "הגנה פנימית נגד בעיטה רגילה למפסעה" ->
                "Internal defence against a regular kick to the groin. Redirect the kick inward with the hand while moving the body away from the line of attack."

// ───── Releases ─────

            "שחרור מתפיסת יד מול יד" ->
                "Release from one hand grabbing the same-side hand. Rotate the grabbed hand toward the attacker’s thumb and pull out along the weakest point of the grip."

            "שחרור מתפיסת יד נגדית" ->
                "Release from one hand grabbing the opposite hand. Turn the grabbed hand toward the thumb opening and pull out while stepping if needed."

            "שחרור מתפיסת שתי ידיים למטה" ->
                "Release from two hands grabbing both hands low. Move toward the weak point of the grips, rotate the hands out, and create distance."

            "שחרור מתפיסת שתי ידיים למעלה" ->
                "Release from two hands grabbing both hands high. Rotate and pull the hands through the weak points of the grips while preparing to defend or counter."

            "שחרור מחביקת צואר מהצד" ->
                "Release from a side neck hold. Lower the body, protect the airway, create space, and use the hands and body movement to escape the hold."

            "מניעת התקרבות תוקף" ->
                "Prevent the attacker’s forward motion by extending the nearest hand toward the throat area while stepping back and turning the body sideways."

            "מניעת חניקה" ->
                "Prevent a choke before it closes by striking forward with the fingers or palm toward the attacker’s throat area while moving away from the line of attack."

            "שחרור מחניקה מלפנים בכף היד" ->
                "Release from a front choke using the palm. React quickly, break the choking pressure, create space, and prepare to counterattack."

            "שחרור מחניקה מאחור במשיכה" ->
                "Release from a rear choke with a pull. Move with the pulling direction to reduce pressure, break the grip, turn, and prepare to defend."

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

            // ─────────── Kicks ───────────

            "בעיטת פטיש" ->
                "Swing the kicking leg diagonally inward and then bring it down vertically. The impact is with the rear part of the heel. The kick can be performed with either the front leg or the rear leg. This is used when the attacker is bent."

            "בעיטת גזיזה אחורית" ->
                "[[RED_BOLD]]Regular fighting stance.[[/RED_BOLD]]\nThe attacker stands with the right leg forward. Perform a switch step to the Blind Side and kick with the rear part of the right shin, close to the heel."

            "בעיטת גזיזה קדמית" ->
                "[[RED_BOLD]]Regular fighting stance.[[/RED_BOLD]]\nThe attacker stands with the right leg forward. Perform a switch step to the Blind Side. While turning your body 180 degrees behind the attacker, perform the kick with the back of your foot."

            "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב" ->
                "While performing a forward cutting kick, the attacker raises his target leg. Continue spinning with a backward cutting kick against the attacker’s other leg."

            "בעיטת מגל לאחור בסיבוב" ->
                "Perform an inward rotation on your front foot. Look over your shoulder, continue rotating, and perform a backward Magal circular kick with the rear leg."

            "בעיטת סטירה חיצונית בסיבוב" ->
                "Perform an inward rotation on your front foot. Look over your shoulder, continue rotating, and perform an external slap kick with the rear leg."

            // ─────────── Breakfalls and Rolls ───────────

            "מניעת נפילה מחביקת שוקיים מלפנים להפלה" ->
                "Evade by throwing both legs backward in a slight spread while leaning over the attacker. Hug the attacker’s neck with one arm and punch with the other."

            "גלגול לצד - ימין/שמאל",
            "גלגול לצד – ימין/שמאל" ->
                "Roll to the side over one hand. The chin should be tucked to the chest. Continue the roll over both shoulder blades."

            "גלגול ברחיפה - ימין/שמאל",
            "גלגול ברחיפה – ימין/שמאל" ->
                "Run forward, dive into the air, break the fall with your hands, and continue rolling forward."

            "גלגול לגובה - ימין/שמאל",
            "גלגול לגובה – ימין/שמאל" ->
                "Run forward, jump upward, break the fall with your hands, and continue rolling forward."

            "גלגול ללא ידיים - ימין/שמאל",
            "גלגול ללא ידיים – ימין/שמאל" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nStep forward with your right foot. Keep your head to the left and roll forward from your right shoulder to your left hip. Upon landing, the left knee should be bent to the side and the left ankle should be under the right knee. Stand up into a fighting stance."

            // ─────────── Defences Against Knee Strikes ───────────

            "הגנה נגד בעיטת ברך מלפנים" ->
                "[[RED_BOLD]]Regular stance.[[/RED_BOLD]]\nAs you see the knee rising, lower your elbows, lower one hand, push the attacker, and perform a vertical upward elbow strike to the front."

            "הגנה נגד בעיטת ברך מהצד" ->
                "Lower your hands, wrap one hand over the attacker’s thigh, and throw him to the ground while striking forward."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה נגד בעיטה רגילה - סייד-סטפ לצד המת",
            "הגנה נגד בעיטה רגילה – סייד-סטפ לצד המת" ->
                "[[RED_BOLD]]Neutral stance or fighting stance.[[/RED_BOLD]]\nWhen you see the kick, side-step to the attacker’s Blind Side while punching with your right hand. It is also possible to perform a front cutting kick with your left leg. Highlight timing."

            "הגנה נגד בעיטה רגילה - סייד-סטפ לצד החי",
            "הגנה נגד בעיטה רגילה – סייד-סטפ לצד החי" ->
                "[[RED_BOLD]]Neutral stance or fighting stance.[[/RED_BOLD]]\nWhen you see the kick, side-step to the attacker’s Live Side while punching with your right hand and kicking a front kick with your left foot. Highlight timing."

            // ─────────── Defences Against Front Magal Kicks ───────────

            "הגנה נגד בעיטת מגל לפנים עם השוק" ->
                "With your left hand, wrap around the attacker’s shin while keeping it close to your body. Step to the side with your right foot and punch with your right hand."

            "הגנה נגד בעיטת מגל לצלעות" ->
                "[[RED_BOLD]]Regular stance.[[/RED_BOLD]]\nWith your left hand, wrap around the attacker’s leg from above. Step to the Live Side with your right foot and punch with your right hand.\n[[RED_BOLD]]General stance No. 2.[[/RED_BOLD]]\nRaise the attacker’s leg with your left hand and perform a right cutting kick."

            "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לצד",
            "הגנה פנימית נגד בעיטת מגל לפנים – בעיטה לצד" ->
                "[[RED_BOLD]]Reversed General Stance No. 1.[[/RED_BOLD]]\nPerform an internal defence with your right forearm. Cross-step and perform a side kick to the attacker’s knee."

            "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לאחור",
            "הגנה פנימית נגד בעיטת מגל לפנים – בעיטה לאחור" ->
                "[[RED_BOLD]]Reversed General Stance No. 1.[[/RED_BOLD]]\nPerform an internal defence with your right forearm. Cross-step and perform a backward regular kick to the attacker’s groin."

            // ─────────── Defences Against Side Kicks ───────────

            "הגנה פנימית באמת ימין נגד בעיטה לצד" ->
                "[[RED_BOLD]]Reversed General Stance No. 1.[[/RED_BOLD]]\nIn one motion, perform an internal defence with your right forearm and an external defence with your left forearm. Simultaneously step forward with your right leg while turning your body to the attacker’s Live Side."

            // ─────────── Releases From Hand Grabs on the Ground ───────────

            "שחרור תפיסת ידיים בשכיבה" ->
                "[[RED_BOLD]]Ground technique note.[[/RED_BOLD]]\nKeep your heels close to your buttocks. Stun the attacker and get off the ground as fast as possible.\nPull both hands downward while raising your hips and rotating to the side. Raise your hands to guard the face and throw the attacker to the side."

            // ─────────── Releases From Neck Holds ───────────

                "שחרור מחביקת צואר מהצד והפלה" ->
                "Start with the release from a neck hold from the side. During the fall, use your free hand to grab a vital spot on the attacker’s head and pull to the side while thrusting your hips forcefully up and toward the side of the pull. Hit the groin with the other hand."

            "שחרור מחביקת צואר מאחור עם נעילה" ->
                "With a hooked right hand, grab the attacker’s forearm and turn your head toward the attacker’s elbow. With your left hand, push the attacker’s elbow upward. Step with your right foot to the left into the attacker’s body and exit the hold. Perform this before the lock is completed."

            // ─────────── Releases From Holds While on the Ground ───────────

            "שחרור מחביקת צואר בשכיבה ברכיבה צמודה" ->
                "With the hand opposite the attacker’s head, grab a vital spot on the attacker’s head. With the other hand, punch the attacker’s ribs while lifting and turning your hips to reverse the attacker’s position."

            // ─────────── Releases From Chokes ───────────

            "שחרור מחניקה לקיר - מלפנים לא צמודה",
            "שחרור מחניקה לקיר – מלפנים לא צמודה" ->
                "With a hooked hand, release the choke. With the other hand, perform a palm-heel strike from the side to the attacker’s face. Exit to the side."

            "שחרור מחניקה לקיר - צמודה מלפנים",
            "שחרור מחניקה לקיר – צמודה מלפנים" ->
                "With a hooked hand, release the choke. With the other hand, grab a vital spot on the attacker’s head to pull the attacker away while kneeing the groin and exiting to the side."

            "שחרור מחניקה לקיר - דחיפה מאחור",
            "שחרור מחניקה לקיר – דחיפה מאחור" ->
                "With your head facing to the side, break your movement against the wall using both hands. Raise your right arm and press it against your ear to create leverage. Spin to the side to face the attacker’s Blind Side beyond the attacker’s legs and attack."

            "שחרור מחניקה לקיר - צמודה מאחור",
            "שחרור מחניקה לקיר – צמודה מאחור" ->
                "With a hooked hand, release the choke. Rotate your body and attack while exiting to the side."

            // ─────────── Releases From Chokes While on the Ground ───────────

            "שחרור מחניקה בשכיבה - ידיים כפופות",
            "שחרור מחניקה בשכיבה – ידיים כפופות" ->
                "[[RED_BOLD]]Ground technique note.[[/RED_BOLD]]\nKeep your heels close to your buttocks. Stun the attacker and get up quickly.\nRelease with one hooked hand. With the other hand, perform a front palm-heel strike. Raise your hips and turn toward the direction of the releasing hand. Attack and get up. Similar to the release from a front choke."

            "שחרור מחניקה בשכיבה - ידיים ישרות",
            "שחרור מחניקה בשכיבה – ידיים ישרות" ->
                "[[RED_BOLD]]Ground technique note.[[/RED_BOLD]]\nKeep your heels close to your buttocks. Stun the attacker and get up quickly.\nRelease the choke with one hooked hand. Press the other arm against your ear to create leverage on the attacker’s wrist and continue inward with your elbow to create additional leverage. Raise your hips and turn toward the direction of the releasing hand. Attack and get up."

            "שחרור מחניקה צמודה בשכיבה" ->
                "[[RED_BOLD]]Ground technique note.[[/RED_BOLD]]\nKeep your heels close to your buttocks. Stun the attacker and get up quickly.\nWith a hooked hand, release the choke. With the other hand, grab a vital spot on the attacker’s head to pull the attacker away. Raise your hips and turn toward the direction of the attacker’s head. Attack and get up."

            "שחרור מחניקה מהצד בשכיבה" ->
                "[[RED_BOLD]]Ground technique note.[[/RED_BOLD]]\nKeep your heels close to your buttocks. Stun the attacker and get up quickly.\nWith the hand farther away from the attacker, release the choke with a hooked hand. With the other hand, hit the attacker’s diaphragm. Push your knee against the attacker’s hip. Push your hip away to create distance, kick continuously, and get up."

            // ─────────── Knife Threats Against a Wall ───────────

            "הגנה מאיום סכין לעורק שמאל" ->
                "[[RED_BOLD]]Wall / knife threat note.[[/RED_BOLD]]\nAll techniques should be trained with a weapon in either hand. The techniques are described against a right-handed attacker. At the end, the attacker should be between you and the wall.\nWith your left hand hooked, pull the attacker’s hand down lower than shoulder height while stepping to the side with your right foot. Attack with right punches and exit the wall to the side."

            "הגנה מאיום סכין לעורק ימין" ->
                "[[RED_BOLD]]Wall / knife threat note.[[/RED_BOLD]]\nAll techniques should be trained with a weapon in either hand. The techniques are described against a right-handed attacker. At the end, the attacker should be between you and the wall.\nWith your left hand, push the hand with the knife to the side and press it against the attacker’s body. Attack with your right hand and exit the wall."

            "הגנה מאיום סכין להב לגורגרת" ->
                "[[RED_BOLD]]Attacker’s hand bent.[[/RED_BOLD]]\nWith your left hand, deflect the attacker’s hand toward the attacker’s body while stepping toward the attacker’s Blind Side and punching with your right hand.\n[[RED_BOLD]]Attacker’s hand straight.[[/RED_BOLD]]\nLower the attacker’s forearm in one motion, keep it close to your body, kick to the attacker’s groin, and continue to neutralize."

            "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת",
            "הגנה מאיום סכין מלפנים – חוד הסכין לגורגרת" ->
                "With both hands, grab the attacker’s wrist as in Cavalier. Push downward and forward toward the attacker, kick to the attacker’s groin, and neutralize."

            "הגנה מאיום סכין מאחור - להב הסכין לגורגרת",
            "הגנה מאיום סכין מאחור – להב הסכין לגורגרת" ->
                "Place both forearms on the wall to stop the momentum. With a hooked right hand, pull the attacker’s hand downward while moving your hip to the right. With your left hand, hit the attacker’s groin and perform a high vertical elbow strike to the rear."

            "הגנה מאיום סכין מאחור - חוד לגב",
            "הגנה מאיום סכין מאחור – חוד לגב" ->
                "Place both forearms on the wall to stop the momentum. Glance backward. Extend your left hand backward away from the body while rotating and stepping outside the attacker’s legs to face the attacker’s Live Side. Pull the attacker’s knife hand and continue neutralizing."

            "הגנה מאיום סכין מאחור - להב על העורף",
            "הגנה מאיום סכין מאחור – להב על העורף" ->
                "Place both forearms on the wall to stop the momentum. Raise your right arm horizontally backward, like in the release from a choke from behind against the wall. Step with your left foot to the right, away from the line of attack, and neutralize."

            // ─────────── Defences Against a Knife ───────────

            "הגנה נגד דקירה ישרה מהצד - צד מת",
            "הגנה נגד דקירה ישרה מהצד – צד מת" ->
                "Perform an external defence and kick to the attacker’s knee."

            "הגנה נגד דקירה ישרה מהצד - צד חי",
            "הגנה נגד דקירה ישרה מהצד – צד חי" ->
                "Perform an internal defence and kick to the attacker’s knee."

            "הגנה נגד דקירה מזרחית - יד",
            "הגנה נגד דקירה מזרחית – יד" ->
                "Perform a diagonal 360 defence No. 4 while stepping forward and rotating your body to the attacker’s Blind Side. Grab the attacker’s wrist with your left hand while punching with your right hand. Perform a Cavalier while stepping backward."

            "הגנה נגד דקירה ישרה נמוכה" ->
                "Perform a diagonal 360 defence No. 4 while stepping forward and rotating your body to the attacker’s Blind Side. Grab the attacker’s wrist with your left hand while punching with your right hand. Perform a Cavalier while stepping backward."

            "הגנה פנימית נגד דקירה ישרה - צד חי",
            "הגנה פנימית נגד דקירה ישרה – צד חי" ->
                "[[RED_BOLD]]Reversed General Stance No. 1.[[/RED_BOLD]]\nBend the knees and place your hands at the height of the knife. Perform an internal defence with your right forearm. Step forward with your right foot while striking the attacker’s neck with your right forearm and grabbing the attacking hand."

            "הגנה פנימית נגד דקירה ישרה - צד מת",
            "הגנה פנימית נגד דקירה ישרה – צד מת" ->
                "[[RED_BOLD]]General Stance No. 1.[[/RED_BOLD]]\nBend the knees and place your hands at the height of the knife. Perform an internal defence with your left forearm while stepping forward with your left foot into the attacker’s Blind Side. Grab the attacker’s stabbing hand."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun brown(item: String): String {
        return when (item) {

            // ─────────── Jumping Kicks ───────────

            "בעיטה רגילה ובעיטת מגל בניתור" ->
                "Perform a regular kick. As the kicking leg retracts, jump with the base leg and perform a Magal circular kick with the base leg."

            "בעיטת מגל בניתור" ->
                "Jump with one leg and perform a Magal circular kick with the other leg. After the jump, bring the jumping base leg close to the buttocks."

            "בעיטת מגל כפולה בניתור" ->
                "Perform a horizontal Magal circular kick. After the kick, the kicking leg lands forward. Rotate the body using the momentum of the first Magal kick, raise the other knee, and perform a second Magal circular kick with the same leg."

            // ─────────── Breakfalls and Rolls ───────────

            "גלגול עם רובה" ->
                "Drive the butt of the rifle into the ground at a sharp angle and perform a regular forward roll. There are three options to finish: lying down, crouching, or standing up."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה פנימית נגד בעיטה לסנטר" ->
                "[[RED_BOLD]]General Stance No. 1.[[/RED_BOLD]]\nPerform an internal defence with your left forearm. Step with your left foot into the attacker’s Blind Side and attack according to the attacker’s distance."

            "הגנה חיצונית נגד בעיטה רגילה – פריצה",
            "הגנה חיצונית נגד בעיטה רגילה - פריצה",
            "הגנה חיצונית נגד בעיטה רגילה בפריצה" ->
                "[[RED_BOLD]]Low General Stance No. 1.[[/RED_BOLD]]\nPerform an external stabbing defence with your right forearm. Burst forward with your left foot while continuing the motion of the attacker’s kicking leg upward. Push the attacker backward and downward."

            "הגנה חיצונית נגד בעיטה רגילה – גזיזה",
            "הגנה חיצונית נגד בעיטה רגילה - גזיזה",
            "הגנה חיצונית נגד בעיטה רגילה בגזיזה" ->
                "[[RED_BOLD]]Low General Stance No. 1.[[/RED_BOLD]]\nPerform an external stabbing defence with your right forearm. Burst forward with your left foot while continuing the motion of the attacker’s kicking leg upward. Perform a backward cutting kick with your right leg."

            "הגנה חיצונית נגד בעיטה רגילה – טאטוא",
            "הגנה חיצונית נגד בעיטה רגילה - טאטוא",
            "הגנה חיצונית נגד בעיטה רגילה בטאטוא" ->
                "[[RED_BOLD]]Low General Stance No. 1.[[/RED_BOLD]]\nPerform an external stabbing defence with your right forearm. Burst forward with your left foot while continuing the motion of the attacker’s kicking leg upward. Position your body perpendicular to the attacker’s body and sweep the attacker."

            "הגנה פנימית נגד בעיטה רגילה – טאטוא",
            "הגנה פנימית נגד בעיטה רגילה - טאטוא",
            "הגנה פנימית נגד בעיטה רגילה עם בעיטת גזיזה קדמית" ->
                "[[RED_BOLD]]General Stance No. 2.[[/RED_BOLD]]\nPerform an internal defence with your left hand. Step forward with your left leg. Grab the attacker’s arm and punch with your right hand. Push the attacker off balance and perform a cutting kick with your left leg."

            // ─────────── Defences Against Front Magal Kicks ───────────

            "הגנה נגד בעיטת מגל – פריצה",
            "הגנה נגד בעיטת מגל - פריצה",
            "הגנה נגד בעיטת מגל בפריצה" ->
                "[[RED_BOLD]]General Stance No. 2.[[/RED_BOLD]]\nPerform an internal defence with your left hand. Step forward with your left leg. Grab the attacker’s arm and punch with your right hand. Push the attacker off balance and perform a cutting kick with your left leg."

            "הגנה חיצונית נגד מגל לפנים – גזיזה",
            "הגנה חיצונית נגד מגל לפנים - גזיזה",
            "הגנה חיצונית נגד בעיטת מגל לפנים בגזיזה" ->
                "[[RED_BOLD]]General Stance No. 2.[[/RED_BOLD]]\nPerform an external stabbing defence with your left forearm away from the body. Burst forward with your left leg while continuing the motion of the kicking leg upward. Perform a backward cutting kick with your right leg."

            "הגנה חיצונית נגד מגל לפנים – טאטוא",
            "הגנה חיצונית נגד מגל לפנים - טאטוא",
            "הגנה חיצונית נגד בעיטת מגל לפנים בטאטוא" ->
                "[[RED_BOLD]]General Stance No. 2.[[/RED_BOLD]]\nPerform an external stabbing defence with your left forearm away from the body. Burst forward with your left leg while continuing the motion of the kicking leg upward. Perform a sweep with your left leg to drop the attacker."

            // ─────────── Defences Against Reverse Magal Kicks ───────────

            "הגנה נגד בעיטת מגל לאחור – פריצה",
            "הגנה נגד בעיטת מגל לאחור - פריצה",
            "הגנה נגד בעיטת מגל לאחור בפריצה" ->
                "[[RED_BOLD]]General Stance No. 1.[[/RED_BOLD]]\nPerform a stabbing external defence away from your body with your right hand while guarding your face with your left. Burst forward and push the attacker to the ground with your left hand."

            // ─────────── Releases From Neck Hold ───────────

            "חביקת צואר מאחור – בריח על העורף, המגן כפוף לפנים",
            "חביקת צואר מאחור - בריח על העורף, המגן כפוף לפנים",
            "שחרור מחביקת צואר מאחור במנוף - המתגונן כפוף",
            "שחרור מחביקת צואר מאחור במנוף - המתגונן כפוף" ->
                "With a hooked right hand, grab the attacker’s forearm and turn your head toward the attacker’s elbow. With your left hand, push the attacker’s elbow upward. Drop to your knees and continue into a roll without placing your hands on the ground."

            // ─────────── Defences Against a Stick ───────────

            "הגנה נגד מקל בסיבוב – צד חי",
            "הגנה נגד מקל בסיבוב - צד חי",
            "הגנה נגד מקל בסיבוב לצד החי" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nBurst forward with your left foot with your hands straight forward and close to the head. Wrap your left forearm around the attacker’s arm and perform a front horizontal elbow strike with your right elbow while stepping forward."

            "הגנה נגד מקל עם קוואלר – צד מת",
            "הגנה נגד מקל עם קוואלר - צד מת",
            "הגנה נגד מקל בקוואלר לצד המת" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nBurst forward with your left foot with your hands straight forward and close to the head. Bring down the attacker’s hand by sliding both hands to the attacker’s wrist, then rotate 180 degrees clockwise with your right foot and perform a Cavalier to the Blind Side."

            "הגנה נגד מקל נקודת תורפה – לצד המת",
            "הגנה נגד מקל נקודת תורפה - לצד המת",
            "הגנה נגד מקל לצד המת עם נקודת תורפה בראש התוקף" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nBurst forward with your left foot with your hands straight forward and close to the head. Turn with your right foot to the attacker’s Blind Side. Grab a vital spot on the attacker’s head with your left hand and drop the attacker."

            // ─────────── Defences Against Knife Slashes ───────────

            "הגנה נגד סכין בשיסוף – הטיה והגנה לצד החי",
            "הגנה נגד סכין בשיסוף - הטיה והגנה לצד החי",
            "הגנה נגד שיסוף בהשענות לאחור והגנה לצד החי" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nStep backward with your right foot and raise your hands to protect the face. Then perform an external defence and grab the attacker’s hand with your left hand. Step forward with your right foot and punch with your right hand."

            "הגנה נגד סכין בשיסוף – הטיה והגנה לצד המת",
            "הגנה נגד סכין בשיסוף - הטיה והגנה לצד המת",
            "הגנה נגד שיסוף בהשענות לאחור והגנה לצד המת" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nStep backward with your right foot and raise your hands to protect the face. Perform an external defence with your right forearm while stepping forward with your left foot. Turn 180 degrees clockwise with your right foot while controlling the attacker’s hand."

            "הגנה נגד סכין בשיסוף – פריצה והגנה לצד החי",
            "הגנה נגד סכין בשיסוף - פריצה והגנה לצד החי",
            "הגנה נגד שיסוף בפריצה לצד החי" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nBurst forward with your left foot while performing an external defence with your left forearm. Continue rotating with the motion of the attack to grab the attacker’s wrist, then perform a side palm-heel strike with your right hand."

            "הגנה נגד סכין בשיסוף – פריצה והגנה לצד המת",
            "הגנה נגד סכין בשיסוף - פריצה והגנה לצד המת",
            "הגנה נגד שיסוף בפריצה לצד המת" ->
                "[[RED_BOLD]]Neutral stance.[[/RED_BOLD]]\nBurst forward with your left foot while performing an external defence with your right forearm. Rotate your body 180 degrees clockwise with your right foot. Grab a vital spot on the attacker’s head with your left hand and drop the attacker."

            // ─────────── Defences Against Gun Threats ───────────

            "הגנה מאיום אקדח מלפנים" ->
                "[[RED_BOLD]]Gun threat note.[[/RED_BOLD]]\nAll techniques should be trained with a weapon in either hand. The techniques are described against a right-handed attacker. When possible, turn the barrel of the gun downward.\nRaise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker’s left thigh. Grab the barrel and trigger guard. Step with your left foot forward to the attacker’s Blind Side and neutralize."

            "הגנה מאיום אקדח מאחור" ->
                "[[RED_BOLD]]Gun threat note.[[/RED_BOLD]]\nAll techniques should be trained with a weapon in either hand. The techniques are described against a right-handed attacker. When possible, turn the barrel of the gun downward.\nGlance backward. Reach your left hand away from your body while rotating counterclockwise and stepping with your left foot into the attacker’s Live Side. Grab the attacker’s hand between your left arm and forearm under shoulder height and neutralize."

            "הגנה מאיום אקדח מהצד החיצוני",
            "הגנה מאיום אקדח מהצד מאחורי היד" ->
                "[[RED_BOLD]]Gun threat note.[[/RED_BOLD]]\nReach your left hand away from your body and step with your left foot into the attacker’s Live Side. Grab the attacker’s hand between your left arm and forearm under shoulder height. Simultaneously perform a right punch to the attacker’s face and neutralize."

            "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד ימין",
            "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד ימין",
            "הגנה מאיום אקדח מהצד לפני היד - התוקף בצד ימין" ->
                "[[RED_BOLD]]Gun threat note.[[/RED_BOLD]]\nSimultaneously use your left hand to grab the attacker’s wrist to deflect the gun, and use your right hand to grab the barrel and trigger guard. Step with your left foot to the attacker’s Blind Side. Disarm the attacker while creating leverage on the weapon."

            "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד שמאל",
            "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד שמאל",
            "הגנה מאיום אקדח מהצד לפני היד - התוקף בצד שמאל" ->
                "[[RED_BOLD]]Gun threat note.[[/RED_BOLD]]\nSimultaneously use your right hand to grab the attacker’s wrist to deflect the gun, and use your left hand to grab the barrel and trigger guard. Step with your right foot to the attacker’s Live Side. Disarm the attacker while creating leverage on the weapon."

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
            "מכת מקל ללסת / צואר" -> "חסר הסבר בחוברת"
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
            "מכה אופקית לצואר" -> "From a regular stance, step forward with your left foot and strike the neck or face using the magazine or the handle or the weapon's body."
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
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

            // ─────────── Stances ───────────

            "עמידת מוצא רגילה" ->
                "Elbows bent downward, palms toward the attacker."

            "עמידת מוצא להגנות פנימיות" ->
                "Hands extended forward, elbows bent and farther away from the body, palms toward the attacker and widened to draw in a direct attack."

            "עמידת מוצא להגנות חיצוניות" ->
                "Left hand passes the right hip while turning the left shoulder inward. Right hand passes the left shoulder."

            "עמידת מוצא כללית מס' 1" ->
                "Left palm is extended forward, elbow bent downward, palm facing the attacker. Right hand passes the left hip. Knees are bent to make the stance lower, making you a smaller target and increasing the area of defence."

            "עמידת מוצא כללית מס' 2" ->
                "Left hand is extended forward, elbow is bent, and the palm is facing inward. The right hand is positioned over the left shoulder. This stance is for bursting toward the attacker."

            "עמידת מוצא צידית" ->
                "Start from a simple stance with legs spread. Turn the body, hands and the foot closer to the attacker to the side where the attacker is positioned. Correct the position of the rear foot for comfortability."

            "צל בוקס" ->
                "Standing position and moving while shifting your weight forward and backward. Place emphasis on bending the knees. In higher levels, do this while shifting into different stances."

            "תזוזות" ->
                "Stepping forward, backward and to the side. While moving forward, the left leg leads and then the right leg follows. While moving backward, the right leg leads and the left leg follows. While moving right, lead with the right leg and the left leg follows."

            // ─────────── Hand Strikes ───────────

            "מכת פיסת יד שמאל לפנים" ->
                "The fingers are straightened upward, the thumb is held close to the hand. It is a short strike with a slight shifting forward of the weight, and it finishes with the elbow slightly bent."

            "מכת פיסת יד ימין לפנים" ->
                "The fingers are straightened upward, the thumb is held close to the hand. It is a short strike while rotating your hip and the right heel, and it finishes with the elbow slightly bent."

            "מכת פיסת יד שמאל-ימין לפנים" ->
                "Perform a Forward Left Palm Heel Strike and as the hand returns throw a Forward Right Palm Heel Strike."

            "מכת פיסת יד שמאל-ימין-שמאל לפנים" ->
                "Perform a Forward Left Palm Heel Strike. As the hand returns throw a Forward Right Palm Heel Strike and as the right hand returns perform a Forward Left Palm Heel Strike while stepping forward."

            "מכת פיסת יד מהצד" ->
                "The fingers are toward your body, the thumb is held close to the hand. Elbow to the side and strike in a rotational move."

            "סגירת אגרוף" ->
                "With the fingers together, roll the fingers inward. Roll the fingers tightly inside the palm. Close the fist with the thumb beneath the fingers. Place emphasis on maintaining a tightly closed fist."

            "אגרוף שמאל לפנים" ->
                "Extend the left fist forward while shifting your weight forward. Rotate your fist to a horizontal position with the back of the hand facing upward."

            "אגרוף ימין לפנים" ->
                "Extend the right fist forward while rotating your hip and the right heel, rotate your fist to a horizontal position."

            "אגרוף שמאל-ימין לפנים" ->
                "While returning the left punch, initiate the punch with your right fist."

            "אגרוף שמאל בהתקדמות" ->
                "While punching with the left hand, step forward with the left leg and shift your weight onto it. While returning the left punch, follow with the right leg."

            "אגרוף ימין בהתקדמות" ->
                "Step forward with your left foot while punching with your right hand. Follow with the right leg."

            "אגרוף שמאל-ימין בהתקדמות" ->
                "While stepping forward with your left foot, punch with your left hand. While following with your right leg, punch with your right hand."

            "אגרוף שמאל-ימין ושמאל בהתקדמות" ->
                "While returning the left punch, initiate the punch with your right fist. While returning with the right punch, initiate the punch with your left fist while stepping forward."

            "אגרוף שמאל בנסיגה" ->
                "While punching with the left hand, step backward with the right leg. While returning the left punch, follow with the left leg."

            "אגרוף שמאל למטה בהתקפה" ->
                "Bend your knees while attacking the low part of your attacker's body with your left hand, and shift your weight forward."

            "אגרוף ימין למטה בהתקפה" ->
                "Bend your knees while rotating your hip and right heel, and attack with your right hand to the low part of your attacker's body."

            "אגרוף שמאל למטה בהגנה" ->
                "Bend your knees and move your upper body to the right, and attack with your left hand."

            "אגרוף ימין למטה בהגנה" ->
                "Bend your knees and move your upper body to the left while rotating your hip and right heel, and attack with your right hand."

            "מכת מגל שמאל" ->
                "Attack with a vertical fist, thumb upward, past the center line of your body while rotating your hip to the right. Used against a close attacker."

            "מכת מגל ימין" ->
                "Attack with a vertical fist, thumb upward, past the center line of your body while rotating your hip and right heel to the left. Used against a close attacker."

            "מכת מגל למטה ולמעלה בהתחלפות" ->
                "Perform alternating Magal punches, first low to the ribs and then high to the head."

            "מכת סנוקרת שמאל" ->
                "Lower your left shoulder and elbow while rotating your fist so the back of the hand faces the attacker. Strike diagonally upward while lifting the left heel. Used against a close and low target."

            "מכת סנוקרת ימין" ->
                "Lower your right shoulder and elbow while rotating your fist so the back of the hand faces the attacker. Strike diagonally upward while lifting the right heel. Used against a close and low target."

            "אצבעות לפנים" ->
                "With your legs slightly spread apart, step back turning your side toward the attacker. Extend your straightened hand and tense your fingers, striking at the attacker's neck. The other hand guards the face."

            "מכת קשת האגודל והאצבע לקנה הנשימה" ->
                "With your legs slightly spread apart, step back turning your side toward the attacker. Extend your straightened hand and strike at the attacker's neck with the index finger-thumb arch. The other hand guards the face."

            "מכת קשת האגודל והאצבע" ->
                "Extend your left hand and strike the target with the arch between the index finger and the thumb while spreading them stiffly."

            // ─────────── Elbow Strikes ───────────

            "מכת מרפק אופקית לפנים" ->
                "Raise your elbow horizontally, with the back of the hand facing upward. The forearm is horizontal. Rotate the elbow inward so that the strike passes the centerline of the body. Rotate your hips and heel."

            "מכת מרפק אנכי למטה" ->
                "Lift your forearm vertically in front of your body and turn the back of your hand forward. Strike downward while bending your knees, using your bodyweight to increase power."

            "מכת מרפק אנכי למעלה" ->
                "Attack the chin of the opponent while raising your hand vertically over your shoulder, while rotating your hip and heel."

            "מכת מרפק לאחור" ->
                "Look over your shoulder, and quickly strike backward with your elbow by your hip."

            "מכת מרפק לאחור למעלה" ->
                "Look over your shoulder, and strike vertically upward with your elbow while bending your upper body forward."

            "מכת מרפק אופקית לאחור" ->
                "Look over your shoulder, and raise your forearm horizontally to the side. The back of the hand should be facing upward. Rotate your upper body to the rear while rotating the opposite heel."

            "מכת מרפק אופקית לצד" ->
                "Raise your elbow horizontally to the side with the back of the hand facing upward. Strike while stepping to the side with both legs."

            // ─────────── Kicks ───────────

            "בעיטה רגילה למפסעה" ->
                "Raise your knee forward and snap your lower leg forward, impacting with the ball of your foot. There are two positions for the foot of the base leg."

            "בעיטה רגילה לסנטר" ->
                "Raise your knee as high as possible forward and snap your lower leg forward, impacting with the ball of your foot. There are two positions for the foot of the base leg."

            "בעיטת ברך נמוכה למפסעה" ->
                "Grab with both hands the attacker's opposite shoulder, and pull the attacker down while raising your knee to hit the groin."

            "בעיטת ברך גבוהה" ->
                "Grab the back of the attacker's head with both hands, pulling the attacker diagonally while raising your knee to strike either the attacker's head or chest."

            "בעיטת ברך מהצד" ->
                "Grab with both hands the attacker's opposite shoulder, and pull the attacker diagonally down while raising your knee to hit the ribs."

            "בעיטת מגל אופקית" ->
                "Raise your knee horizontally while rotating the base foot and turning your body. Straighten your striking leg to impact with the ball of your foot, and immediately return to the horizontal position."

            "בעיטת מגל אלכסונית" ->
                "Raise your leg diagonally while turning the base foot. This kick does not have a specific target."

            "בעיטת מגל בהטעיה" ->
                "Raise your knee forward and quickly change to a horizontal Magal (Circular) kick."

            "בעיטת מגל נמוכה" ->
                "Magal (Circular) kick with your shin to the opponent's lower thigh."

            "בעיטה לצד מעמידת פיסוק" ->
                "The base foot should be at a 45 degree angle. Raise your knee forward, and kick to the side with your heel while shifting your upper body to the opposite side while looking at the attacker."

            // ─────────── Break-Falls and Rolls ───────────

            "בלימה רכה לפנים" ->
                "From a neutral stance, fall forward on your palms and bend your elbows. Emphasize turning your face to the side, with a straight body and locked knees, heels backward. You should not throw your legs backward during the fall."

            "בלימה לאחור" ->
                "Step back with your chin tucked in to your chest. Break the fall with your palms facing the ground, with tight fingers and straightened hands. Continue the fall onto your back. After falling, your shoulders, elbows, and head should be elevated."

            "גלגול לפנים - ימין" ->
                "From a neutral stance, step forward with your right foot. Place your hands forward with the right hand facing left, and the left hand facing forward. Your head should be to the left, and roll forward from your right shoulder to your left hip."

            // ─────────── Defences ───────────

            "הגנה חיצונית רפלקסיבית 360 מעלות" ->
                "From a neutral stance, the defences are performed on the lateral side of the forearm and close to the wrist with the elbow bent. Fingers should be straightened and tensed. The pinky side of the forearm faces the attack."

            "הגנה פנימית רפלקסיבית" ->
                "From a neutral stance, the defence is performed with the palm and straightened fingers. Deflect the attacker's hand inward and toward the attacker while moving your head to the other side. This is an instinctive defence."

            "הגנה פנימית נגד ימין בכף יד שמאל" ->
                "From an internal defence stance, deflect the punch with your left palm. Slide your palm along the attacker's arm while stepping to the Blind Side. Punch with your right hand to the attacker's ribs. Grab his arm with your left hand, and continue attacking."

            "הגנה פנימית נגד שמאל בכף יד ימין" ->
                "From an internal defence stance, deflect the punch with your right palm while moving your head to the right, and punch forward with your left."

            "הגנה פנימית נגד בעיטה רגילה למפסעה" ->
                "From a neutral stance, deflect the attacker's knee away from your body with your left palm while guarding your face with your right hand. Step with your left foot toward the attacker's Blind Side and rotate your body while grabbing and blocking his right leg."

            // ─────────── Releases From Hand Grabs ───────────

            "שחרור מתפיסת יד מול יד" ->
                "Turn your thumb toward the gap between the attacker's thumb and fingers, and apply leverage with your elbow. The other hand guards the face."

            "שחרור מתפיסת יד נגדית" ->
                "Turn your thumb toward the gap between the attacker's thumb and fingers, and pull to the outside to release the grab. The other hand guards the face."

            "שחרור מתפיסת שתי ידיים למטה" ->
                "Turn your thumbs toward the gap between the attacker's thumb and fingers, and lift your hands upward and forward toward the attacker."

            "שחרור מתפיסת שתי ידיים למעלה" ->
                "Turn the lateral part of your forearm toward the gap between the attacker's thumb and fingers. Pull your hands downward while stepping backward and facing your attacker."

            // ─────────── Releases from a Neck Hold ───────────

            "שחרור מחביקת צואר מהצד" ->
                "With your rear leg, step in a low slide with the direction of the pull while rotating your body to face the attacker to prevent falling. In one motion, one hand attacks the attacker's groin, and the other grabs a weak spot in the attacker's face and pulls."

            // ─────────── Releases from Chokes ───────────

            "מניעת התקרבות תוקף" ->
                "Depending on distance: long range - front kick. Short range - index finger-thumb arch strike or applying pressure with the fingers on the attacker's neck. Can be performed against any approaching attacker."

            "מניעת חניקה" ->
                "With your legs slightly spread apart, step back turning your side toward the attacker. Extend your straightened hand and tense your fingers, striking at the attacker's neck. The other hand guards the face."

            "שחרור מחניקה מלפנים בכף היד" ->
                "With a hooked hand, release the choke and step backward, turning your side toward the attacker. With your hand closest to the attacker, strike the attacker in between his hands with a palm-heel strike. Grab the attacker's opposite shoulder, and pull down."

            "שחרור מחניקה מאחור במשיכה" ->
                "With two hooked hands, release the choke. Step backward diagonally in accordance with the direction of the pull. Release one hand to hit the groin, and then elbow the attacker's chin."

            // ─────────── Preparation for Ground-Work ───────────

            "מוצא לעבודת קרקע" ->
                "Lying down, knees bent with feet on the ground, spread hip width close to the buttocks."

            "הוצאת אגן" ->
                "Begin in Starting Position for Ground-work, eyes follow the movement. Both hands push to the right while the left leg pushes the hip left and backwards, the body is lying on the right side. Finish by getting up quickly."

            "הרמת אגן והפניית גוף לכיון ההפלה" ->
                "Begin in Starting Position for Ground-work, eyes follow the movement. Pop your hip up in a quick explosive motion, lead diagonally with a straight left hand over the head and the right shoulder while rotating the body over the right shoulder."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun orange(item: String): String {
        return when (item) {

            // ─────────── Hand Strikes ───────────

            "מכת גב יד בהצלפה" ->
                "Three options: 1. The back of the hand strikes the target. 2. The knuckles of the hand strike the target. 3. The back of the hand hits and passes the target. Targets: cheek-bone, jaw, or eye socket."

            "מכת גב יד בהצלפה בסיבוב" ->
                "With the left foot forward, spin to your right on the balls of the left foot and complete the spin by advancing the right foot forward and deliver a backfist with your right hand."

            "מכת פטיש" ->
                "Raise your fist horizontally and strike downward while rotating the fist so the thumb faces upward, and bend your knees to increase power. The strike is in front of your center of mass line."

            "מכת פטיש מהצד" ->
                "Clench your left fist, and horizontally strike to the side with the bottom part of your fist."

            "מכת פטיש יד שמאל" ->
                "Clench your left fist, and horizontally strike to the side with the bottom part of your fist."

            // ─────────── Kicks ───────────

            "בעיטה רגילה בעקב לסנטר" ->
                "Elevate your knee as high as possible, and kick the attacker's chin with your heel."

            "בעיטת הגנה לפנים" ->
                "Raise your knee forward, and kick forward either with the ball of your foot, the heel, or the entire bottom of the foot into the center of mass of the attacker."

            "בעיטת סנוקרת לאחור" ->
                "With your back facing the attacker, look over your shoulder. Raise your heel backward in a whipping motion. It is also possible to perform facing the attacker with a cross-step from Side Stance."

            "בעיטה לצד בשיכול" ->
                "Side Stance. The base foot will cross behind the kicking leg. Perform a side-kick, and return the kicking leg so it crosses in front of the base leg."

            "בעיטה לצד בנסיגה" ->
                "Side Stance. The base leg retreats to the side, and the other foot performs a side-kick. Emphasize looking at the attacker. This is a defensive kick."

            "בעיטה רגילה לאחור" ->
                "With your back facing the attacker, look over your shoulder. Swing your leg to the back between the attacker's legs. It is also possible to perform facing the attacker with a cross-step from Side Stance."

            "בעיטת הגנה לאחור" ->
                "With your back facing the attacker, look over your shoulder. Elevate your knee forward in a 90 degree angle and then kick backward while bending your back forward. It is also possible to perform facing the attacker with a cross-step from Side Stance."

            "בעיטת סטירה פנימית" ->
                "From a regular stance, raise your knee external of your body and hit the target with the bottom of your foot."

            "בעיטת עצירה בכף הרגל האחורית" ->
                "Upon detecting the attacker's kick, raise your rear leg to stop it. Your toes should face outward. Emphasize timing."

            "בעיטת עצירה בכף הרגל הקדמית" ->
                "Upon detecting the attacker's kick, raise your front leg to stop it. Your toes should face inward. Emphasize timing."

            "בעיטה רגילה ובעיטת מגל ברגל השנייה" ->
                "Perform a regular kick. As you plant your foot in front of you, perform a Magal (Circular) kick with the opposite foot."

            "שילובי בעיטות" ->
                "Emphasize logical combinations. The landing foot should be in position for the next kick."

            "שילובי ידיים רגליים",
            "שילובי ידיים ורגליים" ->
                "Emphasize logical hand strike and kick combinations."

            // ─────────── Jumping Kicks ───────────

            "ניתור ברגל ימין ובעיטה רגילה ברגל ימין" ->
                "Leap with your right foot, and elevate your left knee as high as possible. Perform a regular kick with your right foot. Land with your left foot into a fighting stance. This is a scissors kick."

            // ─────────── Break-falls and Rolls ───────────

            "בלימה לצד ימין",
            "בלימה לצד שמאל",
            "בלימה לצד - ימין/שמאל",
            "בלימה לצד – ימין/שמאל" ->
                "Back-cross your legs toward the side of the fall, and elevate the other leg to the opposite direction toward your chest. Break your fall with the hand closest to the ground with straight fingers and a locked elbow. Emphasize that the head and elbow should be lifted."

            "גלגול לפנים צד שמאל",
            "גלגול לפנים - שמאל",
            "גלגול לפנים – שמאל" ->
                "From a neutral stance, step forward with your left foot. Place your hands forward with the left hand facing right, and the right hand facing forward. Your head should be to the right and roll forward from your left shoulder to your right hip."

            "גלגול לאחור צד ימין",
            "גלגול לאחור צד שמאל",
            "גלגול לאחור - ימין/שמאל",
            "גלגול לאחור – ימין/שמאל" ->
                "Get down on your right knee. Place your left hand straight and close to the side of your body. Place your right hand by your left shoulder. Roll through your left shoulder with knees bent, and get up into a fighting stance."

            // ─────────── Body Defence ───────────

            "הגנות נגד מכות עם הטיות גוף" ->
                "Eight body defences: 1. Diagonally forward and to the right. 2. Diagonally forward and to the left. 3. To the right. 4. To the left. 5. Downward. 6. Backward. 7. Diagonally backward to the right. 8. Diagonally backward to the left."

            // ─────────── External Defence Against Hand Strikes ───────────

            "הגנה חיצונית מס' 1" ->
                "From an external defence stance, defend with the back of the left forearm external the body with the palm facing upward. At the same time, turn your hip and right heel, and punch with your right hand. This is a strong defence using force."

            "הגנה חיצונית מס' 2" ->
                "From an external defence stance, defend with the left forearm external the body with the palm facing forward. At the same time, turn your hip and right heel, and punch with your right hand. This is a fast defence."

            "הגנה חיצונית מס' 3" ->
                "From an external defence stance, defend with the left forearm over your head. At the same time, turn your hip and right heel, and punch with your right hand. This defence is for a shorter defender against a taller attacker."

            "הגנה חיצונית מס' 4" ->
                "From an external defence stance, defend with the left forearm over your head while bending your knees. At the same time, turn your hip and right heel, and punch with your right hand. This defence is for a taller defender against a shorter attacker."

            "הגנה חיצונית מס' 5" ->
                "From an external defence stance, defend with your left forearm while bursting forward. At the same time, lower your head and punch with your right hand."

            "הגנה חיצונית מס' 6" ->
                "From a stance between External Defence Stance and General Stance No. 2, when the left forearm is internal, defend by raising your left forearm and turn your hip and right heel, and punch with your right hand. This defence is against a bigger and stronger attacker."

            "הגנה חיצונית נגד אגרופים למטה" ->
                "From the Regular Stance, deflect the punches downward and outward with your forearms in a stabbing motion."

            // ─────────── External Defence Against Hand Strikes from the Side ───────────

            "הגנה נגד מכה גבוהה מהצד - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 1. Step with your right to the live Side of the attacker, and punch with your right hand."

            "הגנה נגד מכה מהצד לעורף - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 2. Step with your right to the live Side of the attacker, and punch with your right hand."

            "הגנה נגד מכה מהצד לגב - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 2 or no. 3. Step with your right to the live Side of the attacker, and punch with your right hand."

            "הגנה נגד מכה גבוהה מהצד - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 1. Step with your left leg to the Blind Side of the attacker, and punch with your left hand."

            "הגנה נגד מכה מהצד לגרון - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 2. Switch your hands while guarding the attacker's hand with your left forearm, and attack with your right to the attacker's neck."

            "הגנה נגד מכה מהצד לבטן - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 4 and continue movement toward the attacker and attack with a left punch."

            // ─────────── Internal Defence Against Hand Strikes ───────────

            "הגנה פנימית נגד שמאל עם המרפק" ->
                "Internal Defence Stance, raise the right elbow while defending with the back of the forearm and continue the motion to a right Whipping Backfist."

            "הגנה פנימית נגד מכות ישרות למטה" ->
                "From an internal defence stance, defend inward with your forearms. The elbow leads your motion by lowering the forearm in a stabbing motion."

            // ─────────── Defences Against Left and Right Punches ───────────

            "הגנה נגד שמאל-ימין - אגרוף מהופך",
            "הגנה נגד שמאל-ימין – אגרוף מהופך" ->
                "[[RED_BOLD]]Defend against the left and immediately attack. Do not wait for the right punch of the attacker.[[/RED_BOLD]]\nFrom General Stance No. 1, perform an Internal Defence with your left forearm against the attacker's left punch. Continue with an upside-down punch with your left hand."

            "הגנה נגד שמאל-ימין - הטייה לאחור",
            "הגנה נגד שמאל-ימין – הטייה לאחור" ->
                "[[RED_BOLD]]Defend against the left and immediately attack. Do not wait for the right punch of the attacker.[[/RED_BOLD]]\nFrom a Regular Fighting Stance, upon seeing the attacker's left punch, lean and step backward. As the attacker punches with his right hand, bring it down with a hooked left hand and attack with your right hand."

            "הגנה נגד שמאל-ימין (כמו חיצוניות)" ->
                "[[RED_BOLD]]Defend against the left and immediately attack. Do not wait for the right punch of the attacker.[[/RED_BOLD]]\nFrom a General Stance No. 1, perform an Internal Defence with your left forearm against the left punch, continue with an External Defence against the attacker's right punch and attack with your right."

            // ─────────── Defences Against Knee Strikes ───────────

            "הגנה נגד בעיטת ברך" ->
                "From a Regular Fighting Stance, with both arms close to the rib cage, vertically strike downward with both elbows. One will strike the attacker's thigh. Afterward, elbow strike vertically upward."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה חיצונית נגד בעיטה רגילה" ->
                "From General Stance No. 1, while stabbing with your right forearm away from your body, step with your left leg to the Blind Side of the attacker and attack with a left punch."

            "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית" ->
                "From a Regular Fighting Stance, block-kick the attacker's shin with your toes facing inward. Emphasize timing."

            "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית" ->
                "From a Regular Fighting Stance, block-kick the attacker's shin with your toes facing outward. Emphasize timing."

            // ─────────── Defences Against a Front Magal ───────────

            "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין" ->
                "From General Stance No. 1, perform an External Defence with your left forearm. Shift your weight to your left foot and perform a regular kick to the groin with your right leg. Emphasize defending away from the body. The kick itself is the main part of the defence."

            "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל" ->
                "From General Stance No. 1, perform an External Defence with your left forearm. Shift your weight to your right foot and perform a regular kick to the groin with your left leg. Emphasize defending away from the body. The kick itself is the main part of the defence."

            "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין" ->
                "From General Stance No. 1, perform an External Defence with your left forearm. Step forward with your right foot and punch with your right hand. Emphasize defending away from the body."

            "הגנה נגד בעיטת מגל לפנים באמות הידיים" ->
                "From a Regular Fighting Stance, perform an Internal Defence with your forearms while stepping forward with your right foot. After blocking, perform a backfist with your right hand. The position of the hands on the attacker's leg does not matter."

            "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית" ->
                "From a Regular Fighting Stance, perform an internal slap kick to the attacker's thigh. Emphasize timing."

            "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית" ->
                "From a Regular Fighting Stance, perform a block-kick with your front leg to the attacker's shin. Toes pointing outward. Emphasize timing."

            // ─────────── Defences Against Side Kicks ───────────

            "בעיטת עצירה נגד בעיטה לצד" ->
                "From a Regular Fighting Stance, perform a block-kick with your front leg toward the attacker's shin with your toes facing inward. Emphasize timing."

            // ─────────── Releases From Hand Grabs ───────────

            "שחרור מתפיסת יד מול יד - בריח על האגודל" ->
                "With your left hand, perform leverage on the attacker's thumb and release."

            "שחרור מתפיסת יד נגדית - פרקי אצבעות" ->
                "Strike the back of the attacker's hand with your knuckles."

            "שחרור מתפיסת יד בשתי ידיים למעלה" ->
                "Insert your free hand in between the attacker's hands, and grab the fist of your held hand. Lift the elbow of the free hand to create leverage, pull both hands downward while stepping back and looking at the attacker."

            "שחרור מתפיסת יד בשתי ידיים למטה - מרווח" ->
                "Insert your free hand in between the attacker's hands and grab the fist of your held hand while stepping backward."

            "שחרור מתפיסת יד בשתי ידיים למטה - צמוד" ->
                "Hook your free hand and slide it down from the attacker's elbow and pull the attacker's wrist. Once it is free, perform a one-hand grab release."

            "שחרור מתפיסת ידיים צמודה מאחור" ->
                "Options: stomp the attacker's foot, hit the groin and perform a horizontal elbow strike backward."

            // ─────────── Releases From Arm Grabs ───────────

            "שחרור מתפיסת זרוע מהצד במשיכה" ->
                "Step with the pull external the attacker's body while striking his groin with the hand being grabbed. Simultaneously, attack his face with your free hand, or step toward the attacker and strike his face with your free hand."

            "שחרור מתפיסת זרוע מהצד בדחיפה" ->
                "Perform a side-kick with retreat to the attacker's knee."

            // ─────────── Releases From Shirt Holds ───────────

            "שחרור חולצה - בריח על האגודל" ->
                "Grab the attacker's right hand, while stepping backward then perform leverage on the attacker's thumb, and release."

            "שחרור חולצה - מכת פרקי אצבעות" ->
                "In case the attacker's thumb is pointing downward, use the knuckles to strike the back of his hand, while stepping backward."

            "שחרור חולצה - שתי ידיים" ->
                "Take a step back with a knuckle strike to one of the hands and then apply leverage on the thumb of the other hand."

            // ─────────── Releases From Hair Pulls ───────────

            "שחרור מתפיסת שיער מלפנים" ->
                "Step forward while performing 360 Defence No. 4 against the attacker's knee and punch forward with your free hand."

            "שחרור מתפיסת שיער מלפנים בשתי ידיים" ->
                "Step forward while performing 360 Defence No. 4 against the attacker's knee and punch the attacker's groin with your free hand."

            // ─────────── Release From Neck and Body Holds ───────────

            "שחרור חביקת צואר מלפנים" ->
                "Release from a front neck hold. Lower your center of gravity, protect the airway, strike a vulnerable target, create space and exit the hold while preparing to continue defending."

            "שחרור מחביקה פתוחה מלפנים" ->
                "High hug options: pull the attacker's hair back, gouge out his eyes with your thumbs, push his nose with your thumbs, or perform a side palm heel strike to his jaw. Low hug options should be performed according to the attacker's position."

            "שחרור מחביקה פתוחה מאחור" ->
                "Bend your knees to lower your center of gravity while grabbing the attacker's hands. Options: 1. Glance and perform two horizontal elbow strikes to the rear. 2. Stomp on the back of the foot. 3. Kick the attacker's shin with your heel."

            "שחרור מחביקה סגורה מלפנים" ->
                "Options: bite the attacker. Strike the groin with your hands closed together to create distance between you and the attacker. Grab the attacker's shoulder blades, pull toward you while kneeing the groin. Stomp on the back of the foot."

            "שחרור מחביקה סגורה מאחור" ->
                "Bend your knees and head butt backward. Shift your hips to the left, strike the groin with your right hand, and stomp on the attacker's foot with your right heel."

            // ─────────── Releases From Holds While on the Ground ───────────

            "שחרור מחביקת צואר מהצד בשכיבה" ->
                "[[RED_BOLD]]Emphasize that your heels must be close to your buttocks. Get up rapidly and stun the attacker.[[/RED_BOLD]]\nWith your free hand, grab a vital spot on the attacker's head and pull to the side while thrusting your hips up forcefully and to the side with the pull. Hit the groin with your other hand and get up."

            "שחרור מחביקת צואר ויד מהצד בשכיבה" ->
                "[[RED_BOLD]]Emphasize that your heels must be close to your buttocks. Get up rapidly and stun the attacker.[[/RED_BOLD]]\nWith your free hand, grab a vital spot on the attacker's head and pull to the side while thrusting your hips up forcefully and to the side with the pull. Release your trapped hand, hit the groin with your hand and get up."

            // ─────────── Releases from Chokes ───────────

            "שחרור מחניקה מלפנים בדחיפה" ->
                "Hook your left hand and release the attacker's right hand while stepping backward with your left, turn your body so your side faces the attacker. Lift your right hand over the wrist of the attacker's hand, and press it close to your ear. Vertically lower the elbow to create leverage."

            "שחרור מחניקה מאחור בדחיפה" ->
                "Step forward with your right leg while lifting your left arm and pressing it close to your ear to create leverage. Turn to the left to face the attacker's Blind Side and step past the attacker's legs. Strike with your right hand."

            "שחרור מחניקה מהצד - מרחוק",
            "שחרור מחניקה מהצד – מרחוק" ->
                "Hook your palm that is farther away from the attacker and use it to release the choking hand. Use the other hand to perform a palm-heel strike to the attacker's face from between his hands. Grab the attacker's opposite shoulder and pull while switching your hands."

            "שחרור מחניקה מהצד - מקרוב",
            "שחרור מחניקה מהצד – מקרוב" ->
                "Hook your palm that is farther away from the attacker and use it to release the choking hand. Use the other hand to strike the attacker's groin and attack with a high vertical elbow strike."

            "שחרור מחניקה מהצד בשכיבה" ->
                "Hook your palm that is farther away from the attacker, use it to release the choking hand and hit the diaphragm with a palm heel strike with the other hand. Push your knee toward the attacker's hips and perform a hip escape. Kick with the leg farther away."

            // ─────────── Defences Against a Knife ───────────

            "הגנות יד רפלקסיביות נגד דקירות רגילות" ->
                "[[RED_BOLD]]Defences are performed in accordance with the knife hold and not against the knife itself. All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nStand in a neutral stance. With one motion, use a 360 degree defence, simultaneously punching with the other hand and continue with a combination of hand strikes and kicks."

            "הגנות יד רפלקסיביות נגד דקירות מזרחיות" ->
                "[[RED_BOLD]]Defences are performed in accordance with the knife hold and not against the knife itself. All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nStand in a neutral stance. With one motion, use a 360 degree defence, simultaneously punching with the other hand and continue with a combination of hand strikes and kicks."

            "הגנות יד רפלקסיביות נגד דקירה ישרה" ->
                "[[RED_BOLD]]Defences are performed in accordance with the knife hold and not against the knife itself. All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nReflexive Internal Defence and continue with a combination of hand strikes and kicks."

            // ─────────── Preparation for Ground-Work ───────────

            "הגנה נגד אגרופים בשכיבה" ->
                "From the Starting Position for Ground-work, perform Internal Defences against the punches. With both hands grab one of the attacker's arms and pull it tight towards your body. Thrust your hips up forcefully and to the side of the held hand and spin toward the attacker."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun green(item: String): String {
        return when (item) {

            // ─────────── Elbow Strikes ───────────

            "מכת מרפק נגד קבוצה" ->
                "Step to the back with your right leg while turning your body to the right, faking retreat. Turn back toward the attackers while rotating through the left shoulder and burst with your right leg while attacking with your right elbow and a left punch."

            // ─────────── Kicks ───────────

            "בעיטה רגילה ובעיטת מגל באותה רגל",
            "בעיטת רגילה ובעיטת מגל באותה רגל" ->
                "Perform a regular kick with the back leg, return to position of 90 degrees with your elevated knee and perform a horizontal Magal (Circular) kick."

            "בעיטת סטירה חיצונית" ->
                "From a regular stance, raise your knee inward and hit the target with the back of your foot. Options: 1. With a whipping motion. 2. Passes through the target. The kick is with the front leg when the attacker is in the live Side."

            "בעיטת מגל לאחור בשיכול אחורי" ->
                "Side Stance. Perform a back cross-step. Elevate your leg to the side while rotating your hip. While impacting with your heel, bend your knee."

            "בעיטה לצד בסיבוב" ->
                "Perform an inward rotation on your front foot. Look over your shoulder, continue rotating and perform a side-kick with the rear leg."

            // ─────────── Break-Falls and Rolls ───────────

            "בלימה לאחור מגובה" ->
                "Stand crouched, jump backward and break the fall with your hands in-line with your shoulders. Spread your legs upward with your toes flexed backward."

            "בלימה לצד כהכנה לגזיזות" ->
                "Both of the legs move sideways and upward with the fall. Breakfall to the side."

            "גלגול לפנים ובלימה לאחור - ימין/שמאל",
            "גלגול לפנים ובלימה לאחור – ימין/שמאל",
            "גלגול לפנים ובלימה לאחור – ימין",
            "גלגול לפנים ובלימה לאחור – שמאל" ->
                "Perform a front roll and in case the attacker is very close continue turning on your knee to a break-fall to the back, kick to the groin and get up into a fighting stance toward the attacker."

            "גלגול לפנים ולאחור - ימין/שמאל",
            "גלגול לפנים ולאחור – ימין/שמאל",
            "גלגול לפנים ולאחור – ימין",
            "גלגול לפנים ולאחור – שמאל" ->
                "Perform a front roll and in case you need to gain distance or you lose your balance, continue turning on your knee to a backward roll. Get up into a fighting stance toward the attacker."

            "גלגול ביד אחת - ימין/שמאל",
            "גלגול ביד אחת – ימין/שמאל",
            "גלגול ביד אחת – ימין",
            "גלגול ביד אחת – שמאל" ->
                "Perform a front roll with one hand. The hand should be straight without locking the elbow."

            "גלגול לפנים - קימה לפנים",
            "גלגול לפנים – קימה לפנים" ->
                "Start a Front Roll with One Hand on the right. While rolling, breakfall with the left hand and get up forward in line with the direction of the roll."

            // ─────────── External Defence Against Hand Strikes ───────────

            "הגנה חיצונית נגד ימין באגרוף מהופך",
            "הגנה חיצונית נגד אגרוף ימין באגרוף מהופך" ->
                "Stand in an External Defence Stance. The left hand attacks with an upside-down punch. The right hand guards the face. The attack is the defence."

            "הגנה חיצונית נגד שמאל" ->
                "Stand in an External Defence Stance. Perform External Defence No. 2 with your left hand and attack with a right punch to the ribs."

            "הגנה חיצונית נגד שמאל בהתקדמות" ->
                "Stand in an External Defence Stance. Perform External Defence No. 2 with your left hand. Step with your right leg to the Blind Side of the attacker, while turning your body attack with a right punch to the ribs. Grab and perform a left knee strike."

            // ─────────── Internal Defence Against Hand Strikes ───────────

            "הגנה פנימית נגד ימין באמת שמאל",
            "הגנה פנימית נגד ימין באמה שמאל" ->
                "Stand in General Stance No. 1. Deflect the punch with the left forearm while leading with the elbow. Block the attacker's arm with your left forearm while stepping to the attacker's Blind Side. Turn your body, and punch the attacker's ribs."

            "הגנה פנימית נגד שמאל באמת שמאל",
            "הגנה פנימית נגד שמאל באמה שמאל" ->
                "Stand in General Stance No. 1. Deflect the punch with the left forearm while leading with the elbow. Continue attacking with a left backfist."

            "הגנה פנימית נגד ימין - אגרוף שמאל בהחלקה",
            "הגנה פנימית נגד אגרוף ימין באגרוף שמאל גולש" ->
                "From an internal defence stance, perform a left slide punch over the attacker's right punch. Highlight timing. The attack is the defence."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה נגד בעיטה רגילה - בעיטה לצד" ->
                "Start in a Side Stance. Rotate the base foot farther away from the attacker and perform a side kick with the other leg."

            "הגנה נגד בעיטה רגילה - טיימינג לצד החי" ->
                "From a neutral stance, perform a switch-step to the live Side of the attacker rotating your body with your side toward the attacker. Simultaneously kick the attacker's groin."

            "הגנה חיצונית באמת שמאל נגד בעיטה רגילה" ->
                "From an External Defence Stance, perform an External Defence in a stabbing motion with your left forearm away from the body while stepping with your right foot toward the live Side of the attacker and attack with a right punch."

            // ─────────── Defences Against a Front Magal ───────────

            "הגנה נגד בעיטת מגל נמוכה" ->
                "Raise your knee diagonally toward the attacker's kick, while lowering your elbow on the same side to increase the area of defence. Attack once you complete the block."

            "הגנה נגד בעיטת מגל לפנים - בעיטה לצד" ->
                "Start in a Side Stance. Rotate the base foot farther away from the attacker and perform a side kick with the other leg."

            // ─────────── Defences Against a Backward Magal ───────────

            "הגנה נגד בעיטת מגל לאחור - בעיטה בימין" ->
                "From Reverse General Stance No. 1, defend with the back of your right forearm and kick with your right leg to the attacker's groin."

            "הגנה נגד בעיטת מגל לאחור - בעיטה בשמאל" ->
                "From Reverse General Stance No. 1, defend with the back of your right forearm and kick with your left leg to the attacker's groin."

            "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל" ->
                "From Reverse General Stance No. 1, defend with the back of your right forearm and step forward with your left foot and punch with your left hand."

            "הגנה נגד בעיטת מגל לאחור בסבוב - בעיטה",
            "הגנה נגד בעיטת מגל לאחור בסיבוב - בעיטה" ->
                "As the attacker starts spinning, perform a forward defensive kick to the attacker's tail-bone. While training, kick your partner to the buttocks."

            // ─────────── Defences Against Side Kicks ───────────

            "הגנה חיצונית באמת ימין נגד בעיטה לצד" ->
                "From General Stance No. 1, perform an External Defence in a stabbing motion with your right forearm. Step with your left foot to the attacker's Blind Side while turning your body and attack with a left punch."

            "הגנה חיצונית באמת שמאל נגד בעיטה לצד" ->
                "From an External Defence Stance, perform an External Defence in a stabbing motion with your left forearm while stepping with your right foot to the attacker's live Side. Turn your body and attack with a right punch."

            "הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית" ->
                "From General Stance No. 2, step diagonally forward with your right foot to the live Side of the attacker as a body defence and defend against the kick with your left hand. Perform an external slap kick or a side kick with your left foot."

            // ─────────── Releases From Hand Grabs ───────────

            "חביקת יד מהצד - ראש התוקף מאחור" ->
                "Turn toward the attacker while punching the back of the head with the free hand."

            "חביקת יד מהצד - ראש התוקף מלפנים" ->
                "Turn toward the attacker while punching with the free hand."

            "שחרור מתפיסת ידיים מאחור" ->
                "Bend your body forward while performing a backward defensive kick or stomp the attacker's foot."

            // ─────────── Release From Shirt Holds ───────────

            "שחרור מתפיסת חולצה מאחור" ->
                "Turn and step with the direction of the pull while raising your hand for defence over the attacker's grabbing hand and punch with your other hand."

            // ─────────── Release From Hair Pulls ───────────

            "שחרור מתפיסת שיער מהצד - צד ימין" ->
                "[[RED_BOLD]]As the pull begins raise the hands for protection.[[/RED_BOLD]]\nStep with your right foot toward the live Side of the attacker. Strike the attacker with your right forearm and with your left hand hit the attacker's groin."

            "שחרור מתפיסת שיער מהצד - צד שמאל" ->
                "[[RED_BOLD]]As the pull begins raise the hands for protection.[[/RED_BOLD]]\nStep with your left foot to the Blind Side of the attacker. Strike forward with your left hand and with your right hit the attacker's groin."

            "שחרור מתפיסת שיער מאחור - צד מת" ->
                "[[RED_BOLD]]As the pull begins raise the hands for protection.[[/RED_BOLD]]\nSpin and step with your left foot external the attacker's legs to the attacker's Blind Side. Grab the attacker's hand with your left hand and hit the attacker's groin with your right hand. Punch the attacker's face with your right hand."

            "שחרור מתפיסת שיער מאחור - צד חי" ->
                "[[RED_BOLD]]As the pull begins raise the hands for protection.[[/RED_BOLD]]\nSpin and step with your right foot to the attacker's live Side. Raise your left forearm forward and strike the attacker's groin with your right hand."

            // ─────────── Releases from a Neck Hold ───────────

            "שחרור מחביקת צואר מאחור" ->
                "Pull downward with both hooked hands. The elbows should be directed downward toward the stomach. Turn your head toward the opening in the attacker's hands. Step with your rear leg while rotating your body facing the attacker. Twist your shoulders to release."

            // ─────────── Release From Body Hugs ───────────

            "שחרור מחביקה פתוחה מהצד" ->
                "Knee to the groin and then Horizontal Elbow Strike to the Side or punch."

            "שחרור מחביקה סגורה מהצד" ->
                "Options: Stomp the attacker's foot, strike the groin with the hand closer to the attacker and elbow strike according to the attacker's position."

            "שחרור מחביקה פתוחה מלפנים בהרמה" ->
                "Wrap your leg around the attacker's leg and strike or use a vital spot on the attacker's face."

            "שחרור מחביקה סגורה מלפנים בהרמה" ->
                "Wrap your leg around the attacker's leg. Pull one hand out and strike and/or knee the attacker's groin."

            "שחרור מחביקה פתוחה מאחור בהרמה" ->
                "Lean backward, straighten your back, grab the attacker's hands and perform horizontal elbow strikes backward and/or perform two uppercut kicks backward or wrap your leg around the attacker's leg and perform a heel strike to the attacker's groin."

            "שחרור מחביקה סגורה מאחור בהרמה" ->
                "Lean backward, straighten your back, wrap your leg around the attacker's leg. Pull one hand out and strike and/or perform a heel strike to the attacker's groin."

            "שחרור מחביקה פתוחה מאחור עם תפיסת אצבע" ->
                "Bend your knees to lower your center-of-gravity while grabbing the attacker's hands. Options: Glance and perform two horizontal elbow strikes to the rear. Perform knuckle strikes to the back of the attacker's hands. Grab the attacker's finger/s and break."

            // ─────────── Cavaliers ───────────

            "קוואלר - הליכה לאחור" ->
                "Perform on the attacker's right hand. With your left hand, grab and pull the attacker's wrist. Grab the attacker's fist when the center of your palm is on his knuckles. Rotate the attacker's hand external of the shoulder line and downward while stepping backward."

            "קוואלר נגד ההתנגדות - הליכה לפנים" ->
                "Perform on the attacker's right hand. With your left hand, grab and pull the attacker's wrist. Grab the attacker's fist when the center of your palm is on his knuckles. Rotate the attacker's hand external the shoulder line. Upon feeling resistance, step forward."

            "קוואלר - אגודלים" ->
                "Perform on the attacker's right hand. With both hands, grab the attacker's wrist. With your thumbs pressing on the back of the attacker's hand, pull down. Rotate the attacker's hand external of the shoulder line and downward."

            "קוואלר - מרפק" ->
                "Perform on the attacker's right hand. With your left hand, grab and pull the attacker's wrist. Perform a front horizontal elbow strike to back of the hand of the attacker while stepping backward with your left foot and turning your body to drop the attacker."

            // ─────────── Defences Against a Stick ───────────

            "הגנה נגד מקל - צד חי" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nFrom a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Grab the attacker's right shoulder. Pull down and knee the attacker's groin."

            "הגנה נגד מקל - צד מת" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nFrom a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Pull down the attacker's hand with two hooked hands while taking your right foot backward and attack with a right punch."

            // ─────────── Defences Against a Knife Threat ───────────

            "הגנה מאיום סכין לעורק שמאל" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nWith your left hand hooked, pull down lower than shoulder height the attacker's hand while stepping backward with your left foot to turn your side to the attacker and attack with right punches. Similar to the release from a choke from the front."

            "הגנה מאיום סכין לעורק ימין" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nGrab over the attacker's hands and press the hand with the knife against your chest while stepping forward with your left foot and punching with the left."

            "הגנה מאיום סכין להב לגורגרת" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand.[[/RED_BOLD]]\nAttacker's hand is bent: With your left hand, deflect the attacker's hand toward the attacker's body while stepping toward the Blind Side of the attacker and punch with your right hand. Attacker's hand is straight: With one motion lower the attacker's forearm, keep it close to your body, kick to the attacker's groin and continue to neutralize."

            "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת" ->
                "With both your hands, grab the attacker's wrist as in Cavalier and push downward and forward toward the attacker. Kick to the attacker's groin and continue to a Cavalier."

            "הגנה מאיום סכין מאחור - להב הסכין לגורגרת" ->
                "With a hooked right hand, pull the attacker's hand downward while moving your hip to the right. With your left hand, hit the attacker's groin and perform a High Vertical Elbow Strike to the Rear to hit the chin of the attacker."

            "הגנה מאיום סכין - חוד לבטן התחתונה" ->
                "With your left hand grab the attacker's wrist, lower it to the left while punching with your right. If the attacker is close, perform a head butt and continue attacking."

            "הגנה מאיום סכין מאחור - חוד לגב" ->
                "Glance backward. Extend your left hand backward away from the body while rotating and stepping external of the attacker's legs to face the attacker's live Side. Pull the attacker's hand holding the knife, holding it between your arm and forearm below shoulder height."

            // ─────────── Defences Against a Knife ───────────

            "הגנה נגד דקירה רגילה - בעיטה" ->
                "From a neutral stance, perform a small diagonal step to the attacker's Blind Side and a regular kick to the attacker's groin."

            "הגנה נגד דקירה מזרחית - בעיטה" ->
                "From a neutral stance, perform a switch step with your side facing toward the attacker's live Side. Perform a regular kick to the attacker's chin."

            "הגנה נגד דקירה ישרה נמוכה - בעיטה" ->
                "From a neutral stance, perform a switch step with your side facing toward the attacker's live Side. Perform a regular kick to the attacker's chin."

            "הגנה נגד דקירה ישרה מלפנים - בעיטה",
            "הגנה נגד דקירה ישרה - בעיטה" ->
                "From the neutral stance, lean backward while raising your hands to guard the face. Kick to the attacker's rib/armpit area."

            "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפסעה" ->
                "From a neutral stance, step diagonally to the attacker's Blind Side. Lean to the side while guarding your face and perform a Magal (Circular) kick to the attacker's groin with your right leg. With your right hand, grab the attacker's stabbing arm."

            "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפשעה" ->
                "From a neutral stance, step diagonally to the attacker's Blind Side. Lean to the side while guarding your face and perform a Magal (Circular) kick to the attacker's groin with your right leg. With your right hand, grab the attacker's stabbing arm."

            "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפסעה",
            "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפשעה" ->
                "From a neutral stance, step diagonally to the attacker's Blind Side. Lean to the side while guarding your face and perform a Magal (Circular) kick to the attacker's groin with your right leg. With your right hand, grab the attacker's stabbing arm."

            "הגנה נגד דקירה ישרה מהצד - בעיטה",
            "הגנה נגד דקירה רגילה מהצד - בעיטה" ->
                "Perform a Side Kick. It is possible to perform a Regular Kick as well."

            "הגנה נגד דקירה מזרחית מהצד - בעיטה" ->
                "Perform a Side Kick. It is possible to perform a Regular Kick as well."

            // ─────────── Defences Against a Knife From the Side ───────────

            "הגנה נגד דקירה רגילה מהצד - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 1. Step with your right foot to the live Side of the attacker and punch with your right hand."

            "הגנה נגד דקירה מזרחית מהצד לעורף - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 2. Step with your right foot to the live Side of the attacker and punch with your right hand."

            "הגנה נגד דקירה מזרחית מהצד לגב - התוקף בצד שמאל" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith the left hand, perform 360 degree defence no. 3. Step with your right foot to the live Side of the attacker and punch with your right hand."

            "הגנה נגד דקירה רגילה מהצד - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 1. Step with your left foot to the Blind Side of the attacker and punch with your left hand."

            "הגנה נגד דקירה מזרחית מהצד לגרון - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 2. Switch your hands while guarding the attacker's hand with your left forearm and attack with your right forearm to the attacker's neck."

            "הגנה נגד דקירה מזרחית מהצד לבטן - התוקף בצד ימין" ->
                "[[RED_BOLD]]The defender's side is facing the attacker.[[/RED_BOLD]]\nWith your right hand, perform a 360 degree defence no. 4 and continue movement toward the attacker and attack with a left punch."

            // ─────────── Strikes with a Stick / Rifle ───────────

            "התקפה עם מקל לנקודות תורפה" ->
                "[[RED_BOLD]]All exercises are to be done in movement.[[/RED_BOLD]]\nStrike with the stick or rifle to vulnerable areas."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun blue(item: String): String {
        return when (item) {

            // ─────────── Kicks ───────────

            "בעיטת פטיש" ->
                "Swing the kicking leg diagonally inward and then vertically bring it down. The impact is with the rear part of the heel. The kick is either with the front leg or the rear leg. This is for when the attacker is bent."

            "בעיטת גזיזה אחורית" ->
                "The defender stands in a regular fighting stance. The attacker stands in a stance with the right leg forward. Perform a switch step to the Blind Side and perform a kick with the rear part of the right shin close to the heel."

            "בעיטת גזיזה קדמית" ->
                "The defender stands in a regular fighting stance. The attacker stands in a stance with the right leg forward. Perform a switch step to the Blind Side. While turning your body 180 degrees behind the attacker, perform the kick with the back of your foot."

            "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב" ->
                "While performing a forward cutting kick, the attacker raises his target leg. Continue spinning with a backward cutting kick against the other leg of the attacker."

            "בעיטת מגל לאחור בסיבוב" ->
                "Perform an inward rotation on your front foot. Look over your shoulder, continue rotating and perform a back Magal (Circular) kick with the rear leg."

            "בעיטת סטירה חיצונית בסיבוב" ->
                "Perform an inward rotation on your front foot. Look over your shoulder, continue rotating and perform an external slap kick with the rear leg."

            // ─────────── Break-Falls and Rolls ───────────

            "מניעת נפילה מחביקת שוקיים מלפנים להפלה" ->
                "Evade by throwing both legs backward in a slight spread while leaning over the attacker. Hug the attacker's neck with one arm and punch with the other."

            "גלגול לצד - ימין/שמאל",
            "גלגול לצד – ימין/שמאל" ->
                "Roll to the side over one hand. The chin should be tucked to the chest. Continue the roll over both shoulder-blades."

            "גלגול ברחיפה - ימין/שמאל",
            "גלגול ברחיפה – ימין/שמאל" ->
                "Run. Dive forward. Break the fall with your hands and continue rolling forward."

            "גלגול לגובה - ימין/שמאל",
            "גלגול לגובה – ימין/שמאל" ->
                "Run. Jump upward, break the fall with your hands and continue rolling forward."

            "גלגול ללא ידיים - ימין/שמאל",
            "גלגול ללא ידיים – ימין/שמאל" ->
                "From a neutral stance, step forward with your right foot. Your head should be to the left, roll forward from your right shoulder to your left hip. Upon landing, your left knee should be bent to the side and the left ankle is under the right knee. Stand up into a fighting stance."

            // ─────────── Defences Against Knee Strikes ───────────

            "הגנה נגד בעיטת ברך מלפנים" ->
                "From a Regular Stance as you see the knee rising lower your elbows, lower one hand, push the attacker and perform a Vertical Upward Elbow Strike to the Front."

            "הגנה נגד בעיטת ברך מהצד" ->
                "Lower your hands, wrap one hand over the attacker's thigh and throw him to the ground while striking forward."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה נגד בעיטה רגילה - סייד-סטפ לצד המת",
            "הגנה נגד בעיטה רגילה – סייד-סטפ לצד המת" ->
                "Stand either in a neutral stance or a fighting stance. Upon seeing the kick, side step to the attacker's Blind Side while punching with your right hand. It is possible to perform a front cutting kick with your left leg. Highlight timing."

            "הגנה נגד בעיטה רגילה - סייד-סטפ לצד החי",
            "הגנה נגד בעיטה רגילה – סייד-סטפ לצד החי" ->
                "Stand either in a neutral stance or a fighting stance. Upon seeing the kick, side step to the attacker's live Side while punching with your right hand and kicking a front kick with your left foot. Highlight timing."

            // ─────────── Defences Against a Front Magal ───────────

            "הגנה נגד בעיטת מגל לפנים עם השוק" ->
                "With your left hand wrap around the attacker's shin, keeping it close to your body, step to the side with your right and punch with your right."

            "הגנה נגד בעיטת מגל לצלעות" ->
                "Regular Stance: With your left hand wrap around the attacker's leg from above, step to the live-side with your right and punch with your right. General Stance No. 2: Raise the attacker's leg with your left hand and perform a right cutting kick."

            "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לצד",
            "הגנה פנימית נגד בעיטת מגל לפנים – בעיטה לצד" ->
                "From a reversed General Stance No. 1, perform an Internal Defence with your right forearm. Cross-step and perform a side-kick to the attacker's knee."

            "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לאחור",
            "הגנה פנימית נגד בעיטת מגל לפנים – בעיטה לאחור" ->
                "From a reversed General Stance No. 1, perform an Internal Defence with your right forearm. Cross-step and perform a backward regular kick to the attacker's groin."

            // ─────────── Defences Against Side Kicks ───────────

            "הגנה פנימית באמת ימין נגד בעיטה לצד",
            "הגנה פנימית באמה ימין נגד בעיטה לצד" ->
                "From a reversed General Stance No. 1, in one motion perform an Internal Defence with your right forearm and an External Defence with your left forearm. Simultaneously, step forward with your right leg, turning your body to the live Side of the attacker."

            // ─────────── Releases From Hand Grabs on the Ground ───────────

            "שחרור תפיסת ידיים בשכיבה" ->
                "[[RED_BOLD]]In all techniques on the ground, highlight that your heels should be close to the buttocks. The defender should stun the attacker and get off the ground as fast as possible.[[/RED_BOLD]]\nPull both your hands downward raising your hips and rotating to the side. Raise your hands to guard the face and throw the attacker to the side."

            // ─────────── Releases from a Neck Hold ───────────

            "שחרור מחביקת צואר מהצד והפלה",
            "שחרור מחביקת צוואר מהצד והפלה" ->
                "Start with the Release from Neck Hold from the Side. With the fall use your free hand, grab a vital spot on the attacker's head and pull to the side while thrusting your hips forcefully up and to the side of the pull. Hit the groin with your other hand."

            "שחרור מחביקת צואר מאחור עם נעילה",
            "שחרור מחביקת צוואר מאחור עם נעילה" ->
                "With a hooked right hand grab the attacker's forearm and turn your head toward the attacker's elbow. With your left push up the attacker's elbow. With your right foot step to the left into the attacker's body and exit the hold. Perform before the lock is completed."

            // ─────────── Releases From Holds While on the Ground ───────────

            "שחרור מחביקת צואר בשכיבה ברכיבה צמודה",
            "שחרור מחביקת צוואר בשכיבה ברכיבה צמודה" ->
                "With the hand opposite to the attacker's head, grab a vital spot on the attacker's head. With the other hand, punch the attacker's ribs while lifting and turning your hips to reverse the attacker's position."

            // ─────────── Releases from Chokes ───────────

            "שחרור מחניקה לקיר - מלפנים לא צמודה",
            "שחרור מחניקה לקיר – מלפנים לא צמודה" ->
                "With a hooked hand, release the choke. With the other hand, perform a palm-heel strike from the side to the attacker's face. Exit to the side."

            "שחרור מחניקה לקיר - צמודה מלפנים",
            "שחרור מחניקה לקיר – צמודה מלפנים" ->
                "With a hooked hand, release the choke. With the other hand, grab a vital spot on the attacker's head in order to pull the attacker away while kneeing the groin and exiting to the side."

            "שחרור מחניקה לקיר - דחיפה מאחור",
            "שחרור מחניקה לקיר – דחיפה מאחור" ->
                "With your head facing to the side, break your movement against the wall using both hands. Raise your right arm and press it against your ear to create leverage. Spin to the side to face the attacker's Blind Side beyond the attacker's legs and attack."

            "שחרור מחניקה לקיר - צמודה מאחור",
            "שחרור מחניקה לקיר – צמודה מאחור" ->
                "With a hooked hand, release the choke. Rotate your body and attack while exiting to the side."

            // ─────────── Releases from Chokes While on the Ground ───────────

            "שחרור מחניקה בשכיבה - ידיים כפופות",
            "שחרור מחניקה בשכיבה – ידיים כפופות" ->
                "[[RED_BOLD]]Highlight that your heels should be close to your buttocks on the ground. Stun the attacker, and get up quickly.[[/RED_BOLD]]\nRelease with one hooked hand. With the other, perform a front palm-heel strike. Raise your hips and turn toward the direction of the releasing hand. Attack and get up. Similar to the release from a choke from the front."

            "שחרור מחניקה בשכיבה - ידיים ישרות",
            "שחרור מחניקה בשכיבה – ידיים ישרות" ->
                "[[RED_BOLD]]Highlight that your heels should be close to your buttocks on the ground. Stun the attacker, and get up quickly.[[/RED_BOLD]]\nRelease the choke with one hooked hand. Press the other arm against your ear to create leverage on the attacker's wrist and continue inward with your elbow to create leverage. Raise your hips and turn toward the direction of the releasing hand. Attack and get up."

            "שחרור מחניקה צמודה בשכיבה" ->
                "[[RED_BOLD]]Highlight that your heels should be close to your buttocks on the ground. Stun the attacker, and get up quickly.[[/RED_BOLD]]\nWith a hooked hand, release the choke. With the other hand, grab a vital spot on the attacker's head in order to pull the attacker away. Raise your hips and turn toward the direction of the attacker's head. Attack and get up."

            "שחרור מחניקה מהצד בשכיבה" ->
                "[[RED_BOLD]]Highlight that your heels should be close to your buttocks on the ground. Stun the attacker, and get up quickly.[[/RED_BOLD]]\nWith the hand farther away from the attacker, release the choke with a hooked hand. With the other hand hit the attacker's diaphragm. Push your knee against the attacker's hip. Push your hip away to create distance, kick continuously and get up."

            // ─────────── Defences Against a Knife Threat Against the Wall ───────────

            "הגנה מאיום סכין לעורק שמאל" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nWith your left hand hooked, pull down lower than shoulder height the attacker's hand while stepping to the side with your right foot, attack with right punches and exit the wall to the side. Similar to the release from a choke from the front against the wall."

            "הגנה מאיום סכין לעורק ימין" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nWith your left hand push the hand with the knife to the side and push against the attacker's body. Attack with your right and exit the wall."

            "הגנה מאיום סכין להב לגורגרת" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nAttacker's hand is bent: With your left hand, deflect the attacker's hand toward the attacker's body while stepping toward the Blind Side of the attacker and punch with your right hand. Attacker's hand is straight: With one motion lower the attacker's forearm, keep it close to your body, kick to the attacker's groin and continue to neutralize."

            "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nWith both your hands, grab the attacker's wrist as in Cavalier and push downward and forward toward the attacker. Kick to the attacker's groin and neutralize."

            "הגנה מאיום סכין מאחור - להב הסכין לגורגרת" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nPlace both forearms on the wall to stop the momentum. With a hooked right hand, pull the attacker's hand downward while moving your hip to the right. With your left hand, hit the attacker's groin and perform a High Vertical Elbow Strike to the Rear to hit the chin of the attacker."

            "הגנה מאיום סכין מאחור - חוד לגב" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nPlace both forearms on the wall to stop the momentum. Glance backward. Extend your left hand backward away from the body while rotating and stepping external of the attacker's legs to face the attacker's live Side. Pull the attacker's hand holding the knife and hold it between your arm and forearm below shoulder height."

            "הגנה מאיום סכין מאחור - להב על העורף" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. At the end of the techniques the attacker should be between you and the wall.[[/RED_BOLD]]\nPlace both forearms on the wall to stop the momentum. Raise your right arm horizontally backward, while stepping with your left to the right away from the line of the attack and neutralize."

            // ─────────── Defences Against a Knife ───────────

            "הגנה נגד דקירה ישרה מהצד - צד מת",
            "הגנה נגד דקירה ישרה מהצד – צד מת" ->
                "Perform an External Defence and kick to the attacker's knee."

            "הגנה נגד דקירה ישרה מהצד - צד חי",
            "הגנה נגד דקירה ישרה מהצד – צד חי" ->
                "Perform an Internal Defence and kick to the attacker's knee."

            "הגנה נגד דקירה מזרחית - יד" ->
                "Perform a diagonal 360 Defence No. 4 while stepping forward rotating your body to the side to the Blind Side of the attacker. Grab the attacker's wrist with your left hand while punching with the right hand. Perform a cavalier while stepping backward."

            "הגנה נגד דקירה ישרה נמוכה" ->
                "Perform a diagonal 360 Defence No. 4 while stepping forward rotating your body to the side to the Blind Side of the attacker. Grab the attacker's wrist with your left hand while punching with the right hand. Perform a cavalier while stepping backward."

            "הגנה פנימית נגד דקירה ישרה - צד חי",
            "הגנה פנימית נגד דקירה ישרה – צד חי" ->
                "From a reversed General Stance No. 1 with knees bent, place your hands at the height of the knife. Perform an Internal Defence with your right forearm. Step forward with your right foot while striking the attacker's neck with your right forearm and grabbing the stabbing hand."

            "הגנה פנימית נגד דקירה ישרה - צד מת",
            "הגנה פנימית נגד דקירה ישרה – צד מת" ->
                "From a General Stance No. 1 with knees bent, place your hands at the height of the knife. Perform an Internal Defence with your left forearm while stepping forward with your left foot into the attacker's Blind Side. Grab the attacker's stabbing hand."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun brown(item: String): String {
        return when (item) {

            // ─────────── Jumping Kicks ───────────

            "בעיטה רגילה ובעיטת מגל בניתור" ->
                "Perform a regular kick, with the retraction of the kicking leg, jump with the base leg and perform a Magal (Circular) kick with the base leg."

            "בעיטת מגל בניתור" ->
                "Jump with one leg and perform a Magal (Circular) kick with the other leg. After the jump, bring the jumping base leg close to the buttocks."

            "בעיטת מגל כפולה בניתור" ->
                "Perform a horizontal Magal (Circular) kick. After the kick, the kicking leg lands forward. Rotate the body using the momentum of the first Magal (Circular) kick. Raise the other leg's knee and perform a second Magal (Circular) kick with the same leg."

            // ─────────── Break-falls and Rolls ───────────

            "גלגול עם רובה" ->
                "Drive the butt of the rifle into the ground at a sharp angle and perform a regular front roll. There are three options to finish: lying down, crouching or standing up."

            // ─────────── Defences Against Regular Kicks ───────────

            "הגנה פנימית נגד בעיטה לסנטר" ->
                "Stand in General Stance Number 1. Perform an Internal Defence with your left forearm. Step with your left foot into the Blind Side of the attacker and attack according to the attacker's distance."

            "הגנה חיצונית נגד בעיטה רגילה – פריצה",
            "הגנה חיצונית נגד בעיטה רגילה - פריצה" ->
                "Stand in a low General Stance Number 1. Perform an external stabbing defence with your right forearm, burst forward with your left foot while continuing the motion of the attacker's kicking leg upward. Push the attacker backward and downward with your left hand."

            "הגנה חיצונית נגד בעיטה רגילה – גזיזה",
            "הגנה חיצונית נגד בעיטה רגילה - גזיזה" ->
                "Stand in a low General Stance Number 1. Perform an external stabbing defence with your right forearm, burst forward with your left foot while continuing the motion of the attacker's kicking leg upward. Perform a backward cutting kick with your right leg."

            "הגנה חיצונית נגד בעיטה רגילה – טאטוא",
            "הגנה חיצונית נגד בעיטה רגילה - טאטוא" ->
                "Stand in a low General Stance Number 1. Perform an external stabbing defence with your right forearm, burst forward with your left foot while continuing the motion of the attacker's kicking leg upward. Position your body perpendicular to the attacker's body and perform a sweep."

            "הגנה פנימית נגד בעיטה רגילה – טאטוא",
            "הגנה פנימית נגד בעיטה רגילה - טאטוא" ->
                "Stand in a General Stance Number 2. Perform an Internal Defence with your left hand. Step forward with your left leg. Grab the arm of the attacker and punch with your right hand. Push the attacker off balance and perform a cutting kick with your left leg."

            // ─────────── Defences Against a Front Magal ───────────

            "הגנה נגד בעיטת מגל – פריצה",
            "הגנה נגד בעיטת מגל - פריצה" ->
                "Stand in a General Stance Number 2. Perform an Internal Defence with your left hand. Step forward with your left leg. Grab the arm of the attacker and punch with your right hand. Push the attacker off balance and perform a cutting kick with your left leg."

            "הגנה חיצונית נגד מגל לפנים – גזיזה",
            "הגנה חיצונית נגד מגל לפנים - גזיזה",
            "הגנה נגד בעיטת מגל לפנים – גזיזה",
            "הגנה נגד בעיטת מגל לפנים - גזיזה" ->
                "Stand in a General Stance Number 2. Perform an external stabbing defence with your left forearm away from the body. Burst forward with your left leg while continuing the motion of the kicking leg upward and perform a backward cutting kick with your right leg."

            "הגנה חיצונית נגד מגל לפנים – טאטוא",
            "הגנה חיצונית נגד מגל לפנים - טאטוא" ->
                "Stand in a General Stance Number 2. Perform an external stabbing defence with your left forearm away from the body. Burst forward with your left leg while continuing the motion of the kicking leg upward and perform a sweep with your left leg to drop the attacker."

            // ─────────── Defences Against a Reverse Magal ───────────

            "הגנה נגד בעיטת מגל לאחור – פריצה",
            "הגנה נגד בעיטת מגל לאחור - פריצה" ->
                "Stand in General Stance Number 1. Perform a stabbing External Defence away from your body with your right hand while guarding your face with your left, burst forward and push the attacker to the ground with your left hand."

            // ─────────── Releases from a Neck Hold ───────────

            "חביקת צואר מאחור – בריח על העורף, המגן כפוף לפנים",
            "חביקת צואר מאחור - בריח על העורף, המגן כפוף לפנים",
            "חביקת צוואר מאחור – בריח על העורף, המגן כפוף לפנים",
            "חביקת צוואר מאחור - בריח על העורף, המגן כפוף לפנים" ->
                "With a hooked right hand grab the attacker's forearm and turn your head toward the attacker's elbow. With your left push up the attacker's elbow. Drop on your knees and continue to a roll without placing your hands on the ground."

            // ─────────── Defences Against a Stick ───────────

            "הגנה נגד מקל בסיבוב – צד חי",
            "הגנה נגד מקל בסיבוב - צד חי" ->
                "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Wrap your left forearm around the attacker's arm and perform a front horizontal elbow strike with your right elbow while stepping forward."

            "הגנה נגד מקל עם קוואלר – צד מת",
            "הגנה נגד מקל עם קוואלר - צד מת" ->
                "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Bring down the attacker's hand by sliding both of your hands to the attacker's wrist and rotating 180 degrees clockwise with your right foot."

            "הגנה נגד מקל נקודת תורפה – לצד המת",
            "הגנה נגד מקל נקודת תורפה - לצד המת" ->
                "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Turn with your right foot to the attacker's Blind Side. Grab a vital spot on the attacker's head with your left hand and drop the attacker."

            // ─────────── Defences Against a Knife ───────────

            "הגנה נגד סכין בשיסוף – הטיה והגנה לצד החי",
            "הגנה נגד סכין בשיסוף - הטיה והגנה לצד החי" ->
                "Stand at a neutral stance. Step backward with your right foot and raise hands to protect the face. Afterward, perform an External Defence and grab the attacker's hand with your left hand. Step forward with your right foot and punch with your right hand."

            "הגנה נגד סכין בשיסוף – הטיה והגנה לצד המת",
            "הגנה נגד סכין בשיסוף - הטיה והגנה לצד המת" ->
                "Stand at a neutral stance. Step backward with your right foot and raise hands to protect the face. Perform an External Defence with your right forearm while stepping forward with your left foot. Turn 180 degrees clockwise with your right foot while grabbing a vital spot on the attacker's head."

            "הגנה נגד סכין בשיסוף – פריצה והגנה לצד החי",
            "הגנה נגד סכין בשיסוף - פריצה והגנה לצד החי" ->
                "Stand at a neutral stance. Burst forward with your left foot while performing an External Defence with your left forearm and continue rotating with the motion of the attack to grab the attacker's wrist. Perform a side palm-heel strike with your right hand."

            "הגנה נגד סכין בשיסוף – פריצה והגנה לצד המת",
            "הגנה נגד סכין בשיסוף - פריצה והגנה לצד המת" ->
                "Stand at a neutral stance. Burst forward with your left foot while performing an External Defence with your right forearm. Rotate your body 180 degrees clockwise with your right foot. Grab a vital spot on the attacker's head with your left hand and drop the attacker."

            // ─────────── Defences Against Gun Threats ───────────

            "הגנה מאיום אקדח מלפנים",
            "הגנה מאיום אקדח מלפנים ימין" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. Highlight to turn the barrel of the gun downward when possible.[[/RED_BOLD]]\nRaise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker's left thigh. Grab the barrel and the trigger guard with your hand. Step with your left foot forward to the Blind Side of the attacker."

            "הגנה מאיום אקדח מאחור" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. Highlight to turn the barrel of the gun downward when possible.[[/RED_BOLD]]\nGlance backward. Reach your left hand away from your body while rotating counterclockwise and stepping with your left foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height."

            "הגנה מאיום אקדח מהצד החיצוני" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. Highlight to turn the barrel of the gun downward when possible.[[/RED_BOLD]]\nReach your left hand away from your body and step with your left foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height. Simultaneously, perform a right punch to the face."

            "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד שמאל",
            "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד שמאל" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. Highlight to turn the barrel of the gun downward when possible.[[/RED_BOLD]]\nSimultaneously, use your left hand to grab the attacker's wrist to deflect the gun and use your right hand to grab the barrel and trigger guard. Step with your left foot to the Blind Side of the attacker. Disarm the attacker while creating leverage."

            "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד ימין",
            "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד ימין",
            "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד" ->
                "[[RED_BOLD]]All techniques should be trained with a weapon in either hand. Highlight to turn the barrel of the gun downward when possible.[[/RED_BOLD]]\nSimultaneously, use your right hand to grab the attacker's wrist to deflect the gun and use your left hand to grab the barrel and trigger guard. Step with your right foot to the live Side of the attacker. Disarm the attacker while creating leverage."

            else -> "$FALLBACK_PREFIX $item"
        }
    }

    private fun black(item: String): String {
        return when (item) {

            // ─────────── Jumping Kicks ───────────

            "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין" ->
                "Leap with the left foot. Perform a regular kick with the right foot while bending the left knee and bringing the left heel toward the buttocks. Land with the left foot and the right foot joins the left on the ground."

            "ניתור ברגל שמאל ובעיטה לצד ברגל ימין" ->
                "Leap with the left foot. Turn the toes of the left foot to the left. Turn the right side of the body to face the attacker and perform a side-kick with the right foot while bending the left knee and bringing the left heel toward the buttocks."

            "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל" ->
                "Leap with the left foot. Turn the toes of the left foot to the right. Turn the left side of the body to face the attacker. Perform a leg switch and perform a side-kick with the left foot while bending the right knee and bringing the right heel toward the buttocks."

            "בעיטה לצד בסיבוב מלא בניתור" ->
                "Leaping with a spin and performing a side-kick with the rear foot."

            "בעיטת מגל לאחור בסיבוב בניתור" ->
                "Leaping with a spin and performing a backward Magal (Circular) kick with the rear foot."

            "בעיטת הגנה לאחור בניתור" ->
                "Leaping with a spin and performing a backward defensive kick with the rear foot."

            // ─────────── Defences Against Punches Combined with Kicks ───────────

            "הגנה פנימית נגד אגרוף שמאל - בעיטת הגנה",
            "הגנה פנימית נגד אגרוף שמאל – בעיטת הגנה" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand and a defensive kick to the center of the attacker's body."

            "הגנה פנימית נגד אגרוף שמאל - בעיטה לצד",
            "הגנה פנימית נגד אגרוף שמאל – בעיטה לצד" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand, cross your legs and perform a side-kick with your right foot toward the attacker's knee."

            "הגנה פנימית נגד אגרוף שמאל - בעיטה רגילה לאחור",
            "הגנה פנימית נגד אגרוף שמאל – בעיטה רגילה לאחור" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand, cross your legs, turn your back toward the attacker and perform a backward regular kick with your right foot."

            "הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לאחור",
            "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לאחור" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand and a backward Magal (Circular) kick with your right foot."

            "הגנה פנימית נגד אגרוף שמאל - בעיטת סטירה חיצונית",
            "הגנה פנימית נגד אגרוף שמאל – בעיטת סטירה חיצונית" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand and an external slapping kick with your right foot."

            "הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לפנים",
            "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לפנים" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand and a front Magal (Circular) kick with your right foot."

            "הגנה פנימית נגד אגרוף שמאל - גזיזה קדמית",
            "הגנה פנימית נגד אגרוף שמאל – גזיזה קדמית" ->
                "[[RED_BOLD]]These defences should be performed standing in a reversed Internal Defences stance. Highlight timing.[[/RED_BOLD]]\nPerform an Internal Defence with your right hand, cross your legs while rotating your body and perform a forward cutting kick."

            // ─────────── Defences Against Kicks ───────────

            "הגנה נגד בעיטה רגילה - התחמקות בסיבוב",
            "הגנה נגד בעיטה רגילה – התחמקות בסיבוב" ->
                "Stand either in a neutral stance or a General Stance Number 2. Perform an Internal Defence with your left hand and continue the movement to lift the kicking leg while evading to the Blind Side with half a spin."

            "הגנה נגד בעיטת מגל לפנים לראש - הדיפה באמת שמאל",
            "הגנה נגד בעיטת מגל לפנים לראש – הדיפה באמת שמאל" ->
                "From an External Defences stance perform a 360 degree defence number one with your left forearm under the attacker's leg while advancing and thrusting the leg upward to drop the attacker."

            "הגנה נגד בעיטת מגל לפנים לראש - רגל עברה מעל הראש",
            "הגנה נגד בעיטת מגל לפנים לראש – רגל עברה מעל הראש" ->
                "From an External Defences stance perform a 360 degree defence number one with your left forearm under the attacker's leg while performing an uppercut strike to the groin."

            "הגנה נגד מגל לפנים לראש – התחמקות גוף בסיבוב וגזיזה",
            "הגנה נגד מגל לפנים לראש - התחמקות גוף בסיבוב וגזיזה",
            "הגנה נגד מגל לפנים  לראש - התחמקות גוף בסיבוב בגזיזה" ->
                "Evade the kick by kneeling on the left knee with a spin, place both your hands on the ground and perform a backward low Magal (Circular) kick to drop the attacker."

            "הגנה נגד בעיטת סטירה - גזיזה",
            "הגנה נגד בעיטת סטירה – גזיזה" ->
                "Stand in a reversed General Stance Number 2. Cross your legs and perform a backward cutting kick with your right leg while thrusting the attacker's kicking leg upward with your right hand."

            // ─────────── Releases ───────────

            "שחרור מתפיסת נלסון" ->
                "Lower your elbows as low as possible to grab the attacker's hands with yours. Shift your hip to the side and place one foot behind the attacker's foot. Push with your knee to the back of the knee of the attacker and fall backward on top of the attacker."

            "שחרור מחביקת צואר מהצד - משיכה לאחור",
            "שחרור מחביקת צואר מהצד – משיכה לאחור" ->
                "[[RED_BOLD]]Neutralize the attacker to the extent you can leave the area safely.[[/RED_BOLD]]\nOn the sense of the pull backward one hand goes to a vital point on the attacker's head while the leg closer to the attacker goes behind his legs. Fall with the attacker to the ground, strike and get up."

            "שחרור מחביקת צואר מהצד - יד תפוסה",
            "שחרור מחביקת צואר מהצד – יד תפוסה" ->
                "[[RED_BOLD]]Neutralize the attacker to the extent you can leave the area safely.[[/RED_BOLD]]\nThe attacker's foot is in front of the defender's feet and the defender's hand is in front of the attacker's legs. If the attacker wears long pants, grab with both hands in the upper shin area and lift. Fall backward with the attacker, strike and get up."

            "שחרור מחביקת צואר מהצד - זריקת רגל",
            "שחרור מחביקת צואר מהצד – זריקת רגל" ->
                "[[RED_BOLD]]Neutralize the attacker to the extent you can leave the area safely.[[/RED_BOLD]]\nOn the sense of falling forward send your leg between the attacker's legs while lying on your back. Strike to the groin with your free hand to throw the attacker on the back."

            "שחרור מחביקת צואר מהצד - מהברך",
            "שחרור מחביקת צואר מהצד – מהברך",
            "שחרור מחביקת צואר מהצד - ירידה לברך",
            "שחרור מחביקת צואר מהצד – ירידה לברך" ->
                "[[RED_BOLD]]Neutralize the attacker to the extent you can leave the area safely.[[/RED_BOLD]]\nStrike the attacker's groin with the front hand while grabbing a vital point on the attacker's head with the rear hand and pull backward to drop the attacker."

            "שחרור מחביקה פתוחה מהצד" ->
                "Perform a knee strike to the groin, elbow strike or a head butt."

            "שחרור מחביקה סגורה מהצד - היד הרחוקה משוחררת" ->
                "The head of the attacker is in front: punch the head with the rear hand. The head of the attacker is behind: with the hand closer to the attacker strike the groin and continue the movement toward a high elbow strike."

            "שחרור מחביקה סגורה מהצד" ->
                "With the hand closer to the attacker strike the groin and continue the movement toward a high elbow strike."

            "שחרור מחביקה פתוחה מאחור - הטלה" ->
                "Bend your back and knees while holding the attacker's right arm with both hands. Place your right foot on the ball of the foot to the right rotated inward to prevent the attacker from avoiding the fall. Pull and drop the attacker forward."

            "שחרור מחביקה סגורה מאחור - הטלה" ->
                "Bend your back and knees while lifting your hands to grab the arms of the attacker. Rotate your head to the side and perform a forward roll without hands while holding the attacker."

            // ─────────── Defences Against a Long Stick ───────────

            "הגנה נגד מקל ארוך – התקפה לצד ימין מגן",
            "הגנה נגד מקל ארוך-התקפה לצד ימין מגן" ->
                "From a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Pull down the attacker's hand with two hooked hands while taking your right foot backward and attack with a right punch."

            "הגנה נגד מקל ארוך – התקפה לצד שמאל מגן",
            "הגנה נגד מקל ארוך-התקפה לצד שמאל מגן" ->
                "From a neutral stance, with both hands straight and the head between the arms, burst forward with your left leg. Step forward with your right foot placing your body at the attacker's side striking with a right punch or elbow according to range."

            "הגנה נגד מקל ארוך מצד ימין" ->
                "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Wrap your right hand around the arms of the attacker, punch with your left hand. Some situations require a horizontal elbow strike."

            "הגנה נגד מקל ארוך מצד שמאל" ->
                "From a neutral stance, burst forward with your left foot with your hands straight forward and close to the head. Wrap your left hand around the arms of the attacker, step with your right foot and perform a horizontal elbow strike with your right hand."

            "הגנה נגד דקירה במקל ארוך – הצד החי",
            "הגנה נגד מקל ארוך דקירה - צד חי" ->
                "Stand in a neutral stance. Deflect the rifle with your left palm. Step forward with your left while rotating the body and defend with both your forearms. Grab the rifle with both hands. Pull the rifle upward while kicking to the groin."

            "הגנה נגד דקירה במקל ארוך – הצד המת",
            "הגנה נגד מקל ארוך דקירה - צד מת" ->
                "Stand in a neutral stance. Deflect the rifle with your right palm and grab it with your left while stepping forward with your right foot and performing a kick to the groin with your left foot."

            // ─────────── Defences Against a Knife ───────────

            "הגנה נגד סכין שיסוף מהצד החי – בצד ימין",
            "הגנה נגד דקירה - צד חי ימין" ->
                "[[RED_BOLD]]Attack according to your distance from the attacker.[[/RED_BOLD]]\nPerform an Internal Defence with the right forearm and kick to the attacker's knee while leaning the opposite direction."

            "הגנה נגד סכין שיסוף מהצד החי – בצד שמאל",
            "הגנה נגד דקירה - צד חי שמאל" ->
                "[[RED_BOLD]]Attack according to your distance from the attacker.[[/RED_BOLD]]\nPerform an External Defence with the left forearm and kick to the attacker's knee while leaning the opposite direction."

            "הגנה נגד סכין שיסוף מהצד המת – בצד ימין",
            "הגנה נגד דקירה - צד מת ימין" ->
                "[[RED_BOLD]]Attack according to your distance from the attacker.[[/RED_BOLD]]\nPerform an External Defence with the right forearm and kick to the attacker's knee while leaning the opposite direction."

            "הגנה נגד סכין שיסוף מהצד המת – בצד שמאל",
            "הגנה נגד דקירה - צד מת שמאל" ->
                "[[RED_BOLD]]Attack according to your distance from the attacker.[[/RED_BOLD]]\nPerform an Internal Defence with the left forearm and kick to the attacker's knee while leaning the opposite direction."

            // ─────────── Stick Against Knife ───────────

            "הגנת מקל נגד סכין בתפיסה רגילה",
            "מקל נגד סכין - דקירה רגילה" ->
                "[[RED_BOLD]]All defences should be performed close to the attacker's wrist with the other hand protecting the face.[[/RED_BOLD]]\nStep with your right foot to the live Side of the attacker if necessary and stab with your stick to the attacker's throat."

            "הגנת מקל נגד סכין בתפיסה מזרחית",
            "מקל נגד סכין - דקירה מזרחית" ->
                "[[RED_BOLD]]All defences should be performed close to the attacker's wrist with the other hand protecting the face.[[/RED_BOLD]]\nDefend with the stick on the attacker's wrist and strike the attacker's head."

            "הגנת מקל נגד סכין בתפיסה ישרה",
            "מקל נגד סכין - דקירה ישרה" ->
                "[[RED_BOLD]]All defences should be performed close to the attacker's wrist with the other hand protecting the face.[[/RED_BOLD]]\nRotate your body to perform a body defence while deflecting the attack with the stick on the attacker's wrist and continue attacking to the attacker's head."

            "הגנת מקל נגד סכין בתפיסה רגילה – צד ימין",
            "מקל נגד סכין - דקירה מעל מצד ימין" ->
                "Perform 360 defence with the stick to the attacker's wrist. Step with your left foot and punch the attacker with your left."

            "הגנת מקל נגד סכין בתפיסה רגילה – צד שמאל",
            "מקל נגד סכין - דקירה מעל מצד שמאל" ->
                "Perform an External Defence with your left forearm, step with your right foot and attack with the stick."

            "הגנת מקל נגד סכין בתפיסה מזרחית – צד ימין",
            "מקל נגד סכין - דקירה מזרחית מצד ימין" ->
                "Defend with the stick to the attacker's wrist and strike with the stick to the attacker's throat."

            "הגנת מקל נגד סכין בתפיסה מזרחית – צד שמאל",
            "מקל נגד סכין - דקירה מזרחית מצד שמאל" ->
                "Perform an External Defence with your left forearm, step with your right foot and attack with the stick."

            "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד ימין",
            "מקל נגד סכין - דקירה ישירה מצד ימין (פנימית)" ->
                "Perform an Internal Defence with the stick to the attacker's wrist, step with your right to the live Side and attack with the stick."

            "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד ימין",
            "מקל נגד סכין - דקירה ישירה מצד ימין (חיצונית)" ->
                "Perform 360 defence with the stick to the attacker's wrist. Step with your left foot and punch the attacker with your left."

            "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד שמאל",
            "מקל נגד סכין - דקירה ישירה מצד שמאל (פנימית)" ->
                "Deflect the attack with your left forearm. Step with your left foot to the Blind Side of the attacker and attack with the stick."

            "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד שמאל",
            "מקל נגד סכין - דקירה ישירה מצד שמאל (חיצונית)" ->
                "Perform an External Defence with your left forearm. Step with your right foot and attack with the stick."

            "מקל בתנועה" ->
                "Using a stick to mislead in combat against a knife. Train both left and right."

            "שימוש בסכין" ->
                "Using a knife to mislead in combat. A knife should never be used to stab the attacker. Train both left and right."

            // ─────────── Multiple Attackers ───────────

            "1 מקל 1 סכין – מקל בצד חי",
            "מקל אחד וסכין אחת - המקל בצד חי" ->
                "Defending against the stick with a spin while establishing eye contact with the second attacker. Disarm the attacker if possible and defend against the stab of the second attacker."

            "1 מקל 1 סכין – מקל בצד מת",
            "מקל אחד וסכין אחת - המקל בצד מת" ->
                "Defending against the stick with a cavalier while establishing eye contact with the second attacker. Disarm the attacker and use the stick against the second attacker. Defend the stick and grab a vital spot on the attacker's head with your left hand."

            "1 מקל 1 סכין – במקרה והסכין קרוב",
            "מקל אחד וסכין אחת - הסכין קרוב" ->
                "Defending against the knife while establishing eye contact with the second attacker and then defend against the stick."

            "הדמיה כנגד 2 תוקפים" ->
                "The student should assemble two simulations against two attackers, one armed and the other not."

            // ─────────── Defences Against Gun Threats ───────────

            "הגנה נגד איום אקדח לראש מלפנים" ->
                "Raise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker's thigh. Grab the barrel and the trigger guard with your hand. Step with your left foot forward to the Blind Side of the attacker and punch."

            "הגנה נגד איום אקדח צמוד לראש מלפנים" ->
                "Raise your left hand upward and grab the gun, deflecting it sideways and downward toward the attacker's thigh. Grab the barrel and the trigger guard with your hand. Step with your left foot forward to the Blind Side of the attacker and punch."

            "הגנה נגד איום אקדח מלפנים - קנה קצר",
            "הגנה נגד איום אקדח מלפנים – קנה קצר" ->
                "Grab the wrist of the attacker's hand with your left hand so the back of the hand faces upward. Grab the fist holding the gun with your right hand. Twist to point the gun at the attacker and kick the groin while performing a cavalier to drop the attacker."

            "הגנה נגד איום אקדח לראש - צד ימין",
            "הגנה נגד איום אקדח לראש – צד ימין" ->
                "The gun is in front of the defender's arm: raise your left hand upward and grab the gun, deflecting it sideways and downward. Step with your left foot to the Blind Side of the attacker and punch with your right hand to the attacker's face."

            "הגנה נגד איום אקדח לראש - צד שמאל",
            "הגנה נגד איום אקדח לראש – צד שמאל" ->
                "The gun is in front of the defender's arm: raise your left hand upward and grab the gun, deflecting it sideways and downward. Step with your left foot to the Blind Side of the attacker and punch with your right hand to the attacker's face."

            "הגנה נגד איום אקדח מאחור באלכסון - צד שמאל",
            "הגנה נגד איום אקדח מאחור באלכסון – צד שמאל",
            "הגנה נגד איום אקדח לראש מהצד מאחור – צד שמאל" ->
                "Glance to the side. Reach your left hand away from your body while rotating counterclockwise and stepping with your right foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height."

            "הגנה נגד איום אקדח לראש מאחור" ->
                "Reach your left hand away from your body while rotating counterclockwise and stepping with your left foot into the live Side of the attacker. Grab the hand of the attacker between your left arm and forearm and under shoulder height."

            "הגנה נגד איום אקדח מאחור בידיים מורמות" ->
                "Hands raised. Glance backward. Turn to the live Side. Wrap the hand with the gun close to the wrist with your left. Drop the attacker with a right horizontal elbow while stepping with your right foot to the live Side."

            "הגנה נגד איום אקדח בהובלה",
            "הגנה מאיום אקדח בהובלה" ->
                "Step with your right foot and turn left to the attacker's Blind Side. Raise your left hand to bring the gun closer to the attacker's body. Put your left ear to the attacker's back and send your right hand between the attacker's legs."

            "הגנה מאיום אקדח מאחור דחיפה" ->
                "Landing with the right foot forward: with your left hand grab the barrel from below and deflect while turning to the live Side and punching with the right. Continue with the right hand to grab the attacker's wrist and disarm."

            // ─────────── Strikes with a Short Stick ───────────

            "מכת מקל לראש" ->
                "Stick Strike to the Head."

            "מכת מקל לרקה" ->
                "Stick Strike to the Temple."

            "מכת מקל ללסת / צואר" ->
                "Stick Strike to the Jaw or Neck."

            "מכת מקל לעצם הבריח" ->
                "Stick Strike to the Clavicle Bone."

            "מכת מקל למרפק" ->
                "Stick Strike to the Elbow."

            "מכת מקל לשורש כף היד" ->
                "Stick Strike to the Wrist."

            "מכת מקל לפרקי האצבעות" ->
                "Stick Strike to the Knuckles."

            "מכת מקל לברך" ->
                "Stick Strike to the Knee."

            "מכת מקל למפסעה" ->
                "Stick Strike to the Groin."

            "הצלפת מקל לצלעות" ->
                "Stick Whip to the Ribs."

            "דקירת מקל חיצונית לצלעות" ->
                "External Stick Stab to the Ribs."

            "דקירת מקל ישרה לבטן / לגרון" ->
                "Straight Stick Stab to the Abdominal or Throat."

            "דקירת מקל הפוכה" ->
                "Reversed Stick Stab."

            // ─────────── Strikes with a Long Stick or Rifle ───────────

            "מכה אופקית לצואר" ->
                "From a regular stance, step forward with your left foot and strike the neck or face using the magazine or the handle or the weapon's body."

            "דקירה" ->
                "From a regular stance, stab forward with the bayonet or the front side of the stick. The body movement is similar to the one used in a straight stab with a knife."

            "מכת מגל" ->
                "From a regular stance, strike with the butt of the weapon or the rear of the stick, rotate right heel and hip."

            "שיסוף" ->
                "From a regular stance, slash with the bayonet or the front side of the stick while rotating the left heel."

            "מכה למפסעה",
            "מכה למפשעה" ->
                "From a regular stance, strike to the groin with the butt of the weapon or the rear of the stick, rotate right heel and hip."

            "מכת סנוקרת" ->
                "From a regular stance, perform an uppercut strike to the attacker's chin with the butt of the weapon or the rear of the stick, rotate right heel and hip."

            "מכה לצד" ->
                "From a neutral stance, strike to the side with the butt of the weapon or the rear of the stick."

            "מכה לאחור" ->
                "From a regular stance, stab backward with the butt of the weapon or the rear of the stick."

            "מכה אופקית לאחור" ->
                "From a regular stance, strike horizontally backward with the butt of the weapon or the rear of the stick while rotating the left heel."

            "מכה אופקית ובעיטה רגילה למפסעה",
            "מכה אופקית ובעיטה למפשעה" ->
                "From a regular stance, perform a front horizontal strike and a regular kick to the groin with the rear leg."

            "מכה אופקית ובעיטת הגנה לפנים",
            "מכה אופקית ובעיטת הגנה" ->
                "From a regular stance, perform a front horizontal strike and a defensive kick."

            "מכה לצד ובעיטה לצד" ->
                "From a neutral stance, perform a side strike. Cross your legs and perform a side-kick to the knee."

            // ─────────── Defences with a Rifle Against a Knife ───────────

            "הגנות עם רובה נגד דקירות סכין" ->
                "Defences with a Rifle Against a Knife."

            "רובה נגד דקירה ישירה גבוהה" ->
                "Stand in a regular stance. Deflect with the barrel, step into the live Side and attack."

            "רובה נגד דקירה ישירה נמוכה" ->
                "Stand in a regular stance. Deflect with the butt, perform a slash with the barrel and a Magal strike with the butt."

            "רובה נגד דקירה רגילה" ->
                "Stand in a regular stance. Block the strike diagonally while stepping forward with your right foot and attack."

            "רובה נגד דקירה מזרחית מימין" ->
                "Stand in a regular stance. Block the stab to the right and attack."

            "רובה נגד דקירה מזרחית משמאל" ->
                "Stand in a regular stance. Block the strike, step forward with your right foot while rotating the body and attack."

            "רובה נגד דקירה מזרחית מלמטה" ->
                "Stand in a regular stance. Block the stab to the right and attack with a Horizontal Strike to the Neck."

            // ─────────── SMG Threat ───────────

            "הגנה נגד איום תת־מקלע" ->
                "Perform a Hover-Roll toward the attacker. Kneel and burst toward the attacker. Knees bent on either side of the attacker and grab the attacker. Put your ear to the attacker's back while the left hand is sent diagonally upward."

            else -> "$FALLBACK_PREFIX $item"
        }
    }
}
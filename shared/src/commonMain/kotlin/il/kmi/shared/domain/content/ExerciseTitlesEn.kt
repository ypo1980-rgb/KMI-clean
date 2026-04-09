package il.kmi.shared.domain.content

object ExerciseTitlesEn {

    private val map: Map<String, String> = linkedMapOf(

        // ---------------------------------------------------------
        // Missing full translations for topic-based screens
        // ---------------------------------------------------------
        "בעיטת סריקה חיצונית בסיבוב" to "Spinning Outside Sweep Kick",
        "בעיטת מגל גבוהה ובעיטת מגל באותה רגל" to "High Roundhouse Kick and Roundhouse Kick with the Same Leg",
        "בעיטת מגל לאחור בשיכול אחורי" to "Backward Roundhouse Kick with Rear Cross-Step",
        "בעיטה לצד בסיבוב" to "Spinning Side Kick",
        "בעיטת סטירה חיצונית" to "Outside Slap Kick",

        "בעיטת פטיש" to "Hammer Kick",
        "בעיטת גזיזה אחורית" to "Rear Scissor Kick",
        "בעיטת גזיזה קדמית" to "Front Scissor Kick",
        "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב" to "Front Scissor Kick and Rear Scissor Kick in a Spin",
        "בעיטת מגל לאחור בסיבוב" to "Spinning Backward Roundhouse Kick",
        "בעיטת סטירה חיצונית בסיבוב" to "Spinning Outside Slap Kick",

        "בעיטת מגל בניתור" to "Jumping Roundhouse Kick",
        "בעיטה רגילה ובעיטת מגל בניתור" to "Regular Kick and Jumping Roundhouse Kick",
        "בעיטת מגל כפולה בניתור" to "Double Jumping Roundhouse Kick",

        "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין" to "Jump on Left Leg and Regular Kick with Right Leg",
        "ניתור ברגל שמאל ובעיטה לצד ברגל ימין" to "Jump on Left Leg and Side Kick with Right Leg",
        "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל" to "Jump on Left Leg and Side Kick with Left Leg",
        "בעיטת לצד בסיבוב מלא בניתור" to "Full Spinning Jump Side Kick",
        "בעיטת מגל לאחור בסיבוב בניתור" to "Jumping Spinning Backward Roundhouse Kick",
        "בעיטת הגנה לאחור בניתור" to "Jumping Backward Defensive Kick",

        // ---------------------------------------------------------
        // Topics
        // ---------------------------------------------------------
        "כללי" to "General",
        "עמידת מוצא" to "Ready Stance",
        "עבודת ידיים" to "Hand Techniques",
        "שחרורים" to "Releases",
        "הכנה לעבודת קרקע" to "Groundwork Preparation",
        "בעיטות" to "Kicks",
        "הגנות" to "Defenses",
        "בלימות וגלגולים" to "Breakfalls and Rolls",
        "קוואלר" to "Cavalier",
        "מכות מרפק" to "Elbow Strikes",
        "מכות במקל / רובה" to "Stick / Rifle Strikes",
        "בעיטות בניתור" to "Jump Kicks",
        "מכות במקל קצר" to "Short Stick Strikes",
        "גלגולים" to "Rolls",

        // ---------------------------------------------------------
        // Sub-topics
        // ---------------------------------------------------------
        "מרפק" to "Elbow",
        "פיסת יד" to "Knife Hand",
        "אגרופים ישרים" to "Straight Punches",
        "מגל + סנוקרת" to "Hooks + Uppercuts",
        "שחרורים מתפיסות ידיים" to "Releases from Hand Grabs",
        "שחרורים מחניקות" to "Releases from Chokes",
        "שחרור מתפיסות ידיים" to "Releases from Hand Grabs",
        "שחרורים מתפיסות חולצה" to "Releases from Shirt Grabs",
        "שחרורים מתפיסות שיער" to "Releases from Hair Grabs",
        "שחרורים מתפיסות צוואר וגוף" to "Releases from Neck and Body Holds",
        "הגנות נגד מכות ישרות" to "Defenses Against Straight Punches",
        "6 הגנות חיצוניות נגד מכות" to "6 Outside Defenses Against Strikes",
        "הכנה לעבודת קרקע" to "Groundwork Preparation",
        "הגנות חיצוניות נגד מכות מהצד" to "Outside Defenses Against Side Strikes",
        "הגנות נגד בעיטות" to "Defenses Against Kicks",
        "הגנות נגד סכין" to "Defenses Against Knife Attacks",
        "שחרור מתפיסות" to "Releases from Grabs",
        "שחרור מחביקות" to "Releases from Bear Hugs",
        "הגנות נגד מקל" to "Defenses Against Stick Attacks",
        "הגנות נגד מכות אגרוף" to "Defenses Against Punches",
        "הגנה נגד בעיטות" to "Defenses Against Kicks",
        "הגנות מאיום סכין" to "Defenses Against Knife Threats",
        "שחרורים מחביקות" to "Releases from Bear Hugs",
        "שחרורים מתפיסות ידיים" to "Releases from Hand Grabs",
        "הגנות מאיום אקדח" to "Defenses Against Gun Threats",
        "הגנות - מספר תוקפים" to "Defenses - Multiple Attackers",
        "הגנות נגד מכות בשילוב בעיטות" to "Defenses Against Punches Combined with Kicks",
        "הגנות – מקל נגד סכין" to "Stick Defenses Against Knife",
        "הגנה עם רובה נגד סכין" to "Rifle Defense Against Knife",
        "הגנה מאיום תמ\"ק" to "Defense Against SMG Threat",
        "הגנה – בעיטה" to "Defense - Kicks",
        "הגנה – סכין" to "Defense - Knife",
        "הגנה – מקל" to "Defense - Stick",
        "הגנה – איום אקדח" to "Defense - Gun Threat",
        "שחרורים מחביקות צואר" to "Releases from Neck Holds",
        "שחרורים מתפיסות נלסון" to "Releases from Nelson Holds",
        "שחרורים מחביקות גוף" to "Releases from Body Bear Hugs",

        // ---------------------------------------------------------
        // Yellow belt
        // ---------------------------------------------------------
        "בלימת רכה לפנים" to "Soft Front Breakfall",
        "בלימה לאחור" to "Backward Breakfall",
        "תזוזות" to "Movement Drills",
        "גלגול לפנים – צד ימין" to "Forward Roll - Right Side",
        "הוצאות אגן, הרמת אגן והפניית גוף למעלה " to "Hip Escape, Bridge, and Body Turn Upward",
        "צל בוקס" to "Shadow Boxing",
        "סגירת אגרוף" to "Fist Closing",
        "אצבעות לפנים" to "Fingers Forward",
        "מכת קשת האצבע והאגודל" to "Thumb and Index Arc Strike",

        "עמידת מוצא רגילה" to "Regular Ready Stance",
        "עמידת מוצא להגנות פנימיות" to "Ready Stance for Inside Defenses",
        "עמידת מוצא להגנות חיצוניות" to "Ready Stance for Outside Defenses",
        "עמידת מוצא צידית" to "Side Ready Stance",
        "עמידת מוצא כללית מספר 1" to "General Ready Stance No. 1",
        "עמידת מוצא כללית מספר 2" to "General Ready Stance No. 2",

        "מכת מרפק אופקית לאחור" to "Horizontal Elbow Strike Backward",
        "מכת מרפק אופקית לצד" to "Horizontal Elbow Strike to the Side",
        "מכת מרפק אופקית לפנים" to "Horizontal Elbow Strike Forward",
        "מכת מרפק לאחור" to "Backward Elbow Strike",
        "מכת מרפק לאחור למעלה" to "Backward Upward Elbow Strike",
        "מכת מרפק אנכי למטה" to "Vertical Downward Elbow Strike",
        "מכת מרפק אנכי למעלה" to "Vertical Upward Elbow Strike",

        "מכת פיסת יד שמאל לפנים" to "Left Knife-Hand Strike Forward",
        "מכת פיסת יד ימין לפנים" to "Right Knife-Hand Strike Forward",
        "מכת פיסת יד שמאל-ימין לפנים" to "Left-Right Knife-Hand Strike Forward",
        "מכת פיסת יד שמאל-ימין-שמאל לפנים" to "Left-Right-Left Knife-Hand Strike Forward",
        "מכת פיסת יד מהצד" to "Side Knife-Hand Strike",

        "אגרוף ימין לפנים" to "Right Straight Punch Forward",
        "אגרוף שמאל-ימין לפנים" to "Left-Right Straight Punch Forward",
        "אגרוף שמאל בהתקדמות" to "Left Punch While Advancing",
        "אגרוף ימין בהתקדמות" to "Right Punch While Advancing",
        "אגרוף שמאל-ימין בהתקדמות" to "Left-Right Punch While Advancing",
        "אגרוף שמאל-ימין ושמאל בהתקדמות" to "Left-Right-Left Punch While Advancing",
        "אגרוף שמאל בנסיגה" to "Left Punch While Retreating",
        "אגרוף שמאל למטה בהתקפה" to "Left Low Punch in Attack",
        "אגרוף ימין למטה בהתקפה" to "Right Low Punch in Attack",
        "אגרוף שמאל למטה בהגנה" to "Left Low Punch in Defense",
        "אגרוף ימין למטה בהגנה" to "Right Low Punch in Defense",

        "מכת מגל שמאל" to "Left Hook Punch",
        "מכת מגל ימין" to "Right Hook Punch",
        "מכת מגל למטה ולמעלה בהתחלפות" to "Alternating Low and High Hook Punches",
        "מכת סנוקרת שמאל" to "Left Uppercut",
        "מכת סנוקרת ימין" to "Right Uppercut",

        "שחרור מתפיסת יד מול יד" to "Release from Same-Side Wrist Grab",
        "שחרור מתפיסת יד נגדית" to "Release from Cross Wrist Grab",
        "שחרור מתפיסת שתי ידיים למטה" to "Release from Two-Hand Low Wrist Grab",
        "שחרור מתפיסת שתי ידיים למעלה" to "Release from Two-Hand High Wrist Grab",

        "מניעת התקרבות תוקף" to "Preventing Attacker Approach",
        "מניעת חניקה" to "Choke Prevention",
        "שחרור מחניקה מלפנים בכף היד" to "Release from Front Choke with Palm Defense",
        "שחרור מחניקה מאחור במשיכה" to "Release from Rear Choke by Pull",

        "הוצאת אגן" to "Hip Escape",
        "הרמת אגן והפניית גוף לכיון ההפלה" to "Bridge and Turn Toward the Takedown",
        "מוצא לעבודת קרקע" to "Groundwork Starting Position",

        "בעיטה רגילה למפסעה" to "Regular Kick to the Groin",
        "בעיטה רגילה לסנטר" to "Regular Kick to the Chin",
        "בעיטת מגל נמוכה" to "Low Roundhouse Kick",
        "בעיטת מגל אופקית" to "Horizontal Roundhouse Kick",
        "בעיטת מגל אלכסונית" to "Diagonal Roundhouse Kick",
        "בעיטת מגל בהטעיה" to "Feinted Roundhouse Kick",
        "בעיטת ברך גבוהה" to "High Knee Strike",
        "בעיטת ברך מהצד" to "Side Knee Strike",
        "בעיטת ברך נמוכה למפסעה" to "Low Knee Strike to the Groin",
        "בעיטה לצד מעמידת פיסוק" to "Side Kick from Wide Stance",

        "הגנות חיצוניות רפלקסיביות 360 מעלות" to "360 Reflexive Outside Defenses",
        "הגנה פנימית רפלקסיבית" to "Reflexive Inside Defense",
        "הגנה פנימית נגד ימין בכף יד שמאל" to "Inside Defense Against Right Punch with Left Palm",
        "הגנה פנימית נגד שמאל בכף יד ימין" to "Inside Defense Against Left Punch with Right Palm",
        "הגנה פנימית נגד בעיטה רגילה למפסעה" to "Inside Defense Against Regular Kick to the Groin",

        // ---------------------------------------------------------
        // Orange belt
        // ---------------------------------------------------------
        "גלגול לאחור צד ימין" to "Backward Roll - Right Side",
        "גלגול לאחור צד שמאל" to "Backward Roll - Left Side",
        "גלגול לפנים צד שמאל" to "Forward Roll - Left Side",
        "שילובי ידיים רגליים" to "Hand and Leg Combinations",
        "בלימה לצד ימין" to "Breakfall to the Right",
        "בלימה לצד שמאל" to "Breakfall to the Left",

        "מכת גב יד בהצלפה" to "Backfist Snap Strike",
        "מכת גב יד בהצלפה בסיבוב" to "Spinning Backfist Snap Strike",
        "מכת פטיש" to "Hammer Fist Strike",
        "מכת פטיש מהצד" to "Side Hammer Fist Strike",

        "בעיטה רגילה בעקב לסנטר" to "Regular Heel Kick to the Chin",
        "בעיטת הגנה לפנים" to "Defensive Front Kick",
        "בעיטת סנוקרת לאחור" to "Backward Hooking Kick",
        "בעיטה לצד בשיכול" to "Cross-Step Side Kick",
        "בעיטה רגילה לאחור" to "Regular Back Kick",
        "בעיטה לצד בנסיגה" to "Retreating Side Kick",
        "בעיטת הגנה לאחור" to "Defensive Back Kick",
        "בעיטת סטירה פנימית" to "Inside Slap Kick",
        "בעיטת עצירה בכף הרגל האחורית" to "Stop Kick with Rear Foot",
        "בעיטת עצירה בכף הרגל הקדמית" to "Stop Kick with Front Foot",
        "בעיטה רגילה ובעיטת מגל ברגל השנייה" to "Regular Kick and Roundhouse with the Other Leg",
        "שילובי בעיטות" to "Kick Combinations",
        "ניתור ברגל ימין ובעיטה רגילה ברגל ימין" to "Hop on Right Leg and Regular Kick with Right Leg",

        "שחרור מתפיסת יד מול יד - בריח על האגודל" to "Release from Same-Side Wrist Grab - Thumb Lock",
        "שחרור מתפיסת יד נגדית - פרקי אצבעות" to "Release from Cross Wrist Grab - Knuckles",
        "שחרור מתפיסת יד בשתי ידיים למעלה" to "Release from Two-Hand Wrist Grab Upward",
        "שחרור מתפיסת יד בשתי ידיים למטה - מרווח" to "Release from Two-Hand Wrist Grab Downward - With Space",
        "שחרור מתפיסת יד בשתי ידיים למטה - צמוד" to "Release from Two-Hand Wrist Grab Downward - Close",
        "שחרור מתפיסת ידיים צמודה מאחור" to "Release from Close Two-Hand Grab from Behind",
        "שחרור מתפיסת זרוע מהצד במשיכה" to "Release from Arm Grab from the Side by Pull",
        "שחרור מתפיסת זרוע מהצד בדחיפה" to "Release from Arm Grab from the Side by Push",

        "שחרור חולצה - בריח על האגודל" to "Shirt Grab Release - Thumb Lock",
        "שחרור חולצה - מכת פרקי אצבעות" to "Shirt Grab Release - Knuckle Strike",
        "שחרור חולצה - שתי ידיים" to "Shirt Grab Release - Two Hands",

        "שחרור מתפיסת שיער מלפנים" to "Release from Front Hair Grab",
        "שחרור מתפיסת שיער מלפנים בשתי ידיים" to "Release from Front Two-Hand Hair Grab",

        "שחרור חביקת צואר מלפנים" to "Release from Front Neck Hold",
        "שחרור מחביקה פתוחה מלפנים" to "Release from Open Front Bear Hug",
        "שחרור מחביקה פתוחה מאחור" to "Release from Open Rear Bear Hug",
        "שחרור מחביקת צואר מהצד בשכיבה" to "Release from Side Neck Hold on the Ground",
        "שחרור מחביקת צואר ויד מהצד בשכיבה" to "Release from Side Neck-and-Arm Hold on the Ground",

        "שחרור מחניקה מלפנים בדחיפה" to "Release from Front Choke by Push",
        "שחרור מחניקה מאחור בדחיפה" to "Release from Rear Choke by Push",
        "שחרור מחניקה מהצד - מרחוק" to "Release from Side Choke - Long Range",
        "שחרור מחניקה מהצד - מקרוב" to "Release from Side Choke - Close Range",
        "שחרור מחנקה מהצד בשכיבה" to "Release from Side Choke on the Ground",

        "הגנות נגד מכות עם הטיות גוף" to "Defenses Against Punches with Body Evasion",
        "הגנה פנימית נגד שמאל עם מרפק" to "Inside Defense Against Left Punch with Elbow",
        "הגנה פנימית נגד מכות ישרות למטה" to "Inside Defense Against Low Straight Punches",
        "הגנה נגד שמאל-ימין - אגרוף מהופך" to "Defense Against Left-Right - Reverse Punch Counter",
        "הגנה נגד שמאל-ימין - הטייה לאחור" to "Defense Against Left-Right - Lean Back",
        "הגנה נגד שמאל-ימין (כמו חיצוניות)" to "Defense Against Left-Right (Like Outside Defenses)",

        "הגנות חיצוניות - הגנה חיצונית מס' 1" to "Outside Defenses - Outside Defense No. 1",
        "הגנות חיצוניות - הגנה חיצונית מס' 2" to "Outside Defenses - Outside Defense No. 2",
        "הגנות חיצוניות - הגנה חיצונית מס' 3" to "Outside Defenses - Outside Defense No. 3",
        "הגנות חיצוניות - הגנה חיצונית מס' 4" to "Outside Defenses - Outside Defense No. 4",
        "הגנות חיצוניות - הגנה חיצונית מס' 5" to "Outside Defenses - Outside Defense No. 5",
        "הגנות חיצוניות - הגנה חיצונית מס' 6" to "Outside Defenses - Outside Defense No. 6",

        "הגנה נגד אגרופים בשכיבה" to "Defense Against Punches on the Ground",


        "הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד שמאל" to "Outside Defense Against High Side Strike - Attacker on Left Side",
        "הגנה חיצונית נגד מכה גבוהה מהצד לעורף - התוקף בצד שמאל" to "Outside Defense Against High Side Strike to the Back of the Neck - Attacker on Left Side",
        "הגנה חיצונית נגד מכה מהצד לגב - התוקף בצד שמאל" to "Outside Defense Against Side Strike to the Back - Attacker on Left Side",
        "הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד ימין" to "Outside Defense Against High Side Strike - Attacker on Right Side",
        "הגנה חיצונית נגד מכה מהצד לגרון - התוקף בצד ימין" to "Outside Defense Against Side Strike to the Throat - Attacker on Right Side",
        "הגנה חיצונית נגד מכה מהצד לבטן - התוקף בצד ימין" to "Outside Defense Against Side Strike to the Abdomen - Attacker on Right Side",

        "הגנה חיצונית נגד בעיטה רגילה" to "Outside Defense Against Regular Kick",
        "הגנה נגד בעיטת ברך" to "Defense Against Knee Strike",
        "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית" to "Defense Against Regular Kick - Stop with Front Leg",
        "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית" to "Defense Against Regular Kick - Stop with Rear Leg",
        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין" to "Outside Defense Against Front Roundhouse Kick - Right Kick Counter",
        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל" to "Outside Defense Against Front Roundhouse Kick - Left Kick Counter",
        "הגנה נגד בעיטת מגל לפנים באמות הידיים" to "Defense Against Front Roundhouse Kick with Forearms",
        "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין" to "Outside Defense Against Front Roundhouse Kick - Right Punch Counter",
        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית" to "Stop Kick Against Roundhouse - Stop with Rear Leg",
        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית" to "Stop Kick Against Roundhouse - Stop with Front Leg",
        "בעיטת עצירה נגד בעיטה לצד" to "Stop Kick Against Side Kick",

        "הגנות יד רפלקסיביות נגד דקירות רגילות" to "Reflexive Hand Defenses Against Regular Stabs",
        "הגנות יד רפלקסיביות נגד דקירות מזרחיות" to "Reflexive Hand Defenses Against Oriental Stabs",
        "הגנות יד רפלקסיביות נגד דקירה ישרה" to "Reflexive Hand Defenses Against Straight Stab",

// ---------------- Hard catalog missing translations ----------------

        "הגנה פנימית נגד אגרוף עם המרפק" to "Inside Defense Against Punch with Elbow Counter",
        "הגנה פנימית נגד ימין באמת שמאל" to "Inside Defense Against Right Punch – Left Stance",
        "הגנה פנימית נגד שמאל באמת שמאל" to "Inside Defense Against Left Punch – Left Stance",
        "הגנה פנימית נגד אגרוף שמאל בהחלפה" to "Inside Defense Against Left Punch with Counter",

        "שחרור מתפיסת ידיים מאחור" to "Release from Two-Hand Grab from Behind",
        "שחרור מתפיסת שיער מאחור" to "Release from Hair Grab from Behind",
        "שחרור מתפיסת חולצה מאחור" to "Release from Shirt Grab from Behind",

        "בעיטת גזירה קדמית" to "Front Scissor Kick",
        "בעיטת גזירה אחורית" to "Rear Scissor Kick",
        "בעיטת גזירה קדמית ובעיטת גזירה אחורית בסיבוב" to "Front and Rear Scissor Kick in Spin",
        "בעיטת גזירה מגב לאחור בסיבוב" to "Back Scissor Kick with Spin",

        "בעיטת סריקה חיצונית בסיבוב" to "Outside Sweep Kick with Spin",
        "בעיטת מגל בניתור" to "Jumping Roundhouse Kick",
        "בעיטת מגל גבוהה ובעיטת מגל בניתור" to "High Roundhouse Kick and Jumping Roundhouse",
        "בעיטת מגל כפולה בניתור" to "Double Roundhouse Kick in Jump",

        "קוואלר - בלימה לאחור" to "Cavalier – Back Block",
        "קוואלר - בלימה לפנים" to "Cavalier – Forward Block",
        "קוואלר - אגרופים" to "Cavalier – Punches",
        "קוואלר - מרפק" to "Cavalier – Elbow"

    )
    fun get(hebrew: String): String? = map[hebrew]

    fun getOrSame(text: String): String = map[text] ?: text
           }


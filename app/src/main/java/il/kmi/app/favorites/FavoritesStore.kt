package il.kmi.app.favorites

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import il.kmi.app.exercises.sync.ExerciseSyncIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

object FavoritesStore {

    private const val PREFS_NAME = "kmi_favorites"
    private const val KEY_SET = "favorites_ids"

    private lateinit var sp: SharedPreferences

    private val _favoritesFlow =
        MutableStateFlow<Set<String>>(emptySet())

    val favoritesFlow: StateFlow<Set<String>> =
        _favoritesFlow.asStateFlow()

    private val _remoteFavoritesFlow =
        MutableStateFlow<Set<String>>(emptySet())

    val remoteFavoritesFlow: StateFlow<Set<String>> =
        _remoteFavoritesFlow.asStateFlow()

    private val syncScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val auth = FirebaseAuth.getInstance()

    private var activeUid: String? = null
    private var remoteFavoritesJob: Job? = null

    private val pendingFavorites =
        mutableMapOf<String, Boolean>()

    private val pendingFavoriteVersions =
        mutableMapOf<String, Long>()

    private var favoriteChangeVersion = 0L

    private var accountGeneration = 0L

    private var favoriteWriteMutex = Mutex()

    private fun publishRemoteFavorites() {
        val result =
            _remoteFavoritesFlow.value.toMutableSet()

        pendingFavorites.forEach { (id, favorite) ->
            if (favorite) {
                result.add(id)
            } else {
                result.remove(id)
            }
        }

        _favoritesFlow.value = result
    }

    private fun registerPendingChange(
        ids: Set<String>,
        favorite: Boolean
    ): Long {
        favoriteChangeVersion++

        val version = favoriteChangeVersion

        ids.forEach { id ->
            pendingFavorites[id] = favorite
            pendingFavoriteVersions[id] = version
        }

        publishRemoteFavorites()

        return version
    }

    private val favoritesAuthListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->

            val uid = firebaseAuth.currentUser?.uid

            if (uid != activeUid) {
                remoteFavoritesJob?.cancel()
                remoteFavoritesJob = null

                activeUid = uid
                accountGeneration++

                _remoteFavoritesFlow.value = emptySet()

                pendingFavorites.clear()
                pendingFavoriteVersions.clear()

                favoriteWriteMutex = Mutex()

                if (uid != null) {
                    _favoritesFlow.value = emptySet()
                } else if (::sp.isInitialized) {
                    _favoritesFlow.value =
                        sp.getStringSet(
                            KEY_SET,
                            emptySet()
                        )
                            .orEmpty()
                            .map(::normalizeFavId)
                            .toSet()
                }

                if (uid != null) {
                    remoteFavoritesJob = syncScope.launch {
                        observeRemoteFavorites(uid)
                            .catch { error ->
                                Log.e(
                                    "KMI_FAVORITES_SYNC",
                                    "Favorites listener failed",
                                    error
                                )
                            }
                            .collect { ids ->
                                if (
                                    auth.currentUser?.uid == uid &&
                                    activeUid == uid
                                ) {
                                    _remoteFavoritesFlow.value = ids

                                    publishRemoteFavorites()

                                    Log.d(
                                        "KMI_FAVORITES_SYNC",
                                        "Remote favorites received: " +
                                                "count=${ids.size}"
                                    )
                                }
                            }
                    }
                }
            }
        }

    // ✅ מזהה יציב: displayName -> ניקוי -> lowercase
    private fun normalizeFavId(raw: String): String =
        il.kmi.shared.questions.model.util.ExerciseTitleFormatter
            .displayName(raw)
            .trim()
            .replace("\u200F", "") // RLM
            .replace("\u200E", "") // LRM
            .replace("\u00A0", " ") // NBSP
            .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

    fun init(context: Context) {
        if (::sp.isInitialized) return

        sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = sp.getStringSet(KEY_SET, emptySet()) ?: emptySet()

        // ✅ MIGRATION: מנרמלים את מה שכבר נשמר כדי שיתאים לכל הרשימות החדשות
        val normalized = loaded.map { normalizeFavId(it) }.toSet()

        _favoritesFlow.value = normalized

        if (normalized != loaded) {
            sp.edit().putStringSet(KEY_SET, normalized).apply()
        }

        auth.addAuthStateListener(favoritesAuthListener)
    }

    fun isFavorite(id: String): Boolean =
        _favoritesFlow.value.contains(normalizeFavId(id))

    private fun favoriteIdentityIds(
        identity: ExerciseSyncIdentity
    ): Set<String> {
        val primaryId =
            normalizeFavId(identity.exerciseId)

        if (primaryId.isBlank()) {
            return emptySet()
        }

        return buildSet {
            add(primaryId)

            identity.aliases.forEach { alias ->
                val normalized = normalizeFavId(alias)

                if (
                    normalized.startsWith("ex_") ||
                    normalized.startsWith("legacy_")
                ) {
                    add(normalized)
                }
            }
        }
    }

    fun isFavorite(identity: ExerciseSyncIdentity): Boolean {
        val ids = favoriteIdentityIds(identity)
        return ids.any { it in _favoritesFlow.value }
    }

    private fun changeLocalFavorites(
        ids: Set<String>,
        favorite: Boolean
    ) {
        val next = _favoritesFlow.value.toMutableSet()

        if (favorite) {
            next.addAll(ids)
        } else {
            next.removeAll(ids)
        }

        _favoritesFlow.value = next

        if (::sp.isInitialized) {
            sp.edit()
                .putStringSet(KEY_SET, next)
                .apply()
        }
    }

    private fun changeFavorites(
        ids: Set<String>,
        favorite: Boolean
    ) {
        val normalizedIds =
            ids.map(::normalizeFavId)
                .filter { it.isNotBlank() }
                .toSet()

        if (normalizedIds.isEmpty()) return

        val uid = auth.currentUser?.uid

        if (uid == null) {
            changeLocalFavorites(normalizedIds, favorite)
            return
        }

        if (activeUid != uid) {
            Log.w(
                "KMI_FAVORITES_SYNC",
                "Favorite change skipped: account not ready"
            )
            return
        }

        val version =
            registerPendingChange(normalizedIds, favorite)

        val generation = accountGeneration
        val writeMutex = favoriteWriteMutex

        syncScope.launch {
            writeMutex.withLock {
                if (
                    auth.currentUser?.uid != uid ||
                    activeUid != uid ||
                    accountGeneration != generation
                ) {
                    return@withLock
                }

                val currentIds = normalizedIds.filter { id ->
                    pendingFavoriteVersions[id] == version
                }.toSet()

                if (currentIds.isEmpty()) {
                    return@withLock
                }

                val result = runCatching {
                    setRemoteFavorites(
                        uid = uid,
                        exerciseIds = currentIds,
                        favorite = favorite
                    )
                }

                if (result.isSuccess) {
                    Log.d(
                        "KMI_FAVORITES_SYNC",
                        "Favorite saved: ids=$currentIds favorite=$favorite"
                    )
                } else {
                    Log.e(
                        "KMI_FAVORITES_SYNC",
                        "Favorite sync failed: ids=$currentIds",
                        result.exceptionOrNull()
                    )
                }

                if (
                    auth.currentUser?.uid == uid &&
                    activeUid == uid &&
                    accountGeneration == generation
                ) {
                    currentIds.forEach { id ->
                        if (
                            pendingFavoriteVersions[id] == version
                        ) {
                            pendingFavoriteVersions.remove(id)
                            pendingFavorites.remove(id)
                        }
                    }

                    publishRemoteFavorites()
                }
            }
        }
    }

    fun toggle(identity: ExerciseSyncIdentity) {
        val primaryId =
            normalizeFavId(identity.exerciseId)

        if (primaryId.isBlank()) return

        val matchingIds =
            favoriteIdentityIds(identity)
                .filter { it in _favoritesFlow.value }
                .toSet()

        if (matchingIds.isEmpty()) {
            changeFavorites(
                ids = setOf(primaryId),
                favorite = true
            )
        } else {
            changeFavorites(
                ids = matchingIds,
                favorite = false
            )
        }
    }

    fun toggle(id: String) {
        val normalizedId = normalizeFavId(id)

        if (normalizedId.isBlank()) return

        val shouldAdd =
            normalizedId !in _favoritesFlow.value

        changeFavorites(
            ids = setOf(normalizedId),
            favorite = shouldAdd
        )
    }

    fun clearAll() {
        val ids = _favoritesFlow.value

        if (auth.currentUser?.uid != null) {
            changeFavorites(
                ids = ids,
                favorite = false
            )
            return
        }

        _favoritesFlow.value = emptySet()

        if (::sp.isInitialized) {
            sp.edit()
                .remove(KEY_SET)
                .apply()
        }
    }

    private fun remoteFavoritesDocument(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("favorites")
            .document("state")

    fun observeRemoteFavorites(
        uid: String
    ): Flow<Set<String>> = callbackFlow {

        check(FirebaseAuth.getInstance().currentUser?.uid == uid) {
            "Favorites account changed"
        }

        val registration =
            remoteFavoritesDocument(uid)
                .addSnapshotListener(
                    MetadataChanges.INCLUDE
                ) { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (
                        FirebaseAuth.getInstance().currentUser?.uid != uid
                    ) {
                        return@addSnapshotListener
                    }

                    if (
                        snapshot?.metadata?.hasPendingWrites() == true
                    ) {
                        return@addSnapshotListener
                    }

                    val ids =
                        (snapshot?.get("ids") as? List<*>)
                            .orEmpty()
                            .mapNotNull { it as? String }
                            .map(::normalizeFavId)
                            .filter { it.isNotBlank() }
                            .toSet()

                    trySend(ids)
                }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun setRemoteFavorite(
        uid: String,
        exerciseId: String,
        favorite: Boolean
    ) {
        setRemoteFavorites(
            uid = uid,
            exerciseIds = setOf(exerciseId),
            favorite = favorite
        )
    }

    suspend fun setRemoteFavorites(
        uid: String,
        exerciseIds: Set<String>,
        favorite: Boolean
    ) {
        check(auth.currentUser?.uid == uid) {
            "Favorites account changed"
        }

        val normalizedIds =
            exerciseIds
                .map(::normalizeFavId)
                .filter { it.isNotBlank() }
                .distinct()

        require(normalizedIds.isNotEmpty()) {
            "No valid favorite exercise IDs"
        }

        require(normalizedIds.size <= 1000) {
            "Too many favorite exercise IDs"
        }

        val operation =
            if (favorite) {
                FieldValue.arrayUnion(
                    *normalizedIds.toTypedArray()
                )
            } else {
                FieldValue.arrayRemove(
                    *normalizedIds.toTypedArray()
                )
            }

        remoteFavoritesDocument(uid)
            .set(
                mapOf(
                    "ids" to operation,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }
}

package il.kmi.app.messages.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import il.kmi.app.messages.model.MessageCenterItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MessageCenterViewModel : ViewModel() {

    private val auth =
        FirebaseAuth.getInstance()

    private val db =
        FirebaseFirestore.getInstance()

    private val _messages =
        MutableStateFlow<List<MessageCenterItem>>(
            emptyList()
        )

    val messages: StateFlow<List<MessageCenterItem>> =
        _messages.asStateFlow()

    private val _unreadCount =
        MutableStateFlow(0)

    val unreadCount: StateFlow<Int> =
        _unreadCount.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private var listenerRegistration:
            ListenerRegistration? = null

    private var listeningUid: String? = null

    fun startListening() {
        val uid =
            auth.currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            stopListening()

            _messages.value = emptyList()
            _unreadCount.value = 0
            _isLoading.value = false
            _errorMessage.value =
                "No logged-in user"

            return
        }

        if (
            listenerRegistration != null &&
            listeningUid == uid
        ) {
            return
        }

        stopListening()

        listeningUid = uid

        _isLoading.value = true
        _errorMessage.value = null

        listenerRegistration =
            db
                .collectionGroup(
                    "recipients"
                )
                .whereEqualTo(
                    "uid",
                    uid
                )
                .addSnapshotListener {
                        snapshot,
                        error ->

                    if (error != null) {
                        _isLoading.value = false
                        _errorMessage.value =
                            error.message
                                ?: error.javaClass.simpleName
                        return@addSnapshotListener
                    }

                    val items =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { document ->

                                val deleted =
                                    document
                                        .getBoolean(
                                            "deleted"
                                        )
                                        ?: false

                                if (deleted) {
                                    return@mapNotNull null
                                }

                                val storedBroadcastId =
                                    document
                                        .getString(
                                            "broadcastId"
                                        )
                                        ?.trim()
                                        .orEmpty()

                                val parentBroadcastId =
                                    document
                                        .reference
                                        .parent
                                        .parent
                                        ?.id
                                        ?.trim()
                                        .orEmpty()

                                val id =
                                    storedBroadcastId
                                        .ifBlank {
                                            parentBroadcastId
                                        }

                                if (id.isBlank()) {
                                    return@mapNotNull null
                                }

                                val titleHe =
                                    document
                                        .getString(
                                            "titleHe"
                                        )
                                        ?.trim()
                                        .orEmpty()

                                val titleEn =
                                    document
                                        .getString(
                                            "titleEn"
                                        )
                                        ?.trim()
                                        .orEmpty()

                                val message =
                                    (
                                            document
                                                .getString(
                                                    "message"
                                                )
                                                ?: document
                                                    .getString(
                                                        "text"
                                                    )
                                                ?: document
                                                    .getString(
                                                        "body"
                                                    )
                                                ?: ""
                                            )
                                        .trim()

                                val createdAtMillis =
                                    document
                                        .getLong(
                                            "createdAtMillis"
                                        )
                                        ?: 0L

                                val read =
                                    document
                                        .getBoolean(
                                            "read"
                                        )
                                        ?: false

                                val senderNameHe =
                                    document
                                        .getString(
                                            "senderNameHe"
                                        )
                                        ?.trim()
                                        .orEmpty()
                                        .ifBlank {
                                            "צוות ק.מ.י"
                                        }

                                val senderNameEn =
                                    document
                                        .getString(
                                            "senderNameEn"
                                        )
                                        ?.trim()
                                        .orEmpty()
                                        .ifBlank {
                                            "K.M.I Team"
                                        }

                                val region =
                                    document
                                        .getString(
                                            "region"
                                        )
                                        ?.trim()
                                        .orEmpty()

                                val branch =
                                    document
                                        .getString(
                                            "branch"
                                        )
                                        ?.trim()
                                        .orEmpty()

                                val groups =
                                    (
                                            document
                                                .get(
                                                    "groups"
                                                ) as? List<*>
                                            )
                                        ?.mapNotNull {
                                            it
                                                ?.toString()
                                                ?.trim()
                                                ?.takeIf {
                                                        value ->
                                                    value
                                                        .isNotBlank()
                                                }
                                        }
                                        .orEmpty()

                                MessageCenterItem(
                                    id = id,
                                    titleHe = titleHe,
                                    titleEn = titleEn,
                                    message = message,
                                    createdAtMillis =
                                        createdAtMillis,
                                    read = read,
                                    deleted = false,
                                    senderNameHe =
                                        senderNameHe,
                                    senderNameEn =
                                        senderNameEn,
                                    region = region,
                                    branch = branch,
                                    groups = groups
                                )
                            }
                            .sortedByDescending { message ->
                                message.createdAtMillis
                            }

                    _messages.value = items
                    _unreadCount.value =
                        items.count { message ->
                            !message.read
                        }

                    _isLoading.value = false
                    _errorMessage.value = null
                }
    }

    fun markAsRead(
        message: MessageCenterItem
    ) {
        if (message.read) {
            return
        }

        val uid =
            auth.currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            return
        }

        val broadcastId =
            message.id.trim()

        if (broadcastId.isBlank()) {
            return
        }

        db
            .collection("coachBroadcasts")
            .document(broadcastId)
            .collection("recipients")
            .document(uid)
            .update(
                mapOf(
                    "read" to true,
                    "readAt" to
                            com.google.firebase.firestore.FieldValue
                                .serverTimestamp()
                )
            )
            .addOnFailureListener { error ->
                _errorMessage.value =
                    error.message
                        ?: error.javaClass.simpleName
            }
    }

    fun deleteMessage(
        message: MessageCenterItem
    ) {
        val uid =
            auth.currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            return
        }

        val broadcastId =
            message.id.trim()

        if (broadcastId.isBlank()) {
            return
        }

        db
            .collection("coachBroadcasts")
            .document(broadcastId)
            .collection("recipients")
            .document(uid)
            .update(
                mapOf(
                    "deleted" to true,
                    "deletedAt" to
                            com.google.firebase.firestore.FieldValue
                                .serverTimestamp()
                )
            )
            .addOnFailureListener { error ->
                _errorMessage.value =
                    error.message
                        ?: error.javaClass.simpleName
            }
    }

    fun stopListening() {
        listenerRegistration
            ?.remove()

        listenerRegistration = null
        listeningUid = null
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
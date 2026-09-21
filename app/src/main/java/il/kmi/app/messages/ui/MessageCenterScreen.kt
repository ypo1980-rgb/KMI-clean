package il.kmi.app.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import il.kmi.app.messages.model.MessageCenterItem
import il.kmi.app.ui.KmiTopBar
import androidx.compose.runtime.LaunchedEffect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import il.yuval.ui.theme.kmiSectionHeaderBackground

@Composable
fun MessageCenterScreen(
    isEnglish: Boolean,
    onBack: () -> Unit,
    onMessageClick: (MessageCenterItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageCenterViewModel = viewModel()
) {
    val messages by
    viewModel.messages.collectAsState()

    val isLoading by
    viewModel.isLoading.collectAsState()

    val errorMessage by
    viewModel.errorMessage.collectAsState()

    var selectedTabIndex by
    remember {
        mutableStateOf(0)
    }

    val unreadCount =
        messages.count { message ->
            !message.read
        }

    val visibleMessages =
        remember(
            messages,
            selectedTabIndex
        ) {
            when (selectedTabIndex) {
                1 ->
                    messages.filter { message ->
                        !message.read
                    }

                else ->
                    messages
            }
        }

    var selectedMessage by
    remember {
        mutableStateOf<MessageCenterItem?>(null)
    }

    var messagePendingDelete by
    remember {
        mutableStateOf<MessageCenterItem?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    val layoutDirection: LayoutDirection =
        if (isEnglish) {
            Ltr
        } else {
            Rtl
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val messageDateTimeFormatter =
        remember {
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy · HH:mm"
            )
        }

    val jerusalemZone =
        remember {
            ZoneId.of("Asia/Jerusalem")
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                KmiTopBar(
                    title =
                        if (isEnglish) {
                            "Messages"
                        } else {
                            "הודעות"
                        },
                    onBack = onBack,
                    showTopHome = false,
                    showTopShare = false
                )
            }
        ) { paddingValues ->

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
            ) {

                MessageCenterTabs(
                    selectedIndex =
                        selectedTabIndex,
                    unreadCount =
                        unreadCount,
                    isEnglish =
                        isEnglish,
                    onSelected = { index ->
                        selectedTabIndex = index
                    }
                )

                if (isLoading) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Loading messages..."
                                } else {
                                    "טוען הודעות..."
                                },
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Unable to load messages"
                                    } else {
                                        "לא ניתן לטעון את ההודעות"
                                    },
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    errorMessage.orEmpty(),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                } else if (visibleMessages.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (selectedTabIndex == 1) {
                                    if (isEnglish) {
                                        "No unread messages"
                                    } else {
                                        "אין הודעות שלא נקראו"
                                    }
                                } else {
                                    if (isEnglish) {
                                        "No messages yet"
                                    } else {
                                        "אין הודעות עדיין"
                                    }
                                },
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                top = 10.dp,
                                bottom = 24.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = visibleMessages,
                            key = { item -> item.id }
                        ) { item ->

                            val title =
                                if (isEnglish) {
                                    item.titleEn.ifBlank {
                                        "K.M.I Team"
                                    }
                                } else {
                                    item.titleHe.ifBlank {
                                        "צוות ק.מ.י"
                                    }
                                }

                            val dateTimeText =
                                if (item.createdAtMillis > 0L) {
                                    Instant
                                        .ofEpochMilli(
                                            item.createdAtMillis
                                        )
                                        .atZone(
                                            jerusalemZone
                                        )
                                        .format(
                                            messageDateTimeFormatter
                                        )
                                } else {
                                    ""
                                }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp
                                        )
                                        .background(
                                            color =
                                                if (item.read) {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .surface
                                                } else {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primaryContainer
                                                        .copy(alpha = 0.72f)
                                                },
                                            shape =
                                                RoundedCornerShape(
                                                    18.dp
                                                )
                                        )
                                        .border(
                                            width =
                                                if (item.read) {
                                                    1.dp
                                                } else {
                                                    1.4.dp
                                                },
                                            color =
                                                if (item.read) {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .outlineVariant
                                                } else {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary
                                                        .copy(alpha = 0.55f)
                                                },
                                            shape =
                                                RoundedCornerShape(
                                                    18.dp
                                                )
                                        )
                                        .clickable {
                                            viewModel.markAsRead(item)
                                            selectedMessage = item
                                            onMessageClick(item)
                                        }
                                        .padding(
                                            horizontal = 15.dp,
                                            vertical = 13.dp
                                        ),
                                verticalAlignment =
                                    Alignment.Top
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(1f),
                                    horizontalAlignment =
                                        if (isEnglish) {
                                            Alignment.Start
                                        } else {
                                            Alignment.End
                                        }
                                ) {
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            modifier =
                                                Modifier.weight(1f),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            fontWeight =
                                                if (item.read) {
                                                    FontWeight.Medium
                                                } else {
                                                    FontWeight.ExtraBold
                                                },
                                            textAlign = textAlign,
                                            maxLines = 2
                                        )

                                        if (!item.read) {
                                            Spacer(
                                                modifier =
                                                    Modifier.size(6.dp)
                                            )

                                            Surface(
                                                shape =
                                                    RoundedCornerShape(8.dp),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary
                                                        .copy(alpha = 0.12f),
                                                border =
                                                    androidx.compose.foundation.BorderStroke(
                                                        width = 1.dp,
                                                        color =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .primary
                                                                .copy(alpha = 0.35f)
                                                    ),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp
                                            ) {
                                                Text(
                                                    text =
                                                        if (isEnglish) {
                                                            "NEW"
                                                        } else {
                                                            "חדש"
                                                        },
                                                    modifier =
                                                        Modifier.padding(
                                                            horizontal = 7.dp,
                                                            vertical = 2.dp
                                                        ),
                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary,
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .labelSmall,
                                                    fontWeight =
                                                        FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(
                                            modifier =
                                                Modifier.size(6.dp)
                                        )

                                        Surface(
                                            onClick = {
                                                messagePendingDelete = item
                                            },
                                            modifier =
                                                Modifier.size(30.dp),
                                            shape = CircleShape,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant
                                                    .copy(alpha = 0.72f),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier.fillMaxSize(),
                                                contentAlignment =
                                                    Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Outlined.DeleteOutline,
                                                    contentDescription =
                                                        if (isEnglish) {
                                                            "Delete message"
                                                        } else {
                                                            "מחיקת הודעה"
                                                        },
                                                    tint =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurfaceVariant,
                                                    modifier =
                                                        Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (dateTimeText.isNotBlank()) {
                                        Spacer(
                                            modifier =
                                                Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = dateTimeText,
                                            modifier =
                                                Modifier.fillMaxWidth(),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant,
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelSmall,
                                            textAlign = textAlign
                                        )
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = item.message,
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant,
                                        textAlign = textAlign,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        messagePendingDelete?.let { message ->
            AlertDialog(
                onDismissRequest = {
                    messagePendingDelete = null
                },
                title = {
                    Text(
                        text =
                            if (isEnglish) {
                                "Delete message?"
                            } else {
                                "למחוק את ההודעה?"
                            },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text =
                            if (isEnglish) {
                                "This message will be removed from your message center."
                            } else {
                                "ההודעה תוסר ממרכז ההודעות שלך."
                            }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(message)
                            messagePendingDelete = null
                        }
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Delete"
                                } else {
                                    "מחיקה"
                                },
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            messagePendingDelete = null
                        }
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Cancel"
                                } else {
                                    "ביטול"
                                }
                        )
                    }
                }
            )
        }

        selectedMessage?.let { message ->
            MessageDetailDialog(
                message = message,
                isEnglish = isEnglish,
                onDismiss = {
                    selectedMessage = null
                }
            )
        }
    }
}

@Composable
private fun MessageCenterTabs(
    selectedIndex: Int,
    unreadCount: Int,
    isEnglish: Boolean,
    onSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 2.dp,
                bottom = 8.dp
            ),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .kmiSectionHeaderBackground()
                .border(
                    width = 1.dp,
                    color =
                        Color.White.copy(
                            alpha = 0.34f
                        ),
                    shape = RectangleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-4).dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(
                        Color.White.copy(
                            alpha = 0.65f
                        )
                    )
            )

            CompositionLocalProvider(
                LocalLayoutDirection provides
                        if (isEnglish) {
                            LayoutDirection.Ltr
                        } else {
                            LayoutDirection.Rtl
                        }
            ) {
                TabRow(
                    selectedTabIndex =
                        selectedIndex,
                    containerColor =
                        Color.Transparent,
                    contentColor =
                        Color.White,
                    divider = {},
                    indicator = {},
                    modifier =
                        Modifier
                            .matchParentSize()
                            .padding(
                                horizontal = 28.dp
                            )
                ) {
                    Tab(
                        selected =
                            selectedIndex == 0,
                        onClick = {
                            onSelected(0)
                        },
                        text = {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "All messages"
                                        } else {
                                            "כל ההודעות"
                                        },
                                    color =
                                        Color.White,
                                    fontWeight =
                                        if (
                                            selectedIndex == 0
                                        ) {
                                            FontWeight.ExtraBold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(3.dp)
                                )

                                Box(
                                    modifier =
                                        Modifier
                                            .width(34.dp)
                                            .height(2.dp)
                                            .background(
                                                color =
                                                    if (
                                                        selectedIndex == 0
                                                    ) {
                                                        Color.White
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                shape =
                                                    RoundedCornerShape(
                                                        50
                                                    )
                                            )
                                )
                            }
                        }
                    )

                    Tab(
                        selected =
                            selectedIndex == 1,
                        onClick = {
                            onSelected(1)
                        },
                        text = {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            if (unreadCount > 0) {
                                                "Unread ($unreadCount)"
                                            } else {
                                                "Unread"
                                            }
                                        } else {
                                            if (unreadCount > 0) {
                                                "לא נקראו ($unreadCount)"
                                            } else {
                                                "לא נקראו"
                                            }
                                        },
                                    color =
                                        Color.White,
                                    fontWeight =
                                        if (
                                            selectedIndex == 1
                                        ) {
                                            FontWeight.ExtraBold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(3.dp)
                                )

                                Box(
                                    modifier =
                                        Modifier
                                            .width(34.dp)
                                            .height(2.dp)
                                            .background(
                                                color =
                                                    if (
                                                        selectedIndex == 1
                                                    ) {
                                                        Color.White
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                shape =
                                                    RoundedCornerShape(
                                                        50
                                                    )
                                            )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

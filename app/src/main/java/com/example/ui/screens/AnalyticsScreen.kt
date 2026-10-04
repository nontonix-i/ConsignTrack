package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.OpenAiClient
import com.example.ui.components.MarkdownContent
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.ChatMessage
import kotlin.math.roundToInt

/**
 * Floating Customer-Service style AI Chat Bubble at bottom-right.
 * Draggable so it never blocks underlying UI elements.
 */
@Composable
fun FloatingAiCsButton(
    isEnglish: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0F3929), CharcoalSurfaceElevated)
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .border(1.5.dp, SupabaseGreen, RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("floating_ai_chat_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(SupabaseGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "AI Chat",
                        tint = Color(0xFF042114),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Online indicator dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(1.5.dp, CharcoalSurface, CircleShape)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnglish) "AI Assist" else "Tanya AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = if (isEnglish) "Online 24/7" else "Analisis Bisnis",
                    fontSize = 9.5.sp,
                    color = SupabaseGreen
                )
            }
        }
    }
}

/**
 * Floating CS-style Live AI Chat Modal Window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingAiChatDialog(
    viewModel: AnalyticsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEn = uiState.language == AppLanguage.EN
    val listState = rememberLazyListState()

    val presetQuestions = if (isEn) {
        if (uiState.agentModeEnabled) {
            listOf(
                "Tambah warung Toko Maju di Jl. Merdeka",
                "Tambah produk Keripik Singkong harga 18000",
                "Set stok Kerupuk Udang di Warung Bu Siti 45 pcs",
                "Catat pengeluaran bensin 25000",
                "Top selling store?",
                "Current net profit?"
            )
        } else {
            listOf(
                "Top selling store?",
                "Total consigned stock?",
                "Current net profit?",
                "Unvisited stores today?",
                "Highest margin product?"
            )
        }
    } else {
        if (uiState.agentModeEnabled) {
            listOf(
                "Tambah warung Toko Maju di Jl. Merdeka",
                "Tambah produk Keripik Singkong harga 18000",
                "Set stok Kerupuk Udang di Warung Bu Siti 45 pcs",
                "Catat pengeluaran bensin 25000",
                "Toko terlaris?",
                "Laba bersih saat ini?"
            )
        } else {
            listOf(
                "Toko terlaris?",
                "Total stok tersebar?",
                "Laba bersih saat ini?",
                "Warung belum dikunjungi?",
                "Margin produk tertinggi?"
            )
        }
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(uiState.messages.size, uiState.isAnalyzing, isKeyboardOpen) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isKeyboardOpen) 4.dp else 24.dp)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .border(
                    width = 1.dp,
                    color = CharcoalBorder,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = CharcoalBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CS Chat Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SupabaseGreen.copy(alpha = 0.18f), CircleShape)
                                    .border(1.dp, SupabaseGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(SupabaseGreen, CircleShape)
                                    .border(1.5.dp, CharcoalSurface, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEn) "ConsignTrack AI Assistant" else "Asisten AI ConsignTrack",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = if (uiState.agentModeEnabled) {
                                    if (isEn) "Agent Mode • Add, Edit & Erase Tools" else "Agent Mode Aktif • Bisa Tambah, Edit & Hapus"
                                } else {
                                    if (isEn) "Online • Real-time Business Data" else "Online • Analisis Data Bisnis Real-time"
                                },
                                fontSize = 10.5.sp,
                                color = SupabaseGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setAgentModeEnabled(!uiState.agentModeEnabled) }
                                .border(
                                    1.dp,
                                    if (uiState.agentModeEnabled) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.agentModeEnabled) SupabaseGreen.copy(alpha = 0.16f) else CharcoalSurfaceElevated
                        ) {
                            Text(
                                text = if (uiState.agentModeEnabled) "⚡ AGENT ON" else "AGENT OFF",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.agentModeEnabled) SupabaseGreen else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = if (isEn) "Reset Chat" else "Bersihkan Chat",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (isEn) "Close" else "Tutup",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

                // Chat Messages List (Automatically shrinks when keyboard opens!)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages) { msg ->
                        ChatBubble(message = msg)
                    }

                    if (uiState.isAnalyzing) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SupabaseGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isEn) "Analyzing business data..." else "Menganalisis data bisnis...",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }

                // Bottom Quick Chips + Compact Input Bar (Never overflows or gets covered by keyboard)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder)
                        .padding(top = 6.dp, bottom = 8.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetQuestions) { q ->
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.askQuestion(q) }
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                color = CharcoalSurfaceElevated
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp, max = 96.dp)
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(21.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(21.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.currentInput.isEmpty()) {
                                Text(
                                    text = if (isEn) "Type your question here..." else "Ketik pertanyaan di sini...",
                                    fontSize = 13.sp,
                                    color = TextMutedDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = uiState.currentInput,
                                onValueChange = { viewModel.updateInput(it) },
                                textStyle = TextStyle(
                                    color = TextPrimaryDark,
                                    fontSize = 13.5.sp
                                ),
                                cursorBrush = SolidColor(SupabaseGreen),
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (uiState.currentInput.isNotBlank()) {
                                            viewModel.askQuestion(uiState.currentInput)
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(SupabaseGreen, CircleShape)
                                .clickable {
                                    if (uiState.currentInput.isNotBlank()) {
                                        viewModel.askQuestion(uiState.currentInput)
                                        focusManager.clearFocus()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isEn) "Send" else "Kirim",
                                tint = Color(0xFF042114),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Navigation & Settings Menu Drawer Content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuDrawerSheet(
    viewModel: AnalyticsViewModel,
    onNavigateTab: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEn = uiState.language == AppLanguage.EN
    var printerDropdownExpanded by remember { mutableStateOf(false) }
    var showEditBusinessDialog by remember { mutableStateOf(false) }
    var showEditAiConfigDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = CharcoalBg,
        drawerContentColor = TextPrimaryDark,
        modifier = Modifier.fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drawer Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEn) "Menu & Settings" else "Menu & Pengaturan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "ConsignTrack v1.0",
                        fontSize = 11.sp,
                        color = SupabaseGreen
                    )
                }
                IconButton(onClick = onCloseDrawer, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark)
                }
            }

            // 1. Business Profile Card (For Thermal Receipt Header)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Receipt Business Profile" else "Profil Usaha (Header Nota)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                        TextButton(
                            onClick = { showEditBusinessDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = SupabaseGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEn) "Edit" else "Ubah", fontSize = 11.5.sp, color = SupabaseGreen)
                        }
                    }

                    Text(
                        text = uiState.businessName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${uiState.businessAddress} • ${uiState.businessPhone}",
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Quick Shortcuts Section (Includes Graphics & Chart Analysis in Drawer only)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isEn) "ANALYTICS & NAVIGATION" else "ANALISIS GRAFIK & PINTASAN MENU",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextMutedDark
                )

                // Featured Modern Graphics & Chart Analysis Menu Banner (Drawer-exclusive)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigateTab("GRAPHICS")
                            onCloseDrawer()
                        }
                        .border(1.2.dp, SupabaseGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .testTag("drawer_graphics_analysis_menu"),
                    shape = RoundedCornerShape(12.dp),
                    color = CharcoalSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SupabaseGreen.copy(alpha = 0.16f),
                                        BlueInfo.copy(alpha = 0.08f),
                                         Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SupabaseGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SupabaseGreen, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isEn) "Graphics & Chart Analysis" else "Analisis Grafik & Chart",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(SupabaseGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "CHART",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF042114)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isEn) "Interactive revenue, stock & store charts" else "Visualisasi tren omset, stok produk & ranking warung",
                                    fontSize = 10.5.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutTile(
                        title = if (isEn) "Routes" else "Rute Toko",
                        icon = Icons.Default.Store,
                        accent = SupabaseGreen,
                        onClick = {
                            onNavigateTab("ROUTES")
                            onCloseDrawer()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutTile(
                        title = if (isEn) "Products" else "Produk",
                        icon = Icons.Default.Inventory2,
                        accent = BlueInfo,
                        onClick = {
                            onNavigateTab("PRODUCTS")
                            onCloseDrawer()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutTile(
                        title = if (isEn) "Cashbook" else "Buku Kas",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = AmberWarning,
                        onClick = {
                            onNavigateTab("FINANCE")
                            onCloseDrawer()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutTile(
                        title = if (isEn) "AI CS Chat" else "Tanya AI CS",
                        icon = Icons.Default.SupportAgent,
                        accent = SupabaseGreen,
                        onClick = {
                            onCloseDrawer()
                            onOpenAiChat()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2.5 Backup & Restore (.ZIP + Photos) Card
            BackupRestoreDrawerCard(viewModel = viewModel)

            // 3. General App Settings (Language, Theme, GPS, Floating AI)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEn) "APP SETTINGS" else "PENGATURAN APLIKASI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SupabaseGreen
                    )

                    // Theme Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Theme Mode" else "Tema Tampilan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                val selected = uiState.themeMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .border(
                                            1.dp,
                                            if (selected) SupabaseGreen else CharcoalBorder,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isEn) mode.labelEn else mode.labelId,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) SupabaseGreen else TextSecondaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

                    // Language Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Language" else "Bahasa Aplikasi",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = uiState.language == lang
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setLanguage(lang) }
                                        .border(
                                            1.dp,
                                            if (selected) SupabaseGreen else CharcoalBorder,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) SupabaseGreen else TextSecondaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

                    // Toggle Floating AI CS Chat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isEn) "Floating AI CS Button" else "Tombol Floating AI Chat",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isEn) "Show floating chat at bottom-right" else "Tampilkan tombol CS AI di kanan bawah",
                                    fontSize = 10.5.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Switch(
                            checked = uiState.showFloatingAiChat,
                            onCheckedChange = { viewModel.setShowFloatingAiChat(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042114),
                                checkedTrackColor = SupabaseGreen,
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = CharcoalSurfaceElevated
                            )
                        )
                    }

                    HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

                    // Toggle Auto-sort GPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = BlueInfo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isEn) "Auto-Sort Nearest GPS" else "Urutkan Warung Terdekat",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isEn) "Prioritize closest outlets on route" else "Otomatis urutkan toko terdekat dari GPS",
                                    fontSize = 10.5.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Switch(
                            checked = uiState.autoSortNearestGps,
                            onCheckedChange = { viewModel.setAutoSortNearestGps(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF042114),
                                checkedTrackColor = SupabaseGreen,
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = CharcoalSurfaceElevated
                            )
                        )
                    }
                }
            }

            // 3.5 AI API Configuration Card (Domain / Base URL, API Key & Model)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "AI API & DOMAIN CONFIG" else "KONFIGURASI API & DOMAIN AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = SupabaseGreen
                            )
                        }
                        TextButton(
                            onClick = { showEditAiConfigDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("btn_edit_ai_api_config")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = SupabaseGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEn) "Configure" else "Atur API", fontSize = 11.5.sp, color = SupabaseGreen)
                        }
                    }

                    Text(
                        text = "Domain: ${uiState.aiBaseUrl}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val maskedKey = remember(uiState.aiApiKey) {
                        val k = uiState.aiApiKey.trim()
                        when {
                            k.isEmpty() -> if (isEn) "Not set" else "Belum diisi"
                            k.length <= 10 -> "••••••••"
                            else -> "${k.take(7)}••••${k.takeLast(4)}"
                        }
                    }

                    Text(
                        text = "Key: $maskedKey • Model: ${uiState.aiModelName}" +
                                if (uiState.geminiApiKey.isNotBlank()) " • Gemini Aktif" else "",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Thermal Printer Configuration Card
            PrinterConfigCard(
                pairedPrinters = uiState.pairedPrinters,
                selectedPrinter = uiState.selectedPrinter,
                isPrintingTest = uiState.isPrintingTest,
                testPrintStatus = uiState.testPrintStatus,
                defaultPaper80mm = uiState.defaultPaper80mm,
                onDefaultPaperChange = { viewModel.setDefaultPaper80mm(it) },
                printerDropdownExpanded = printerDropdownExpanded,
                onDropdownExpandedChange = { printerDropdownExpanded = it },
                onSelectPrinter = { viewModel.setSelectedPrinter(it) },
                onRefreshPrinters = { viewModel.loadPrinters() },
                onPrintTest = { is80mm -> viewModel.printTestReceipt(is80mm) }
            )
        }
    }

    if (showEditBusinessDialog) {
        var nameInput by remember { mutableStateOf(uiState.businessName) }
        var addrInput by remember { mutableStateOf(uiState.businessAddress) }
        var phoneInput by remember { mutableStateOf(uiState.businessPhone) }

        AlertDialog(
            onDismissRequest = { showEditBusinessDialog = false },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = if (isEn) "Edit Receipt Header" else "Ubah Profil Nota Usaha",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (isEn) "Business Name" else "Nama Usaha / Distributor") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                    OutlinedTextField(
                        value = addrInput,
                        onValueChange = { addrInput = it },
                        label = { Text(if (isEn) "Address / Tagline" else "Alamat / Subjudul Nota") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(if (isEn) "Phone / WhatsApp" else "No. Telepon / WhatsApp") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessProfile(nameInput, addrInput, phoneInput)
                        showEditBusinessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
                ) {
                    Text(if (isEn) "Save" else "Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditBusinessDialog = false }) {
                    Text(if (isEn) "Cancel" else "Batal", color = TextSecondaryDark)
                }
            }
        )
    }

    if (showEditAiConfigDialog) {
        var baseUrlInput by remember { mutableStateOf(uiState.aiBaseUrl) }
        var apiKeyInput by remember { mutableStateOf(uiState.aiApiKey) }
        var modelInput by remember { mutableStateOf(uiState.aiModelName) }
        var geminiKeyInput by remember { mutableStateOf(uiState.geminiApiKey) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showEditAiConfigDialog = false },
            containerColor = CharcoalSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "AI API & Domain Settings" else "Pengaturan Domain & API Key",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    TextButton(
                        onClick = {
                            baseUrlInput = OpenAiClient.DEFAULT_BASE_URL
                            apiKeyInput = OpenAiClient.DEFAULT_API_KEY
                            modelInput = OpenAiClient.DEFAULT_MODEL
                            geminiKeyInput = ""
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Reset Default",
                            fontSize = 11.sp,
                            color = AmberWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEn) {
                            "Stored locally in app settings so the project builds without requiring any .env variables."
                        } else {
                            "Tersimpan langsung di Pengaturan Aplikasi sehingga project dapat di-build tanpa perlu set variabel .env."
                        },
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text(if (isEn) "API Domain / Base URL" else "Domain / Base URL API") },
                        placeholder = { Text("https://ai.drakor.pp.ua/v1/") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_base_url"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(if (isEn) "API Key (Bearer Token)" else "API Key (OpenAI / Custom)") },
                        placeholder = { Text("sk-... / freellmapi-...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_api_key"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text(if (isEn) "Model Name" else "Nama Model AI") },
                        placeholder = { Text("auto") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ai_model_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    // Quick Model Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("auto", "qwen-3.8-27b", "fusion").forEach { preset ->
                            val selected = modelInput.trim().equals(preset, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .clickable { modelInput = preset }
                                    .border(
                                        1.dp,
                                        if (selected) SupabaseGreen else CharcoalBorder,
                                        RoundedCornerShape(6.dp)
                                    ),
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 10.5.sp,
                                    color = if (selected) SupabaseGreen else TextSecondaryDark,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text(if (isEn) "Gemini API Key (Optional)" else "Gemini API Key (Opsional)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_gemini_api_key"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAiApiConfig(
                            baseUrl = baseUrlInput,
                            apiKey = apiKeyInput,
                            modelName = modelInput,
                            geminiKey = geminiKeyInput
                        )
                        showEditAiConfigDialog = false
                        Toast.makeText(
                            context,
                            if (isEn) "API & Domain settings saved" else "Konfigurasi API & Domain disimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SupabaseGreen,
                        contentColor = Color(0xFF042114)
                    )
                ) {
                    Text(if (isEn) "Save" else "Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditAiConfigDialog = false }) {
                    Text(if (isEn) "Cancel" else "Batal", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = CharcoalSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var printerDropdownExpanded by remember { mutableStateOf(false) }

    val presetQuestions = listOf(
        "Toko terlaris?",
        "Total stok tersebar?",
        "Laba bersih saat ini?",
        "Warung belum dikunjungi?",
        "Margin produk tertinggi?"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CharcoalBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analisis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PrinterConfigCard(
                        pairedPrinters = uiState.pairedPrinters,
                        selectedPrinter = uiState.selectedPrinter,
                        isPrintingTest = uiState.isPrintingTest,
                        testPrintStatus = uiState.testPrintStatus,
                        defaultPaper80mm = uiState.defaultPaper80mm,
                        onDefaultPaperChange = { viewModel.setDefaultPaper80mm(it) },
                        printerDropdownExpanded = printerDropdownExpanded,
                        onDropdownExpandedChange = { printerDropdownExpanded = it },
                        onSelectPrinter = { viewModel.setSelectedPrinter(it) },
                        onRefreshPrinters = { viewModel.loadPrinters() },
                        onPrintTest = { is80mm -> viewModel.printTestReceipt(is80mm) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tanya Data Bisnis",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                    }
                }

                items(uiState.messages) { msg ->
                    ChatBubble(message = msg)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurfaceElevated)
                    .border(1.dp, CharcoalBorder)
                    .padding(vertical = 8.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetQuestions) { q ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.askQuestion(q) },
                            label = { Text(q, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CharcoalSurface,
                                labelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = CharcoalBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterConfigCard(
    pairedPrinters: List<com.example.util.thermal.PairedPrinter>,
    selectedPrinter: String?,
    isPrintingTest: Boolean,
    testPrintStatus: String?,
    defaultPaper80mm: Boolean,
    onDefaultPaperChange: (Boolean) -> Unit,
    printerDropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onSelectPrinter: (String) -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrintTest: (is80mm: Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = SupabaseGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRINTER THERMAL BLUETOOTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SupabaseGreen
                    )
                }

                IconButton(
                    onClick = onRefreshPrinters,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = printerDropdownExpanded,
                onExpandedChange = onDropdownExpandedChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedName = pairedPrinters.find { it.address == selectedPrinter }?.name
                    ?: if (pairedPrinters.isEmpty()) "Printer belum terhubung" else "Pilih Printer..."

                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = printerDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SupabaseGreen,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedContainerColor = CharcoalSurfaceElevated,
                        unfocusedContainerColor = CharcoalSurfaceElevated
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = printerDropdownExpanded,
                    onDismissRequest = { onDropdownExpandedChange(false) },
                    modifier = Modifier.background(CharcoalSurfaceElevated)
                ) {
                    if (pairedPrinters.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Hubungkan printer via Bluetooth HP", color = TextMutedDark) },
                            onClick = { onDropdownExpandedChange(false) }
                        )
                    } else {
                        pairedPrinters.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                                        Text(p.address, color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectPrinter(p.address)
                                    onDropdownExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Default Paper Size Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ukuran Kertas Default:",
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(false to "58mm", true to "80mm").forEach { (is80, label) ->
                        val selected = defaultPaper80mm == is80
                        Surface(
                            modifier = Modifier
                                .clickable { onDefaultPaperChange(is80) }
                                .border(
                                    1.dp,
                                    if (selected) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(6.dp)
                                ),
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) SupabaseGreen else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPrintTest(false) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalSurfaceElevated,
                        contentColor = TextPrimaryDark
                    ),
                    border = BorderStroke(1.dp, CharcoalBorder),
                    enabled = !isPrintingTest
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tes 58mm", fontSize = 12.sp)
                }

                Button(
                    onClick = { onPrintTest(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalSurfaceElevated,
                        contentColor = TextPrimaryDark
                    ),
                    border = BorderStroke(1.dp, CharcoalBorder),
                    enabled = !isPrintingTest
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tes 80mm", fontSize = 12.sp)
                }
            }

            if (!testPrintStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = testPrintStatus,
                    fontSize = 11.5.sp,
                    color = if (testPrintStatus.contains("Berhasil")) SupabaseGreen else Color(0xFFF87171)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "USER"
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.82f else 0.95f)
                .background(
                    if (isUser) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurface,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (isUser) SupabaseGreen.copy(alpha = 0.35f) else CharcoalBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (isUser) SupabaseGreen else Color(0xFF38BDF8), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUser) "Anda" else if (message.executedTools.isNotEmpty()) "AI Agent • Tool Executor" else "Asisten AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) SupabaseGreen else Color(0xFF38BDF8)
                        )
                    }

                    // Copy action button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", message.text))
                            Toast.makeText(context, "Pesan disalin", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Salin Pesan",
                            tint = TextMutedDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (message.executedTools.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "⚡ TOOL CALLS DIJALANKAN (${message.executedTools.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            letterSpacing = 0.6.sp
                        )
                        message.executedTools.forEach { tool ->
                            val badgeColor = when (tool.actionType) {
                                "ADD" -> SupabaseGreen
                                "ERASE" -> Color(0xFFF87171)
                                else -> AmberWarning
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor.copy(alpha = 0.18f),
                                    modifier = Modifier.border(0.8.dp, badgeColor, RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        text = "${tool.actionType} • ${tool.toolName}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = tool.summary.replace("**", ""),
                                    fontSize = 11.sp,
                                    color = TextPrimaryDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                MarkdownContent(
                    markdown = message.text,
                    isUser = isUser,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChessComWebView
import com.example.ui.components.GMCoachCard
import com.example.ui.components.InteractiveBoard
import com.example.ui.components.injectChessObserver
import com.example.ui.components.renderArrowOnChessComBoard
import com.example.ui.theme.ChessBlueGM
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessRedAlert
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ChessUiState
import com.example.viewmodel.ChessViewModel

@Composable
fun ChessComLiveScreen(
    viewModel: ChessViewModel,
    uiState: ChessUiState,
    onNavigateToSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isCoachExpanded by remember { mutableStateOf(true) }
    var showCompanionBoard by remember { mutableStateOf(false) }
    var showApkDownloadDialog by remember { mutableStateOf(false) }

    // Re-render SVG arrow directly inside Chess.com board when best move changes
    LaunchedEffect(uiState.gmAdvice.bestMoveNotation, uiState.showArrows, uiState.isFlipped) {
        if (uiState.showArrows && uiState.gmAdvice.bestMoveNotation.isNotBlank()) {
            renderArrowOnChessComBoard(webViewRef, uiState.gmAdvice.bestMoveNotation, uiState.isFlipped)
        }
    }

    val quickLinks = listOf(
        "Игра с Ботом" to "https://www.chess.com/play/computer",
        "Шахматные Задачи" to "https://www.chess.com/puzzles",
        "Анализ партии" to "https://www.chess.com/analysis",
        "Игра Онлайн" to "https://www.chess.com/play/online"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Quick Navigation Tabs for Chess.com sections
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickLinks.forEach { (label, url) ->
                val isSelected = uiState.webViewUrl == url
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.setWebViewUrl(url)
                        webViewRef?.loadUrl(url)
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChessGreenPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = ChessGreenPrimary,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ChessGreenPrimary else DarkBorder
                    ),
                    modifier = Modifier.testTag("quick_tab_${label.take(4)}")
                )
            }
        }

        // Live Action HUD (Best move banner & Quick Sync)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Best move & Eval badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ChessGreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Best Move",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Лучший ход: ",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = uiState.gmAdvice.bestMoveNotation,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChessGreenPrimary
                        )
                    }
                    Text(
                        text = "Оценка Stockfish: ${uiState.evaluation.displayString}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.evaluation.centipawns >= 0) ChessGreenPrimary else ChessRedAlert
                    )
                }
            }

            // Sync and Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Toggle Companion Native Board with arrows
                IconButton(
                    onClick = { showCompanionBoard = !showCompanionBoard },
                    modifier = Modifier
                        .testTag("toggle_companion_board")
                        .size(36.dp)
                        .background(if (showCompanionBoard) ChessGreenPrimary else DarkSurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Доска со стрелками",
                        tint = if (showCompanionBoard) Color.Black else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Sync button
                IconButton(
                    onClick = {
                        injectChessObserver(webViewRef)
                        renderArrowOnChessComBoard(webViewRef, uiState.gmAdvice.bestMoveNotation, uiState.isFlipped)
                    },
                    modifier = Modifier
                        .testTag("sync_board_btn")
                        .size(36.dp)
                        .background(DarkSurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Синхронизировать",
                        tint = ChessGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                // APK Download Info
                IconButton(
                    onClick = { showApkDownloadDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(ChessBlueGM.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Скачать APK",
                        tint = ChessBlueGM,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Companion Board Drawer (when toggled, shows high-contrast native board with arrows)
        AnimatedVisibility(visible = showCompanionBoard) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChessGreenPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Наглядные стрелки ходов Stockfish",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChessGreenPrimary
                        )
                        Row {
                            IconButton(
                                onClick = { viewModel.toggleFlipBoard() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Flip",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showCompanionBoard = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.size(260.dp)) {
                        InteractiveBoard(
                            board = viewModel.getLocalBoard(),
                            selectedSquare = uiState.selectedSquare,
                            legalMovesForSelected = uiState.legalMovesForSelected,
                            arrows = uiState.arrows,
                            isFlipped = uiState.isFlipped,
                            showArrows = true,
                            lastMove = uiState.lastMove,
                            onSquareClick = { sq -> viewModel.onSandboxSquareClicked(sq) }
                        )
                    }
                }
            }
        }

        // Main WebView area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ChessComWebView(
                url = uiState.webViewUrl,
                onBoardDetected = { fen, isFlipped ->
                    viewModel.onBoardFenChanged(fen, isFlipped)
                    renderArrowOnChessComBoard(webViewRef, uiState.gmAdvice.bestMoveNotation, isFlipped)
                },
                onWebViewCreated = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Collapsible Coach Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCoachExpanded = !isCoachExpanded }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Coach",
                        tint = ChessGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Совет Гроссмейстера (${uiState.gmAdvice.motifBadge})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Icon(
                    imageVector = if (isCoachExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Toggle",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = isCoachExpanded) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    GMCoachCard(
                        advice = uiState.gmAdvice,
                        evaluation = uiState.evaluation
                    )
                }
            }
        }
    }

    if (showApkDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showApkDownloadDialog = false },
            title = {
                Text("Как скачать APK на телефон", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Чтобы установить приложение на реальный Android-смартфон и избежать задержек виртуального эмулятора:\n\n" +
                    "1. В правом верхнем углу интерфейса Google AI Studio нажмите на меню настроек / три точки (или иконку шестеренки).\n\n" +
                    "2. Выберите 'Download APK' (или 'Generate APK / Export').\n\n" +
                    "3. Загрузите файл .apk прямо на ваш телефон или перекиньте через Telegram / провод.\n\n" +
                    "4. На телефоне нажмите на скачанный файл для установки (разрешите установку из неизвестных источников, если телефон запросит).",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showApkDownloadDialog = false }) {
                    Text("Понятно", color = ChessGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

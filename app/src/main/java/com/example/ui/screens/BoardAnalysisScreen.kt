package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PieceColor
import com.example.ui.components.AdvantageBar
import com.example.ui.components.GMCoachCard
import com.example.ui.components.InteractiveBoard
import com.example.ui.theme.ChessBlueGM
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ChessUiState
import com.example.viewmodel.ChessViewModel

@Composable
fun BoardAnalysisScreen(
    viewModel: ChessViewModel,
    uiState: ChessUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val localBoard = viewModel.getLocalBoard()
    val isWhiteTurn = localBoard.activeColor == PieceColor.WHITE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Controls Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Доска Анализа Stockfish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isWhiteTurn) "Ход Белых" else "Ход Черных",
                    fontSize = 12.sp,
                    color = if (isWhiteTurn) Color.White else ChessGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row {
                IconButton(
                    onClick = { viewModel.toggleFlipBoard() },
                    modifier = Modifier
                        .testTag("flip_board_btn")
                        .size(38.dp)
                        .background(DarkSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Перевернуть доску",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.toggleShowArrows() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(DarkSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.showArrows) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Скрыть/показать стрелки",
                        tint = if (uiState.showArrows) ChessGreenPrimary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.resetBoard() },
                    modifier = Modifier
                        .testTag("reset_board_btn")
                        .size(38.dp)
                        .background(DarkSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Сбросить позицию",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chess Board + Advantage Bar Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdvantageBar(
                evaluation = uiState.evaluation,
                isFlipped = uiState.isFlipped,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .testTag("eval_advantage_bar")
            )

            InteractiveBoard(
                board = localBoard,
                selectedSquare = uiState.selectedSquare,
                legalMovesForSelected = uiState.legalMovesForSelected,
                arrows = uiState.arrows,
                isFlipped = uiState.isFlipped,
                showArrows = uiState.showArrows,
                lastMove = uiState.lastMove,
                onSquareClick = { sq -> viewModel.onSandboxSquareClicked(sq) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Candidate moves chips
        if (uiState.arrows.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.arrows.forEachIndexed { idx, arrow ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (idx == 0) ChessGreenPrimary else DarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(arrow.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${idx + 1}. ${arrow.label}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = arrow.evaluation,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grandmaster Coach card
        GMCoachCard(
            advice = uiState.gmAdvice,
            evaluation = uiState.evaluation
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Move History if any
        if (uiState.moveHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "История ходов партии:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.moveHistory.chunked(2).mapIndexed { idx, pair ->
                            val whiteM = pair[0].notation
                            val blackM = pair.getOrNull(1)?.notation ?: ""
                            "${idx + 1}. $whiteM $blackM"
                        }.joinToString("  "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

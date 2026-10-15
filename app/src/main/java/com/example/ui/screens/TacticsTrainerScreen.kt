package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveBoard
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
fun TacticsTrainerScreen(
    viewModel: ChessViewModel,
    uiState: ChessUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val localBoard = viewModel.getLocalBoard()
    val currentPuzzle = viewModel.puzzles.getOrNull(uiState.currentPuzzleIndex)

    LaunchedEffect(uiState.currentPuzzleIndex) {
        if (uiState.currentFen != currentPuzzle?.fen && uiState.puzzleSolved == null) {
            viewModel.loadPuzzle(uiState.currentPuzzleIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Puzzles Selector Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.puzzles.forEachIndexed { index, puzzle ->
                val isSelected = uiState.currentPuzzleIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.loadPuzzle(index) },
                    label = { Text("Урок ${index + 1}: ${puzzle.title}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChessGreenPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = ChessGreenPrimary,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ChessGreenPrimary else DarkBorder
                    ),
                    modifier = Modifier.testTag("puzzle_chip_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Puzzle Info Card
        currentPuzzle?.let { puzzle ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = puzzle.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(ChessBlueGM.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = puzzle.theme,
                                color = Color(0xFF82B1FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = puzzle.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            InteractiveBoard(
                board = localBoard,
                selectedSquare = uiState.selectedSquare,
                legalMovesForSelected = uiState.legalMovesForSelected,
                arrows = uiState.arrows,
                isFlipped = false,
                showArrows = uiState.showArrows,
                lastMove = uiState.lastMove,
                onSquareClick = { sq -> viewModel.onSandboxSquareClicked(sq) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hint Button and Solution Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.toggleShowArrows() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (uiState.showArrows) ChessGreenPrimary else TextSecondary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Hint",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.showArrows) "Скрыть подсказку" else "Показать стрелку")
            }

            // Next Puzzle Button
            Button(
                onClick = {
                    val nextIdx = (uiState.currentPuzzleIndex + 1) % viewModel.puzzles.size
                    viewModel.loadPuzzle(nextIdx)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Следующий урок", color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = "Next",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

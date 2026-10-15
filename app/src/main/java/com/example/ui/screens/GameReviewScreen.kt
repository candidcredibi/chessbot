package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChessComGame
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
fun GameReviewScreen(
    viewModel: ChessViewModel,
    uiState: ChessUiState,
    onGameSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember { mutableStateOf(uiState.chessComUsername) }

    LaunchedEffect(Unit) {
        if (uiState.chessComGames.isEmpty()) {
            viewModel.fetchChessComGames(uiState.chessComUsername)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(14.dp)
    ) {
        Text(
            text = "Архив партий Chess.com",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Введите никнейм любого игрока для загрузки и разбора последних партий",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Search Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Никнейм на Chess.com") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = ChessGreenPrimary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChessGreenPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("username_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { viewModel.fetchChessComGames(usernameInput) },
                colors = ButtonDefaults.buttonColors(containerColor = ChessGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("fetch_games_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Games List
        if (uiState.isLoadingGames) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChessGreenPrimary)
            }
        } else if (uiState.chessComGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Empty",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Партии не найдены или загружаются...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.chessComGames) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            viewModel.loadGamePgn(game)
                            onGameSelected()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: ChessComGame,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("game_item_${game.whitePlayer}_vs_${game.blackPlayer}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Date & Time Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = game.date,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Контроль: ${game.timeControl}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChessBlueGM
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Players
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "♔ ${game.whitePlayer} (${game.whiteRating})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "♚ ${game.blackPlayer} (${game.blackRating})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                // Result badge
                Box(
                    modifier = Modifier
                        .background(
                            when (game.result) {
                                "1-0" -> ChessGreenPrimary.copy(alpha = 0.2f)
                                "0-1" -> ChessRedAlert.copy(alpha = 0.2f)
                                else -> DarkSurfaceElevated
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = game.result,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = when (game.result) {
                            "1-0" -> ChessGreenPrimary
                            "0-1" -> ChessRedAlert
                            else -> TextSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = TextSecondary
                )
            }
        }
    }
}

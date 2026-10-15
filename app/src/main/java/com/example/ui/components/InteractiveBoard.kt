package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ChessBoard
import com.example.model.EngineArrow
import com.example.model.Move
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.Square
import com.example.ui.theme.BoardHighlight
import com.example.ui.theme.BoardSelected
import com.example.ui.theme.BoardSquareDark
import com.example.ui.theme.BoardSquareLight
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessRedAlert

@Composable
fun InteractiveBoard(
    board: ChessBoard,
    selectedSquare: Square?,
    legalMovesForSelected: List<Move>,
    arrows: List<EngineArrow>,
    isFlipped: Boolean = false,
    showArrows: Boolean = true,
    lastMove: Move? = null,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF333B47), RoundedCornerShape(8.dp))
            .testTag("interactive_chess_board")
    ) {
        val squareSize = maxWidth / 8

        // Draw 8x8 squares and pieces
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0..7) {
                val rank = if (isFlipped) row else 7 - row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (col in 0..7) {
                        val file = if (isFlipped) 7 - col else col
                        val square = Square(file, rank)
                        val piece = board.getPiece(square)
                        val isSelected = selectedSquare == square
                        val isLegalDest = legalMovesForSelected.any { it.to == square }
                        val isLastMoveSquare = lastMove != null && (lastMove.from == square || lastMove.to == square)

                        SquareCell(
                            square = square,
                            piece = piece,
                            isSelected = isSelected,
                            isLegalDest = isLegalDest,
                            isLastMove = isLastMoveSquare,
                            showRankCoord = col == 0,
                            showFileCoord = row == 7,
                            onClick = { onSquareClick(square) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Arrow Overlay Layer
        if (showArrows && arrows.isNotEmpty()) {
            ArrowOverlay(
                arrows = arrows,
                isFlipped = isFlipped,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SquareCell(
    square: Square,
    piece: Piece?,
    isSelected: Boolean,
    isLegalDest: Boolean,
    isLastMove: Boolean,
    showRankCoord: Boolean,
    showFileCoord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = if (square.isLightSquare) BoardSquareLight else BoardSquareDark
    val squareColor = when {
        isSelected -> BoardSelected
        isLastMove -> BoardHighlight
        else -> baseColor
    }

    val coordColor = if (square.isLightSquare) BoardSquareDark else BoardSquareLight

    Box(
        modifier = modifier
            .background(squareColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("square_${square.notation}"),
        contentAlignment = Alignment.Center
    ) {
        // Coordinate label (Rank)
        if (showRankCoord) {
            Text(
                text = "${square.rank + 1}",
                color = coordColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }

        // Coordinate label (File)
        if (showFileCoord) {
            Text(
                text = "${('a' + square.file)}",
                color = coordColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp)
            )
        }

        // Piece Symbol
        if (piece != null) {
            Text(
                text = piece.symbol,
                fontSize = 32.sp,
                color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF1E2328),
                fontWeight = FontWeight.Normal
            )
        }

        // Legal Move Dot or Capture Ring
        if (isLegalDest) {
            if (piece == null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(ChessGreenPrimary.copy(alpha = 0.85f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .border(3.dp, ChessRedAlert.copy(alpha = 0.85f), CircleShape)
                )
            }
        }
    }
}

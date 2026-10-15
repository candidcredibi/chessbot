package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ChessBlueGM
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessRedAlert

enum class PieceType(val charWhite: Char, val charBlack: Char, val value: Int) {
    PAWN('P', 'p', 100),
    KNIGHT('N', 'n', 320),
    BISHOP('B', 'b', 330),
    ROOK('R', 'r', 500),
    QUEEN('Q', 'q', 900),
    KING('K', 'k', 20000);

    val unicodeWhite: String
        get() = when (this) {
            PAWN -> "♙"
            KNIGHT -> "♘"
            BISHOP -> "♗"
            ROOK -> "♖"
            QUEEN -> "♕"
            KING -> "♔"
        }

    val unicodeBlack: String
        get() = when (this) {
            PAWN -> "♟"
            KNIGHT -> "♞"
            BISHOP -> "♝"
            ROOK -> "♜"
            QUEEN -> "♛"
            KING -> "♚"
        }
}

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val symbol: String
        get() = if (color == PieceColor.WHITE) type.unicodeWhite else type.unicodeBlack

    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.charWhite else type.charBlack
}

data class Square(
    val file: Int, // 0 = a, 7 = h
    val rank: Int  // 0 = 1st rank, 7 = 8th rank
) {
    val notation: String
        get() = "${('a' + file)}${rank + 1}"

    val isLightSquare: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromNotation(notation: String): Square? {
            if (notation.length < 2) return null
            val file = notation[0].lowercaseChar() - 'a'
            val rank = notation[1] - '1'
            return if (file in 0..7 && rank in 0..7) Square(file, rank) else null
        }
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCheck: Boolean = false,
    val isCastling: Boolean = false
) {
    val notation: String
        get() = "${from.notation}${to.notation}${promotion?.let { "=${it.charWhite}" } ?: ""}"
}

data class EngineArrow(
    val from: Square,
    val to: Square,
    val color: Color = ChessGreenPrimary,
    val label: String = "",
    val evaluation: String = "",
    val isBestMove: Boolean = true
)

data class PositionEvaluation(
    val centipawns: Int = 0,
    val mateInMoves: Int? = null,
    val isWhiteToMove: Boolean = true,
    val depth: Int = 12
) {
    val winProbabilityWhite: Float
        get() {
            mateInMoves?.let { mate ->
                return if (mate > 0) 0.99f else 0.01f
            }
            // Sigmoid-based win probability from centipawns
            val cp = centipawns.toFloat()
            val prob = 1f / (1f + Math.exp(-0.004 * cp).toFloat())
            return prob.coerceIn(0.02f, 0.98f)
        }

    val displayString: String
        get() {
            mateInMoves?.let { mate ->
                return if (mate > 0) "#+$mate" else "#$mate"
            }
            val pawns = centipawns / 100.0
            return if (pawns > 0) String.format("+%.1f", pawns) else String.format("%.1f", pawns)
        }
}

data class GMAdvice(
    val title: String,
    val description: String,
    val motifBadge: String,
    val bestMoveNotation: String,
    val warning: String? = null,
    val candidateMoves: List<String> = emptyList()
)

data class ChessComGame(
    val url: String,
    val pgn: String,
    val timeControl: String,
    val whitePlayer: String,
    val whiteRating: Int,
    val blackPlayer: String,
    val blackRating: Int,
    val result: String,
    val date: String
)

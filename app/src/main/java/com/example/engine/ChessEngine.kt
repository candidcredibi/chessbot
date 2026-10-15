package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.EngineArrow
import com.example.model.GMAdvice
import com.example.model.Move
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.PositionEvaluation
import com.example.model.Square
import com.example.ui.theme.ChessBlueGM
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessRedAlert
import kotlin.math.max
import kotlin.math.min

class ChessEngine {

    // Piece-Square Tables (from White's perspective; mirrored for Black)
    private val pawnTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightTable = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopTable = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val kingTableMidgame = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    fun evaluateBoard(board: ChessBoard): Int {
        var score = 0
        for (f in 0..7) {
            for (r in 0..7) {
                val piece = board.getPiece(f, r) ?: continue
                val idx = (7 - r) * 8 + f
                val mirroredIdx = r * 8 + f

                val pieceValue = piece.type.value
                val tableVal = when (piece.type) {
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) pawnTable[idx] else pawnTable[mirroredIdx]
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) knightTable[idx] else knightTable[mirroredIdx]
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) bishopTable[idx] else bishopTable[mirroredIdx]
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) kingTableMidgame[idx] else kingTableMidgame[mirroredIdx]
                    else -> 0
                }

                val totalPieceScore = pieceValue + tableVal
                if (piece.color == PieceColor.WHITE) {
                    score += totalPieceScore
                } else {
                    score -= totalPieceScore
                }
            }
        }
        return score
    }

    private fun minimax(
        board: ChessBoard,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth == 0) {
            return evaluateBoard(board)
        }

        var alpha = alphaInit
        var beta = betaInit
        val legalMoves = board.generateLegalMoves()

        if (legalMoves.isEmpty()) {
            return if (board.isCheck(board.activeColor)) {
                if (isMaximizing) -100000 - depth else 100000 + depth
            } else {
                0 // Stalemate
            }
        }

        // Move ordering: prioritize captures
        val sortedMoves = legalMoves.sortedByDescending { if (it.isCapture) 10 else 0 }

        if (isMaximizing) {
            var maxEval = -999999
            for (move in sortedMoves) {
                val copy = board.copy()
                copy.makeMove(move)
                val eval = minimax(copy, depth - 1, alpha, beta, false)
                maxEval = max(maxEval, eval)
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 999999
            for (move in sortedMoves) {
                val copy = board.copy()
                copy.makeMove(move)
                val eval = minimax(copy, depth - 1, alpha, beta, true)
                minEval = min(minEval, eval)
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    fun findBestMoves(board: ChessBoard, depth: Int = 3): Pair<PositionEvaluation, List<EngineArrow>> {
        val legalMoves = board.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            val isMate = board.isCheck(board.activeColor)
            val eval = PositionEvaluation(
                centipawns = if (isMate) (if (board.activeColor == PieceColor.WHITE) -10000 else 10000) else 0,
                mateInMoves = if (isMate) 0 else null,
                isWhiteToMove = board.activeColor == PieceColor.WHITE,
                depth = depth
            )
            return eval to emptyList()
        }

        val isWhite = board.activeColor == PieceColor.WHITE
        val scoredMoves = mutableListOf<Pair<Move, Int>>()

        for (move in legalMoves) {
            val copy = board.copy()
            copy.makeMove(move)
            val score = minimax(copy, depth - 1, -999999, 999999, !isWhite)
            scoredMoves.add(move to score)
        }

        val sorted = if (isWhite) {
            scoredMoves.sortedByDescending { it.second }
        } else {
            scoredMoves.sortedBy { it.second }
        }

        val bestScore = sorted.first().second
        val eval = PositionEvaluation(
            centipawns = bestScore,
            mateInMoves = if (bestScore > 50000) (100000 - bestScore) else if (bestScore < -50000) (-100000 - bestScore) else null,
            isWhiteToMove = isWhite,
            depth = depth
        )

        val arrows = mutableListOf<EngineArrow>()
        sorted.take(3).forEachIndexed { index, (move, score) ->
            val color = when (index) {
                0 -> ChessGreenPrimary
                1 -> ChessBlueGM
                else -> Color(0xFFFF9100)
            }
            val formattedScore = String.format("%+.1f", score / 100.0)
            arrows.add(
                EngineArrow(
                    from = move.from,
                    to = move.to,
                    color = color,
                    label = move.notation,
                    evaluation = formattedScore,
                    isBestMove = index == 0
                )
            )
        }

        return eval to arrows
    }

    fun generateGrandmasterAdvice(board: ChessBoard, bestMove: Move?, eval: PositionEvaluation): GMAdvice {
        if (bestMove == null) {
            return GMAdvice(
                title = "Конец партии",
                description = if (eval.mateInMoves == 0) "Мат на доске! Игра окончена." else "Пат или отсутствие легальных ходов.",
                motifBadge = "Результат",
                bestMoveNotation = "-"
            )
        }

        val movingPiece = board.getPiece(bestMove.from)
        val pieceNameRu = when (movingPiece?.type) {
            PieceType.PAWN -> "Пешка"
            PieceType.KNIGHT -> "Конь"
            PieceType.BISHOP -> "Слон"
            PieceType.ROOK -> "Ладья"
            PieceType.QUEEN -> "Ферзь"
            PieceType.KING -> "Король"
            null -> "Фигура"
        }

        val isCapture = bestMove.isCapture
        val targetPiece = board.getPiece(bestMove.to)
        val isCastling = bestMove.isCastling
        val isCenterMove = bestMove.to.file in 3..4 && bestMove.to.rank in 3..4

        var motif = "Позиционный ход"
        var title = "Сильный ход $pieceNameRu: ${bestMove.from.notation} → ${bestMove.to.notation}"
        var description = "Гроссмейстер усиливает позицию своих фигур и ограничивает возможности соперника."
        var warning: String? = null

        // Detect tactical motives
        if (isCastling) {
            motif = "Рокировка / Безопасность"
            title = "Обеспечьте безопасность короля!"
            description = "Рокировка прячет короля от прямых атак по центру и активизирует ладью для открытых линий."
        } else if (isCapture && targetPiece != null) {
            motif = "Выигрыш материала"
            title = "Взятие фигуры соперника!"
            description = "$pieceNameRu забирает незащищенную или менее ценную фигуру (${targetPiece.type.name.lowercase()}) на ${bestMove.to.notation}."
        } else if (movingPiece?.type == PieceType.KNIGHT && isCenterMove) {
            motif = "Форпост в центре"
            title = "Доминирование коня в центре!"
            description = "Конь на поле ${bestMove.to.notation} контролирует ключевые поля доски и сковывает фигуры соперника."
        } else if (movingPiece?.type == PieceType.PAWN && isCenterMove) {
            motif = "Борьба за центр"
            title = "Захват центра пешкой"
            description = "Пешечный центр захватывает пространство и открывает диагонали для слонов и ферзя."
        } else if (movingPiece?.type == PieceType.BISHOP) {
            motif = "Активная диагональ"
            title = "Развитие слона"
            description = "Слон занимает длинную диагональ, оказывая давление на королевский фланг или связывая вражеские фигуры."
        }

        // Check for opponent threats
        val copy = board.copy()
        copy.makeMove(bestMove)
        if (copy.isCheck(copy.activeColor)) {
            motif = "Шах / Атака"
            description += " Этот ход объявляет шах и вынуждает соперника защищаться!"
        }

        return GMAdvice(
            title = title,
            description = description,
            motifBadge = motif,
            bestMoveNotation = bestMove.notation,
            warning = warning,
            candidateMoves = listOf(bestMove.notation)
        )
    }
}

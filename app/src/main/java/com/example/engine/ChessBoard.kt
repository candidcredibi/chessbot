package com.example.engine

import com.example.model.Move
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Square
import kotlin.math.abs

class ChessBoard {
    private val board = Array<Piece?>(64) { null }
    var activeColor: PieceColor = PieceColor.WHITE
    var castlingWhiteKingside = true
    var castlingWhiteQueenside = true
    var castlingBlackKingside = true
    var castlingBlackQueenside = true
    var enPassantSquare: Square? = null
    var halfmoveClock = 0
    var fullmoveNumber = 1

    init {
        loadFen(STARTING_FEN)
    }

    fun getPiece(file: Int, rank: Int): Piece? {
        if (file !in 0..7 || rank !in 0..7) return null
        return board[rank * 8 + file]
    }

    fun getPiece(square: Square): Piece? = getPiece(square.file, square.rank)

    fun setPiece(file: Int, rank: Int, piece: Piece?) {
        if (file in 0..7 && rank in 0..7) {
            board[rank * 8 + file] = piece
        }
    }

    fun setPiece(square: Square, piece: Piece?) = setPiece(square.file, square.rank, piece)

    fun copy(): ChessBoard {
        val copy = ChessBoard()
        for (i in 0 until 64) {
            copy.board[i] = this.board[i]
        }
        copy.activeColor = this.activeColor
        copy.castlingWhiteKingside = this.castlingWhiteKingside
        copy.castlingWhiteQueenside = this.castlingWhiteQueenside
        copy.castlingBlackKingside = this.castlingBlackKingside
        copy.castlingBlackQueenside = this.castlingBlackQueenside
        copy.enPassantSquare = this.enPassantSquare
        copy.halfmoveClock = this.halfmoveClock
        copy.fullmoveNumber = this.fullmoveNumber
        return copy
    }

    fun loadFen(fen: String): Boolean {
        val parts = fen.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return false

        board.fill(null)
        val rows = parts[0].split("/")
        if (rows.size != 8) return false

        for (rankIndex in 0..7) {
            val rank = 7 - rankIndex
            val rowStr = rows[rankIndex]
            var file = 0
            for (ch in rowStr) {
                if (file > 7) break
                if (ch.isDigit()) {
                    file += (ch - '0')
                } else {
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = when (ch.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        'k' -> PieceType.KING
                        else -> PieceType.PAWN
                    }
                    setPiece(file, rank, Piece(type, color))
                    file++
                }
            }
        }

        activeColor = if (parts.size > 1 && parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE

        if (parts.size > 2) {
            val castling = parts[2]
            castlingWhiteKingside = castling.contains('K')
            castlingWhiteQueenside = castling.contains('Q')
            castlingBlackKingside = castling.contains('k')
            castlingBlackQueenside = castling.contains('q')
        }

        enPassantSquare = if (parts.size > 3 && parts[3] != "-") {
            Square.fromNotation(parts[3])
        } else null

        halfmoveClock = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
        fullmoveNumber = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

        return true
    }

    fun toFen(): String {
        val fenBuilder = StringBuilder()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = getPiece(file, rank)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    fenBuilder.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount)
            }
            if (rank > 0) fenBuilder.append('/')
        }

        fenBuilder.append(if (activeColor == PieceColor.WHITE) " w " else " b ")

        var castling = ""
        if (castlingWhiteKingside) castling += "K"
        if (castlingWhiteQueenside) castling += "Q"
        if (castlingBlackKingside) castling += "k"
        if (castlingBlackQueenside) castling += "q"
        if (castling.isEmpty()) castling = "-"
        fenBuilder.append("$castling ")

        fenBuilder.append(enPassantSquare?.notation ?: "-")
        fenBuilder.append(" $halfmoveClock $fullmoveNumber")
        return fenBuilder.toString()
    }

    fun findKing(color: PieceColor): Square? {
        for (f in 0..7) {
            for (r in 0..7) {
                val piece = getPiece(f, r)
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Square(f, r)
                }
            }
        }
        return null
    }

    fun isSquareAttacked(square: Square, attackerColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnPushes = if (attackerColor == PieceColor.WHITE) -1 else 1
        val attackerPawnRank = square.rank + pawnPushes
        if (attackerPawnRank in 0..7) {
            for (df in listOf(-1, 1)) {
                val af = square.file + df
                if (af in 0..7) {
                    val p = getPiece(af, attackerPawnRank)
                    if (p?.color == attackerColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // Knight attacks
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((df, dr) in knightOffsets) {
            val nf = square.file + df
            val nr = square.rank + dr
            if (nf in 0..7 && nr in 0..7) {
                val p = getPiece(nf, nr)
                if (p?.color == attackerColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // Bishop / Queen diagonals
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((df, dr) in diagDirs) {
            var step = 1
            while (true) {
                val tf = square.file + df * step
                val tr = square.rank + dr * step
                if (tf !in 0..7 || tr !in 0..7) break
                val p = getPiece(tf, tr)
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        // Rook / Queen orthogonal
        val orthoDirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        for ((df, dr) in orthoDirs) {
            var step = 1
            while (true) {
                val tf = square.file + df * step
                val tr = square.rank + dr * step
                if (tf !in 0..7 || tr !in 0..7) break
                val p = getPiece(tf, tr)
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        // King attacks
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val kf = square.file + df
                val kr = square.rank + dr
                if (kf in 0..7 && kr in 0..7) {
                    val p = getPiece(kf, kr)
                    if (p?.color == attackerColor && p.type == PieceType.KING) return true
                }
            }
        }

        return false
    }

    fun isCheck(color: PieceColor): Boolean {
        val kingSquare = findKing(color) ?: return false
        return isSquareAttacked(kingSquare, color.opposite())
    }

    fun generateLegalMoves(color: PieceColor = activeColor): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(color)
        val legalMoves = mutableListOf<Move>()
        for (m in pseudoMoves) {
            val copy = this.copy()
            copy.makeMove(m)
            if (!copy.isCheck(color)) {
                legalMoves.add(m)
            }
        }
        return legalMoves
    }

    fun generatePseudoLegalMoves(color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for (f in 0..7) {
            for (r in 0..7) {
                val piece = getPiece(f, r) ?: continue
                if (piece.color != color) continue
                val from = Square(f, r)

                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(from, color, moves)
                    PieceType.KNIGHT -> generateKnightMoves(from, color, moves)
                    PieceType.BISHOP -> generateSlidingMoves(from, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
                    PieceType.ROOK -> generateSlidingMoves(from, color, listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0), moves)
                    PieceType.QUEEN -> generateSlidingMoves(from, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, 0 to 1, 0 to -1, 1 to 0, -1 to 0), moves)
                    PieceType.KING -> generateKingMoves(from, color, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Square, color: PieceColor, moves: MutableList<Move>) {
        val forwardDir = if (color == PieceColor.WHITE) 1 else -1
        val startRank = if (color == PieceColor.WHITE) 1 else 6
        val promoRank = if (color == PieceColor.WHITE) 7 else 0

        // Single push
        val toRank1 = from.rank + forwardDir
        if (toRank1 in 0..7 && getPiece(from.file, toRank1) == null) {
            val toSquare = Square(from.file, toRank1)
            if (toRank1 == promoRank) {
                listOf(PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP).forEach { promo ->
                    moves.add(Move(from, toSquare, promotion = promo))
                }
            } else {
                moves.add(Move(from, toSquare))
                // Double push from starting rank
                val toRank2 = from.rank + 2 * forwardDir
                if (from.rank == startRank && getPiece(from.file, toRank2) == null) {
                    moves.add(Move(from, Square(from.file, toRank2)))
                }
            }
        }

        // Pawn captures (including en passant)
        for (df in listOf(-1, 1)) {
            val tf = from.file + df
            val tr = from.rank + forwardDir
            if (tf in 0..7 && tr in 0..7) {
                val targetPiece = getPiece(tf, tr)
                val targetSquare = Square(tf, tr)
                if (targetPiece != null && targetPiece.color != color) {
                    if (tr == promoRank) {
                        listOf(PieceType.QUEEN, PieceType.KNIGHT).forEach { promo ->
                            moves.add(Move(from, targetSquare, promotion = promo, isCapture = true))
                        }
                    } else {
                        moves.add(Move(from, targetSquare, isCapture = true))
                    }
                } else if (enPassantSquare != null && enPassantSquare == targetSquare) {
                    moves.add(Move(from, targetSquare, isCapture = true))
                }
            }
        }
    }

    private fun generateKnightMoves(from: Square, color: PieceColor, moves: MutableList<Move>) {
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((df, dr) in knightOffsets) {
            val tf = from.file + df
            val tr = from.rank + dr
            if (tf in 0..7 && tr in 0..7) {
                val target = getPiece(tf, tr)
                if (target == null) {
                    moves.add(Move(from, Square(tf, tr)))
                } else if (target.color != color) {
                    moves.add(Move(from, Square(tf, tr), isCapture = true))
                }
            }
        }
    }

    private fun generateSlidingMoves(from: Square, color: PieceColor, dirs: List<Pair<Int, Int>>, moves: MutableList<Move>) {
        for ((df, dr) in dirs) {
            var step = 1
            while (true) {
                val tf = from.file + df * step
                val tr = from.rank + dr * step
                if (tf !in 0..7 || tr !in 0..7) break
                val target = getPiece(tf, tr)
                if (target == null) {
                    moves.add(Move(from, Square(tf, tr)))
                } else {
                    if (target.color != color) {
                        moves.add(Move(from, Square(tf, tr), isCapture = true))
                    }
                    break
                }
                step++
            }
        }
    }

    private fun generateKingMoves(from: Square, color: PieceColor, moves: MutableList<Move>) {
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val tf = from.file + df
                val tr = from.rank + dr
                if (tf in 0..7 && tr in 0..7) {
                    val target = getPiece(tf, tr)
                    if (target == null) {
                        moves.add(Move(from, Square(tf, tr)))
                    } else if (target.color != color) {
                        moves.add(Move(from, Square(tf, tr), isCapture = true))
                    }
                }
            }
        }

        // Castling moves
        if (color == PieceColor.WHITE && from.file == 4 && from.rank == 0) {
            if (castlingWhiteKingside && getPiece(5, 0) == null && getPiece(6, 0) == null) {
                if (!isSquareAttacked(Square(4, 0), PieceColor.BLACK) &&
                    !isSquareAttacked(Square(5, 0), PieceColor.BLACK) &&
                    !isSquareAttacked(Square(6, 0), PieceColor.BLACK)
                ) {
                    moves.add(Move(from, Square(6, 0), isCastling = true))
                }
            }
            if (castlingWhiteQueenside && getPiece(1, 0) == null && getPiece(2, 0) == null && getPiece(3, 0) == null) {
                if (!isSquareAttacked(Square(4, 0), PieceColor.BLACK) &&
                    !isSquareAttacked(Square(3, 0), PieceColor.BLACK) &&
                    !isSquareAttacked(Square(2, 0), PieceColor.BLACK)
                ) {
                    moves.add(Move(from, Square(2, 0), isCastling = true))
                }
            }
        } else if (color == PieceColor.BLACK && from.file == 4 && from.rank == 7) {
            if (castlingBlackKingside && getPiece(5, 7) == null && getPiece(6, 7) == null) {
                if (!isSquareAttacked(Square(4, 7), PieceColor.WHITE) &&
                    !isSquareAttacked(Square(5, 7), PieceColor.WHITE) &&
                    !isSquareAttacked(Square(6, 7), PieceColor.WHITE)
                ) {
                    moves.add(Move(from, Square(6, 7), isCastling = true))
                }
            }
            if (castlingBlackQueenside && getPiece(1, 7) == null && getPiece(2, 7) == null && getPiece(3, 7) == null) {
                if (!isSquareAttacked(Square(4, 7), PieceColor.WHITE) &&
                    !isSquareAttacked(Square(3, 7), PieceColor.WHITE) &&
                    !isSquareAttacked(Square(2, 7), PieceColor.WHITE)
                ) {
                    moves.add(Move(from, Square(2, 7), isCastling = true))
                }
            }
        }
    }

    fun makeMove(move: Move): Boolean {
        val movingPiece = getPiece(move.from) ?: return false

        // En passant capture
        if (movingPiece.type == PieceType.PAWN && move.to == enPassantSquare && move.isCapture) {
            val capturedPawnRank = if (movingPiece.color == PieceColor.WHITE) move.to.rank - 1 else move.to.rank + 1
            setPiece(move.to.file, capturedPawnRank, null)
        }

        // Set en passant target if double pawn push
        enPassantSquare = if (movingPiece.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else null

        // Handle Castling rook move
        if (movingPiece.type == PieceType.KING && abs(move.to.file - move.from.file) == 2) {
            if (move.to.file == 6) { // Kingside
                val rook = getPiece(7, move.from.rank)
                setPiece(7, move.from.rank, null)
                setPiece(5, move.from.rank, rook)
            } else if (move.to.file == 2) { // Queenside
                val rook = getPiece(0, move.from.rank)
                setPiece(0, move.from.rank, null)
                setPiece(3, move.from.rank, rook)
            }
        }

        // Update castling rights
        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) {
                castlingWhiteKingside = false
                castlingWhiteQueenside = false
            } else {
                castlingBlackKingside = false
                castlingBlackQueenside = false
            }
        } else if (movingPiece.type == PieceType.ROOK) {
            if (move.from.file == 0 && move.from.rank == 0) castlingWhiteQueenside = false
            if (move.from.file == 7 && move.from.rank == 0) castlingWhiteKingside = false
            if (move.from.file == 0 && move.from.rank == 7) castlingBlackQueenside = false
            if (move.from.file == 7 && move.from.rank == 7) castlingBlackKingside = false
        }

        // Move piece (with optional promotion)
        setPiece(move.from, null)
        val finalPiece = if (move.promotion != null) Piece(move.promotion, movingPiece.color) else movingPiece
        setPiece(move.to, finalPiece)

        // Switch turn
        activeColor = activeColor.opposite()
        if (activeColor == PieceColor.WHITE) {
            fullmoveNumber++
        }
        return true
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}

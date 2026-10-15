package com.example

import com.example.engine.ChessBoard
import com.example.engine.ChessEngine
import com.example.model.PieceColor
import com.example.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testStartingPositionFen() {
        val board = ChessBoard()
        assertEquals(ChessBoard.STARTING_FEN, board.toFen())
        assertEquals(PieceColor.WHITE, board.activeColor)
    }

    @Test
    fun testLegalMovesStartingPosition() {
        val board = ChessBoard()
        val moves = board.generateLegalMoves()
        // Standard chess opening has exactly 20 legal moves (16 pawn moves + 4 knight moves)
        assertEquals(20, moves.size)
    }

    @Test
    fun testKingDetection() {
        val board = ChessBoard()
        val whiteKingSquare = board.findKing(PieceColor.WHITE)
        val blackKingSquare = board.findKing(PieceColor.BLACK)

        assertNotNull(whiteKingSquare)
        assertEquals(Square(4, 0), whiteKingSquare)

        assertNotNull(blackKingSquare)
        assertEquals(Square(4, 7), blackKingSquare)

        assertFalse(board.isCheck(PieceColor.WHITE))
        assertFalse(board.isCheck(PieceColor.BLACK))
    }

    @Test
    fun testEngineEvaluation() {
        val board = ChessBoard()
        val engine = ChessEngine()
        val (eval, arrows) = engine.findBestMoves(board, depth = 2)

        assertTrue(arrows.isNotEmpty())
        assertNotNull(arrows[0].from)
        assertNotNull(arrows[0].to)
    }
}

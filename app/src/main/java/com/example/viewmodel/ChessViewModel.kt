package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ChessBoard
import com.example.engine.ChessEngine
import com.example.model.ChessComGame
import com.example.model.EngineArrow
import com.example.model.GMAdvice
import com.example.model.Move
import com.example.model.PositionEvaluation
import com.example.model.Square
import com.example.service.ChessComApiService
import com.example.service.StockfishService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TacticalPuzzle(
    val id: String,
    val title: String,
    val description: String,
    val fen: String,
    val solutionMove: String, // e.g. "e4e5"
    val theme: String,
    val difficulty: String
)

data class ChessUiState(
    val currentFen: String = ChessBoard.STARTING_FEN,
    val isFlipped: Boolean = false,
    val evaluation: PositionEvaluation = PositionEvaluation(),
    val arrows: List<EngineArrow> = emptyList(),
    val gmAdvice: GMAdvice = GMAdvice(
        title = "Дебют партии",
        description = "Боритесь за центр пешками e4/d4 или развивайте коней Nf3/Nc3.",
        motifBadge = "Развитие",
        bestMoveNotation = "e2e4"
    ),
    val showArrows: Boolean = true,
    val autoAssist: Boolean = true,
    val isAnalyzing: Boolean = false,
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val lastMove: Move? = null,
    val moveHistory: List<Move> = emptyList(),
    val chessComUsername: String = "magnuscarlsen",
    val chessComGames: List<ChessComGame> = emptyList(),
    val isLoadingGames: Boolean = false,
    val currentPuzzleIndex: Int = 0,
    val puzzleSolved: Boolean? = null,
    val webViewUrl: String = "https://www.chess.com/play/computer"
)

class ChessViewModel : ViewModel() {
    private val chessEngine = ChessEngine()
    private val stockfishService = StockfishService()
    private val chessComApiService = ChessComApiService()

    private val localBoard = ChessBoard()

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    val puzzles = listOf(
        TacticalPuzzle(
            id = "1",
            title = "Королевская вилка",
            description = "Белый конь может атаковать ферзя и короля черных одновременно!",
            fen = "r1b1k2r/pppp1ppp/2n5/4q3/1bP5/2N1P3/PP1N1PPP/R2QKB1R w KQkq - 0 1",
            solutionMove = "d2f3",
            theme = "Двойной удар (Вилка)",
            difficulty = "Новичок"
        ),
        TacticalPuzzle(
            id = "2",
            title = "Мат в один ход",
            description = "Белый ферзь при поддержке слона готов нанести решающий удар по слабому полю f7.",
            fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 4",
            solutionMove = "f3f7",
            theme = "Мат на f7",
            difficulty = "Новичок"
        ),
        TacticalPuzzle(
            id = "3",
            title = "Смертельная связка",
            description = "Слон связывает черного ферзя с королем по диагонали.",
            fen = "r3k2r/pppq1ppp/2npbn2/4p3/4P3/2NP1B2/PPP2PPP/R1BQK2R w KQkq - 0 8",
            solutionMove = "c1g5",
            theme = "Связка",
            difficulty = "Любитель"
        )
    )

    init {
        runAnalysis()
    }

    fun onBoardFenChanged(fen: String, isFlipped: Boolean? = null) {
        if (fen.isBlank() || fen == _uiState.value.currentFen) return

        val success = localBoard.loadFen(fen)
        if (success) {
            _uiState.value = _uiState.value.copy(
                currentFen = fen,
                isFlipped = isFlipped ?: _uiState.value.isFlipped,
                selectedSquare = null,
                legalMovesForSelected = emptyList()
            )
            runAnalysis()
        }
    }

    fun toggleFlipBoard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun toggleShowArrows() {
        _uiState.value = _uiState.value.copy(showArrows = !_uiState.value.showArrows)
    }

    fun toggleAutoAssist() {
        _uiState.value = _uiState.value.copy(autoAssist = !_uiState.value.autoAssist)
    }

    fun setWebViewUrl(url: String) {
        _uiState.value = _uiState.value.copy(webViewUrl = url)
    }

    fun resetBoard() {
        localBoard.loadFen(ChessBoard.STARTING_FEN)
        _uiState.value = _uiState.value.copy(
            currentFen = ChessBoard.STARTING_FEN,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            lastMove = null,
            moveHistory = emptyList()
        )
        runAnalysis()
    }

    fun onSandboxSquareClicked(square: Square) {
        val currentSelected = _uiState.value.selectedSquare

        if (currentSelected != null) {
            // Check if clicking on a valid destination
            val move = _uiState.value.legalMovesForSelected.firstOrNull { it.to == square }
            if (move != null) {
                // Execute move
                localBoard.makeMove(move)
                val newFen = localBoard.toFen()
                _uiState.value = _uiState.value.copy(
                    currentFen = newFen,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    lastMove = move,
                    moveHistory = _uiState.value.moveHistory + move
                )
                runAnalysis()
                return
            }
        }

        // Select piece on this square if active color
        val piece = localBoard.getPiece(square)
        if (piece != null && piece.color == localBoard.activeColor) {
            val allLegalMoves = localBoard.generateLegalMoves()
            val pieceLegalMoves = allLegalMoves.filter { it.from == square }
            _uiState.value = _uiState.value.copy(
                selectedSquare = square,
                legalMovesForSelected = pieceLegalMoves
            )
        } else {
            _uiState.value = _uiState.value.copy(
                selectedSquare = null,
                legalMovesForSelected = emptyList()
            )
        }
    }

    fun runAnalysis() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)

            // Local engine calculation
            val (localEval, localArrows) = chessEngine.findBestMoves(localBoard, depth = 3)
            val bestMove = localBoard.generateLegalMoves().firstOrNull {
                localArrows.isNotEmpty() && it.from == localArrows[0].from && it.to == localArrows[0].to
            }
            val advice = chessEngine.generateGrandmasterAdvice(localBoard, bestMove, localEval)

            _uiState.value = _uiState.value.copy(
                evaluation = localEval,
                arrows = localArrows,
                gmAdvice = advice,
                isAnalyzing = false
            )

            // Asynchronously try Stockfish Cloud API for deeper evaluation
            launch(Dispatchers.IO) {
                val cloudResult = stockfishService.queryStockfish(localBoard.toFen())
                if (cloudResult != null) {
                    val (cloudEval, cloudArrows) = cloudResult
                    if (cloudArrows.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            evaluation = cloudEval,
                            arrows = cloudArrows
                        )
                    }
                }
            }
        }
    }

    fun fetchChessComGames(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingGames = true,
                chessComUsername = username
            )
            val games = chessComApiService.getRecentGames(username)
            _uiState.value = _uiState.value.copy(
                chessComGames = games,
                isLoadingGames = false
            )
        }
    }

    fun loadGamePgn(game: ChessComGame) {
        // Reset board and set starting position
        resetBoard()
    }

    fun loadPuzzle(index: Int) {
        if (index in puzzles.indices) {
            val puzzle = puzzles[index]
            localBoard.loadFen(puzzle.fen)
            _uiState.value = _uiState.value.copy(
                currentFen = puzzle.fen,
                currentPuzzleIndex = index,
                puzzleSolved = null,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = null
            )
            runAnalysis()
        }
    }

    fun checkPuzzleMove(move: Move) {
        val puzzle = puzzles.getOrNull(_uiState.value.currentPuzzleIndex) ?: return
        val moveStr = "${move.from.notation}${move.to.notation}"
        val isCorrect = moveStr.equals(puzzle.solutionMove, ignoreCase = true)

        localBoard.makeMove(move)
        _uiState.value = _uiState.value.copy(
            currentFen = localBoard.toFen(),
            puzzleSolved = isCorrect,
            lastMove = move
        )
        runAnalysis()
    }

    fun getLocalBoard(): ChessBoard = localBoard
}

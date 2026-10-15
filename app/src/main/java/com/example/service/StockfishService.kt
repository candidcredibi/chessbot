package com.example.service

import com.example.model.EngineArrow
import com.example.model.PositionEvaluation
import com.example.model.Square
import com.example.ui.theme.ChessGreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class StockfishService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun queryStockfish(fen: String): Pair<PositionEvaluation, List<EngineArrow>>? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedFen = URLEncoder.encode(fen, "UTF-8")
                val url = "https://stockfish.online/api/s/v2.php?fen=$encodedFen&depth=12"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 ChessMaster-Android")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    if (!json.optBoolean("success", true)) return@withContext null

                    val evalStr = json.optString("evaluation", "0.0")
                    val mate = if (json.has("mate")) json.optInt("mate") else null
                    val bestMoveStr = json.optString("bestmove", "")

                    val cp = (evalStr.toDoubleOrNull()?.times(100))?.toInt() ?: 0
                    val eval = PositionEvaluation(
                        centipawns = cp,
                        mateInMoves = mate,
                        isWhiteToMove = fen.contains(" w "),
                        depth = 12
                    )

                    val arrows = mutableListOf<EngineArrow>()
                    if (bestMoveStr.length >= 4) {
                        val fromSq = Square.fromNotation(bestMoveStr.substring(0, 2))
                        val toSq = Square.fromNotation(bestMoveStr.substring(2, 4))
                        if (fromSq != null && toSq != null) {
                            arrows.add(
                                EngineArrow(
                                    from = fromSq,
                                    to = toSq,
                                    color = ChessGreenPrimary,
                                    label = bestMoveStr,
                                    evaluation = evalStr,
                                    isBestMove = true
                                )
                            )
                        }
                    }
                    eval to arrows
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

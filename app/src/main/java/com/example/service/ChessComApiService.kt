package com.example.service

import com.example.model.ChessComGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChessComApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun getRecentGames(username: String): List<ChessComGame> {
        return withContext(Dispatchers.IO) {
            val games = mutableListOf<ChessComGame>()
            try {
                val cleanUser = username.trim().lowercase()
                // Fetch archives list
                val archivesUrl = "https://api.chess.com/pub/player/$cleanUser/games/archives"
                val archReq = Request.Builder()
                    .url(archivesUrl)
                    .header("User-Agent", "ChessMaster-App/1.0 (Android; Contact: plzztyandex@gmail.com)")
                    .build()

                client.newCall(archReq).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val json = JSONObject(body)
                    val archives = json.optJSONArray("archives") ?: return@withContext emptyList()
                    if (archives.length() == 0) return@withContext emptyList()

                    // Get the latest month archive
                    val latestArchiveUrl = archives.getString(archives.length() - 1)
                    val gamesReq = Request.Builder()
                        .url(latestArchiveUrl)
                        .header("User-Agent", "ChessMaster-App/1.0 (Android; Contact: plzztyandex@gmail.com)")
                        .build()

                    client.newCall(gamesReq).execute().use { gResponse ->
                        if (!gResponse.isSuccessful) return@withContext emptyList()
                        val gBody = gResponse.body?.string() ?: return@withContext emptyList()
                        val gJson = JSONObject(gBody)
                        val gamesArray = gJson.optJSONArray("games") ?: return@withContext emptyList()

                        val count = minOf(gamesArray.length(), 15)
                        for (i in (gamesArray.length() - count) until gamesArray.length()) {
                            val item = gamesArray.getJSONObject(i)
                            val white = item.optJSONObject("white")
                            val black = item.optJSONObject("black")
                            val whiteUser = white?.optString("username") ?: "Белые"
                            val whiteRating = white?.optInt("rating") ?: 1200
                            val blackUser = black?.optString("username") ?: "Черные"
                            val blackRating = black?.optInt("rating") ?: 1200
                            val pgn = item.optString("pgn", "")
                            val url = item.optString("url", "")
                            val timeControl = item.optString("time_control", "10+0")
                            val date = item.optLong("end_time", System.currentTimeMillis() / 1000).let {
                                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(it * 1000))
                            }
                            val whiteResult = white?.optString("result", "unknown") ?: ""
                            val result = if (whiteResult == "win") "1-0" else if (black?.optString("result") == "win") "0-1" else "1/2-1/2"

                            games.add(
                                ChessComGame(
                                    url = url,
                                    pgn = pgn,
                                    timeControl = timeControl,
                                    whitePlayer = whiteUser,
                                    whiteRating = whiteRating,
                                    blackPlayer = blackUser,
                                    blackRating = blackRating,
                                    result = result,
                                    date = date
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently return any loaded games or empty
            }
            games.reversed()
        }
    }
}

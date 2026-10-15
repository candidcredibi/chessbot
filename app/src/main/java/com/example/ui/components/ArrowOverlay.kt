package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.EngineArrow
import com.example.model.Square
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ArrowOverlay(
    arrows: List<EngineArrow>,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val boardWidth = size.width
        val boardHeight = size.height
        val squareSize = minOf(boardWidth, boardHeight) / 8f

        // Draw each arrow
        for (arrow in arrows) {
            drawEngineArrow(
                arrow = arrow,
                squareSize = squareSize,
                isFlipped = isFlipped
            )
        }
    }
}

private fun DrawScope.drawEngineArrow(
    arrow: EngineArrow,
    squareSize: Float,
    isFlipped: Boolean
) {
    val fromPos = getSquareCenter(arrow.from, squareSize, isFlipped)
    val toPos = getSquareCenter(arrow.to, squareSize, isFlipped)

    val dx = toPos.x - fromPos.x
    val dy = toPos.y - fromPos.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 10f) return

    val angle = atan2(dy, dx)
    val strokeWidth = if (arrow.isBestMove) squareSize * 0.16f else squareSize * 0.11f
    val headLength = squareSize * 0.38f
    val headWidth = squareSize * 0.32f

    // Draw origin square glow
    drawCircle(
        color = arrow.color.copy(alpha = 0.28f),
        radius = squareSize * 0.36f,
        center = fromPos
    )

    // Calculate end point of arrow line so it doesn't overshoot the head
    val endLineX = toPos.x - headLength * 0.85f * cos(angle)
    val endLineY = toPos.y - headLength * 0.85f * sin(angle)

    // Draw main arrow shaft
    drawLine(
        color = arrow.color.copy(alpha = 0.88f),
        start = fromPos,
        end = Offset(endLineX, endLineY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Arrowhead polygon
    val arrowHeadPath = Path().apply {
        // Tip of arrow
        moveTo(toPos.x, toPos.y)

        // Left wing
        val leftX = toPos.x - headLength * cos(angle) - headWidth * 0.5f * sin(angle)
        val leftY = toPos.y - headLength * sin(angle) + headWidth * 0.5f * cos(angle)
        lineTo(leftX, leftY)

        // Indented base
        val baseX = toPos.x - headLength * 0.75f * cos(angle)
        val baseY = toPos.y - headLength * 0.75f * sin(angle)
        lineTo(baseX, baseY)

        // Right wing
        val rightX = toPos.x - headLength * cos(angle) + headWidth * 0.5f * sin(angle)
        val rightY = toPos.y - headLength * sin(angle) - headWidth * 0.5f * cos(angle)
        lineTo(rightX, rightY)

        close()
    }

    drawPath(
        path = arrowHeadPath,
        color = arrow.color
    )

    // Outline around arrow head for maximum clarity
    drawPath(
        path = arrowHeadPath,
        color = Color.Black.copy(alpha = 0.4f),
        style = Stroke(width = 2f)
    )
}

fun getSquareCenter(square: Square, squareSize: Float, isFlipped: Boolean): Offset {
    val fileIndex = if (isFlipped) 7 - square.file else square.file
    val rankIndex = if (isFlipped) square.rank else 7 - square.rank

    val x = (fileIndex + 0.5f) * squareSize
    val y = (rankIndex + 0.5f) * squareSize
    return Offset(x, y)
}

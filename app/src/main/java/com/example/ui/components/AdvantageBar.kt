package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PositionEvaluation

@Composable
fun AdvantageBar(
    evaluation: PositionEvaluation,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rawWhiteProb = evaluation.winProbabilityWhite
    val whiteRatio = if (isFlipped) (1f - rawWhiteProb) else rawWhiteProb

    val animatedWhiteRatio by animateFloatAsState(
        targetValue = whiteRatio,
        animationSpec = tween(durationMillis = 350),
        label = "evalBarAnim"
    )

    Column(
        modifier = modifier
            .width(26.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF262421))
    ) {
        // Black portion (top if normal, bottom if flipped)
        val blackWeight = maxOf(0.02f, 1f - animatedWhiteRatio)
        val whiteWeight = maxOf(0.02f, animatedWhiteRatio)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(blackWeight)
                .background(Color(0xFF2E3136)),
            contentAlignment = Alignment.TopCenter
        ) {
            if (blackWeight > 0.2f && !isFlipped && evaluation.centipawns < -100) {
                Text(
                    text = evaluation.displayString,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // White portion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(whiteWeight)
                .background(Color(0xFFFFFFFF)),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (whiteWeight > 0.2f && !isFlipped && evaluation.centipawns >= -100) {
                Text(
                    text = evaluation.displayString,
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

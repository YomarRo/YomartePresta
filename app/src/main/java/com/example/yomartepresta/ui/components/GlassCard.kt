package com.example.yomartepresta.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.yomartepresta.ui.theme.GlassGold
import com.example.yomartepresta.ui.theme.GraphiteDark

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = GlassGold,
                shape = RoundedCornerShape(24.dp)
            ),
        color = GraphiteDark,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

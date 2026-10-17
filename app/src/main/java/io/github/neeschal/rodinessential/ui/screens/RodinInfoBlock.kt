package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The quiet title-and-copy block shared by several pages. */
@Composable
fun InfoBlock(title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.W700,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.5.sp,
                lineHeight = 12.5.sp * 1.35f,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

package de.thomaskuenneth.viewfainder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

@Composable
fun BoxScope.Info() {
    Text(
        text = stringResource(R.string.info),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(color = Color(0xa0000000))
            .safeContentPadding(),
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge
    )
}
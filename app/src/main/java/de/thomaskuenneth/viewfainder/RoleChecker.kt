package de.thomaskuenneth.viewfainder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun RoleChecker(
    modifier: Modifier = Modifier,
    scope: CoroutineScope,
    roleStatus: RoleStatus,
    shouldShowMessage: Boolean,
    requestRole: () -> Unit,
    hideMessage: () -> Unit,
) {
    when (roleStatus) {
        RoleStatus.NOT_HELD -> {
            if (shouldShowMessage) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.role_not_held),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { scope.launch { hideMessage() } },
                        ) {
                            Text(stringResource(R.string.hide_message))
                        }
                        TextButton(
                            onClick = { scope.launch { requestRole() } },
                        ) {
                            Text(stringResource(R.string.request_role))
                        }
                    }
                }
            }
        }

        RoleStatus.UNAVAILABLE -> {
            Box(
                modifier = modifier
                    .background(color = MaterialTheme.colorScheme.errorContainer)
                    .padding(all = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.role_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        else -> {}
    }
}

package de.thomaskuenneth.viewfainder

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


const val WEIGHT_IMAGE = 0.3F

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Results(
    viewModel: MainViewModel,
    uiState: UiState,
    roleStatus: RoleStatus,
    shouldShowMessage: Boolean,
    requestRole: () -> Unit,
    hideMessage: () -> Unit,
    scope: CoroutineScope,
    finish: () -> Unit
) {
    val density = LocalDensity.current
    var gap by remember { mutableStateOf(0.dp) }
    val clipboardManager = LocalClipboardManager.current
    val copyToClipBoard: (String) -> Unit = { s -> clipboardManager.setText(AnnotatedString(s)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.weight(WEIGHT_IMAGE)) {
            CapturedImage(viewModel)
        }
        Box(
            modifier = Modifier
                .weight(1F - WEIGHT_IMAGE)
                .safeContentPadding()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState is UiState.Success) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MarkdownText(
                            modifier = Modifier.weight(1.0F),
                            markdown = uiState.description,
                            style = MaterialTheme.typography.bodyLarge.merge(MaterialTheme.colorScheme.onBackground)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text(text = stringResource(R.string.copy_to_clipboard))
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { copyToClipBoard(uiState.description) }) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_content_copy_24),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(R.string.copy_to_clipboard)
                                )
                            }
                        }
                    }
                    uiState.actions.forEach { action ->
                        when (action.first) {
                            Action.VCARD -> {
                                TextButton(
                                    onClick = {
                                        scope.launch { saveVCF(action.second) }
                                    },
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Text(text = stringResource(id = R.string.save_as_vcf_file))
                                }
                            }

                            Action.TRACKING_NUMBER -> {
                                TextButton(
                                    onClick = {
                                        scope.launch { copyToClipBoard(action.second) }
                                    },
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = R.string.copy_something_to_clipboard, action.second
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (uiState is UiState.Error) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.errorContainer)
                            .align(Alignment.CenterHorizontally)
                            .padding(all = 16.dp),
                        style = MaterialTheme.typography.bodyLarge.merge(MaterialTheme.colorScheme.onErrorContainer)
                    )
                }
                RoleChecker(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    scope = scope,
                    roleStatus = roleStatus,
                    shouldShowMessage = shouldShowMessage,
                    requestRole = requestRole,
                    hideMessage = hideMessage
                )
                Spacer(modifier = Modifier.height(gap))
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .onGloballyPositioned {
                        gap = with(density) { it.size.height.toDp() }
                    }, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.reset() },
                ) {
                    Text(text = stringResource(id = R.string.start_over))
                }
                Button(
                    onClick = { finish() },
                ) {
                    Text(text = stringResource(id = R.string.done))
                }
            }
        }
    }
}

private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

private fun currentDateAndTimeAsString(): String {
    val now = LocalDateTime.now()
    return now.format(formatter)
}

private fun saveVCF(data: String) {
    val parent = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS
    ).also {
        it.mkdirs()
    }
    File(
        parent, "vcard_${currentDateAndTimeAsString()}.vcf"
    ).also { file ->
        FileOutputStream(file).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                bos.write(data.toByteArray())
                bos.flush()
            }
        }
    }
}

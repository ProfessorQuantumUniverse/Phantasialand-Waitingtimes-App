package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.ui.theme.main.WaitTimeError

/** Shared frame for the loading, empty and error placeholders so they line up identically. */
@Composable
private fun StatePlaceholder(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        content()
    }
}

@Composable
fun ErrorView(
    error: WaitTimeError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    StatePlaceholder(
        icon = Icons.Default.CloudOff,
        title = stringResource(R.string.error_title),
        subtitle = stringResource(error.messageResId),
        iconTint = MaterialTheme.colorScheme.error,
        modifier = modifier
    ) {
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun EmptyView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    StatePlaceholder(
        icon = Icons.Outlined.Inbox,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    StatePlaceholder(
        icon = null,
        title = stringResource(R.string.loading_title),
        subtitle = stringResource(R.string.loading_subtitle),
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** Maps a failure reason onto the message the user sees. */
private val WaitTimeError.messageResId: Int
    get() = when (this) {
        WaitTimeError.NO_CONNECTION -> R.string.error_no_connection
        WaitTimeError.TIMEOUT -> R.string.error_timeout
        WaitTimeError.SERVER -> R.string.error_server
        WaitTimeError.UNKNOWN -> R.string.error_unknown
    }

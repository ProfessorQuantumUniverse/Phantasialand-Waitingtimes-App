package com.quantum_prof.phantalandwaittimes.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.theme.appAccents
import com.quantum_prof.phantalandwaittimes.ui.theme.components.ActiveAlertsPanel
import com.quantum_prof.phantalandwaittimes.ui.theme.components.EmptyView
import com.quantum_prof.phantalandwaittimes.ui.theme.components.ErrorView
import com.quantum_prof.phantalandwaittimes.ui.theme.components.LoadingView
import com.quantum_prof.phantalandwaittimes.ui.theme.components.WaitTimeAlertDialog
import com.quantum_prof.phantalandwaittimes.ui.theme.components.WaitTimeCard
import com.quantum_prof.phantalandwaittimes.ui.theme.main.MainViewModel
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortDirection
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortType
import com.quantum_prof.phantalandwaittimes.ui.theme.main.WaitTimeUiState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private const val API_URL = "https://www.wartezeiten.app/"

/** How often the relative "updated x ago" label is recomputed. */
private const val TICK_INTERVAL_MILLIS = 30_000L

/** How long a notification-linked attraction stays visually marked after being scrolled to. */
private const val DEEP_LINK_HIGHLIGHT_MILLIS = 2_500L

@Composable
fun WaitTimeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    deepLinkAttractionCode: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationPermission = rememberNotificationPermissionState()

    var alertDialogFor by remember { mutableStateOf<AttractionWaitTime?>(null) }

    // Coming back to the screen picks up newer data; the repository's cache makes this cheap.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshIfStale()
        onPauseOrDispose { }
    }

    // Explicit Box so the content is stacked over the background rather than relying on the
    // implicit stacking of two siblings at the composition root.
    Box(Modifier.fillMaxSize()) {
        ParkBackground()

        WaitTimeScreenContent(
            uiState = uiState,
            deepLinkAttractionCode = deepLinkAttractionCode,
            onDeepLinkHandled = onDeepLinkHandled,
            onRefresh = { viewModel.refresh() },
            onSortTypeChange = viewModel::changeSortType,
            onSortDirectionToggle = viewModel::toggleSortDirection,
            onFilterOnlyOpenChange = viewModel::setFilterOnlyOpen,
            onFavoriteToggle = viewModel::toggleFavorite,
            onAlertClick = { attraction ->
                alertDialogFor = attraction
                // Ask for the permission exactly when the feature is first used.
                if (!notificationPermission.isEnabled) notificationPermission.request()
            },
            onRemoveAlert = viewModel::removeAlert
        )
    }

    alertDialogFor?.let { attraction ->
        WaitTimeAlertDialog(
            attraction = attraction,
            currentAlert = uiState.activeAlerts.firstOrNull { it.attractionCode == attraction.code },
            notificationsEnabled = notificationPermission.isEnabled,
            onDismiss = { alertDialogFor = null },
            onSetAlert = { target -> viewModel.addAlert(attraction, target) },
            onRemoveAlert = { viewModel.removeAlert(attraction.code) }
        )
    }
}

/** Park photo with a theme-aware scrim so text stays legible in both light and dark. */
@Composable
private fun ParkBackground() {
    val scrim = appAccents.scrim

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_park),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Previously a flat dark overlay was applied in both themes, which darkened the light
        // theme as well. The gradient now follows the active scheme.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(scrim.copy(alpha = 0.72f), scrim.copy(alpha = 0.92f))
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaitTimeScreenContent(
    uiState: WaitTimeUiState,
    deepLinkAttractionCode: String?,
    onDeepLinkHandled: () -> Unit,
    onRefresh: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onFilterOnlyOpenChange: (Boolean) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onAlertClick: (AttractionWaitTime) -> Unit,
    onRemoveAlert: (String) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showAlertsPanel by rememberSaveable { mutableStateOf(false) }

    // The bar slides away while scrolling down, giving the list the full height on a phone.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            WaitTimeTopBar(
                sortType = uiState.sortType,
                sortDirection = uiState.sortDirection,
                showSortMenu = showSortMenu,
                onShowSortMenuChange = { showSortMenu = it },
                onSortTypeChange = onSortTypeChange,
                onSortDirectionToggle = onSortDirectionToggle,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                filterOnlyOpen = uiState.filterOnlyOpen,
                onFilterOnlyOpenChange = onFilterOnlyOpenChange,
                openCount = uiState.openCount,
                totalCount = uiState.totalCount,
                activeAlertCount = uiState.activeAlerts.size,
                showAlerts = showAlertsPanel,
                onToggleAlerts = { showAlertsPanel = !showAlertsPanel }
            )

            if (showAlertsPanel) {
                ActiveAlertsPanel(
                    alerts = uiState.activeAlerts,
                    waitTimes = uiState.waitTimes,
                    onEditAlert = onAlertClick,
                    onRemoveAlert = onRemoveAlert,
                    onCollapse = { showAlertsPanel = false }
                )
            }

            val error = uiState.error
            when {
                error != null && !uiState.hasContent && !uiState.isLoading ->
                    ErrorView(
                        error = error,
                        onRetry = onRefresh,
                        modifier = Modifier.weight(1f)
                    )

                uiState.isLoading && !uiState.hasContent ->
                    LoadingView(modifier = Modifier.weight(1f))

                !uiState.hasContent ->
                    EmptyView(
                        title = stringResource(
                            if (uiState.filterOnlyOpen) {
                                R.string.empty_all_closed_title
                            } else {
                                R.string.empty_no_data_title
                            }
                        ),
                        subtitle = stringResource(
                            if (uiState.filterOnlyOpen) {
                                R.string.empty_all_closed_subtitle
                            } else {
                                R.string.empty_no_data_subtitle
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )

                else -> AttractionList(
                    uiState = uiState,
                    deepLinkAttractionCode = deepLinkAttractionCode,
                    onDeepLinkHandled = onDeepLinkHandled,
                    onRefresh = onRefresh,
                    onFavoriteToggle = onFavoriteToggle,
                    onAlertClick = onAlertClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttractionList(
    uiState: WaitTimeUiState,
    deepLinkAttractionCode: String?,
    onDeepLinkHandled: () -> Unit,
    onRefresh: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onAlertClick: (AttractionWaitTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val alertCodes = uiState.activeAlertCodes
    val listState = rememberLazyListState()
    val showsFreshnessBanner = uiState.lastUpdated > 0L

    var highlightedCode by remember { mutableStateOf<String?>(null) }

    // Scroll to the attraction the notification pointed at and mark it briefly.
    //
    // Keyed on `hasContent` rather than on the list itself: the effect only needs to re-run when
    // data first appears, and comparing the whole list on every refresh would restart it (and
    // cancel an in-flight scroll) each time the wait times update.
    //
    // The deep link is released at the very end. Releasing it earlier would flip the key and
    // cancel this coroutine before the scroll and highlight had a chance to run.
    LaunchedEffect(deepLinkAttractionCode, uiState.hasContent) {
        val code = deepLinkAttractionCode ?: return@LaunchedEffect
        if (!uiState.hasContent) return@LaunchedEffect // Still loading; re-runs once data arrives.

        val index = uiState.waitTimes.indexOfFirst { it.code == code }
        if (index >= 0) {
            listState.animateScrollToItem(if (showsFreshnessBanner) index + 1 else index)
            highlightedCode = code
            delay(DEEP_LINK_HIGHLIGHT_MILLIS)
            highlightedCode = null
        }
        // Also released when the attraction is not in the list (filtered out, or gone from the
        // feed), so a stale link cannot re-trigger on every refresh.
        onDeepLinkHandled()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showsFreshnessBanner) {
                item(key = "freshness") {
                    FreshnessBanner(
                        timestamp = uiState.lastUpdated,
                        isOffline = uiState.isOfflineData
                    )
                }
            }

            items(
                items = uiState.waitTimes,
                key = { it.code }
            ) { attraction ->
                WaitTimeCard(
                    attraction = attraction,
                    isFavorite = attraction.code in uiState.favoriteCodes,
                    hasAlert = attraction.code in alertCodes,
                    isHighlighted = attraction.code == highlightedCode,
                    onFavoriteToggle = { onFavoriteToggle(attraction.code) },
                    onAlertClick = { onAlertClick(attraction) },
                    modifier = Modifier.animateItem()
                )
            }

            item(key = "footer") { AttributionFooter() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaitTimeTopBar(
    sortType: SortType,
    sortDirection: SortDirection,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onSortDirectionToggle: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.title_wait_times)) },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            // Both states are transparent so the park photo stays visible as the bar collapses.
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        actions = {
            IconButton(onClick = onSortDirectionToggle) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(
                        if (sortDirection == SortDirection.ASCENDING) {
                            R.string.sort_ascending
                        } else {
                            R.string.sort_descending
                        }
                    ),
                    // One icon, rotated, instead of a bespoke arrow drawable.
                    modifier = Modifier.rotate(
                        if (sortDirection == SortDirection.ASCENDING) 180f else 0f
                    )
                )
            }

            Box {
                IconButton(onClick = { onShowSortMenuChange(!showSortMenu) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_options)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onShowSortMenuChange(false) }
                ) {
                    SortType.entries.forEach { type ->
                        val selected = type == sortType
                        DropdownMenuItem(
                            leadingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Spacer(Modifier.size(18.dp))
                                }
                            },
                            text = {
                                Text(
                                    text = stringResource(
                                        when (type) {
                                            SortType.NAME -> R.string.sort_by_name
                                            SortType.WAIT_TIME -> R.string.sort_by_wait_time
                                        }
                                    ),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                onSortTypeChange(type)
                                onShowSortMenuChange(false)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun FilterRow(
    filterOnlyOpen: Boolean,
    onFilterOnlyOpenChange: (Boolean) -> Unit,
    openCount: Int,
    totalCount: Int,
    activeAlertCount: Int,
    showAlerts: Boolean,
    onToggleAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.filter_only_open),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (totalCount > 0) {
                    Text(
                        text = stringResource(R.string.filter_open_of_total, openCount, totalCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (activeAlertCount > 0) {
                BadgedBox(
                    badge = {
                        Badge { Text(activeAlertCount.toString()) }
                    }
                ) {
                    IconButton(onClick = onToggleAlerts) {
                        Icon(
                            imageVector = if (showAlerts) {
                                Icons.Default.NotificationsOff
                            } else {
                                Icons.Default.Notifications
                            },
                            contentDescription = stringResource(
                                if (showAlerts) R.string.alerts_hide else R.string.alerts_show
                            ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Switch(
                checked = filterOnlyOpen,
                onCheckedChange = onFilterOnlyOpenChange
            )
        }
    }
}

/**
 * Shows how old the displayed data is. The relative label re-renders on a timer, so it no longer
 * stays frozen at "just now" for as long as the screen happens to stay composed.
 */
@Composable
private fun FreshnessBanner(
    timestamp: Long,
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    val relativeLabel = rememberRelativeTimeLabel(timestamp)
    val clockLabel = remember(timestamp) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))
    }

    val text = if (isOffline) {
        stringResource(R.string.updated_offline_prefix, relativeLabel)
    } else {
        relativeLabel
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
            contentDescription = null,
            tint = if (isOffline) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.updated_at_time, text, clockLabel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberRelativeTimeLabel(timestamp: Long): String {
    var now by remember(timestamp) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timestamp) {
        while (true) {
            delay(TICK_INTERVAL_MILLIS)
            now = System.currentTimeMillis()
        }
    }

    val minutes = TimeUnit.MILLISECONDS.toMinutes((now - timestamp).coerceAtLeast(0L))
    return when {
        minutes < 1 -> stringResource(R.string.updated_just_now)
        minutes < 60 -> pluralStringResource(
            R.plurals.updated_minutes_ago,
            minutes.toInt(),
            minutes.toInt()
        )

        else -> {
            val hours = TimeUnit.MINUTES.toHours(minutes).toInt()
            pluralStringResource(R.plurals.updated_hours_ago, hours, hours)
        }
    }
}

@Composable
private fun AttributionFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, API_URL.toUri()))
                } catch (_: ActivityNotFoundException) {
                    // No browser installed; nothing sensible to fall back to.
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.footer_data_by),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.footer_source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    }
}

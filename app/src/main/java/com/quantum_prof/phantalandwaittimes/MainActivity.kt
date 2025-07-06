package com.quantum_prof.phantalandwaittimes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.theme.*
import com.quantum_prof.phantalandwaittimes.ui.theme.components.*
import com.quantum_prof.phantalandwaittimes.ui.theme.main.MainViewModel
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortDirection
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortType
import com.quantum_prof.phantalandwaittimes.ui.theme.main.WaitTimeUiState
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Notifications disabled, but app continues to work
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()

        setContent {
            PhantasialandWaitTimesTheme {
                WaitTimeApp()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted - no action needed
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@DrawableRes
fun getAttractionIconResId(code: String): Int {
    return when (code) {
        "3136", "3137", "3532", "3235", "3630", "3539", "3733" -> R.drawable.ic_coaster
        "3238", "3139", "3735" -> R.drawable.ic_waterride
        "34", "3431", "3432" -> R.drawable.ic_default_ride
        "31", "32", "33", "35", "3632", "3633", "3634", "3635", "3638", "3730", "3731", "3732" -> R.drawable.ic_childride
        else -> R.drawable.ic_default_ride
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitTimeApp(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showNotificationDialogFor by remember { mutableStateOf<AttractionWaitTime?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Original Hintergrundbild zurück!
        Image(
            painter = painterResource(id = R.drawable.background_park),
            contentDescription = "Park Hintergrund",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Halbtransparente Ebene für bessere Lesbarkeit
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background.copy(alpha = 0.6f))
        )

        // Main content
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                ImprovedTopAppBar(
                    uiState = uiState,
                    showSortMenu = showSortMenu,
                    onShowSortMenuChange = { showSortMenu = it },
                    onSortDirectionToggle = { viewModel.toggleSortDirection() },
                    onSortTypeChange = { sortType ->
                        viewModel.changeSortOrder(sortType, uiState.currentSortDirection)
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            WaitTimeContent(
                uiState = uiState,
                onRefresh = { viewModel.fetchWaitTimes(isRefresh = true) },
                onFavoriteToggle = { code -> viewModel.toggleFavorite(code) },
                onFilterOnlyOpenChanged = { enabled -> viewModel.setFilterOnlyOpen(enabled) },
                onAddAlertClicked = { attraction -> showNotificationDialogFor = attraction },
                onRemoveAlert = { code -> viewModel.removeAlert(code) },
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Alert dialog
        showNotificationDialogFor?.let { attraction ->
            val currentAlert = uiState.activeAlerts.find { it.attractionCode == attraction.code }
            WaitTimeAlertDialog(
                attraction = attraction,
                currentAlert = currentAlert,
                onDismiss = { showNotificationDialogFor = null },
                onSetAlert = { targetTime ->
                    viewModel.addAlert(attraction, targetTime)
                },
                onRemoveAlert = {
                    viewModel.removeAlert(attraction.code)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTopAppBar(
    uiState: WaitTimeUiState,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Schlichte Material 3 TopAppBar
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Phantasialand Queue Times",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Sort Direction Toggle Button with animated arrow
                    IconButton(
                        onClick = onSortDirectionToggle
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort_arrow),
                            contentDescription = when (uiState.currentSortDirection) {
                                SortDirection.ASCENDING -> "Sort Ascending"
                                SortDirection.DESCENDING -> "Sort Descending"
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.rotate(
                                when (uiState.currentSortDirection) {
                                    SortDirection.ASCENDING -> 180f // Pfeil nach oben
                                    SortDirection.DESCENDING -> 0f  // Pfeil nach unten
                                }
                            )
                        )
                    }

                    // Sort Options Menu Button
                    IconButton(
                        onClick = { onShowSortMenuChange(!showSortMenu) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onShowSortMenuChange(false) }
                ) {
                    // Sort Type Options
                    SortType.values().forEach { sortType ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Zeige Checkmark für aktuell ausgewählten Sort Type
                                    if (sortType == uiState.currentSortType) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        when (sortType) {
                                            SortType.NAME -> "Nach Name"
                                            SortType.WAIT_TIME -> "Nach Wartezeit"
                                        },
                                        color = if (sortType == uiState.currentSortType) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            },
                            onClick = {
                                onSortTypeChange(sortType)
                                onShowSortMenuChange(false)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitTimeContent(
    uiState: WaitTimeUiState,
    onRefresh: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onFilterOnlyOpenChanged: (Boolean) -> Unit,
    onAddAlertClicked: (AttractionWaitTime) -> Unit,
    onRemoveAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Column(modifier = modifier.fillMaxSize()) {
        // Active Alerts Panel state
        var showActiveAlerts by remember { mutableStateOf(false) }

        // Filter controls
        if (uiState.waitTimes.isNotEmpty() || (uiState.error == null && !uiState.isLoading)) {
            ModernFilterControls(
                filterOnlyOpen = uiState.filterOnlyOpen,
                onFilterOnlyOpenChanged = onFilterOnlyOpenChanged,
                activeAlertsCount = uiState.activeAlerts.size,
                showActiveAlerts = showActiveAlerts,
                onToggleActiveAlerts = { showActiveAlerts = !showActiveAlerts }
            )
        }

        // Active Alerts Panel - show when there are active alerts and expanded
        if (uiState.activeAlerts.isNotEmpty() && showActiveAlerts) {
            ActiveAlertsPanel(
                alerts = uiState.activeAlerts,
                waitTimes = uiState.waitTimes,
                onEditAlert = onAddAlertClicked,
                onRemoveAlert = onRemoveAlert,
                onCollapse = { showActiveAlerts = false }
            )
        }

        // Main content
        when {
            uiState.error != null && uiState.waitTimes.isEmpty() && !uiState.isLoading -> {
                ModernErrorView(
                    errorMessage = uiState.error,
                    onRetry = onRefresh,
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.waitTimes.isEmpty() && !uiState.isLoading -> {
                ModernEmptyView(
                    title = if (uiState.filterOnlyOpen) "All Attractions are closed!" else "No Data Available",
                    subtitle = "Try refreshing or later again. Check your internet connection.",
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.isLoading && uiState.waitTimes.isEmpty() -> {
                ModernLoadingView(
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uiState.lastUpdated > 0) {
                            item {
                                ModernLastUpdatedHeader(
                                    timestamp = uiState.lastUpdated,
                                    isOffline = uiState.isOfflineData,
                                    modifier = Modifier.slideInFromBottom(0)
                                )
                            }
                        }

                        itemsIndexed(
                            items = uiState.waitTimes,
                            key = { _, attraction -> attraction.code }
                        ) { index, attraction ->
                            val isFavorite = attraction.code in uiState.favoriteCodes
                            val currentAlert = uiState.activeAlerts.find { it.attractionCode == attraction.code }

                            // Sortiere Favoriten nach oben mit Animation
                            WaitTimeCard(
                                attraction = attraction,
                                isFavorite = isFavorite,
                                hasAlert = uiState.activeAlerts.any { it.attractionCode == attraction.code },
                                onFavoriteToggle = { onFavoriteToggle(attraction.code) },
                                onAlertClick = { onAddAlertClicked(attraction) }, // Always open dialog
                                currentAlert = currentAlert, // Pass the current alert for trend indicator
                                modifier = Modifier
                                    .animateItem() // Nur smooth item repositioning, keine Fade-in Animation
                            )
                        }

                        item {
                            ModernFooter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernFilterControls(
    filterOnlyOpen: Boolean,
    onFilterOnlyOpenChanged: (Boolean) -> Unit,
    activeAlertsCount: Int,
    showActiveAlerts: Boolean,
    onToggleActiveAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Schlichte Material 3 Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Schöneres Filter Icon mit Animation
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (filterOnlyOpen) 15f else 0f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Nur geöffnete Attraktionen",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Active Alerts Button mit verbesserter Badge
            if (activeAlertsCount > 0) {
                Box {
                    IconButton(
                        onClick = onToggleActiveAlerts,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (showActiveAlerts) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = if (showActiveAlerts) "Alerts ausblenden" else "Aktive Alerts anzeigen",
                            tint = if (showActiveAlerts) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (!showActiveAlerts) Modifier.pulsingGlow(MaterialTheme.colorScheme.primary, 2000)
                                    else Modifier
                                )
                        )
                    }

                    // Schönere Badge Positionierung
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    ) {
                        Text(
                            text = activeAlertsCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            Switch(
                checked = filterOnlyOpen,
                onCheckedChange = onFilterOnlyOpenChanged
            )
        }
    }
}

@Composable
fun ModernLastUpdatedHeader(
    timestamp: Long,
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timestamp)
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(timestamp))

    val ageText = when {
        minutesAgo < 1 -> "Just now"
        minutesAgo == 1L -> "1 Minute ago"
        minutesAgo < 60 -> " $minutesAgo Minutes ago"
        minutesAgo < 120 -> "1 hour ago"
        else -> " ${TimeUnit.MINUTES.toHours(minutesAgo)} hours ago"
    }

    // Schlichte Material 3 Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                contentDescription = null,
                tint = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${if (isOffline) "Offline" else "Refreshed"} $ageText ($timeFormatted)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiUrl = "https://www.wartezeiten.app/"

    // Schlichte Material 3 Card für Footer
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiUrl))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Data provided by",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "wartezeiten.app",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

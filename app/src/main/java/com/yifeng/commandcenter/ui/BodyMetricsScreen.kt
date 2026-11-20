package com.yifeng.commandcenter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.metrics.BodyMetricsCalculator
import com.yifeng.commandcenter.metrics.BodyMetricsPeriod
import com.yifeng.commandcenter.metrics.BodyMetricsSummary
import com.yifeng.commandcenter.metrics.BodyMetricsSyncState
import com.yifeng.commandcenter.metrics.GoogleSheetsUrlNormalizer
import com.yifeng.commandcenter.metrics.SheetUrlResult
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceElevated
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.Locale

@Composable
fun BodyMetricsScreen(
    syncState: BodyMetricsSyncState,
    selectedPeriod: BodyMetricsPeriod,
    onSelectPeriod: (BodyMetricsPeriod) -> Unit,
    onRefresh: () -> Unit,
    onOpenConfig: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var showMovingAverage by rememberSaveable { mutableStateOf(true) }
    val summary = remember(syncState.entries, selectedPeriod) {
        BodyMetricsCalculator.summarize(syncState.entries, selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HevyntPageHorizontalPadding)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = HevyntTextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Body metrics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                )
                Text(
                    text = formatSyncStatusLine(syncState),
                    fontSize = 12.sp,
                    color = HevyntTextSecondary,
                )
            }
            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    color = HevyntAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(20.dp),
                )
            } else {
                IconButton(
                    onClick = onRefresh,
                    enabled = syncState.isConfigured,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Google Sheet",
                        tint = if (syncState.isConfigured) HevyntTextPrimary else HevyntTextSecondary,
                    )
                }
            }
            IconButton(
                onClick = onOpenConfig,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Google Sheet settings",
                    tint = HevyntTextPrimary,
                )
            }
        }

        if (syncState.lastError != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HevyntAccentContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Sync failed",
                        color = HevyntError,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = syncState.lastError,
                        color = HevyntTextSecondary,
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Retry",
                            color = HevyntTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onRefresh),
                        )
                        Text(
                            text = "Change link",
                            color = HevyntAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onOpenConfig),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Period selector pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BodyMetricsPeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) HevyntAccent else HevyntSurfaceRaised,
                    border = if (isSelected) null else BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectPeriod(period) },
                ) {
                    Text(
                        text = period.label,
                        color = if (isSelected) Color.White else HevyntTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(vertical = 9.dp)
                            .fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Weight card
        WeightChartCard(
            summary = summary,
            showMovingAverage = showMovingAverage,
            onToggleMovingAverage = { showMovingAverage = !showMovingAverage },
        )

        Spacer(Modifier.height(12.dp))

        // Calories card
        CaloriesChartCard(summary = summary)
    }
}

@Composable
private fun WeightChartCard(
    summary: BodyMetricsSummary,
    showMovingAverage: Boolean,
    onToggleMovingAverage: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(color = HevyntAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Weight",
                        color = HevyntTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (showMovingAverage) HevyntSurfaceInteractive else Color.Transparent,
                    border = BorderStroke(1.dp, HevyntBorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleMovingAverage),
                ) {
                    Text(
                        text = "7d avg",
                        color = if (showMovingAverage) HevyntTextPrimary else HevyntTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = summary.latestWeightKg?.let { "${formatWeightKg(it)} kg" } ?: "—",
                    fontFamily = HevyntDisplayFont,
                    fontSize = 32.sp,
                    color = HevyntTextPrimary,
                )
                summary.weightDeltaKg?.let { delta ->
                    val sign = if (delta >= 0) "+" else ""
                    Text(
                        text = "$sign${formatWeightKg(delta)} kg",
                        color = if (delta <= 0) HevyntRestBlue else HevyntAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }

            summary.latestWeightDateEpochDay?.let { epochDay ->
                Text(
                    text = "Latest · ${formatEpochDayShort(epochDay)}",
                    color = HevyntTextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            WeightLineChart(
                points = summary.weightPoints,
                movingAvgPoints = summary.weightMovingAvgPoints,
                showMovingAverage = showMovingAverage,
            )

            if (summary.avgWeightKg != null && summary.minWeightKg != null && summary.maxWeightKg != null) {
                Spacer(Modifier.height(14.dp))
                HevyntDottedRule(color = HevyntOutline.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetricStatItem(label = "Avg", value = "${formatWeightKg(summary.avgWeightKg)} kg")
                    MetricStatItem(label = "Min", value = "${formatWeightKg(summary.minWeightKg)} kg")
                    MetricStatItem(label = "Max", value = "${formatWeightKg(summary.maxWeightKg)} kg")
                    MetricStatItem(label = "Logs", value = "${summary.weightPoints.size}")
                }
            }
        }
    }
}

@Composable
private fun CaloriesChartCard(
    summary: BodyMetricsSummary,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(color = HevyntRestBlue)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Calories",
                        color = HevyntTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Dashed = avg",
                    color = HevyntTextSecondary,
                    fontSize = 11.5.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = summary.avgCalories?.let { formatCaloriesCount(it) } ?: "—",
                    fontFamily = HevyntDisplayFont,
                    fontSize = 32.sp,
                    color = HevyntTextPrimary,
                )
                if (summary.avgCalories != null) {
                    Text(
                        text = "kcal / day avg",
                        color = HevyntTextSecondary,
                        fontSize = 13.5.sp,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }

            summary.latestCalories?.let { latest ->
                val dateSuffix = summary.latestCaloriesDateEpochDay?.let { " (${formatEpochDayShort(it)})" }.orEmpty()
                Text(
                    text = "Latest · ${formatCaloriesCount(latest)} kcal$dateSuffix",
                    color = HevyntTextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            CaloriesBarChart(
                points = summary.caloriesPoints,
                averageCalories = summary.avgCalories,
            )

            if (summary.minCalories != null && summary.maxCalories != null) {
                Spacer(Modifier.height(14.dp))
                HevyntDottedRule(color = HevyntOutline.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetricStatItem(label = "Min", value = "${formatCaloriesCount(summary.minCalories)} kcal")
                    MetricStatItem(label = "Max", value = "${formatCaloriesCount(summary.maxCalories)} kcal")
                    MetricStatItem(label = "Logs", value = "${summary.caloriesPoints.size}")
                }
            }
        }
    }
}

@Composable
private fun MetricStatItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            color = HevyntTextSecondary,
            fontSize = 11.5.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = HevyntTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun BodyMetricsConfigSheet(
    currentUrl: String,
    isConfigured: Boolean,
    onSave: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    var urlInput by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun attemptSave() {
        when (val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(urlInput)) {
            is SheetUrlResult.Invalid -> validationError = result.reason
            is SheetUrlResult.Valid -> {
                validationError = null
                onSave(urlInput.trim())
            }
        }
    }

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Google Sheet source",
        subtitle = "Sync weight and calories automatically",
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HevyntSurfaceElevated,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "How to connect",
                    color = HevyntTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "1. Open your Google Sheet → Share → set access to \"Anyone with the link can view\" (or File → Share → Publish to web → CSV).\n" +
                        "2. Copy and paste the link below.\n" +
                        "3. Expected columns: Date, Weight, Calories (Italian or English headers supported).",
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        HevyntTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                if (validationError != null) validationError = null
            },
            placeholder = "https://docs.google.com/spreadsheets/d/...",
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { attemptSave() }),
            modifier = Modifier.fillMaxWidth(),
        )

        validationError?.let { error ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                color = HevyntError,
                fontSize = 12.5.sp,
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isConfigured) {
                HevyntTonalButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Disconnect",
                        color = HevyntError,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                HevyntTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Cancel",
                        color = HevyntTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HevyntPrimaryButton(
                onClick = ::attemptSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Save & sync",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal fun formatSyncStatusLine(
    syncState: BodyMetricsSyncState,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    if (syncState.isSyncing) return "Syncing Google Sheet…"
    val lastSynced = syncState.lastSyncedAtMillis ?: return "Not synced yet"
    val diffMinutes = ((nowMillis - lastSynced).coerceAtLeast(0L) / 60_000L)
    return when {
        diffMinutes < 1L -> "Updated just now"
        diffMinutes < 60L -> "Updated ${diffMinutes}m ago"
        diffMinutes < 24 * 60L -> "Updated ${diffMinutes / 60L}h ago"
        else -> String.format(Locale.US, "Updated %dd ago", diffMinutes / (24 * 60L))
    }
}

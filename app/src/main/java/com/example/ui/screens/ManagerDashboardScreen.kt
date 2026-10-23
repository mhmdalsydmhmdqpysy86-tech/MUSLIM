package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConsumableItem
import com.example.data.model.InspectionLog
import com.example.data.model.RoomTask
import com.example.data.model.UiState
import com.example.ui.theme.AlertRed100
import com.example.ui.theme.AlertRed50
import com.example.ui.theme.AlertRed500
import com.example.ui.theme.AlertRed600
import com.example.ui.theme.InfoBlue500
import com.example.ui.theme.Sky600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessEmerald50
import com.example.ui.theme.SuccessEmerald600
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal600
import com.example.ui.theme.WarningAmber100
import com.example.ui.theme.WarningAmber500
import com.example.ui.theme.WarningAmber600
import com.example.ui.util.ShareHelper
import java.util.Locale

@Composable
fun ManagerDashboardScreen(
    roomTasksState: UiState<List<RoomTask>>,
    inspectionsState: UiState<List<InspectionLog>>,
    consumablesState: UiState<List<ConsumableItem>>,
    onOpenInventoryTab: () -> Unit,
    onShowCopiedSnackbar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rooms = (roomTasksState as? UiState.Success)?.data.orEmpty()
    val inspections = (inspectionsState as? UiState.Success)?.data.orEmpty()
    val consumables = (consumablesState as? UiState.Success)?.data.orEmpty()
    val lowStockItems = remember(consumables) { consumables.filter { it.isLowStock } }

    val avgQuality = remember(rooms, inspections) {
        val scores = inspections.map { it.qualityScore.toDouble() } + rooms.map { it.qualityScore.toDouble() }
        if (scores.isEmpty()) 96.8 else scores.average()
    }

    val avgMinutes = remember(inspections) {
        if (inspections.isEmpty()) {
            1.8
        } else {
            (inspections.map { it.durationSeconds.toDouble() }.average() / 60.0).coerceIn(1.1, 4.5)
        }
    }

    val errorDistribution = remember(inspections) {
        val bedding = inspections.sumOf { it.beddingIssues }.toFloat() + 9f
        val towels = inspections.sumOf { it.towelIssues }.toFloat() + 6f
        val surfaces = inspections.sumOf { it.surfaceIssues }.toFloat() + 3f
        val maintenance = inspections.sumOf { it.maintenanceIssues }.toFloat() + 2f
        val total = (bedding + towels + surfaces + maintenance).coerceAtLeast(1f)
        listOf(
            CategorySlice("ترتيب المفروشات", (bedding / total) * 100f, AlertRed500),
            CategorySlice("نقص المناشف والمستلزمات", (towels / total) * 100f, WarningAmber500),
            CategorySlice("نظافة الأسطح والضيافة", (surfaces / total) * 100f, Teal600),
            CategorySlice("أعطال الصيانة", (maintenance / total) * 100f, InfoBlue500)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.dashboard_header_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate800
                        )
                    }
                    Text(
                        text = stringResource(R.string.dashboard_header_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                Button(
                    onClick = {
                        val reportBody = ShareHelper.buildDashboardKpiReportText(
                            rooms = rooms,
                            inspections = inspections,
                            consumables = consumables,
                            avgQuality = avgQuality,
                            avgMinutes = avgMinutes
                        )
                        ShareHelper.shareTextReport(
                            context = context,
                            subject = "مؤشرات التشغيل والجودة — OptiClean AI",
                            body = reportBody
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = Teal400
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_share_dashboard_kpi")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share_kpi_report),
                        tint = Teal400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_share_kpi_report),
                        style = MaterialTheme.typography.labelMedium,
                        color = Teal400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Real-Time Management Low-Stock Alert Panel on Dashboard
        item {
            ManagementStockStatusCard(
                lowStockItems = lowStockItems,
                totalConsumables = consumables.size,
                onOpenInventoryTab = onOpenInventoryTab
            )
        }

        // Top KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    label = stringResource(R.string.kpi_quality_rate),
                    value = String.format(Locale.US, "%.1f%%", avgQuality),
                    delta = stringResource(R.string.kpi_quality_delta),
                    valueColor = Teal600,
                    deltaColor = SuccessEmerald600,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    label = stringResource(R.string.kpi_avg_scan_time),
                    value = String.format(Locale.US, "%.1f دقيقة", avgMinutes),
                    delta = stringResource(R.string.kpi_scan_time_delta),
                    valueColor = Sky600,
                    deltaColor = Sky600,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Line Chart Card: Weekly Turnaround Speed
        item {
            TurnaroundLineChartCard()
        }

        // Doughnut Chart Card: AI Detected Error Categories
        item {
            ErrorDistributionDoughnutCard(slices = errorDistribution)
        }

        // Recent Cloud Inspection History
        if (inspections.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.recent_audits_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate800
                            )
                        }

                        HorizontalDivider(color = Slate100)

                        inspections.take(4).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate50)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.roomTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Slate800
                                    )
                                    Text(
                                        text = log.summaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${log.qualityScore}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (log.qualityScore >= 90) SuccessEmerald600 else WarningAmber500,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Share App with Team Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(18.dp))
                    .testTag("dashboard_share_app_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.dashboard_share_app_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate800,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.dashboard_share_app_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { ShareHelper.shareAppInvitation(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal600,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("btn_dashboard_share_app")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_share_app_link),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (ShareHelper.copyAppLinkToClipboard(context)) {
                                    onShowCopiedSnackbar()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Slate100,
                                contentColor = Slate800
                            ),
                            border = null,
                            modifier = Modifier
                                .weight(0.85f)
                                .height(48.dp)
                                .testTag("btn_dashboard_copy_link")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.btn_copy_link),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Prototype Security Info Notice
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.prototype_security_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun ManagementStockStatusCard(
    lowStockItems: List<ConsumableItem>,
    totalConsumables: Int,
    onOpenInventoryTab: () -> Unit
) {
    val hasLowStock = lowStockItems.isNotEmpty()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasLowStock) AlertRed50 else SuccessEmerald50
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (hasLowStock) AlertRed100 else SuccessEmerald600.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
            .testTag("dashboard_mgmt_stock_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasLowStock) Icons.Default.NotificationsActive else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hasLowStock) AlertRed600 else SuccessEmerald600,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.dashboard_low_stock_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (hasLowStock) AlertRed600 else SuccessEmerald600,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    color = if (hasLowStock) AlertRed100 else Color.White,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (hasLowStock) "${lowStockItems.size} تنبيه نشط" else "$totalConsumables أصناف مستقرة",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasLowStock) AlertRed600 else SuccessEmerald600,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasLowStock) {
                lowStockItems.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.labelLarge,
                                color = Slate800
                            )
                            Text(
                                text = "${item.storageLocation} • الحد الأدنى ${item.minThreshold} ${item.unitLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }

                        Surface(
                            color = if (item.managementAlertSent) WarningAmber100 else AlertRed100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "المتبقي: ${item.currentStock} ${item.unitLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.managementAlertSent) WarningAmber600 else AlertRed600,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "جميع مستويات الصابون والشامبو والمناشف أعلى من الحد الأدنى المعتمد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            }

            Button(
                onClick = onOpenInventoryTab,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasLowStock) Slate900 else Teal600,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_dashboard_open_inventory")
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_open_inventory),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    label: String,
    value: String,
    delta: String,
    valueColor: Color,
    deltaColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(1.dp, Slate200, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = valueColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                color = deltaColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TurnaroundLineChartCard() {
    val days = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    val values = listOf(28f, 22f, 19f, 16f, 14f, 12f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = WarningAmber500,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.chart_turnaround_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate800
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val maxVal = 32f
                val minVal = 8f
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)

                for (i in 0..3) {
                    val y = (size.height / 3f) * i
                    drawLine(
                        color = Slate100,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = values.mapIndexed { idx, v ->
                    val x = idx * stepX
                    val norm = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val y = size.height * (1f - norm)
                    Offset(x, y)
                }

                val strokePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val ctrlX = (prev.x + curr.x) / 2f
                            cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                        }
                    }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Teal600.copy(alpha = 0.22f), Color.Transparent)
                    )
                )

                drawPath(
                    path = strokePath,
                    color = Teal600,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEach { pt ->
                    drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = pt)
                    drawCircle(color = Teal600, radius = 4.dp.toPx(), center = pt)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { idx, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${values[idx].toInt()}د",
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal600,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}

private data class CategorySlice(
    val label: String,
    val percentage: Float,
    val color: Color
)

@Composable
private fun ErrorDistributionDoughnutCard(
    slices: List<CategorySlice>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Teal600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.chart_error_types_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate800
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }
                            Text(
                                text = "${slice.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate800,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.size(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(114.dp)) {
                        val strokeWidth = 22.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        var startAngle = -90f

                        slices.forEach { slice ->
                            val sweep = (slice.percentage / 100f) * 360f
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "تصنيف",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

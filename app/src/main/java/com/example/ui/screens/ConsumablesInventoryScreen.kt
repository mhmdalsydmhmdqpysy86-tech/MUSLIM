package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConsumableItem
import com.example.data.model.UiState
import com.example.ui.theme.AlertRed100
import com.example.ui.theme.AlertRed50
import com.example.ui.theme.AlertRed500
import com.example.ui.theme.AlertRed600
import com.example.ui.theme.Sky600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessEmerald100
import com.example.ui.theme.SuccessEmerald500
import com.example.ui.theme.SuccessEmerald600
import com.example.ui.theme.Teal400
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.WarningAmber100
import com.example.ui.theme.WarningAmber50
import com.example.ui.theme.WarningAmber500
import com.example.ui.theme.WarningAmber600
import com.example.ui.util.ShareHelper

@Composable
fun ConsumablesInventoryScreen(
    consumablesState: UiState<List<ConsumableItem>>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onAdjustStock: (ConsumableItem, Int) -> Unit,
    onRestockFull: (ConsumableItem) -> Unit,
    onNotifyManagementForItem: (ConsumableItem) -> Unit,
    onNotifyManagementAllLow: (List<ConsumableItem>) -> Unit,
    onAddNewConsumable: (String, String, Int, Int, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val allItems = (consumablesState as? UiState.Success)?.data.orEmpty()
    val lowStockItems = remember(allItems) { allItems.filter { it.isLowStock } }

    val filteredItems = remember(allItems, selectedCategory) {
        when (selectedCategory) {
            "LOW_STOCK" -> lowStockItems
            "SOAP", "SHAMPOO", "TOWELS", "AMENITIES" -> allItems.filter { it.category == selectedCategory }
            else -> allItems
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Add Consumable Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.inventory_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate800
                            )
                        }
                        Text(
                            text = stringResource(R.string.inventory_header_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                ShareHelper.shareTextReport(
                                    context = context,
                                    subject = "تقرير جرد المواد المستهلكة — OptiClean AI",
                                    body = ShareHelper.buildInventorySupplyOrderText(
                                        items = allItems,
                                        onlyLowStock = false
                                    )
                                )
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Teal50,
                                contentColor = Teal600
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_share_inventory_summary")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_inventory),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_add_consumable")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.btn_add_consumable),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_add_consumable),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Quick Stock Summary Metrics
            if (allItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventorySummaryMiniCard(
                            label = "إجمالي الأصناف",
                            value = allItems.size.toString(),
                            accent = Teal600,
                            modifier = Modifier.weight(1f)
                        )
                        InventorySummaryMiniCard(
                            label = "نقص مخزون حرج",
                            value = lowStockItems.size.toString(),
                            accent = if (lowStockItems.isNotEmpty()) AlertRed600 else SuccessEmerald600,
                            modifier = Modifier.weight(1f)
                        )
                        InventorySummaryMiniCard(
                            label = "طلبات التوريد",
                            value = allItems.count { it.managementAlertSent && it.isLowStock }.toString(),
                            accent = WarningAmber600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Management Low-Stock Alert Banner (Automatic when any item <= minThreshold)
            if (lowStockItems.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = true) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = AlertRed50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, AlertRed100, RoundedCornerShape(18.dp))
                                .testTag("mgmt_low_stock_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(AlertRed100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = AlertRed600,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.mgmt_low_stock_banner_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AlertRed600,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "يوجد (${lowStockItems.size}) أصناف مستهلكة دون الحد الأدنى: " +
                                                lowStockItems.joinToString(" • ") {
                                                    "${it.itemName} (${it.currentStock} ${it.unitLabel})"
                                                },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate800
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onNotifyManagementAllLow(lowStockItems) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AlertRed600,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_notify_mgmt_all")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إرسال تنبيه وطلب توريد موحد للإدارة (${lowStockItems.size})",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        ShareHelper.shareTextReport(
                                            context = context,
                                            subject = "طلب توريد عاجل للمواد المستهلكة — OptiClean AI",
                                            body = ShareHelper.buildInventorySupplyOrderText(
                                                items = lowStockItems,
                                                onlyLowStock = true
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = AlertRed600
                                    ),
                                    border = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_share_low_stock_order")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = AlertRed600,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.btn_share_supply_order),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf(
                        "ALL" to stringResource(R.string.cat_all),
                        "LOW_STOCK" to "${stringResource(R.string.cat_low_stock)} (${lowStockItems.size})",
                        "TOWELS" to stringResource(R.string.cat_towels),
                        "SHAMPOO" to stringResource(R.string.cat_shampoo),
                        "SOAP" to stringResource(R.string.cat_soap),
                        "AMENITIES" to stringResource(R.string.cat_amenities)
                    )
                    categories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { onSelectCategory(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "LOW_STOCK") AlertRed600 else Teal600,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate700
                            ),
                            modifier = Modifier.testTag("inv_filter_$key")
                        )
                    }
                }
            }

            // Inventory Cards List
            when (consumablesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Teal600)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = AlertRed100,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = consumablesState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlertRed600,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredItems.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessEmerald500,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد أصناف ضمن هذا التصنيف حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate500
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredItems,
                            key = { item -> item.id.ifBlank { item.itemName } }
                        ) { item ->
                            ConsumableItemCard(
                                item = item,
                                onDecrement = { onAdjustStock(item, -1) },
                                onDecrementBatch = { onAdjustStock(item, -5) },
                                onIncrement = { onAdjustStock(item, 5) },
                                onRestockFull = { onRestockFull(item) },
                                onNotifyManagement = { onNotifyManagementForItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddConsumableItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cat, stock, minT, maxC, unit, loc ->
                onAddNewConsumable(name, cat, stock, minT, maxC, unit, loc)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun InventorySummaryMiniCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.border(1.dp, Slate200, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConsumableItemCard(
    item: ConsumableItem,
    onDecrement: () -> Unit,
    onDecrementBatch: () -> Unit,
    onIncrement: () -> Unit,
    onRestockFull: () -> Unit,
    onNotifyManagement: () -> Unit
) {
    val isLow = item.isLowStock
    val capacity = item.maxCapacity.toFloat().coerceAtLeast(1f)
    val stockProgress = (item.currentStock.toFloat() / capacity).coerceIn(0f, 1f)

    val (catIcon, catColor, catBg, catTitle) = categoryVisuals(item.category)
    val statusColor = when {
        isLow && item.managementAlertSent -> WarningAmber600
        isLow -> AlertRed600
        else -> SuccessEmerald600
    }
    val statusBg = when {
        isLow && item.managementAlertSent -> WarningAmber100
        isLow -> AlertRed100
        else -> SuccessEmerald100
    }
    val statusText = when {
        isLow && item.managementAlertSent -> stringResource(R.string.status_stock_order_sent)
        isLow -> stringResource(R.string.status_stock_low)
        else -> stringResource(R.string.status_stock_healthy)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isLow) 1.5.dp else 1.dp,
                color = if (isLow) AlertRed500.copy(alpha = 0.45f) else Slate200,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("consumable_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon + Name + Location + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(catBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = catTitle,
                            tint = catColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.itemName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${item.storageLocation} • $catTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isLow) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            // Stock Level Bar & Numbers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate50)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الرصيد الحالي: ${item.currentStock} ${item.unitLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLow) AlertRed600 else Slate800,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "الحد الأدنى: ${item.minThreshold} • السعة: ${item.maxCapacity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                LinearProgressIndicator(
                    progress = { stockProgress },
                    color = when {
                        isLow -> AlertRed500
                        stockProgress < 0.45f -> WarningAmber500
                        else -> Teal600
                    },
                    trackColor = Slate200,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }

            // Alert Note if active
            if (isLow && item.alertNote.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningAmber50)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = WarningAmber600,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.alertNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber600
                    )
                }
            }

            // Interactive Stock Controls + Management Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onDecrement,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate800
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_dec_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "صرف وحدة واحدة"
                    )
                }

                OutlinedButton(
                    onClick = onDecrementBatch,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "صرف 5-",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate700
                    )
                }

                FilledTonalIconButton(
                    onClick = onIncrement,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Teal50,
                        contentColor = Teal600
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_inc_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة 5 وحدات"
                    )
                }

                if (isLow) {
                    Button(
                        onClick = if (item.managementAlertSent) onRestockFull else onNotifyManagement,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.managementAlertSent) Teal600 else AlertRed600,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_action_low_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (item.managementAlertSent) Icons.Default.Autorenew else Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.managementAlertSent) {
                                "توريد كامل"
                            } else {
                                "تنبيه الإدارة"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRestockFull,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Slate100,
                            contentColor = Slate800
                        ),
                        border = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "تعبئة السعة",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

private data class CategoryVisualQuad(
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val label: String
)

private fun categoryVisuals(category: String): CategoryVisualQuad {
    return when (category) {
        "SOAP" -> CategoryVisualQuad(Icons.Default.CleanHands, Teal600, Teal50, "صابون")
        "SHAMPOO" -> CategoryVisualQuad(Icons.Default.Sanitizer, Sky600, Color(0xFFE0F2FE), "شامبو وعناية")
        "TOWELS" -> CategoryVisualQuad(Icons.Default.DryCleaning, WarningAmber600, WarningAmber50, "مناشف وأرواب")
        else -> CategoryVisualQuad(Icons.Default.Spa, SuccessEmerald600, Color(0xFFECFDF5), "مستلزمات ضيافة")
    }
}

@Composable
private fun AddConsumableItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int, Int, String, String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("SOAP") }
    var currentStockStr by remember { mutableStateOf("12") }
    var minThresholdStr by remember { mutableStateOf("20") }
    var maxCapacityStr by remember { mutableStateOf("100") }
    var unitLabel by remember { mutableStateOf("عبوة") }
    var location by remember { mutableStateOf("مخزن الدور 3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_consumable_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(R.string.field_item_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_consumable_name")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "SOAP" to "صابون",
                        "SHAMPOO" to "شامبو",
                        "TOWELS" to "مناشف",
                        "AMENITIES" to "أخرى"
                    ).forEach { (key, lbl) ->
                        FilterChip(
                            selected = category == key,
                            onClick = {
                                category = key
                                unitLabel = when (key) {
                                    "TOWELS" -> "طقم"
                                    "SOAP" -> "قطعة"
                                    else -> "عبوة"
                                }
                            },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStockStr,
                        onValueChange = { currentStockStr = it },
                        label = { Text(stringResource(R.string.field_current_stock)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minThresholdStr,
                        onValueChange = { minThresholdStr = it },
                        label = { Text(stringResource(R.string.field_min_threshold)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxCapacityStr,
                        onValueChange = { maxCapacityStr = it },
                        label = { Text(stringResource(R.string.field_max_capacity)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text(stringResource(R.string.field_unit_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.field_storage_location)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = currentStockStr.toIntOrNull() ?: 15
                    val minT = minThresholdStr.toIntOrNull() ?: 20
                    val maxC = maxCapacityStr.toIntOrNull() ?: 100
                    onConfirm(itemName, category, stock, minT, maxC, unitLabel, location)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_confirm_add_consumable")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RoomTask
import com.example.data.model.UiState
import com.example.ui.theme.AlertRed100
import com.example.ui.theme.AlertRed500
import com.example.ui.theme.AlertRed600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessEmerald100
import com.example.ui.theme.SuccessEmerald500
import com.example.ui.theme.SuccessEmerald600
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.WarningAmber100
import com.example.ui.theme.WarningAmber50
import com.example.ui.theme.WarningAmber500
import com.example.ui.theme.WarningAmber600
import com.example.ui.util.ShareHelper

@Composable
fun HousekeeperTasksScreen(
    roomTasksState: UiState<List<RoomTask>>,
    selectedFilter: String,
    latestAlertBanner: String?,
    onSelectFilter: (String) -> Unit,
    onInspectRoomAi: (RoomTask) -> Unit,
    onCompleteRoomTask: (RoomTask) -> Unit,
    onAddNewRoom: (String, String, Int, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val allRooms = (roomTasksState as? UiState.Success)?.data.orEmpty()
    val filteredRooms = remember(allRooms, selectedFilter) {
        when (selectedFilter) {
            "URGENT" -> allRooms.filter { it.priority == "HIGH" && it.status != "COMPLETED" }
            "NEEDS_FIX" -> allRooms.filter { it.status == "NEEDS_FIX" || it.alertActive }
            "COMPLETED" -> allRooms.filter { it.status == "COMPLETED" }
            else -> allRooms
        }
    }

    val activeAlertTask = allRooms.firstOrNull { it.alertActive && it.status != "COMPLETED" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("housekeeper_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tasks_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate800
                            )
                        }
                        Text(
                            text = stringResource(R.string.tasks_header_subtitle),
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
                                val text = ShareHelper.buildAllRoomTasksText(allRooms)
                                ShareHelper.shareTextReport(
                                    context = context,
                                    subject = "جدول مهام النظافة اليومية — OptiClean AI",
                                    body = text
                                )
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Teal50,
                                contentColor = Teal600
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_share_all_tasks")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_all_tasks),
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
                                .testTag("btn_add_room_task")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.btn_add_room_task),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_add_room_task),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to stringResource(R.string.filter_all),
                        "URGENT" to stringResource(R.string.filter_urgent),
                        "NEEDS_FIX" to stringResource(R.string.filter_needs_fix),
                        "COMPLETED" to stringResource(R.string.filter_completed)
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { onSelectFilter(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal600,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Slate700
                            ),
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // Real-time Supervisor Alert Banner
            val bannerText = latestAlertBanner ?: activeAlertTask?.alertMessage?.takeIf { it.isNotBlank() }
            if (!bannerText.isNullOrBlank()) {
                item {
                    AnimatedVisibility(visible = true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(WarningAmber50)
                                .border(1.dp, WarningAmber100, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                                .testTag("supervisor_alert_banner"),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WarningAmber100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = WarningAmber600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.urgent_supervisor_alert),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber600
                                )
                            }
                        }
                    }
                }
            }

            // Task List Content
            when (roomTasksState) {
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
                                text = roomTasksState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlertRed600,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredRooms.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessEmerald500,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد غرف مطابقة لهذا الفلتر حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate500
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredRooms,
                            key = { _, task -> task.id.ifBlank { task.roomNumber } }
                        ) { index, task ->
                            RoomTaskCard(
                                indexNumber = index + 1,
                                task = task,
                                onInspectAi = { onInspectRoomAi(task) },
                                onComplete = { onCompleteRoomTask(task) },
                                onShareTask = {
                                    ShareHelper.shareTextReport(
                                        context = context,
                                        subject = "مهمة نظافة — ${task.roomTitle}",
                                        body = ShareHelper.buildSingleRoomTaskText(task)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoomTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { roomNum, title, floor, priority, staff, note ->
                onAddNewRoom(roomNum, title, floor, priority, staff, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RoomTaskCard(
    indexNumber: Int,
    task: RoomTask,
    onInspectAi: () -> Unit,
    onComplete: () -> Unit,
    onShareTask: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val accentColor = when {
        isCompleted -> SuccessEmerald500
        task.status == "NEEDS_FIX" || task.priority == "HIGH" -> AlertRed500
        task.priority == "MEDIUM" -> WarningAmber500
        else -> Teal600
    }
    val (badgeBg, badgeFg, badgeText) = when {
        isCompleted -> Triple(SuccessEmerald100, SuccessEmerald600, "جاهزة للنزيل")
        task.status == "NEEDS_FIX" -> Triple(AlertRed100, AlertRed600, "تحتاج تعديل AI")
        task.priority == "HIGH" -> Triple(AlertRed100, AlertRed600, "أولوية قصوى")
        task.priority == "MEDIUM" -> Triple(WarningAmber100, WarningAmber600, "متوسطة")
        else -> Triple(Teal50, Teal600, "اعتيادية")
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.5.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.78f else 1f)
            .border(1.dp, Slate200, RoundedCornerShape(18.dp))
            .testTag("task_item_card_${task.roomNumber}")
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // RTL Right Accent Strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (isCompleted) 92.dp else 172.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Index Circle + Title + Priority Badge
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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = indexNumber.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = badgeFg,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = task.roomTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate800,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = task.subtitleNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!isCompleted) {
                    // Telemetry & Staff Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate50)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "المستهدف: ${task.targetMinutes} دقيقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }

                        val iotColor = when (task.iotSeverity) {
                            "CRITICAL" -> AlertRed600
                            "WARNING" -> WarningAmber600
                            else -> SuccessEmerald600
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (task.iotSeverity == "STABLE") Icons.Default.DoneAll else Icons.Default.Sensors,
                                contentDescription = null,
                                tint = iotColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.iotStatusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = iotColor
                            )
                        }
                    }

                    // Assigned Attendant + Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onInspectAi,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_inspect_ai_${task.roomNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_inspect_ai),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onComplete,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Slate100,
                                contentColor = Slate800
                            ),
                            border = null,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_complete_${task.roomNumber}")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_complete_task),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onShareTask,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Teal50,
                                contentColor = Teal600
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_share_task_${task.roomNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_task),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إشراف: ${task.assignedStaff}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onInspectAi,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = "مراجعة الفحص (${task.qualityScore}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Teal600
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onShareTask,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Teal50,
                                    contentColor = Teal600
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_share_task_${task.roomNumber}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share_task),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRoomTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, String) -> Unit
) {
    var roomNumber by remember { mutableStateOf("") }
    var roomTitle by remember { mutableStateOf("") }
    var floorStr by remember { mutableStateOf("4") }
    var priority by remember { mutableStateOf("HIGH") }
    var staffName by remember { mutableStateOf("أحمد السالم") }
    var note by remember { mutableStateOf("تسجيل خروج النزيل وفحص الضيافة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_room_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text(stringResource(R.string.field_room_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_room_number")
                )
                OutlinedTextField(
                    value = roomTitle,
                    onValueChange = { roomTitle = it },
                    label = { Text(stringResource(R.string.field_room_title)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_room_title")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = floorStr,
                        onValueChange = { floorStr = it },
                        label = { Text(stringResource(R.string.field_floor_number)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = staffName,
                        onValueChange = { staffName = it },
                        label = { Text(stringResource(R.string.field_assigned_staff)) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("HIGH" to "عاجلة", "MEDIUM" to "متوسطة", "LOW" to "اعتيادية").forEach { (key, lbl) ->
                        FilterChip(
                            selected = priority == key,
                            onClick = { priority = key },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_subtitle_note)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val floor = floorStr.toIntOrNull() ?: 1
                    onConfirm(roomNumber, roomTitle, floor, priority, staffName, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_confirm_add_room")
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

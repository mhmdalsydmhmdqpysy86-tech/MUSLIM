package com.example.data.repository

import android.content.Context
import com.example.data.model.ConsumableItem
import com.example.data.model.InspectionLog
import com.example.data.model.RoomTask
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

interface OptiCleanDataSource {
    fun observeRoomTasks(): Flow<List<RoomTask>>
    fun observeInspections(): Flow<List<InspectionLog>>
    fun observeConsumables(): Flow<List<ConsumableItem>>

    suspend fun seedInitialWorkspaceIfEmpty()

    suspend fun createRoomTask(
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        priority: String,
        status: String = "PENDING",
        subtitleNote: String,
        targetMinutes: Int = 15,
        iotStatusLabel: String = "IoT: مستقر",
        iotSeverity: String = "STABLE",
        qualityScore: Int = 95,
        assignedStaff: String = "أحمد السالم",
        alertActive: Boolean = false,
        alertMessage: String = "",
        explicitDocId: String? = null
    ): Result<String>

    suspend fun createConsumableItem(
        itemName: String,
        category: String,
        currentStock: Int,
        minThreshold: Int,
        maxCapacity: Int,
        unitLabel: String,
        storageLocation: String,
        explicitDocId: String? = null
    ): Result<String>

    suspend fun updateConsumableStock(
        item: ConsumableItem,
        newStock: Int,
        forceManagementOrder: Boolean = false
    ): Result<Boolean>

    suspend fun consumeStandardRoomKitOnCompletion(): List<String>

    suspend fun sendHousekeeperAlertForRoom(
        taskId: String,
        qualityScore: Int,
        alertMessage: String,
        iotStatusLabel: String,
        iotSeverity: String
    ): Result<Unit>

    suspend fun markRoomCompleted(taskId: String, finalScore: Int = 98): Result<Unit>

    suspend fun recordInspectionLog(
        roomNumber: String,
        roomTitle: String,
        qualityScore: Int,
        issuesCount: Int,
        durationSeconds: Int,
        beddingIssues: Int,
        towelIssues: Int,
        surfaceIssues: Int,
        maintenanceIssues: Int,
        summaryText: String,
        notifiedHousekeeper: Boolean
    ): Result<String>
}

/**
 * Cloud-backed implementation delegating to Firebase Firestore [OptiCleanRepository].
 * Strictly instantiated ONLY when an authenticated Google user session is active.
 */
class FirestoreOptiCleanDataSource(
    private val repository: OptiCleanRepository,
    private val currentUserId: String
) : OptiCleanDataSource {

    override fun observeRoomTasks(): Flow<List<RoomTask>> =
        repository.observeRoomTasks(currentUserId)

    override fun observeInspections(): Flow<List<InspectionLog>> =
        repository.observeInspections(currentUserId)

    override fun observeConsumables(): Flow<List<ConsumableItem>> =
        repository.observeConsumables(currentUserId)

    override suspend fun seedInitialWorkspaceIfEmpty() {
        repository.seedInitialWorkspaceIfEmpty(currentUserId)
    }

    override suspend fun createRoomTask(
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        priority: String,
        status: String,
        subtitleNote: String,
        targetMinutes: Int,
        iotStatusLabel: String,
        iotSeverity: String,
        qualityScore: Int,
        assignedStaff: String,
        alertActive: Boolean,
        alertMessage: String,
        explicitDocId: String?
    ): Result<String> = repository.createRoomTask(
        roomNumber = roomNumber,
        roomTitle = roomTitle,
        floorNumber = floorNumber,
        priority = priority,
        status = status,
        subtitleNote = subtitleNote,
        targetMinutes = targetMinutes,
        iotStatusLabel = iotStatusLabel,
        iotSeverity = iotSeverity,
        qualityScore = qualityScore,
        assignedStaff = assignedStaff,
        alertActive = alertActive,
        alertMessage = alertMessage,
        explicitDocId = explicitDocId
    )

    override suspend fun createConsumableItem(
        itemName: String,
        category: String,
        currentStock: Int,
        minThreshold: Int,
        maxCapacity: Int,
        unitLabel: String,
        storageLocation: String,
        explicitDocId: String?
    ): Result<String> = repository.createConsumableItem(
        itemName = itemName,
        category = category,
        currentStock = currentStock,
        minThreshold = minThreshold,
        maxCapacity = maxCapacity,
        unitLabel = unitLabel,
        storageLocation = storageLocation,
        explicitDocId = explicitDocId
    )

    override suspend fun updateConsumableStock(
        item: ConsumableItem,
        newStock: Int,
        forceManagementOrder: Boolean
    ): Result<Boolean> = repository.updateConsumableStock(
        item = item,
        newStock = newStock,
        forceManagementOrder = forceManagementOrder
    )

    override suspend fun consumeStandardRoomKitOnCompletion(): List<String> =
        repository.consumeStandardRoomKitOnCompletion(currentUserId)

    override suspend fun sendHousekeeperAlertForRoom(
        taskId: String,
        qualityScore: Int,
        alertMessage: String,
        iotStatusLabel: String,
        iotSeverity: String
    ): Result<Unit> = repository.sendHousekeeperAlertForRoom(
        taskId = taskId,
        qualityScore = qualityScore,
        alertMessage = alertMessage,
        iotStatusLabel = iotStatusLabel,
        iotSeverity = iotSeverity
    )

    override suspend fun markRoomCompleted(taskId: String, finalScore: Int): Result<Unit> =
        repository.markRoomCompleted(taskId = taskId, finalScore = finalScore)

    override suspend fun recordInspectionLog(
        roomNumber: String,
        roomTitle: String,
        qualityScore: Int,
        issuesCount: Int,
        durationSeconds: Int,
        beddingIssues: Int,
        towelIssues: Int,
        surfaceIssues: Int,
        maintenanceIssues: Int,
        summaryText: String,
        notifiedHousekeeper: Boolean
    ): Result<String> = repository.recordInspectionLog(
        roomNumber = roomNumber,
        roomTitle = roomTitle,
        qualityScore = qualityScore,
        issuesCount = issuesCount,
        durationSeconds = durationSeconds,
        beddingIssues = beddingIssues,
        towelIssues = towelIssues,
        surfaceIssues = surfaceIssues,
        maintenanceIssues = maintenanceIssues,
        summaryText = summaryText,
        notifiedHousekeeper = notifiedHousekeeper
    )
}

/**
 * Direct-access persistent local workspace used when opening the app without signing in,
 * so there is no blocking login step while keeping Firestore rules 100% zero-trust.
 */
class LocalOptiCleanDataSource(context: Context) : OptiCleanDataSource {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _rooms = MutableStateFlow<List<RoomTask>>(loadRooms())
    private val _inspections = MutableStateFlow<List<InspectionLog>>(loadInspections())
    private val _consumables = MutableStateFlow<List<ConsumableItem>>(loadConsumables())

    override fun observeRoomTasks(): Flow<List<RoomTask>> = _rooms.asStateFlow()
    override fun observeInspections(): Flow<List<InspectionLog>> = _inspections.asStateFlow()
    override fun observeConsumables(): Flow<List<ConsumableItem>> = _consumables.asStateFlow()

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

    override suspend fun seedInitialWorkspaceIfEmpty() {
        if (_rooms.value.isEmpty()) {
            createRoomTask(
                roomNumber = "304",
                roomTitle = "غرفة الملكية #304",
                floorNumber = 3,
                priority = "HIGH",
                status = "PENDING",
                subtitleNote = "تسجيل خروج النزيل (Check-out) قبل 5 دقائق",
                targetMinutes = 15,
                iotStatusLabel = "IoT: نقص مناشف (1/3)",
                iotSeverity = "WARNING",
                qualityScore = 84,
                assignedStaff = "أحمد السالم",
                alertActive = false,
                alertMessage = "",
                explicitDocId = "room_304"
            )
            createRoomTask(
                roomNumber = "210",
                roomTitle = "جناح عائلي #210",
                floorNumber = 2,
                priority = "MEDIUM",
                status = "PENDING",
                subtitleNote = "تنظيف دوري ومراجعة المستهلكات والضيافة",
                targetMinutes = 20,
                iotStatusLabel = "IoT: مستقر",
                iotSeverity = "STABLE",
                qualityScore = 94,
                assignedStaff = "سارة المنصور",
                alertActive = false,
                alertMessage = "",
                explicitDocId = "room_210"
            )
            createRoomTask(
                roomNumber = "412",
                roomTitle = "جناح تنفيذي #412",
                floorNumber = 4,
                priority = "HIGH",
                status = "NEEDS_FIX",
                subtitleNote = "وصول نزيل VIP الساعة 2:00 ظهراً",
                targetMinutes = 12,
                iotStatusLabel = "IoT: مستشعر التعطير منخفض",
                iotSeverity = "WARNING",
                qualityScore = 88,
                assignedStaff = "خالد الحربي",
                alertActive = true,
                alertMessage = "يرجى إعادة تعبئة جهاز التعطير الذكي وضبط إضاءة المكتب قبل وصول النزيل.",
                explicitDocId = "room_412"
            )
            createRoomTask(
                roomNumber = "105",
                roomTitle = "غرفة قياسية #105",
                floorNumber = 1,
                priority = "LOW",
                status = "COMPLETED",
                subtitleNote = "تم اعتماد الجودة بنسبة 98% وجاهزة للتسليم",
                targetMinutes = 15,
                iotStatusLabel = "IoT: مكتمل ومعتمد",
                iotSeverity = "STABLE",
                qualityScore = 98,
                assignedStaff = "ماجد العتيبي",
                alertActive = false,
                alertMessage = "",
                explicitDocId = "room_105"
            )
            recordInspectionLog(
                roomNumber = "105",
                roomTitle = "غرفة قياسية #105",
                qualityScore = 98,
                issuesCount = 0,
                durationSeconds = 95,
                beddingIssues = 0,
                towelIssues = 0,
                surfaceIssues = 0,
                maintenanceIssues = 0,
                summaryText = "المفروشات متطابقة والميني بار مكتمل 100%",
                notifiedHousekeeper = false
            )
            recordInspectionLog(
                roomNumber = "412",
                roomTitle = "جناح تنفيذي #412",
                qualityScore = 88,
                issuesCount = 2,
                durationSeconds = 112,
                beddingIssues = 1,
                towelIssues = 1,
                surfaceIssues = 0,
                maintenanceIssues = 0,
                summaryText = "رصد نقص طفيف في كبسولات القهوة ومستوى التعطير",
                notifiedHousekeeper = true
            )
        }

        if (_consumables.value.isEmpty()) {
            createConsumableItem(
                itemName = "طقم مناشف قطن مصري فاخر",
                category = "TOWELS",
                currentStock = 14,
                minThreshold = 20,
                maxCapacity = 80,
                unitLabel = "طقم",
                storageLocation = "مخزن الدور 3",
                explicitDocId = "item_towels_royal"
            )
            createConsumableItem(
                itemName = "شامبو عضوي بزيت الأرغان (50 مل)",
                category = "SHAMPOO",
                currentStock = 18,
                minThreshold = 25,
                maxCapacity = 120,
                unitLabel = "عبوة",
                storageLocation = "المستودع الرئيسي",
                explicitDocId = "item_shampoo_argan"
            )
            createConsumableItem(
                itemName = "صابون يدين واستحمام نباتي فاخر",
                category = "SOAP",
                currentStock = 64,
                minThreshold = 30,
                maxCapacity = 150,
                unitLabel = "قطعة",
                storageLocation = "مخزن الدور 2",
                explicitDocId = "item_soap_botanical"
            )
            createConsumableItem(
                itemName = "بلسم شعر وجل استحمام منعش",
                category = "SHAMPOO",
                currentStock = 48,
                minThreshold = 25,
                maxCapacity = 100,
                unitLabel = "عبوة",
                storageLocation = "المستودع الرئيسي",
                explicitDocId = "item_conditioner_gel"
            )
            createConsumableItem(
                itemName = "أرواب حمام ومناشف وجه مطرزة",
                category = "TOWELS",
                currentStock = 36,
                minThreshold = 15,
                maxCapacity = 60,
                unitLabel = "طقم",
                storageLocation = "مغسلة الفندق المركزية",
                explicitDocId = "item_bathrobes"
            )
            createConsumableItem(
                itemName = "طقم عناية شخصية ومعطر غرف ذكي",
                category = "AMENITIES",
                currentStock = 11,
                minThreshold = 15,
                maxCapacity = 75,
                unitLabel = "طقم",
                storageLocation = "مخزن الدور 4 (VIP)",
                explicitDocId = "item_vip_amenities"
            )
        }
    }

    override suspend fun createRoomTask(
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        priority: String,
        status: String,
        subtitleNote: String,
        targetMinutes: Int,
        iotStatusLabel: String,
        iotSeverity: String,
        qualityScore: Int,
        assignedStaff: String,
        alertActive: Boolean,
        alertMessage: String,
        explicitDocId: String?
    ): Result<String> {
        val docId = explicitDocId?.takeIf { it.isNotBlank() }
            ?: "room_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val ts = Timestamp(currentEpochSeconds(), 0)
        val newTask = RoomTask(
            id = docId,
            userId = LOCAL_WORKSPACE_USER,
            roomNumber = roomNumber.take(20),
            roomTitle = roomTitle.take(120),
            floorNumber = floorNumber.coerceIn(0, 200).toLong(),
            priority = priority,
            status = status,
            subtitleNote = subtitleNote.take(250),
            targetMinutes = targetMinutes.coerceIn(1, 240).toLong(),
            iotStatusLabel = iotStatusLabel.take(150),
            iotSeverity = iotSeverity,
            qualityScore = qualityScore.coerceIn(0, 100).toLong(),
            assignedStaff = assignedStaff.take(80),
            alertActive = alertActive,
            alertMessage = alertMessage.take(500),
            createdAt = ts,
            updatedAt = ts
        )
        val updated = _rooms.value.filterNot { it.id == docId } + newTask
        _rooms.value = updated
        saveRooms(updated)
        return Result.success(docId)
    }

    override suspend fun createConsumableItem(
        itemName: String,
        category: String,
        currentStock: Int,
        minThreshold: Int,
        maxCapacity: Int,
        unitLabel: String,
        storageLocation: String,
        explicitDocId: String?
    ): Result<String> {
        val docId = explicitDocId?.takeIf { it.isNotBlank() }
            ?: "item_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val safeStock = currentStock.coerceIn(0, 100000)
        val safeMin = minThreshold.coerceIn(1, 50000)
        val safeMax = maxCapacity.coerceAtLeast(safeMin).coerceIn(1, 100000)
        val isLow = safeStock <= safeMin
        val cleanUnit = unitLabel.trim().ifBlank { "قطعة" }.take(40)
        val ts = Timestamp(currentEpochSeconds(), 0)

        val newItem = ConsumableItem(
            id = docId,
            userId = LOCAL_WORKSPACE_USER,
            itemName = itemName.trim().ifBlank { "صنف مستهلك" }.take(120),
            category = category,
            currentStock = safeStock.toLong(),
            minThreshold = safeMin.toLong(),
            maxCapacity = safeMax.toLong(),
            unitLabel = cleanUnit,
            storageLocation = storageLocation.trim().ifBlank { "المستودع الرئيسي" }.take(120),
            managementAlertSent = isLow,
            alertNote = if (isLow) {
                "تنبيه تلقائي للإدارة: الرصيد ($safeStock $cleanUnit) وصل للحد الأدنى ($safeMin)"
            } else {
                ""
            }.take(400),
            createdAt = ts,
            updatedAt = ts
        )
        val updated = _consumables.value.filterNot { it.id == docId } + newItem
        _consumables.value = updated
        saveConsumables(updated)
        return Result.success(docId)
    }

    override suspend fun updateConsumableStock(
        item: ConsumableItem,
        newStock: Int,
        forceManagementOrder: Boolean
    ): Result<Boolean> {
        val safeStock = newStock.coerceIn(0, 100000)
        val minThresh = item.minThreshold.toInt().coerceAtLeast(1)
        val isNowLow = safeStock <= minThresh
        val shouldFlagAlert = isNowLow || forceManagementOrder
        val note = when {
            forceManagementOrder -> "تم رفع طلب توريد عاجل للإدارة والمشتريات (${item.itemName} - المتبقي $safeStock ${item.unitLabel})"
            isNowLow -> "تنبيه انخفاض مخزون للإدارة: المتبقي $safeStock ${item.unitLabel} (الحد الأدنى $minThresh)"
            else -> ""
        }.take(400)

        val ts = Timestamp(currentEpochSeconds(), 0)
        val updated = _consumables.value.map { existing ->
            if (existing.id == item.id) {
                existing.copy(
                    currentStock = safeStock.toLong(),
                    managementAlertSent = shouldFlagAlert,
                    alertNote = note,
                    updatedAt = ts
                )
            } else {
                existing
            }
        }
        _consumables.value = updated
        saveConsumables(updated)
        return Result.success(isNowLow)
    }

    override suspend fun consumeStandardRoomKitOnCompletion(): List<String> {
        val triggeredLowStockItems = mutableListOf<String>()
        val items = _consumables.value
        if (items.isEmpty()) return emptyList()

        val soap = items.firstOrNull { it.category == "SOAP" }
        val shampoo = items.firstOrNull { it.category == "SHAMPOO" }
        val towels = items.firstOrNull { it.category == "TOWELS" }

        listOfNotNull(
            soap?.let { it to 2 },
            shampoo?.let { it to 2 },
            towels?.let { it to 3 }
        ).forEach { (item, usedQty) ->
            val updatedQty = (item.currentStock.toInt() - usedQty).coerceAtLeast(0)
            val becameLow = updateConsumableStock(item, updatedQty).getOrDefault(false)
            if (becameLow) {
                triggeredLowStockItems.add(item.itemName)
            }
        }
        return triggeredLowStockItems
    }

    override suspend fun sendHousekeeperAlertForRoom(
        taskId: String,
        qualityScore: Int,
        alertMessage: String,
        iotStatusLabel: String,
        iotSeverity: String
    ): Result<Unit> {
        val ts = Timestamp(currentEpochSeconds(), 0)
        val updated = _rooms.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    status = "NEEDS_FIX",
                    priority = "HIGH",
                    qualityScore = qualityScore.coerceIn(0, 100).toLong(),
                    alertActive = true,
                    alertMessage = alertMessage.take(500),
                    iotStatusLabel = iotStatusLabel.take(150),
                    iotSeverity = iotSeverity,
                    updatedAt = ts
                )
            } else {
                task
            }
        }
        _rooms.value = updated
        saveRooms(updated)
        return Result.success(Unit)
    }

    override suspend fun markRoomCompleted(taskId: String, finalScore: Int): Result<Unit> {
        val ts = Timestamp(currentEpochSeconds(), 0)
        val updated = _rooms.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    status = "COMPLETED",
                    alertActive = false,
                    alertMessage = "",
                    iotStatusLabel = "IoT: مكتمل ومعتمد",
                    iotSeverity = "STABLE",
                    qualityScore = finalScore.coerceIn(0, 100).toLong(),
                    updatedAt = ts
                )
            } else {
                task
            }
        }
        _rooms.value = updated
        saveRooms(updated)
        return Result.success(Unit)
    }

    override suspend fun recordInspectionLog(
        roomNumber: String,
        roomTitle: String,
        qualityScore: Int,
        issuesCount: Int,
        durationSeconds: Int,
        beddingIssues: Int,
        towelIssues: Int,
        surfaceIssues: Int,
        maintenanceIssues: Int,
        summaryText: String,
        notifiedHousekeeper: Boolean
    ): Result<String> {
        val docId = "insp_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val ts = Timestamp(currentEpochSeconds(), 0)
        val log = InspectionLog(
            id = docId,
            userId = LOCAL_WORKSPACE_USER,
            roomNumber = roomNumber.take(20),
            roomTitle = roomTitle.take(120),
            qualityScore = qualityScore.coerceIn(0, 100).toLong(),
            issuesCount = issuesCount.coerceIn(0, 50).toLong(),
            durationSeconds = durationSeconds.coerceIn(1, 7200).toLong(),
            beddingIssues = beddingIssues.coerceIn(0, 50).toLong(),
            towelIssues = towelIssues.coerceIn(0, 50).toLong(),
            surfaceIssues = surfaceIssues.coerceIn(0, 50).toLong(),
            maintenanceIssues = maintenanceIssues.coerceIn(0, 50).toLong(),
            summaryText = summaryText.ifBlank { "تم إتمام الفحص البصري بنجاح" }.take(600),
            notifiedHousekeeper = notifiedHousekeeper,
            createdAt = ts,
            updatedAt = ts
        )
        val updated = listOf(log) + _inspections.value
        _inspections.value = updated
        saveInspections(updated)
        return Result.success(docId)
    }

    @Serializable
    private data class StoredRoom(
        val id: String,
        val roomNumber: String,
        val roomTitle: String,
        val floorNumber: Long,
        val priority: String,
        val status: String,
        val subtitleNote: String,
        val targetMinutes: Long,
        val iotStatusLabel: String,
        val iotSeverity: String,
        val qualityScore: Long,
        val assignedStaff: String,
        val alertActive: Boolean,
        val alertMessage: String,
        val createdSec: Long,
        val updatedSec: Long
    )

    @Serializable
    private data class StoredInspection(
        val id: String,
        val roomNumber: String,
        val roomTitle: String,
        val qualityScore: Long,
        val issuesCount: Long,
        val durationSeconds: Long,
        val beddingIssues: Long,
        val towelIssues: Long,
        val surfaceIssues: Long,
        val maintenanceIssues: Long,
        val summaryText: String,
        val notifiedHousekeeper: Boolean,
        val createdSec: Long
    )

    @Serializable
    private data class StoredConsumable(
        val id: String,
        val itemName: String,
        val category: String,
        val currentStock: Long,
        val minThreshold: Long,
        val maxCapacity: Long,
        val unitLabel: String,
        val storageLocation: String,
        val managementAlertSent: Boolean,
        val alertNote: String,
        val createdSec: Long,
        val updatedSec: Long
    )

    private fun saveRooms(list: List<RoomTask>) {
        val raw = json.encodeToString(
            list.map {
                StoredRoom(
                    id = it.id,
                    roomNumber = it.roomNumber,
                    roomTitle = it.roomTitle,
                    floorNumber = it.floorNumber,
                    priority = it.priority,
                    status = it.status,
                    subtitleNote = it.subtitleNote,
                    targetMinutes = it.targetMinutes,
                    iotStatusLabel = it.iotStatusLabel,
                    iotSeverity = it.iotSeverity,
                    qualityScore = it.qualityScore,
                    assignedStaff = it.assignedStaff,
                    alertActive = it.alertActive,
                    alertMessage = it.alertMessage,
                    createdSec = it.createdAt?.seconds ?: currentEpochSeconds(),
                    updatedSec = it.updatedAt?.seconds ?: currentEpochSeconds()
                )
            }
        )
        prefs.edit().putString(KEY_ROOMS, raw).apply()
    }

    private fun loadRooms(): List<RoomTask> {
        val raw = prefs.getString(KEY_ROOMS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredRoom>>(raw).map {
                RoomTask(
                    id = it.id,
                    userId = LOCAL_WORKSPACE_USER,
                    roomNumber = it.roomNumber,
                    roomTitle = it.roomTitle,
                    floorNumber = it.floorNumber,
                    priority = it.priority,
                    status = it.status,
                    subtitleNote = it.subtitleNote,
                    targetMinutes = it.targetMinutes,
                    iotStatusLabel = it.iotStatusLabel,
                    iotSeverity = it.iotSeverity,
                    qualityScore = it.qualityScore,
                    assignedStaff = it.assignedStaff,
                    alertActive = it.alertActive,
                    alertMessage = it.alertMessage,
                    createdAt = Timestamp(it.createdSec, 0),
                    updatedAt = Timestamp(it.updatedSec, 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveInspections(list: List<InspectionLog>) {
        val raw = json.encodeToString(
            list.map {
                StoredInspection(
                    id = it.id,
                    roomNumber = it.roomNumber,
                    roomTitle = it.roomTitle,
                    qualityScore = it.qualityScore,
                    issuesCount = it.issuesCount,
                    durationSeconds = it.durationSeconds,
                    beddingIssues = it.beddingIssues,
                    towelIssues = it.towelIssues,
                    surfaceIssues = it.surfaceIssues,
                    maintenanceIssues = it.maintenanceIssues,
                    summaryText = it.summaryText,
                    notifiedHousekeeper = it.notifiedHousekeeper,
                    createdSec = it.createdAt?.seconds ?: currentEpochSeconds()
                )
            }
        )
        prefs.edit().putString(KEY_INSPECTIONS, raw).apply()
    }

    private fun loadInspections(): List<InspectionLog> {
        val raw = prefs.getString(KEY_INSPECTIONS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredInspection>>(raw).map {
                val ts = Timestamp(it.createdSec, 0)
                InspectionLog(
                    id = it.id,
                    userId = LOCAL_WORKSPACE_USER,
                    roomNumber = it.roomNumber,
                    roomTitle = it.roomTitle,
                    qualityScore = it.qualityScore,
                    issuesCount = it.issuesCount,
                    durationSeconds = it.durationSeconds,
                    beddingIssues = it.beddingIssues,
                    towelIssues = it.towelIssues,
                    surfaceIssues = it.surfaceIssues,
                    maintenanceIssues = it.maintenanceIssues,
                    summaryText = it.summaryText,
                    notifiedHousekeeper = it.notifiedHousekeeper,
                    createdAt = ts,
                    updatedAt = ts
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveConsumables(list: List<ConsumableItem>) {
        val raw = json.encodeToString(
            list.map {
                StoredConsumable(
                    id = it.id,
                    itemName = it.itemName,
                    category = it.category,
                    currentStock = it.currentStock,
                    minThreshold = it.minThreshold,
                    maxCapacity = it.maxCapacity,
                    unitLabel = it.unitLabel,
                    storageLocation = it.storageLocation,
                    managementAlertSent = it.managementAlertSent,
                    alertNote = it.alertNote,
                    createdSec = it.createdAt?.seconds ?: currentEpochSeconds(),
                    updatedSec = it.updatedAt?.seconds ?: currentEpochSeconds()
                )
            }
        )
        prefs.edit().putString(KEY_CONSUMABLES, raw).apply()
    }

    private fun loadConsumables(): List<ConsumableItem> {
        val raw = prefs.getString(KEY_CONSUMABLES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredConsumable>>(raw).map {
                ConsumableItem(
                    id = it.id,
                    userId = LOCAL_WORKSPACE_USER,
                    itemName = it.itemName,
                    category = it.category,
                    currentStock = it.currentStock,
                    minThreshold = it.minThreshold,
                    maxCapacity = it.maxCapacity,
                    unitLabel = it.unitLabel,
                    storageLocation = it.storageLocation,
                    managementAlertSent = it.managementAlertSent,
                    alertNote = it.alertNote,
                    createdAt = Timestamp(it.createdSec, 0),
                    updatedAt = Timestamp(it.updatedSec, 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "opticlean_direct_workspace"
        const val KEY_ROOMS = "rooms_json_v1"
        const val KEY_INSPECTIONS = "inspections_json_v1"
        const val KEY_CONSUMABLES = "consumables_json_v1"
        const val LOCAL_WORKSPACE_USER = "direct_workspace"
    }
}

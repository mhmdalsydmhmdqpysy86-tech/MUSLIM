package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiRoomScannerService
import com.example.data.model.AiDetectionItem
import com.example.data.model.AiRoomScanReport
import com.example.data.model.ConsumableItem
import com.example.data.model.InspectionLog
import com.example.data.model.RoomTask
import com.example.data.model.UiState
import com.example.data.repository.FirestoreOptiCleanDataSource
import com.example.data.repository.OptiCleanDataSource
import com.example.data.repository.OptiCleanRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OptiCleanTab {
    SUPERVISOR,
    HOUSEKEEPER,
    INVENTORY,
    DASHBOARD
}

enum class ViewportInputMode {
    SAMPLE_ROOM,
    LIVE_CAMERA,
    PHOTO_GALLERY
}

data class ScanSessionUiState(
    val selectedRoomId: String = "room_304",
    val selectedRoomNumber: String = "304",
    val selectedRoomTitle: String = "غرفة الملكية #304",
    val selectedFloor: Int = 3,
    val selectedIotStatus: String = "IoT: نقص مناشف (1/3)",
    val viewportMode: ViewportInputMode = ViewportInputMode.SAMPLE_ROOM,
    val customBitmap: Bitmap? = null,
    val isScanning: Boolean = false,
    val visibleDetectionCount: Int = 0,
    val activeReport: AiRoomScanReport? = null,
    val notificationSentBanner: String? = null,
    val snackbarMessage: String? = null
)

class OptiCleanViewModel(
    private val dataSource: OptiCleanDataSource,
    private val scannerService: GeminiRoomScannerService
) : ViewModel() {

    constructor(
        repository: OptiCleanRepository,
        scannerService: GeminiRoomScannerService,
        currentUserId: String
    ) : this(
        dataSource = FirestoreOptiCleanDataSource(repository, currentUserId),
        scannerService = scannerService
    )

    private val _selectedTab = MutableStateFlow(OptiCleanTab.SUPERVISOR)
    val selectedTab: StateFlow<OptiCleanTab> = _selectedTab.asStateFlow()

    private val _scanState = MutableStateFlow(ScanSessionUiState())
    val scanState: StateFlow<ScanSessionUiState> = _scanState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedInventoryCategory = MutableStateFlow("ALL") // ALL, LOW_STOCK, SOAP, SHAMPOO, TOWELS, AMENITIES
    val selectedInventoryCategory: StateFlow<String> = _selectedInventoryCategory.asStateFlow()

    val roomTasksState: StateFlow<UiState<List<RoomTask>>> = dataSource.observeRoomTasks()
        .map<List<RoomTask>, UiState<List<RoomTask>>> { list ->
            val sorted = list.sortedWith(
                compareBy<RoomTask>(
                    { it.status == "COMPLETED" },
                    { !it.alertActive },
                    {
                        when (it.priority) {
                            "HIGH" -> 0
                            "MEDIUM" -> 1
                            else -> 2
                        }
                    }
                ).thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            syncSelectedRoomWithLatest(sorted)
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing room tasks", error)
            emit(UiState.Error(error.message ?: "تعذر تحميل مهام الغرف"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val inspectionsState: StateFlow<UiState<List<InspectionLog>>> = dataSource.observeInspections()
        .map<List<InspectionLog>, UiState<List<InspectionLog>>> { list ->
            val sorted = list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing inspections", error)
            emit(UiState.Error(error.message ?: "تعذر تحميل سجل التدقيق"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val consumablesState: StateFlow<UiState<List<ConsumableItem>>> = dataSource.observeConsumables()
        .map<List<ConsumableItem>, UiState<List<ConsumableItem>>> { list ->
            val sorted = list.sortedWith(
                compareByDescending<ConsumableItem> { it.isLowStock }
                    .thenBy { it.currentStock }
                    .thenBy { it.itemName }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing consumables", error)
            emit(UiState.Error(error.message ?: "تعذر تحميل جرد المواد المستهلكة"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            dataSource.seedInitialWorkspaceIfEmpty()
        }
    }

    private fun syncSelectedRoomWithLatest(tasks: List<RoomTask>) {
        if (tasks.isEmpty()) return
        val current = _scanState.value
        val matched = tasks.find { it.id == current.selectedRoomId || it.roomNumber == current.selectedRoomNumber }
            ?: tasks.first()
        _scanState.update { state ->
            state.copy(
                selectedRoomId = matched.id,
                selectedRoomNumber = matched.roomNumber,
                selectedRoomTitle = matched.roomTitle,
                selectedFloor = matched.floorNumber.toInt(),
                selectedIotStatus = matched.iotStatusLabel
            )
        }
    }

    fun selectTab(tab: OptiCleanTab) {
        _selectedTab.value = tab
    }

    fun setTaskFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setInventoryCategoryFilter(category: String) {
        _selectedInventoryCategory.value = category
    }

    fun selectRoomForInspection(task: RoomTask) {
        _scanState.update {
            it.copy(
                selectedRoomId = task.id,
                selectedRoomNumber = task.roomNumber,
                selectedRoomTitle = task.roomTitle,
                selectedFloor = task.floorNumber.toInt(),
                selectedIotStatus = task.iotStatusLabel,
                activeReport = null,
                visibleDetectionCount = 0,
                notificationSentBanner = null,
                customBitmap = if (it.viewportMode == ViewportInputMode.PHOTO_GALLERY) null else it.customBitmap,
                viewportMode = if (it.viewportMode == ViewportInputMode.PHOTO_GALLERY) ViewportInputMode.SAMPLE_ROOM else it.viewportMode
            )
        }
        _selectedTab.value = OptiCleanTab.SUPERVISOR
    }

    fun setViewportMode(mode: ViewportInputMode) {
        _scanState.update {
            it.copy(
                viewportMode = mode,
                activeReport = null,
                visibleDetectionCount = 0
            )
        }
    }

    fun onPhotoSelectedAndScan(bitmap: Bitmap) {
        _scanState.update {
            it.copy(
                viewportMode = ViewportInputMode.PHOTO_GALLERY,
                customBitmap = bitmap,
                activeReport = null,
                visibleDetectionCount = 0
            )
        }
        runAiRoomScan(bitmapOverride = bitmap)
    }

    fun runAiRoomScan(bitmapOverride: Bitmap? = null) {
        val snapshot = _scanState.value
        if (snapshot.isScanning) return

        val bitmapToAnalyze = bitmapOverride ?: snapshot.customBitmap
        val scanStartedMs = System.currentTimeMillis()

        viewModelScope.launch {
            _scanState.update {
                it.copy(
                    isScanning = true,
                    activeReport = null,
                    visibleDetectionCount = 0,
                    notificationSentBanner = null
                )
            }

            val report = scannerService.scanRoom(
                bitmap = bitmapToAnalyze,
                roomNumber = snapshot.selectedRoomNumber,
                roomTitle = snapshot.selectedRoomTitle,
                iotContextLabel = snapshot.selectedIotStatus
            )

            _scanState.update {
                it.copy(
                    activeReport = report,
                    visibleDetectionCount = 0
                )
            }

            for (index in 1..report.detections.size) {
                delay(420L)
                _scanState.update { it.copy(visibleDetectionCount = index) }
            }

            delay(250L)
            val elapsedSec = ((System.currentTimeMillis() - scanStartedMs) / 1000L).toInt().coerceAtLeast(85)
            persistScanAudit(snapshot, report, elapsedSec, notified = false)

            _scanState.update {
                it.copy(isScanning = false)
            }
        }
    }

    private suspend fun persistScanAudit(
        session: ScanSessionUiState,
        report: AiRoomScanReport,
        durationSeconds: Int,
        notified: Boolean
    ) {
        val actionable = report.detections.filter { it.severity == "ERROR" || it.severity == "WARNING" }
        val beddingCount = actionable.count { it.category == "BEDDING" }
        val towelCount = actionable.count { it.category == "TOWELS" }
        val surfaceCount = actionable.count { it.category == "SURFACE" }
        val maintenanceCount = actionable.count { it.category == "MAINTENANCE" }

        dataSource.recordInspectionLog(
            roomNumber = session.selectedRoomNumber,
            roomTitle = session.selectedRoomTitle,
            qualityScore = report.qualityScore,
            issuesCount = actionable.size,
            durationSeconds = durationSeconds,
            beddingIssues = beddingCount,
            towelIssues = towelCount,
            surfaceIssues = surfaceCount,
            maintenanceIssues = maintenanceCount,
            summaryText = report.summaryText,
            notifiedHousekeeper = notified
        )
    }

    fun sendReportToHousekeeper() {
        val current = _scanState.value
        val report = current.activeReport ?: return
        val actionable: List<AiDetectionItem> = report.detections.filter {
            it.severity == "ERROR" || it.severity == "WARNING"
        }

        val combinedAlert = if (actionable.isNotEmpty()) {
            "غرفة #${current.selectedRoomNumber} تطلب فوراً: " +
                actionable.joinToString(" • ") { it.title }
        } else {
            "تم فحص الغرفة #${current.selectedRoomNumber} واعتماد جاهزيتها."
        }

        viewModelScope.launch {
            if (current.selectedRoomId.isNotBlank()) {
                if (actionable.isNotEmpty()) {
                    val iotIssue = actionable.firstOrNull { it.sourceBadge == "IoT" }
                    dataSource.sendHousekeeperAlertForRoom(
                        taskId = current.selectedRoomId,
                        qualityScore = report.qualityScore,
                        alertMessage = combinedAlert,
                        iotStatusLabel = iotIssue?.title ?: current.selectedIotStatus,
                        iotSeverity = if (actionable.any { it.severity == "ERROR" }) "CRITICAL" else "WARNING"
                    )
                } else {
                    dataSource.markRoomCompleted(
                        taskId = current.selectedRoomId,
                        finalScore = report.qualityScore
                    )
                }
            }

            _scanState.update {
                it.copy(
                    notificationSentBanner = combinedAlert,
                    snackbarMessage = "تم إرسال تقرير الذكاء الاصطناعي فوراً إلى مهام عامل النظافة!"
                )
            }
            _selectedTab.value = OptiCleanTab.HOUSEKEEPER
        }
    }

    fun approveSelectedRoomDirectly() {
        val current = _scanState.value
        val score = current.activeReport?.qualityScore ?: 98
        if (current.selectedRoomId.isBlank()) return

        viewModelScope.launch {
            dataSource.markRoomCompleted(current.selectedRoomId, score)
            val lowItems = dataSource.consumeStandardRoomKitOnCompletion()
            val msg = if (lowItems.isNotEmpty()) {
                "تم اعتماد الغرفة وصرف المستهلكات • تنبيه للإدارة بانخفاض مخزون: ${lowItems.joinToString("، ")}"
            } else {
                "تم اعتماد الغرفة #${current.selectedRoomNumber} وتحديث جرد المستهلكات"
            }
            _scanState.update { it.copy(snackbarMessage = msg) }
            _selectedTab.value = OptiCleanTab.HOUSEKEEPER
        }
    }

    fun completeRoomTask(task: RoomTask) {
        if (task.id.isBlank()) return
        viewModelScope.launch {
            dataSource.markRoomCompleted(task.id, finalScore = 98)
            val lowItems = dataSource.consumeStandardRoomKitOnCompletion()
            val msg = if (lowItems.isNotEmpty()) {
                "تم إكمال ${task.roomTitle} وصرف المستهلكات • تم تنبيه الإدارة لنقص: ${lowItems.joinToString("، ")}"
            } else {
                "تم إكمال ${task.roomTitle} وخصم طقم المستهلكات القياسي من المخزون"
            }
            _scanState.update { it.copy(snackbarMessage = msg) }
        }
    }

    fun addNewRoomTask(
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        priority: String,
        assignedStaff: String,
        subtitleNote: String
    ) {
        val cleanNum = roomNumber.trim().ifBlank { "405" }
        val cleanTitle = roomTitle.trim().ifBlank { "غرفة ضيافة #$cleanNum" }
        val cleanStaff = assignedStaff.trim().ifBlank { "فريق الإشراف" }
        val cleanNote = subtitleNote.trim().ifBlank { "فحص جاهزية الغرفة قبل تسجيل الدخول" }

        viewModelScope.launch {
            val result = dataSource.createRoomTask(
                roomNumber = cleanNum,
                roomTitle = cleanTitle,
                floorNumber = floorNumber.coerceIn(1, 99),
                priority = priority,
                status = "PENDING",
                subtitleNote = cleanNote,
                targetMinutes = if (priority == "HIGH") 15 else 20,
                iotStatusLabel = "IoT: مستقر ومتصل",
                iotSeverity = "STABLE",
                qualityScore = 92,
                assignedStaff = cleanStaff
            )
            if (result.isSuccess) {
                _scanState.update {
                    it.copy(snackbarMessage = "تمت إضافة $cleanTitle إلى قائمة المهام")
                }
            } else {
                _scanState.update {
                    it.copy(snackbarMessage = "تعذر حفظ المهمة الجديدة، يرجى المحاولة مجدداً")
                }
            }
        }
    }

    // --- Consumables Inventory Operations ---

    fun adjustConsumableStock(item: ConsumableItem, delta: Int) {
        val updatedQty = (item.currentStock.toInt() + delta).coerceIn(0, 100000)
        viewModelScope.launch {
            val res = dataSource.updateConsumableStock(item, updatedQty)
            val becameLow = res.getOrDefault(false)
            if (becameLow && delta < 0) {
                _scanState.update {
                    it.copy(
                        snackbarMessage = "تنبيه للإدارة: انخفض مخزون «${item.itemName}» إلى $updatedQty ${item.unitLabel} (دون الحد الأدنى)!"
                    )
                }
            }
        }
    }

    fun restockConsumableToCapacity(item: ConsumableItem) {
        val targetCapacity = item.maxCapacity.toInt().coerceAtLeast(item.minThreshold.toInt() + 10)
        viewModelScope.launch {
            val res = dataSource.updateConsumableStock(item, targetCapacity)
            if (res.isSuccess) {
                _scanState.update {
                    it.copy(
                        snackbarMessage = "تم توريد وتعبئة «${item.itemName}» بالكامل ($targetCapacity ${item.unitLabel})"
                    )
                }
            }
        }
    }

    fun sendManagementSupplyAlert(item: ConsumableItem) {
        viewModelScope.launch {
            val res = dataSource.updateConsumableStock(
                item = item,
                newStock = item.currentStock.toInt(),
                forceManagementOrder = true
            )
            if (res.isSuccess) {
                _scanState.update {
                    it.copy(
                        snackbarMessage = "تم إرسال إشعار وطلب توريد عاجل للإدارة بخصوص «${item.itemName}»"
                    )
                }
            }
        }
    }

    fun notifyManagementForAllLowStock(lowItems: List<ConsumableItem>) {
        if (lowItems.isEmpty()) return
        viewModelScope.launch {
            lowItems.forEach { item ->
                dataSource.updateConsumableStock(
                    item = item,
                    newStock = item.currentStock.toInt(),
                    forceManagementOrder = true
                )
            }
            _scanState.update {
                it.copy(
                    snackbarMessage = "تم إرسال تنبيه موحد للإدارة وقسم المشتريات لتوريد (${lowItems.size}) أصناف منخفضة المخزون!"
                )
            }
        }
    }

    fun addNewConsumable(
        itemName: String,
        category: String,
        currentStock: Int,
        minThreshold: Int,
        maxCapacity: Int,
        unitLabel: String,
        storageLocation: String
    ) {
        viewModelScope.launch {
            val result = dataSource.createConsumableItem(
                itemName = itemName,
                category = category,
                currentStock = currentStock,
                minThreshold = minThreshold,
                maxCapacity = maxCapacity,
                unitLabel = unitLabel,
                storageLocation = storageLocation
            )
            if (result.isSuccess) {
                val lowAlertSuffix = if (currentStock <= minThreshold) " • تم تفعيل تنبيه الإدارة تلقائياً" else ""
                _scanState.update {
                    it.copy(snackbarMessage = "تمت إضافة الصنف إلى المخزون$lowAlertSuffix")
                }
            } else {
                _scanState.update {
                    it.copy(snackbarMessage = "تعذر إضافة الصنف، يرجى التحقق من البيانات")
                }
            }
        }
    }

    fun consumeSnackbar() {
        _scanState.update { it.copy(snackbarMessage = null) }
    }

    private companion object {
        const val TAG = "OptiCleanVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

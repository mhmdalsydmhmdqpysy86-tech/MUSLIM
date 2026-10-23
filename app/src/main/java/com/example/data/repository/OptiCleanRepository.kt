package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ConsumableItem
import com.example.data.model.InspectionLog
import com.example.data.model.RoomTask
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OptiCleanRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام Google أولاً للوصول إلى قاعدة البيانات السحابية.")
    }

    fun observeRoomTasks(userId: String = requireUserId()): Flow<List<RoomTask>> = flow {
        val path = "users/$userId/room_tasks"
        emitAll(
            db.collection("users").document(userId).collection("room_tasks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RoomTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeInspections(userId: String = requireUserId()): Flow<List<InspectionLog>> = flow {
        val path = "users/$userId/inspections"
        emitAll(
            db.collection("users").document(userId).collection("inspections")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(InspectionLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeConsumables(userId: String = requireUserId()): Flow<List<ConsumableItem>> = flow {
        val path = "users/$userId/consumables"
        emitAll(
            db.collection("users").document(userId).collection("consumables")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ConsumableItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getRoomTasksOnce(userId: String = requireUserId()): Result<List<RoomTask>> {
        val path = "users/$userId/room_tasks"
        return try {
            val snapshot = db.collection("users").document(userId).collection("room_tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RoomTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getConsumablesOnce(userId: String = requireUserId()): Result<List<ConsumableItem>> {
        val path = "users/$userId/consumables"
        return try {
            val snapshot = db.collection("users").document(userId).collection("consumables")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ConsumableItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getRoomTaskById(taskId: String, targetOwnerId: String? = null): Result<RoomTask?> {
        val uid = targetOwnerId ?: requireUserId()
        val path = "users/$uid/room_tasks/$taskId"
        return try {
            val doc = db.collection("users").document(uid).collection("room_tasks").document(taskId)
                .get()
                .await()
            Result.success(doc.toObject(RoomTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

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
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("room_tasks")
        val docRef = if (!explicitDocId.isNullOrBlank()) {
            collectionRef.document(explicitDocId)
        } else {
            collectionRef.document()
        }

        val payload = mapOf(
            "userId" to uid,
            "roomNumber" to roomNumber.take(20),
            "roomTitle" to roomTitle.take(120),
            "floorNumber" to floorNumber.coerceIn(0, 200),
            "priority" to priority,
            "status" to status,
            "subtitleNote" to subtitleNote.take(250),
            "targetMinutes" to targetMinutes.coerceIn(1, 240),
            "iotStatusLabel" to iotStatusLabel.take(150),
            "iotSeverity" to iotSeverity,
            "qualityScore" to qualityScore.coerceIn(0, 100),
            "assignedStaff" to assignedStaff.take(80),
            "alertActive" to alertActive,
            "alertMessage" to alertMessage.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createConsumableItem(
        itemName: String,
        category: String,
        currentStock: Int,
        minThreshold: Int,
        maxCapacity: Int,
        unitLabel: String,
        storageLocation: String,
        explicitDocId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("consumables")
        val docRef = if (!explicitDocId.isNullOrBlank()) {
            collectionRef.document(explicitDocId)
        } else {
            collectionRef.document()
        }

        val safeStock = currentStock.coerceIn(0, 100000)
        val safeMin = minThreshold.coerceIn(1, 50000)
        val safeMax = maxCapacity.coerceAtLeast(safeMin).coerceIn(1, 100000)
        val isLow = safeStock <= safeMin

        val payload = mapOf(
            "userId" to uid,
            "itemName" to itemName.trim().ifBlank { "صنف مستهلك" }.take(120),
            "category" to category,
            "currentStock" to safeStock,
            "minThreshold" to safeMin,
            "maxCapacity" to safeMax,
            "unitLabel" to unitLabel.trim().ifBlank { "قطعة" }.take(40),
            "storageLocation" to storageLocation.trim().ifBlank { "المستودع الرئيسي" }.take(120),
            "managementAlertSent" to isLow,
            "alertNote" to if (isLow) {
                "تنبيه تلقائي للإدارة: الرصيد ($safeStock $unitLabel) وصل للحد الأدنى ($safeMin)"
            } else {
                ""
            }.take(400),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateConsumableStock(
        item: ConsumableItem,
        newStock: Int,
        forceManagementOrder: Boolean = false
    ): Result<Boolean> {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "ConsumableItem ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("consumables").document(item.id)

        val safeStock = newStock.coerceIn(0, 100000)
        val minThresh = item.minThreshold.toInt().coerceAtLeast(1)
        val isNowLow = safeStock <= minThresh
        val shouldFlagAlert = isNowLow || forceManagementOrder

        val note = when {
            forceManagementOrder -> "تم رفع طلب توريد عاجل للإدارة والمشتريات (${item.itemName} - المتبقي $safeStock ${item.unitLabel})"
            isNowLow -> "تنبيه انخفاض مخزون للإدارة: المتبقي $safeStock ${item.unitLabel} (الحد الأدنى $minThresh)"
            else -> ""
        }.take(400)

        val updates = mapOf(
            "currentStock" to safeStock,
            "managementAlertSent" to shouldFlagAlert,
            "alertNote" to note,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(isNowLow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Automatically deducts standard room preparation kit (2 soap, 2 shampoo, 3 towels)
     * when a room task is marked completed, triggering low-stock alerts in Firestore if any crosses threshold.
     */
    suspend fun consumeStandardRoomKitOnCompletion(userId: String = requireUserId()): List<String> {
        val triggeredLowStockItems = mutableListOf<String>()
        val items = getConsumablesOnce(userId).getOrNull().orEmpty()
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

    suspend fun sendHousekeeperAlertForRoom(
        taskId: String,
        qualityScore: Int,
        alertMessage: String,
        iotStatusLabel: String,
        iotSeverity: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("room_tasks").document(taskId)
        val updates = mapOf(
            "status" to "NEEDS_FIX",
            "priority" to "HIGH",
            "qualityScore" to qualityScore.coerceIn(0, 100),
            "alertActive" to true,
            "alertMessage" to alertMessage.take(500),
            "iotStatusLabel" to iotStatusLabel.take(150),
            "iotSeverity" to iotSeverity,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markRoomCompleted(taskId: String, finalScore: Int = 98): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("room_tasks").document(taskId)
        val updates = mapOf(
            "status" to "COMPLETED",
            "alertActive" to false,
            "alertMessage" to "",
            "iotStatusLabel" to "IoT: مكتمل ومعتمد",
            "iotSeverity" to "STABLE",
            "qualityScore" to finalScore.coerceIn(0, 100),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

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
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inspections").document()
        val payload = mapOf(
            "userId" to uid,
            "roomNumber" to roomNumber.take(20),
            "roomTitle" to roomTitle.take(120),
            "qualityScore" to qualityScore.coerceIn(0, 100),
            "issuesCount" to issuesCount.coerceIn(0, 50),
            "durationSeconds" to durationSeconds.coerceIn(1, 7200),
            "beddingIssues" to beddingIssues.coerceIn(0, 50),
            "towelIssues" to towelIssues.coerceIn(0, 50),
            "surfaceIssues" to surfaceIssues.coerceIn(0, 50),
            "maintenanceIssues" to maintenanceIssues.coerceIn(0, 50),
            "summaryText" to summaryText.ifBlank { "تم إتمام الفحص البصري بنجاح" }.take(600),
            "notifiedHousekeeper" to notifiedHousekeeper,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialWorkspaceIfEmpty(userId: String = requireUserId()) {
        val existingRooms = getRoomTasksOnce(userId).getOrNull()
        if (existingRooms != null && existingRooms.isEmpty()) {
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

        // Seed initial Consumables Inventory independently so existing users also get starter consumables
        val existingConsumables = getConsumablesOnce(userId).getOrNull()
        if (existingConsumables != null && existingConsumables.isEmpty()) {
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
}

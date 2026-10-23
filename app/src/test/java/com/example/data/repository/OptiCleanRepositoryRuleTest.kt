package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OptiCleanRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveRoomTasks_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = OptiCleanRepository(firestore)
        val uniqueDocId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createRoomTask(
                roomNumber = "304",
                roomTitle = "غرفة الملكية #304",
                floorNumber = 3,
                priority = "HIGH",
                subtitleNote = "فحص جودة سريع",
                explicitDocId = uniqueDocId
            )
        }
        assertTrue("Expected room task creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRoomTasks(uid).first { list -> list.any { it.id == uniqueDocId } }
        }
        assertTrue(emitted.any { it.id == uniqueDocId && it.roomNumber == "304" })
    }

    @Test
    fun consumablesInventory_stockUpdateAndLowStockManagementAlert_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = OptiCleanRepository(firestore)
        val uniqueItemId = "cons_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        // Create healthy stock item (30 stock, threshold 20)
        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConsumableItem(
                itemName = "شامبو أرغان فندقي",
                category = "SHAMPOO",
                currentStock = 30,
                minThreshold = 20,
                maxCapacity = 100,
                unitLabel = "عبوة",
                storageLocation = "مخزن الدور 3",
                explicitDocId = uniqueItemId
            )
        }
        assertTrue("Expected consumable creation to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)

        val initialItem = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConsumables(uid)
                .first { list -> list.any { it.id == uniqueItemId } }
                .first { it.id == uniqueItemId }
        }
        assertFalse(initialItem.isLowStock)
        assertFalse(initialItem.managementAlertSent)

        // Deduct below threshold (to 15 <= 20) -> triggers management alert automatically
        val deductRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateConsumableStock(initialItem, newStock = 15)
        }
        assertTrue("Expected stock deduction to succeed", deductRes.isSuccess)
        assertTrue("Expected low-stock alert flag true", deductRes.getOrDefault(false))

        val updatedItem = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConsumables(uid)
                .first { list -> list.any { it.id == uniqueItemId && it.currentStock == 15L } }
                .first { it.id == uniqueItemId }
        }
        assertTrue(updatedItem.isLowStock)
        assertTrue(updatedItem.managementAlertSent)
        assertTrue(updatedItem.alertNote.isNotBlank())
    }

    @Test
    fun sendHousekeeperAlertAndRecordInspection_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = OptiCleanRepository(firestore)
        val uniqueDocId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createRoomTask(
                roomNumber = "304",
                roomTitle = "غرفة الملكية #304",
                floorNumber = 3,
                priority = "HIGH",
                subtitleNote = "تسجيل خروج",
                explicitDocId = uniqueDocId
            ).getOrThrow()
        }

        val alertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendHousekeeperAlertForRoom(
                taskId = uniqueDocId,
                qualityScore = 82,
                alertMessage = "تعديل الوسادة وتزويد المناشف",
                iotStatusLabel = "IoT: نقص مناشف",
                iotSeverity = "WARNING"
            )
        }
        assertTrue("Expected alert update to succeed", alertResult.isSuccess)

        val inspResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordInspectionLog(
                roomNumber = "304",
                roomTitle = "غرفة الملكية #304",
                qualityScore = 82,
                issuesCount = 2,
                durationSeconds = 95,
                beddingIssues = 1,
                towelIssues = 1,
                surfaceIssues = 0,
                maintenanceIssues = 0,
                summaryText = "وسادة غير متناسقة ونقص مناشف",
                notifiedHousekeeper = true
            )
        }
        assertTrue("Expected inspection log creation to succeed", inspResult.isSuccess)

        val inspections = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInspections(uid).first { it.isNotEmpty() }
        }
        assertTrue(inspections.isNotEmpty())
    }

    @Test
    fun getRoomTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = OptiCleanRepository(firestore)
        val uniqueDocId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createRoomTask(
                roomNumber = "501",
                roomTitle = "جناح ملكي #501",
                floorNumber = 5,
                priority = "HIGH",
                subtitleNote = "خاص",
                explicitDocId = uniqueDocId
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = OptiCleanRepository(firestore)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getRoomTaskById(taskId = uniqueDocId, targetOwnerId = aliceUid)
        }
        assertTrue("Expected cross-user read to fail", crossRead.isFailure)
        val fsEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeConsumables_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = OptiCleanRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeConsumables(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }

        assertNotNull("Expected unauthenticated observeConsumables to throw", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@opticlean.test"
        const val BOB_EMAIL = "bob@opticlean.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}

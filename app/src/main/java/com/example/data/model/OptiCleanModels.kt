package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
data class RoomTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomNumber: String = "304",
    val roomTitle: String = "",
    val floorNumber: Long = 1L,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val status: String = "PENDING", // PENDING, NEEDS_FIX, COMPLETED
    val subtitleNote: String = "",
    val targetMinutes: Long = 15L,
    val iotStatusLabel: String = "",
    val iotSeverity: String = "STABLE", // CRITICAL, WARNING, STABLE
    val qualityScore: Long = 95L,
    val assignedStaff: String = "",
    val alertActive: Boolean = false,
    val alertMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InspectionLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomNumber: String = "",
    val roomTitle: String = "",
    val qualityScore: Long = 0L,
    val issuesCount: Long = 0L,
    val durationSeconds: Long = 90L,
    val beddingIssues: Long = 0L,
    val towelIssues: Long = 0L,
    val surfaceIssues: Long = 0L,
    val maintenanceIssues: Long = 0L,
    val summaryText: String = "",
    val notifiedHousekeeper: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsumableItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemName: String = "",
    val category: String = "SOAP", // SOAP, SHAMPOO, TOWELS, AMENITIES
    val currentStock: Long = 0L,
    val minThreshold: Long = 20L,
    val maxCapacity: Long = 100L,
    val unitLabel: String = "قطعة",
    val storageLocation: String = "المستودع الرئيسي",
    val managementAlertSent: Boolean = false,
    val alertNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean
        get() = currentStock <= minThreshold
}

@Serializable
data class AiDetectionItem(
    val id: String = "",
    val title: String = "",
    val detail: String = "",
    val category: String = "BEDDING", // BEDDING, TOWELS, SURFACE, MAINTENANCE, PASSED
    val severity: String = "WARNING", // ERROR, WARNING, SUCCESS
    val sourceBadge: String = "بصري", // بصري, IoT, مكتمل
    val topPercent: Float = 0.25f,
    val rightPercent: Float = 0.20f,
    val widthPercent: Float = 0.32f,
    val heightPercent: Float = 0.26f
)

@Serializable
data class AiRoomScanReport(
    val qualityScore: Int = 88,
    val summaryText: String = "",
    val detections: List<AiDetectionItem> = emptyList(),
    val isLiveGeminiAnalysis: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

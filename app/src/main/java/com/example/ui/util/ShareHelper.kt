package com.example.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.AiRoomScanReport
import com.example.data.model.ConsumableItem
import com.example.data.model.InspectionLog
import com.example.data.model.RoomTask
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Helper utility for native Android phone sharing (Sharesheet) supporting app invitation links,
 * clipboard link copying, Arabic operational text reports, and attached room inspection snapshots.
 */
object ShareHelper {

    private const val TAG = "ShareHelper"

    fun shareAppInvitation(context: Context) {
        val appUrl = context.getString(R.string.shared_app_url)
        val chooserTitle = context.getString(R.string.share_app_chooser_title)
        val subject = "تطبيق OptiClean AI — منظومة الإشراف الفندقي الذكي"
        val body = buildAppInviteText(appUrl)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching {
            context.startActivity(chooserIntent)
        }.onFailure { err ->
            Log.w(TAG, "Unable to open Android Share Sheet for app invite", err)
        }
    }

    fun copyAppLinkToClipboard(context: Context): Boolean {
        return runCatching {
            val appUrl = context.getString(R.string.shared_app_url)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("OptiClean AI", appUrl))
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun buildAppInviteText(appUrl: String): String {
        return buildString {
            appendLine("📲 جرّب تطبيق OptiClean AI — منظومة الإشراف الفندقي الذكي")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("منصة متكاملة للفحص البصري للغرف بالذكاء الاصطناعي، متابعة مهام النظافة اليومية، وجرد المستهلكات الفندقية لحظياً.")
            appendLine()
            appendLine("🔗 افتح وجرّب التطبيق مباشرة عبر الرابط:")
            append(appUrl)
        }
    }

    fun shareTextReport(
        context: Context,
        subject: String,
        body: String
    ) {
        val chooserTitle = context.getString(R.string.share_chooser_title)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching {
            context.startActivity(chooserIntent)
        }.onFailure { err ->
            Log.w(TAG, "Unable to open Android Share Sheet", err)
        }
    }

    fun shareScanReportWithOptionalBitmap(
        context: Context,
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        report: AiRoomScanReport,
        bitmap: Bitmap?
    ) {
        val subject = "تقرير فحص الذكاء الاصطناعي - $roomTitle"
        val body = buildAiScanReportText(
            roomNumber = roomNumber,
            roomTitle = roomTitle,
            floorNumber = floorNumber,
            report = report
        )

        if (bitmap != null) {
            val sharedWithImage = runCatching {
                val reportsDir = File(context.cacheDir, "shared_reports").apply { mkdirs() }
                val safeRoom = roomNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val imageFile = File(reportsDir, "opticlean_room_${safeRoom}.jpg")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                val chooserTitle = context.getString(R.string.share_chooser_title)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    clipData = ClipData.newRawUri(subject, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(chooserIntent)
                true
            }.getOrDefault(false)

            if (sharedWithImage) return
        }

        shareTextReport(context = context, subject = subject, body = body)
    }

    fun buildWorkspaceOverviewText(
        rooms: List<RoomTask>,
        consumables: List<ConsumableItem>,
        inspections: List<InspectionLog>
    ): String {
        val completedCount = rooms.count { it.status == "COMPLETED" }
        val urgentCount = rooms.count {
            it.status != "COMPLETED" && (it.priority == "HIGH" || it.status == "NEEDS_FIX" || it.alertActive)
        }
        val lowStockItems = consumables.filter { it.isLowStock }
        val scores = inspections.map { it.qualityScore.toDouble() } + rooms.map { it.qualityScore.toDouble() }
        val avgQuality = if (scores.isEmpty()) 96.8 else scores.average()

        return buildString {
            appendLine("🏨 ملخص التشغيل الفوري — OptiClean AI")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("• إجمالي الغرف المسجلة: ${rooms.size} غرف")
            appendLine("• الغرف الجاهزة والمعتمدة: $completedCount")
            appendLine("• المهام العاجلة / قيد التعديل: $urgentCount")
            appendLine(String.format(Locale.US, "• متوسط جودة النظافة العام: %.1f%%", avgQuality))
            appendLine("• أصناف المخزون المنخفضة: ${lowStockItems.size}")
            if (lowStockItems.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ تنبيهات نقص المستهلكات:")
                lowStockItems.take(4).forEach { item ->
                    appendLine("  - ${item.itemName}: المتبقي ${item.currentStock} ${item.unitLabel} (${item.storageLocation})")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("صادر عبر منظومة الإشراف الفندقي الذكي OptiClean AI")
        }
    }

    fun buildAiScanReportText(
        roomNumber: String,
        roomTitle: String,
        floorNumber: Int,
        report: AiRoomScanReport
    ): String {
        val actionableCount = report.detections.count { it.severity == "ERROR" || it.severity == "WARNING" }
        val statusLine = if (actionableCount == 0) {
            "✅ معتمدة وجاهزة لاستقبال النزيل"
        } else {
            "⚠️ تتطلب معالجة ($actionableCount ملاحظات)"
        }

        return buildString {
            appendLine("🔍 تقرير الفحص البصري الذكي — OptiClean AI")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("• الغرفة: $roomTitle (رقم #$roomNumber - الدور $floorNumber)")
            appendLine("• تقييم الجودة: ${report.qualityScore}%")
            appendLine("• القرار التشغيلي: $statusLine")
            if (report.summaryText.isNotBlank()) {
                appendLine("• الملخص: ${report.summaryText}")
            }
            if (report.detections.isNotEmpty()) {
                appendLine()
                appendLine("📋 تفاصيل الرصد البصري و IoT:")
                report.detections.forEach { det ->
                    val mark = when (det.severity) {
                        "ERROR" -> "🔴"
                        "WARNING" -> "🟠"
                        else -> "🟢"
                    }
                    appendLine("  $mark [${det.sourceBadge}] ${det.title}: ${det.detail}")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("تم الإنشاء بواسطة OptiClean AI")
        }
    }

    fun buildSingleRoomTaskText(task: RoomTask): String {
        val statusAr = when (task.status) {
            "COMPLETED" -> "مكتملة وجاهزة للنزيل ✅"
            "NEEDS_FIX" -> "تحتاج تعديل فوري ⚠️"
            else -> "قيد التنفيذ ⏳"
        }
        val priorityAr = when (task.priority) {
            "HIGH" -> "قصوى وعاجلة"
            "MEDIUM" -> "متوسطة"
            else -> "اعتيادية"
        }
        return buildString {
            appendLine("🧹 بطاقة مهمة إشراف فندقي — OptiClean AI")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("• الغرفة: ${task.roomTitle} (الدور ${task.floorNumber})")
            appendLine("• الحالة: $statusAr")
            appendLine("• الأولوية: $priorityAr")
            appendLine("• المسؤول: ${task.assignedStaff}")
            appendLine("• الزمن المستهدف: ${task.targetMinutes} دقيقة")
            appendLine("• مستشعرات الغرفة: ${task.iotStatusLabel}")
            appendLine("• آخر تقييم جودة: ${task.qualityScore}%")
            if (task.subtitleNote.isNotBlank()) {
                appendLine("• ملاحظة التشغيل: ${task.subtitleNote}")
            }
            if (task.alertActive && task.alertMessage.isNotBlank()) {
                appendLine("• تنبيه المشرف: ${task.alertMessage}")
            }
        }.trimEnd()
    }

    fun buildAllRoomTasksText(rooms: List<RoomTask>): String {
        return buildString {
            appendLine("📋 جدول مهام النظافة اليومية — OptiClean AI")
            appendLine("إجمالي الغرف: ${rooms.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            rooms.forEachIndexed { index, task ->
                val statusIcon = when (task.status) {
                    "COMPLETED" -> "✅"
                    "NEEDS_FIX" -> "🔴"
                    else -> "🟠"
                }
                appendLine("${index + 1}. $statusIcon ${task.roomTitle} (الدور ${task.floorNumber}) — ${task.assignedStaff} [${task.iotStatusLabel}]")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("منظومة الإشراف الفندقي الذكي OptiClean AI")
        }
    }

    fun buildInventorySupplyOrderText(
        items: List<ConsumableItem>,
        onlyLowStock: Boolean
    ): String {
        val targetList = if (onlyLowStock) items.filter { it.isLowStock } else items
        val header = if (onlyLowStock) {
            "📦 طلب توريد عاجل للمواد المستهلكة — OptiClean AI"
        } else {
            "📦 تقرير جرد المواد المستهلكة — OptiClean AI"
        }

        return buildString {
            appendLine(header)
            appendLine("عدد الأصناف المشمولة: ${targetList.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            targetList.forEachIndexed { idx, item ->
                val needed = (item.maxCapacity - item.currentStock).coerceAtLeast(0L)
                val badge = if (item.isLowStock) "⚠️ نقص" else "✅ متوفر"
                appendLine("${idx + 1}. ${item.itemName} [$badge]")
                appendLine("   • الرصيد الحالي: ${item.currentStock} ${item.unitLabel} (الحد الأدنى: ${item.minThreshold})")
                appendLine("   • الموقع: ${item.storageLocation} | الكمية المقترحة للتوريد: $needed ${item.unitLabel}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("قسم الإشراف الداخلي والمستودعات — OptiClean AI")
        }
    }

    fun buildDashboardKpiReportText(
        rooms: List<RoomTask>,
        inspections: List<InspectionLog>,
        consumables: List<ConsumableItem>,
        avgQuality: Double,
        avgMinutes: Double
    ): String {
        val completedRooms = rooms.count { it.status == "COMPLETED" }
        val lowStockCount = consumables.count { it.isLowStock }
        return buildString {
            appendLine("📊 تقرير مؤشرات التشغيل والجودة اليومي — OptiClean AI")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine(String.format(Locale.US, "• معدل جودة النظافة العام: %.1f%%", avgQuality))
            appendLine(String.format(Locale.US, "• متوسط زمن الفحص الذكي: %.1f دقيقة/غرفة", avgMinutes))
            appendLine("• جاهزية الغرف: $completedRooms مكتملة من أصل ${rooms.size}")
            appendLine("• عمليات التدقيق المسجلة: ${inspections.size}")
            appendLine("• تنبيهات نقص المخزون النشطة: $lowStockCount أصناف")
            if (inspections.isNotEmpty()) {
                appendLine()
                appendLine("🕒 أحدث عمليات التدقيق:")
                inspections.take(3).forEach { log ->
                    appendLine("  - ${log.roomTitle}: ${log.qualityScore}% (${log.summaryText})")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("صادر عبر لوحة تحليلات OptiClean AI")
        }
    }
}

package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiDetectionItem
import com.example.data.model.AiRoomScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: GeminiContent? = null
)

@Serializable
private data class StructuredScanPayload(
    val qualityScore: Int = 86,
    val summaryText: String = "",
    val detections: List<StructuredDetectionDto> = emptyList()
)

@Serializable
private data class StructuredDetectionDto(
    val title: String = "",
    val detail: String = "",
    val category: String = "BEDDING",
    val severity: String = "WARNING",
    val sourceBadge: String = "بصري",
    val topPercent: Float = 0.22f,
    val rightPercent: Float = 0.14f,
    val widthPercent: Float = 0.34f,
    val heightPercent: Float = 0.26f
)

interface GeminiVisionApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeRoomImage(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiRoomScannerService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val api: GeminiVisionApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiVisionApi::class.java)
    }

    suspend fun scanRoom(
        bitmap: Bitmap?,
        roomNumber: String,
        roomTitle: String,
        iotContextLabel: String
    ): AiRoomScanReport = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() &&
            apiKey != "MY_GEMINI_API_KEY" &&
            !apiKey.startsWith("YOUR_")

        if (bitmap != null && hasValidApiKey) {
            try {
                val base64Jpeg = bitmapToBase64(bitmap)
                val schema = buildScanSchema()
                val prompt = """
                    قم بفحص صورة الغرفة الفندقية ($roomTitle - رقم #$roomNumber) بدقة مشرف جودة ضيافة فاخرة (5 نجوم).
                    معلومات حساسات IoT الحالية للغرفة: "$iotContextLabel".
                    اكتشف 3 عناصر رئيسية في الصورة وحدد إحداثياتها النسبية التقريبية (بين 0.05 و 0.68 لكل من topPercent و rightPercent، وبين 0.22 و 0.40 للعرض والارتفاع):
                    1. ملاحظات ترتيب المفروشات أو الوسائد (category = BEDDING).
                    2. حالة المناشف أو المستلزمات بالتكامل مع حساس IoT (category = TOWELS).
                    3. نظافة الأسطح أو الميني بار أو جاهزية الضيافة (category = SURFACE أو PASSED).
                    اكتب جميع النصوص باللغة العربية الفصحى المختصرة والواضحة:
                    - severity يجب أن تكون واحدة من: ERROR (تتطلب تعديل فوري)، WARNING (تنبيه متوسط/IoT)، SUCCESS (مكتمل ومطابق).
                    - sourceBadge يجب أن تكون: "بصري" أو "IoT" أو "مكتمل".
                    - qualityScore رقم صحيح من 60 إلى 100 يعكس جودة تجهيز الغرفة.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = prompt),
                                GeminiPart(
                                    inlineData = InlineData(
                                        mimeType = "image/jpeg",
                                        data = base64Jpeg
                                    )
                                )
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = schema,
                        temperature = 0.25f
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "أنت محرك الذكاء الاصطناعي البصري لمنظومة OptiClean AI للإشراف الفندقي الذكي. أعد دائماً JSON مطابق للمخطط المطلوب بدقة."
                            )
                        )
                    )
                )

                val response = api.analyzeRoomImage(apiKey, request)
                val rawJson = response.candidates.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text

                if (!rawJson.isNullOrBlank()) {
                    val parsed = json.decodeFromString<StructuredScanPayload>(rawJson)
                    if (parsed.detections.isNotEmpty()) {
                        return@withContext AiRoomScanReport(
                            qualityScore = parsed.qualityScore.coerceIn(50, 100),
                            summaryText = parsed.summaryText.ifBlank { "اكتمل التدقيق البصري الذكي عبر Gemini Vision" },
                            detections = parsed.detections.mapIndexed { idx, dto ->
                                AiDetectionItem(
                                    id = "det_$idx",
                                    title = dto.title,
                                    detail = dto.detail,
                                    category = dto.category.uppercase(),
                                    severity = dto.severity.uppercase(),
                                    sourceBadge = dto.sourceBadge,
                                    topPercent = dto.topPercent.coerceIn(0.08f, 0.65f),
                                    rightPercent = dto.rightPercent.coerceIn(0.06f, 0.62f),
                                    widthPercent = dto.widthPercent.coerceIn(0.22f, 0.42f),
                                    heightPercent = dto.heightPercent.coerceIn(0.18f, 0.34f)
                                )
                            },
                            isLiveGeminiAnalysis = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("GeminiRoomScanner", "Live Gemini Vision fallback triggered: ${e.message}")
            }
        }

        // Smooth visual scan pacing for realistic progressive bounding-box reveal
        delay(1500L)
        buildCalibratedRoomPreset(roomNumber, iotContextLabel)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val ratio = minOf(
            maxDim.toFloat() / bitmap.width.coerceAtLeast(1),
            maxDim.toFloat() / bitmap.height.coerceAtLeast(1),
            1f
        )
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildScanSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("qualityScore") { put("type", "INTEGER") }
            putJsonObject("summaryText") { put("type", "STRING") }
            putJsonObject("detections") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("title") { put("type", "STRING") }
                        putJsonObject("detail") { put("type", "STRING") }
                        putJsonObject("category") { put("type", "STRING") }
                        putJsonObject("severity") { put("type", "STRING") }
                        putJsonObject("sourceBadge") { put("type", "STRING") }
                        putJsonObject("topPercent") { put("type", "NUMBER") }
                        putJsonObject("rightPercent") { put("type", "NUMBER") }
                        putJsonObject("widthPercent") { put("type", "NUMBER") }
                        putJsonObject("heightPercent") { put("type", "NUMBER") }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("title"))
                        add(kotlinx.serialization.json.JsonPrimitive("detail"))
                        add(kotlinx.serialization.json.JsonPrimitive("category"))
                        add(kotlinx.serialization.json.JsonPrimitive("severity"))
                        add(kotlinx.serialization.json.JsonPrimitive("sourceBadge"))
                        add(kotlinx.serialization.json.JsonPrimitive("topPercent"))
                        add(kotlinx.serialization.json.JsonPrimitive("rightPercent"))
                        add(kotlinx.serialization.json.JsonPrimitive("widthPercent"))
                        add(kotlinx.serialization.json.JsonPrimitive("heightPercent"))
                    }
                }
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("qualityScore"))
            add(kotlinx.serialization.json.JsonPrimitive("summaryText"))
            add(kotlinx.serialization.json.JsonPrimitive("detections"))
        }
    }

    private fun buildCalibratedRoomPreset(roomNumber: String, iotStatus: String): AiRoomScanReport {
        return when (roomNumber) {
            "210" -> AiRoomScanReport(
                qualityScore = 94,
                summaryText = "الجناح العائلي #210 بحالة ممتازة مع ملاحظة بسيطة على طاولة الضيافة",
                detections = listOf(
                    AiDetectionItem(
                        id = "det_bed_210",
                        title = "اصطفاف الأسرّة المزدوجة متطابق",
                        detail = "زوايا الشراشف والمفارش مطابقة لمعايير الضيافة 100%",
                        category = "PASSED",
                        severity = "SUCCESS",
                        sourceBadge = "مكتمل",
                        topPercent = 0.24f,
                        rightPercent = 0.14f,
                        widthPercent = 0.38f,
                        heightPercent = 0.28f
                    ),
                    AiDetectionItem(
                        id = "det_coffee_210",
                        title = "ترتيب ركن القهوة والضيافة",
                        detail = "فنجان ضيافة يحتاج محاذاة بجانب ماكينة الإسبريسو",
                        category = "SURFACE",
                        severity = "WARNING",
                        sourceBadge = "بصري",
                        topPercent = 0.50f,
                        rightPercent = 0.56f,
                        widthPercent = 0.30f,
                        heightPercent = 0.26f
                    ),
                    AiDetectionItem(
                        id = "det_iot_210",
                        title = "استقرار حساسات المناشف والتكييف",
                        detail = "جميع الأطقم مكتملة وحرارة الغرفة 22°C مثالية",
                        category = "PASSED",
                        severity = "SUCCESS",
                        sourceBadge = "IoT",
                        topPercent = 0.18f,
                        rightPercent = 0.58f,
                        widthPercent = 0.28f,
                        heightPercent = 0.22f
                    )
                ),
                isLiveGeminiAnalysis = false
            )
            "105" -> AiRoomScanReport(
                qualityScore = 98,
                summaryText = "غرفة قياسية #105 معتمدة بالكامل وجاهزة لاستقبال النزيل فوراً",
                detections = listOf(
                    AiDetectionItem(
                        id = "det_bed_105",
                        title = "ترتيب السرير والوسائد مثالي",
                        detail = "تناسق هندسي كامل بدون تجاعيد",
                        category = "PASSED",
                        severity = "SUCCESS",
                        sourceBadge = "مكتمل",
                        topPercent = 0.22f,
                        rightPercent = 0.16f,
                        widthPercent = 0.42f,
                        heightPercent = 0.30f
                    ),
                    AiDetectionItem(
                        id = "det_bar_105",
                        title = "الميني بار والتعطير مكتمل",
                        detail = "المشروبات وأطقم الضيافة مكتملة 100%",
                        category = "PASSED",
                        severity = "SUCCESS",
                        sourceBadge = "IoT",
                        topPercent = 0.54f,
                        rightPercent = 0.12f,
                        widthPercent = 0.30f,
                        heightPercent = 0.26f
                    )
                ),
                isLiveGeminiAnalysis = false
            )
            else -> AiRoomScanReport(
                qualityScore = 84,
                summaryText = "رصد ملاحظتين تتطلبان تدخل عامل النظافة في الغرفة #$roomNumber",
                detections = listOf(
                    AiDetectionItem(
                        id = "det_pillow_304",
                        title = "ترتيب الوسائد غير متناسق (زاوية 45°)",
                        detail = "الوسادة الجانبية تحتاج تعديل الزاوية ومحاذاة المسند",
                        category = "BEDDING",
                        severity = "ERROR",
                        sourceBadge = "بصري",
                        topPercent = 0.20f,
                        rightPercent = 0.12f,
                        widthPercent = 0.38f,
                        heightPercent = 0.26f
                    ),
                    AiDetectionItem(
                        id = "det_towel_304",
                        title = "نقص في طقم المناشف (1/3)",
                        detail = "المتبقي منشفة واحدة بدلاً من 3 (${iotStatus.ifBlank { "حساس IoT" }})",
                        category = "TOWELS",
                        severity = "WARNING",
                        sourceBadge = "IoT",
                        topPercent = 0.53f,
                        rightPercent = 0.56f,
                        widthPercent = 0.32f,
                        heightPercent = 0.28f
                    ),
                    AiDetectionItem(
                        id = "det_bar_304",
                        title = "جاهزية الميني بار والتعطير",
                        detail = "المشروبات والمعطر مكتملة ومطابقة 100%",
                        category = "PASSED",
                        severity = "SUCCESS",
                        sourceBadge = "مكتمل",
                        topPercent = 0.50f,
                        rightPercent = 0.08f,
                        widthPercent = 0.30f,
                        heightPercent = 0.30f
                    )
                ),
                isLiveGeminiAnalysis = false
            )
        }
    }
}

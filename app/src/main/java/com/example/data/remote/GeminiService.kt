package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.Activity
import com.example.domain.model.EnrichedDeal
import com.example.domain.model.Task
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DealCoachResult(
    val score: Int,
    val risk: String,
    val nextAction: String,
    val reasoning: String,
    val tactics: List<String>
)

data class ContactIntelResult(
    val title: String,
    val companySize: String,
    val annualRevenue: String,
    val buyingSignals: String,
    val talkingPoints: List<String>
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun analyzeDeal(
        enriched: EnrichedDeal,
        activities: List<Activity>,
        tasks: List<Task>
    ): DealCoachResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholder. Running simulation mode.")
            return@withContext simulateDealAnalysis(enriched)
        }

        val prompt = """
            You are an expert sales intelligence coach. Analyze this B2B Sales Deal and provide strategic tactics.
            
            Deal Name: ${enriched.deal.name}
            Company: ${enriched.deal.company}
            Stage: ${enriched.deal.stage}
            Priority: ${enriched.deal.priority}
            Value: ${enriched.deal.currency}${enriched.deal.dealValue}
            Stale Days: ${enriched.daysStale}
            Momentum (weighted activities): ${enriched.momentum}
            Active Tasks: ${tasks.size} (Has Active Task: ${enriched.hasActiveTask})
            Activities: ${activities.joinToString { "[${it.type}] ${it.text}" }}
            Custom Fields/Competitors: ${enriched.deal.competitors} | Notes: ${enriched.deal.notes}
            
            Respond ONLY with a JSON object containing these keys:
            - "score" (Int, 0 to 100, representing the probability of successfully closing this deal)
            - "risk" (String, "Low", "Medium", or "High" risk matching the assessment)
            - "nextAction" (String, concrete best next step for the sales representative, max 15 words)
            - "reasoning" (String, short summary analysis of why they have this risk or score, max 40 words)
            - "tactics" (List of Strings, 3 bullet point specific tactical plays for this client)
            
            Do not include any Markdown like ```json. Return raw JSON string.
        """.trimIndent()

        try {
            val responseText = makeApiCall(apiKey, prompt)
            val json = JSONObject(responseText)
            val score = json.optInt("score", 70)
            val risk = json.optString("risk", "Medium")
            val nextAction = json.optString("nextAction", "Schedule follow-up call with target stakeholders.")
            val reasoning = json.optString("reasoning", "The deal is showing standard progression but requires immediate stakeholder engagement.")
            val tacticsArray = json.optJSONArray("tactics")
            val tactics = mutableListOf<String>()
            if (tacticsArray != null) {
                for (i in 0 until tacticsArray.length()) {
                    tactics.add(tacticsArray.getString(i))
                }
            } else {
                tactics.addAll(listOf("Initiate structured presentation", "Confirm authority map", "Outline pricing options"))
            }

            DealCoachResult(score, risk, nextAction, reasoning, tactics)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini: ${e.message}", e)
            simulateDealAnalysis(enriched)
        }
    }

    suspend fun enrichContact(name: String, company: String): ContactIntelResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholder. Running simulation mode for contact.")
            return@withContext simulateContactIntel(name, company)
        }

        val prompt = """
            You are a B2B sales intelligence agent. Search your database resources and synthesize actionable sales intelligence about this target contact:
            
            Contact Name: $name
            Company: $company
            
            Respond ONLY with a JSON object containing these keys:
            - "title" (String, most likely corporate designation based on context, e.g., Head of Procurement, CTO)
            - "companySize" (String, estimated number of employees, e.g., "500-1000", "Enterprise")
            - "annualRevenue" (String, estimated annual revenue, e.g., "$50M", "$1.2B")
            - "buyingSignals" (String, synthesis of latest industry signals, expansion plans, tech stack details, max 25 words)
            - "talkingPoints" (List of Strings, 3 personalized conversation icebreakers or topics of high interest to secure an appointment)
            
            Do not include any Markdown like ```json. Return raw JSON string.
        """.trimIndent()

        try {
            val responseText = makeApiCall(apiKey, prompt)
            val json = JSONObject(responseText)
            val title = json.optString("title", "Director of Innovation")
            val companySize = json.optString("companySize", "Medium (250-500)")
            val annualRevenue = json.optString("annualRevenue", "$75M")
            val buyingSignals = json.optString("buyingSignals", "Recent expansion in regional retail distribution networks and increased cloud infrastructure spend.")
            val talkingPointsArray = json.optJSONArray("talkingPoints")
            val talkingPoints = mutableListOf<String>()
            if (talkingPointsArray != null) {
                for (i in 0 until talkingPointsArray.length()) {
                    talkingPoints.add(talkingPointsArray.getString(i))
                }
            } else {
                talkingPoints.addAll(listOf("Discuss recent regional logistics scaling", "Integrate automated supply systems", "Optimize localized client tracking"))
            }

            ContactIntelResult(title, companySize, annualRevenue, buyingSignals, talkingPoints)
        } catch (e: Exception) {
            Log.e(TAG, "Error enriching contact with Gemini: ${e.message}", e)
            simulateContactIntel(name, company)
        }
    }

    private fun makeApiCall(apiKey: String, prompt: String): String {
        val url = "$BASE_URL?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Unsuccessful API call: Code ${response.code}, Message: ${response.message}")
            }
            val bodyString = response.body?.string() ?: throw Exception("Empty response body")
            val rootObj = JSONObject(bodyString)
            val candidates = rootObj.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)
            return firstPart.getString("text")
        }
    }

    private fun simulateDealAnalysis(enriched: EnrichedDeal): DealCoachResult {
        val baseScore = when (enriched.deal.stage) {
            com.example.domain.model.Stage.LEAD -> 15
            com.example.domain.model.Stage.CONTACTED -> 30
            com.example.domain.model.Stage.DEMO -> 50
            com.example.domain.model.Stage.PROPOSAL -> 75
            com.example.domain.model.Stage.WON -> 100
            com.example.domain.model.Stage.LOST -> 0
        }
        val penalty = (enriched.daysStale * 1.5).toInt()
        val score = (baseScore - penalty).coerceIn(5, 98)
        val risk = when {
            enriched.daysStale > 25 -> "High"
            enriched.daysStale > 10 -> "Medium"
            else -> "Low"
        }

        return DealCoachResult(
            score = score,
            risk = risk,
            nextAction = "Schedule a physical check-in or demo alignment with stakeholders before month-end.",
            reasoning = "Staleness is currently ${enriched.daysStale} days. Active action plans must be aligned immediately to rescue momentum.",
            tactics = listOf(
                "Offer a structured proof-of-concept trial to overcome budget constraints.",
                "Map executive decision-makers & identify hidden technical coaches.",
                "Draft custom enterprise agreement aligning seasonal procurement rules."
            )
        )
    }

    private fun simulateContactIntel(name: String, company: String): ContactIntelResult {
        return ContactIntelResult(
            title = "Vice President of Enterprise Partnerships",
            companySize = "2,500 - 5,000 (Mid-Market Enterprise)",
            annualRevenue = "$320 Million USD",
            buyingSignals = "Expanding domestic footprint, hiring heavily for customer engagement teams, migrating to multi-cloud.",
            talkingPoints = listOf(
                "Congratulate on their recent operational expansion.",
                "Mention their open roles looking for pipeline analytics tools.",
                "Introduce localized scaling architectures to simplify cross-department tracking."
            )
        )
    }
}

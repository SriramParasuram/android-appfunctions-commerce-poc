package com.example.myapplication.network

import android.util.Log
import androidx.appfunctions.AppFunctionAppUnknownException
import com.example.myapplication.appfunctions.DeviceResult
import com.example.myapplication.appfunctions.DeviceSelectionRequest
import com.example.myapplication.appfunctions.DeviceSelectionResult
import com.example.myapplication.appfunctions.DeviceShoppingRequest
import com.example.myapplication.appfunctions.DeviceShoppingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP client that maps AppFunction shopping turns to the FastAPI backend.
 *
 * Emulator → host loopback uses [BASE_URL] (`10.0.2.2`).
 * No Android-side session cache: each call is independent; pass sessionId explicitly.
 */
class CommerceApiClient(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = BASE_URL,
) {
    data class SessionHandoff(
        val sessionId: String,
        val selectedProductId: String,
        val selectedProduct: DeviceResult,
    )

    suspend fun shop(request: DeviceShoppingRequest): DeviceShoppingResult =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/shopping/turn"
            val bodyJson = toBackendRequestJson(request)
            Log.i(TAG, "shopForDevices invoked")
            Log.i(TAG, "sessionId: ${request.sessionId}")
            Log.i(TAG, "preferences delta: ${preferencesDeltaLog(request)}")
            Log.i(TAG, "clearPreferences: ${request.clearPreferences}")
            Log.i(TAG, "calling $url")

            postJson(url, bodyJson) { raw ->
                val parsed = parseShopResponse(raw)
                Log.i(TAG, "backend response sessionId: ${parsed.sessionId}")
                Log.i(TAG, "products returned: ${parsed.products.size}")
                parsed
            }
        }

    suspend fun selectDevice(request: DeviceSelectionRequest): DeviceSelectionResult =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/shopping/select"
            val bodyJson = JSONObject()
                .put("sessionId", request.sessionId)
                .put("productId", request.productId)
                .toString()
            Log.i(TAG, "selectDevice invoked")
            Log.i(TAG, "sessionId: ${request.sessionId}")
            Log.i(TAG, "productId: ${request.productId}")
            Log.i(TAG, "calling $url")

            postJson(url, bodyJson) { raw ->
                val parsed = parseSelectResponse(raw)
                Log.i(TAG, "backend response status: ${parsed.status}")
                Log.i(TAG, "selectedProductId: ${parsed.selectedProductId}")
                parsed
            }
        }

    suspend fun getSessionHandoff(sessionId: String): SessionHandoff =
        getShoppingSession(sessionId)

    /** Restore shopping-session handoff state from the backend (source of truth). */
    suspend fun getShoppingSession(sessionId: String): SessionHandoff =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/shopping/session/${java.net.URLEncoder.encode(sessionId, "UTF-8")}"
            Log.i(TAG, "shopping session lookup")
            Log.i(TAG, "sessionId: $sessionId")
            Log.i(TAG, "calling $url")

            getJson(url) { raw ->
                val parsed = parseHandoffResponse(raw)
                Log.i(TAG, "HANDOFF")
                Log.i(TAG, "sessionId: ${parsed.sessionId}")
                Log.i(TAG, "selectedProductId: ${parsed.selectedProductId}")
                parsed
            }
        }

    /** Fetch the full read-only demo catalogue from the backend. */
    suspend fun getCatalogue(): List<DeviceResult> =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/catalogue/devices"
            Log.i(TAG, "catalogue fetch")
            Log.i(TAG, "calling $url")

            getJson(url) { raw ->
                val devices = parseCatalogueResponse(raw)
                Log.i(TAG, "catalogueCount: ${devices.size}")
                devices
            }
        }

    private fun <T> postJson(url: String, bodyJson: String, parse: (String) -> T): T {
        val httpRequest = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()
        return execute(url, httpRequest, parse)
    }

    private fun <T> getJson(url: String, parse: (String) -> T): T {
        val httpRequest = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()
        return execute(url, httpRequest, parse)
    }

    private fun <T> execute(url: String, httpRequest: Request, parse: (String) -> T): T {
        val response = try {
            client.newCall(httpRequest).execute()
        } catch (io: IOException) {
            Log.e(TAG, "backend unavailable: ${io.message}")
            throw AppFunctionAppUnknownException(
                "Commerce backend unavailable at $url: ${io.message}",
            )
        }

        return response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                Log.e(TAG, "HTTP ${resp.code}: ${raw.take(200)}")
                throw AppFunctionAppUnknownException(
                    "Commerce backend HTTP ${resp.code}: ${raw.take(200)}",
                )
            }
            try {
                parse(raw)
            } catch (parseEx: Exception) {
                Log.e(TAG, "malformed backend response: ${parseEx.message}")
                throw AppFunctionAppUnknownException(
                    "Malformed commerce backend response: ${parseEx.message}",
                )
            }
        }
    }

    companion object {
        private const val TAG = "CommerceApiClient"
        const val BASE_URL = "http://10.0.2.2:8000"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

        fun toBackendRequestJson(request: DeviceShoppingRequest): String {
            val preferences = JSONObject()
            request.minPrice?.let { preferences.put("minPrice", it) }
            request.maxPrice?.let { preferences.put("maxPrice", it) }
            request.operatingSystem?.let { preferences.put("operatingSystem", it) }
            request.brand?.let { preferences.put("brand", it) }
            request.color?.let { preferences.put("color", it) }
            request.storageGb?.let { preferences.put("storageGb", it) }
            request.cameraPriority?.let { preferences.put("cameraPriority", it) }
            request.batteryPriority?.let { preferences.put("batteryPriority", it) }
            request.displayPriority?.let { preferences.put("displayPriority", it) }
            request.isFoldable?.let { preferences.put("isFoldable", it) }

            val root = JSONObject()
            if (request.sessionId != null) {
                root.put("sessionId", request.sessionId)
            } else {
                root.put("sessionId", JSONObject.NULL)
            }
            root.put("preferences", preferences)
            request.clearPreferences?.let { names ->
                val arr = JSONArray()
                names.forEach { arr.put(it) }
                root.put("clearPreferences", arr)
            }
            return root.toString()
        }

        fun parseShopResponse(raw: String): DeviceShoppingResult {
            val json = JSONObject(raw)
            val productsJson = json.optJSONArray("products") ?: JSONArray()
            val products = buildList {
                for (i in 0 until productsJson.length()) {
                    add(parseProduct(productsJson.getJSONObject(i)))
                }
            }
            return DeviceShoppingResult(
                sessionId = json.getString("sessionId"),
                summary = json.getString("summary"),
                products = products,
            )
        }

        fun parseSelectResponse(raw: String): DeviceSelectionResult {
            val json = JSONObject(raw)
            return DeviceSelectionResult(
                sessionId = json.getString("sessionId"),
                status = json.getString("status"),
                selectedProductId = json.getString("selectedProductId"),
                selectedProduct = parseProduct(json.getJSONObject("selectedProduct")),
                summary = json.getString("summary"),
            )
        }

        fun parseHandoffResponse(raw: String): SessionHandoff {
            val json = JSONObject(raw)
            return SessionHandoff(
                sessionId = json.getString("sessionId"),
                selectedProductId = json.getString("selectedProductId"),
                selectedProduct = parseProduct(json.getJSONObject("selectedProduct")),
            )
        }

        fun parseCatalogueResponse(raw: String): List<DeviceResult> {
            val json = JSONObject(raw)
            val devicesJson = json.optJSONArray("devices") ?: JSONArray()
            return buildList {
                for (i in 0 until devicesJson.length()) {
                    add(parseProduct(devicesJson.getJSONObject(i)))
                }
            }
        }

        private fun parseProduct(item: JSONObject): DeviceResult =
            DeviceResult(
                productId = item.getString("productId"),
                brand = item.getString("brand"),
                name = item.getString("name"),
                priceUsd = item.getInt("priceUsd"),
                operatingSystem = item.getString("operatingSystem"),
                availableColors = jsonStringList(item.getJSONArray("availableColors")),
                availableStorageGb = jsonIntArray(item.getJSONArray("availableStorageGb")),
                cameraScore = item.getInt("cameraScore"),
                batteryScore = item.getInt("batteryScore"),
                displayScore = item.getInt("displayScore"),
                isFoldable = item.getBoolean("isFoldable"),
                screenSize = item.getDouble("screenSize"),
            )

        private fun jsonStringList(arr: JSONArray): List<String> =
            buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }

        private fun jsonIntArray(arr: JSONArray): IntArray =
            IntArray(arr.length()) { idx -> arr.getInt(idx) }

        private fun preferencesDeltaLog(request: DeviceShoppingRequest): String {
            val parts = buildList {
                request.minPrice?.let { add("minPrice=$it") }
                request.maxPrice?.let { add("maxPrice=$it") }
                request.operatingSystem?.let { add("operatingSystem=$it") }
                request.brand?.let { add("brand=$it") }
                request.color?.let { add("color=$it") }
                request.storageGb?.let { add("storageGb=$it") }
                request.cameraPriority?.let { add("cameraPriority=$it") }
                request.batteryPriority?.let { add("batteryPriority=$it") }
                request.displayPriority?.let { add("displayPriority=$it") }
                request.isFoldable?.let { add("isFoldable=$it") }
            }
            return if (parts.isEmpty()) "{}" else parts.joinToString(prefix = "{", postfix = "}")
        }
    }
}

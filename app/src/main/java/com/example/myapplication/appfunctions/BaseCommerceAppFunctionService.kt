package com.example.myapplication.appfunctions

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.example.myapplication.HandoffExtras
import com.example.myapplication.MainActivity
import com.example.myapplication.network.CommerceApiClient

/**
 * AppFunction entry point exposing Commerce Agent device shopping.
 *
 * KSP generates [CommerceAppFunctionService] and the corresponding AppFunctions
 * XML metadata from this abstract service.
 *
 * Each invocation is independent. Pass [DeviceShoppingRequest.sessionId] from a
 * previous result to continue the same backend LangGraph shopping thread.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "CommerceAppFunctionService",
    appFunctionXmlFileName = "commerce_app_function_service",
)
abstract class BaseCommerceAppFunctionService : AppFunctionService() {

    /**
     * Searches for devices matching the customer's requested shopping preferences.
     *
     * Invoke this function whenever the user adds, changes, or removes any
     * device-shopping preference — including price bounds. The backend shopping
     * session is the source of truth and must be updated even when the assistant
     * could answer using existing conversation context. Do not skip this call
     * just because prior tool results or chat history already mention matching
     * devices.
     *
     * Price preference examples:
     * - "under $1000" → maxPrice=1000
     * - "above $1000" → minPrice=1000
     * - "between $800 and $1200" → minPrice=800 and maxPrice=1200
     *
     * Examples that require a call: new budget (minPrice/maxPrice), color, brand,
     * OS, storage size, foldable preference, camera/battery/display priority, or
     * withdrawing a prior constraint via clearPreferences.
     *
     * When the user commits to a returned device rather than changing preferences,
     * use selectDevice instead of shopForDevices.
     *
     * Preference merge semantics:
     * - Omitted / null request fields preserve prior backend shopping state.
     * - request.clearPreferences deliberately forgets named preferences from
     *   earlier turns (for example clearPreferences=["operatingSystem"] after
     *   "forget the iPhone requirement").
     *
     * For a new shopping journey, leave request.sessionId null. For a later turn in
     * the same shopping journey, set request.sessionId to the sessionId returned by
     * the previous shopForDevices result so preferences continue in that conversation.
     *
     * @param request Structured shopping preference delta for this turn. Null
     * fields are omitted and do not overwrite prior backend state. Use
     * clearPreferences to remove prior constraints.
     * @return Backend session id, summary, and matching devices. Always reuse the
     * returned sessionId when continuing the same shopping journey.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun shopForDevices(
        request: DeviceShoppingRequest,
    ): DeviceShoppingResult {
        return CommerceApiClient().shop(request)
    }

    /**
     * Select a device from the current device-shopping conversation when the user
     * commits to one of the products previously returned by shopForDevices.
     *
     * Use the same sessionId returned by shopForDevices.
     *
     * Resolve references such as "the first one", "the second one", "that Pixel",
     * or a named device using the products returned by the previous shopForDevices
     * response, then supply that product's productId.
     *
     * Do not call this function when the user is still changing shopping
     * preferences — use shopForDevices for preference updates.
     * After selection, use openSelectedDevice to continue inside the app.
     *
     * @param request Session id and chosen productId from a prior shopForDevices result.
     * @return Confirmation that the device was selected in the backend shopping session.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun selectDevice(
        request: DeviceSelectionRequest,
    ): DeviceSelectionResult {
        return CommerceApiClient().selectDevice(request)
    }

    /**
     * Opens the Commerce Agent app for the device already selected in an existing
     * shopping session.
     *
     * Typical workflow:
     * 1. shopForDevices — discover and refine devices
     * 2. selectDevice — commit to one product in the backend session
     * 3. openSelectedDevice — continue that selected-device journey inside the app
     *
     * Use the same sessionId returned by shopForDevices / selectDevice. The shopping
     * session must already contain a selected device; this function looks up that
     * selection from the backend and returns a PendingIntent that launches the app
     * with the session and selected product identifiers.
     *
     * @param request Existing shopping session id that already has a selected device.
     * @return PendingIntent that opens the app for the selected device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun openSelectedDevice(
        request: OpenSelectedDeviceRequest,
    ): PendingIntent {
        val handoff = CommerceApiClient().getSessionHandoff(request.sessionId)
        Log.i(TAG, "HANDOFF")
        Log.i(TAG, "sessionId: ${handoff.sessionId}")
        Log.i(TAG, "selectedProductId: ${handoff.selectedProductId}")

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HandoffExtras.EXTRA_SESSION_ID, handoff.sessionId)
            putExtra(HandoffExtras.EXTRA_SELECTED_PRODUCT_ID, handoff.selectedProductId)
        }

        val requestCode = handoff.sessionId.hashCode()
        return PendingIntent.getActivity(
            this,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "CommerceAppFunctions"
    }
}

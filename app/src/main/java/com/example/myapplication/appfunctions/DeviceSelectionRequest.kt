package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Request to commit to a device in an existing shopping conversation.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceSelectionRequest(
    /**
     * Shopping conversation identifier returned by shopForDevices.
     * Always reuse the same sessionId from the current shopping journey.
     */
    val sessionId: String,
    /**
     * Product identifier of the device the user chose.
     * Use the productId from the products previously returned by shopForDevices.
     * Resolve references such as "the first one", "the second one", "that Pixel",
     * or a named device using that prior product list, then supply that productId.
     */
    val productId: String,
)

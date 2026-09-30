package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Structured result of a device shopping search.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceShoppingResult(
    /**
     * Identifier for this shopping conversation.
     * Supply this value to later shopForDevices calls that continue the same
     * shopping journey.
     */
    val sessionId: String,
    /** Short human-readable summary of the matched products. */
    val summary: String,
    /** Devices that best match the requested shopping preferences. */
    val products: List<DeviceResult>,
)

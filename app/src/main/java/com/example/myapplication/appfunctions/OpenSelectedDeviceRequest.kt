package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Request to open the app for a device already selected in a shopping session.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OpenSelectedDeviceRequest(
    /** Existing shopping session containing a selected device. */
    val sessionId: String,
)

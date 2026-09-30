package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Confirmation that a device was selected in the shopping conversation.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceSelectionResult(
    /**
     * Shopping conversation identifier. Same sessionId used for this selection.
     */
    val sessionId: String,
    /** Selection outcome status, for example SELECTED. */
    val status: String,
    /** Product identifier that was persisted as the customer's choice. */
    val selectedProductId: String,
    /** Full details of the selected device. */
    val selectedProduct: DeviceResult,
    /** Short human-readable confirmation of the selection. */
    val summary: String,
)

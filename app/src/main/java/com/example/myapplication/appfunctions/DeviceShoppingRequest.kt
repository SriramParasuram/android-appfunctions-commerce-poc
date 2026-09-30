package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Structured shopping preferences for finding devices.
 *
 * Agents should populate whichever fields are known from the customer and leave
 * unknown preferences null so the search can start broad and refine later.
 *
 * Preference merge semantics on the backend:
 * - Omitted / null field: keep the previous value for this shopping session.
 * - [clearPreferences]: deliberately forget named preferences from prior turns.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceShoppingRequest(
    /**
     * Identifier for an existing shopping conversation.
     * When continuing a shopping journey, reuse the sessionId returned by the
     * previous shopForDevices result. Leave null for a new shopping journey.
     */
    val sessionId: String? = null,
    /**
     * Minimum device price in USD. Set this when the user asks for phones above,
     * over, or starting from a certain price.
     *
     * Examples: "phones above $1000" → minPrice=1000;
     * "only phones over $800" → minPrice=800;
     * "between $800 and $1200" → minPrice=800 and maxPrice=1200.
     */
    val minPrice: Int? = null,
    /** Maximum device price the customer is willing to pay, in US dollars. */
    val maxPrice: Int? = null,
    /** Preferred device operating system, for example ANDROID or IOS. */
    val operatingSystem: String? = null,
    /** Preferred manufacturer brand, for example Apple, Samsung, or Google. */
    val brand: String? = null,
    /** Preferred device color, for example BLACK or BLUE. */
    val color: String? = null,
    /**
     * Preferred onboard storage capacity in gigabytes.
     * Set storageGb when the user requests a specific storage capacity such as
     * 128 GB, 256 GB, 512 GB, or 1 TB (use 1024 for 1 TB).
     */
    val storageGb: Int? = null,
    /** How important camera quality is, for example HIGH, MEDIUM, or LOW. */
    val cameraPriority: String? = null,
    /** How important battery life is, for example HIGH, MEDIUM, or LOW. */
    val batteryPriority: String? = null,
    /** How important display quality is, for example HIGH, MEDIUM, or LOW. */
    val displayPriority: String? = null,
    /** Whether the customer wants a foldable device. */
    val isFoldable: Boolean? = null,
    /**
     * Preference field names to deliberately forget from the shopping session.
     *
     * Allowed names: minPrice, maxPrice, operatingSystem, brand, color, storageGb,
     * cameraPriority, batteryPriority, displayPriority, isFoldable.
     *
     * Omitted fields keep their previous value. Use clearPreferences when the
     * customer withdraws a constraint (for example "forget the iPhone
     * requirement" → clearPreferences=["operatingSystem"]).
     */
    val clearPreferences: List<String>? = null,
)

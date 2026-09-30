package com.example.myapplication.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * A single device product returned from a shopping search.
 *
 * cameraScore, batteryScore, and displayScore are internal demo ranking values
 * only — not manufacturer specifications or authoritative benchmarks.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceResult(
    /** Stable product identifier used for later selection or checkout. */
    val productId: String,
    /** Manufacturer brand name. */
    val brand: String,
    /** Customer-facing product display name. */
    val name: String,
    /** Product price in US dollars. */
    val priceUsd: Int,
    /** Device operating system, for example ANDROID or IOS. */
    val operatingSystem: String,
    /** Colors offered for this product. */
    val availableColors: List<String>,
    /** Storage sizes in gigabytes offered for this product. */
    val availableStorageGb: IntArray,
    /** Demo camera ranking score from 0–100 (not a manufacturer spec). */
    val cameraScore: Int,
    /** Demo battery ranking score from 0–100 (not a manufacturer spec). */
    val batteryScore: Int,
    /** Demo display ranking score from 0–100 (not a manufacturer spec). */
    val displayScore: Int,
    /** Whether this device is a foldable form factor. */
    val isFoldable: Boolean,
    /** Diagonal screen size in inches. */
    val screenSize: Double,
)

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.appfunctions.DeviceResult
import com.example.myapplication.network.CommerceApiClient
import com.example.myapplication.ui.shop.ShopPhonesScreen
import com.example.myapplication.ui.shop.ShopPhonesUiState
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val api = CommerceApiClient()
    private var uiState by mutableStateOf(ShopPhonesUiState(isLoading = true))
    private var loadJob: Job? = null

    /** Latest Intent handoff hints; backend session remains authoritative. */
    private var pendingIntentSessionId: String? = null
    private var pendingIntentSelectedProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureHandoffExtras(intent)
        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShopPhonesScreen(
                        state = uiState,
                        onRetry = { loadShopPhones() },
                        onBack = { finish() },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
        loadShopPhones()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureHandoffExtras(intent)
        Log.i(TAG, "MainActivity onNewIntent handoff")
        Log.i(TAG, "sessionId: $pendingIntentSessionId")
        Log.i(TAG, "selectedProductId: $pendingIntentSelectedProductId")
        loadShopPhones()
    }

    private fun captureHandoffExtras(intent: Intent?) {
        val sessionId = intent?.getStringExtra(HandoffExtras.EXTRA_SESSION_ID)
        val productId = intent?.getStringExtra(HandoffExtras.EXTRA_SELECTED_PRODUCT_ID)
        if (!sessionId.isNullOrBlank()) {
            pendingIntentSessionId = sessionId
            pendingIntentSelectedProductId = productId
            Log.i(TAG, "MainActivity received handoff")
            Log.i(TAG, "sessionId: $sessionId")
            Log.i(TAG, "selectedProductId: $productId")
        }
    }

    private fun loadShopPhones() {
        val intentSessionId = pendingIntentSessionId
        val intentSelectedProductId = pendingIntentSelectedProductId
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, restoreMessage = null)
            try {
                val catalogue = api.getCatalogue()
                var sessionId: String? = null
                var selectedProductId: String? = null
                var restoreMessage: String? = null

                if (!intentSessionId.isNullOrBlank()) {
                    try {
                        val handoff = api.getShoppingSession(intentSessionId)
                        sessionId = handoff.sessionId
                        selectedProductId = handoff.selectedProductId

                        if (
                            !intentSelectedProductId.isNullOrBlank() &&
                            intentSelectedProductId != handoff.selectedProductId
                        ) {
                            Log.w(
                                TAG,
                                "Intent/backend selectedProductId mismatch; using backend",
                            )
                            Log.w(TAG, "intentSelectedProductId: $intentSelectedProductId")
                            Log.w(TAG, "backendSelectedProductId: ${handoff.selectedProductId}")
                        }

                        Log.i(TAG, "UI HANDOFF RESTORE")
                        Log.i(TAG, "sessionId: ${handoff.sessionId}")
                        Log.i(TAG, "intentSelectedProductId: $intentSelectedProductId")
                        Log.i(TAG, "backendSelectedProductId: ${handoff.selectedProductId}")
                        Log.i(TAG, "catalogueCount: ${catalogue.size}")

                        val inCatalogue = catalogue.any { it.productId == selectedProductId }
                        if (!inCatalogue) {
                            Log.w(TAG, "selected product missing from catalogue: $selectedProductId")
                            restoreMessage = "Your assistant selection could not be restored."
                            selectedProductId = null
                        }
                    } catch (sessionEx: Exception) {
                        Log.e(TAG, "session restore failed: ${sessionEx.message}")
                        restoreMessage = "Your assistant selection could not be restored."
                        sessionId = null
                        selectedProductId = null
                    }
                }

                uiState = ShopPhonesUiState(
                    isLoading = false,
                    devices = orderForDisplay(catalogue, selectedProductId),
                    selectedProductId = selectedProductId,
                    sessionId = sessionId,
                    error = null,
                    restoreMessage = restoreMessage,
                )
            } catch (ex: Exception) {
                Log.e(TAG, "catalogue load failed: ${ex.message}")
                uiState = ShopPhonesUiState(
                    isLoading = false,
                    devices = emptyList(),
                    selectedProductId = null,
                    sessionId = null,
                    error = ex.message ?: "Unknown error",
                    restoreMessage = null,
                )
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        fun orderForDisplay(
            catalogue: List<DeviceResult>,
            selectedProductId: String?,
        ): List<DeviceResult> {
            if (selectedProductId.isNullOrBlank()) return catalogue
            val selected = catalogue.filter { it.productId == selectedProductId }
            val rest = catalogue.filter { it.productId != selectedProductId }
            return selected + rest
        }
    }
}

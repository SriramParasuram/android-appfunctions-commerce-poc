package com.example.myapplication.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.appfunctions.DeviceResult

data class ShopPhonesUiState(
    val isLoading: Boolean = false,
    val devices: List<DeviceResult> = emptyList(),
    val selectedProductId: String? = null,
    val sessionId: String? = null,
    val error: String? = null,
    val restoreMessage: String? = null,
)

// Blue-led commerce palette — restrained, not flashy
private val PageBackground = Color(0xFFF3F4F6) // warm off-white / light gray
private val CardBackground = Color(0xFFFFFFFF)
private val SelectedCardBackground = Color(0xFFEEF4FC) // pale blue tint
private val SelectedBorder = Color(0xFF2F6FED) // strong medium blue accent
private val TitleColor = Color(0xFF0B1F3A) // deep navy primary text
private val SecondaryColor = Color(0xFF3D5A80) // navy-slate secondary
private val TertiaryColor = Color(0xFF6B7C93) // muted blue-slate
private val DividerSoft = Color(0xFFDCE6F2)
private val PlaceholderFill = Color(0xFFE8EEF6)
private val PlaceholderStroke = Color(0xFFD0DBE8)
private val PhoneFrame = Color(0xFF9BB0C9)
private val BadgeBackground = Color(0xFFD9E8FC)
private val BadgeText = Color(0xFF1A4FA3)
private val HighlightChipBg = Color(0xFFE6EEF8) // subtle blue/slate chip
private val HighlightChipText = Color(0xFF2F4A6E)
private val CardShadow = Color(0x1A0B1F3A)
private val RegularCardBorder = Color(0xFFE2E8F0)

@Composable
fun ShopPhonesScreen(
    state: ShopPhonesUiState,
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground),
    ) {
        ShopPhonesHeader(
            resultCount = state.devices.size,
            onBack = onBack,
            showCount = !state.isLoading && state.error == null,
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = SelectedBorder,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Loading phones…",
                            color = SecondaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "We couldn't load phones.",
                            color = TitleColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.error,
                            color = SecondaryColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SelectedBorder,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "Retry",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 6.dp,
                        bottom = 40.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.restoreMessage != null) {
                        item(key = "restore-message") {
                            ContextMessage(state.restoreMessage)
                        }
                    } else if (state.selectedProductId != null) {
                        item(key = "selection-ready") {
                            ContextMessage("Your selection is ready")
                        }
                    }

                    items(
                        items = state.devices,
                        key = { it.productId },
                    ) { device ->
                        DeviceCard(
                            device = device,
                            assistantSelected = device.productId == state.selectedProductId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMessage(text: String) {
    Text(
        text = text,
        color = SecondaryColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ShopPhonesHeader(
    resultCount: Int,
    onBack: (() -> Unit)?,
    showCount: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    ) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = "←",
                    fontSize = 22.sp,
                    color = TitleColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Shop Phones",
                color = TitleColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
            if (showCount) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$resultCount Results",
                    color = TertiaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceResult,
    assistantSelected: Boolean,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (assistantSelected) 10.dp else 3.dp,
                shape = shape,
                ambientColor = CardShadow,
                spotColor = CardShadow,
            )
            .clip(shape)
            .background(if (assistantSelected) SelectedCardBackground else CardBackground)
            .then(
                if (assistantSelected) {
                    Modifier.border(2.dp, SelectedBorder, shape)
                } else {
                    Modifier.border(0.5.dp, RegularCardBorder, shape)
                },
            )
            .padding(
                start = 16.dp,
                end = 18.dp,
                top = if (assistantSelected) 14.dp else 18.dp,
                bottom = 18.dp,
            ),
    ) {
        if (assistantSelected) {
            Text(
                text = "✓ Selected with your assistant",
                color = BadgeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BadgeBackground)
                    .border(1.dp, SelectedBorder.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DevicePlaceholder()
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.brand.uppercase(),
                    color = TertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = device.name,
                    color = TitleColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 24.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Starting at",
                    color = TertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$${device.priceUsd}",
                    color = TitleColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DividerSoft),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    device.availableColors.take(5).forEach { colorName ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(swatchColor(colorName))
                .border(
                                    width = 1.dp,
                                    color = Color(0xFFB8C5D6).copy(alpha = 0.85f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                    Text(
                        text = "·",
                        color = TertiaryColor,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = storageLine(device.availableStorageGb),
                        color = SecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                FeatureHighlights(device)
            }
        }
    }
}

@Composable
private fun FeatureHighlights(device: DeviceResult) {
    val parts = featureParts(device)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEach { part ->
            Text(
                text = part,
                color = HighlightChipText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(HighlightChipBg)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun DevicePlaceholder() {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(82.dp)
            .height(118.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F8FC),
                        PlaceholderFill,
                    ),
                ),
            )
            .border(1.dp, PlaceholderStroke, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.5.dp, PhoneFrame, RoundedCornerShape(9.dp)),
        ) {
            // subtle top speaker notch
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 7.dp)
                    .width(12.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PhoneFrame.copy(alpha = 0.7f)),
            )
        }
    }
}

internal fun storageLine(storageGb: IntArray): String =
    storageGb.joinToString(" · ") { "$it GB" }

internal fun featureParts(device: DeviceResult): List<String> {
    val traits = buildList {
        when {
            device.cameraScore >= 90 -> add("Great camera")
            device.batteryScore >= 90 -> add("Strong battery")
            device.displayScore >= 90 -> add("Great display")
        }
        if (device.isFoldable) add("Foldable")
        add(
            when (device.operatingSystem.uppercase()) {
                "IOS" -> "iOS"
                "ANDROID" -> "Android"
                else -> device.operatingSystem
            },
        )
    }
    return traits.take(2)
}

internal fun featureLine(device: DeviceResult): String =
    featureParts(device).joinToString(" · ")

internal fun swatchColor(name: String): Color =
    when (name.uppercase()) {
        "BLACK" -> Color(0xFF1C1C1C)
        "WHITE" -> Color(0xFFF9FAFB)
        "BLUE" -> Color(0xFF3B6FBF)
        "RED" -> Color(0xFFC62828)
        "GREEN" -> Color(0xFF2E7D4F)
        "PURPLE" -> Color(0xFF6A4C9C)
        "GRAY", "GREY" -> Color(0xFF9E9E9E)
        "SILVER" -> Color(0xFFC0C4C8)
        "HAZEL" -> Color(0xFFA88A6E)
        else -> Color(0xFFBDBDBD)
    }

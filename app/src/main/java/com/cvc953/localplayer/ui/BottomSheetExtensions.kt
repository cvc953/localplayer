package com.cvc953.localplayer.ui

import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue

/**
 * Convierte el estado del BottomSheet en un valor único de 0.0 a 1.0 0.0f = PartiallyExpanded
 * (MiniPlayer visible) 1.0f = Expanded (PlayerScreen completo)
 *
 * Adaptado para Material3
 */
@OptIn(ExperimentalMaterial3Api::class)
val BottomSheetScaffoldState.currentFraction: Float
  get() {
    val currentValue = bottomSheetState.currentValue

    return when (currentValue) {
      SheetValue.PartiallyExpanded -> 0f
      SheetValue.Expanded -> 1f
      SheetValue.Hidden -> 0f
      else -> 0f
    }
  }

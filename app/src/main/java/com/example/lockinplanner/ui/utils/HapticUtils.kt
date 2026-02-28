package com.example.lockinplanner.ui.utils

import android.view.HapticFeedbackConstants
import android.view.View

fun View.performLightHapticFeedback(isEnabled: Boolean) {
    if (isEnabled) {
        this.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

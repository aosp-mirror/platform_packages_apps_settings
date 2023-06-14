/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.biometrics2.ui.view

import android.app.Activity
import android.text.TextUtils
import android.view.View
import androidx.annotation.StringRes
import com.google.android.setupdesign.GlifLayout

/**
 * Utils class for GlifLayout
 */
class GlifLayoutHelper(val activity: Activity, val glifLayout: GlifLayout) {

    /**
     * Sets header text to GlifLayout
     */
    fun setHeaderText(@StringRes textResId: Int) {
        val layoutTitle = glifLayout.headerTextView
        val previousTitle = layoutTitle.text
        val title = activity.getText(textResId)
        if (previousTitle !== title) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            glifLayout.headerText = title
            glifLayout.headerTextView.contentDescription = title
            activity.title = title
        }
    }

    /**
     * Sets description text to GlifLayout
     */
    fun setDescriptionText(description: CharSequence?) {
        val previousDescription = glifLayout.descriptionText
        // Prevent a11y for re-reading the same string
        if (!TextUtils.equals(previousDescription, description)) {
            glifLayout.descriptionText = description
        }
    }
}

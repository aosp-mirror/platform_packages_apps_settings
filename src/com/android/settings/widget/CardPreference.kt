/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.widget

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spa.widget.card.CardButton
import com.android.settingslib.spa.widget.card.CardModel
import com.android.settingslib.spa.widget.card.SettingsCard

/** A preference for settings banner tips card. */
class CardPreference
@JvmOverloads
constructor(
    context: Context,
    attr: AttributeSet? = null,
) : ComposePreference(context, attr) {

    /** A icon resource id for displaying icon on tips card. */
    var iconResId: Int? = null

    /** The primary button's text. */
    var primaryButtonText: String = ""

    /** The accessibility content description of the primary button. */
    var primaryButtonContentDescription: String? = null

    /** The action for click on primary button. */
    var primaryButtonAction: () -> Unit = {}

    /** The visibility of primary button on tips card. The default value is `false`. */
    var primaryButtonVisibility: Boolean = false

    /** The text on the second button of this [SettingsCard]. */
    var secondaryButtonText: String = ""

    /** The accessibility content description of the secondary button. */
    var secondaryButtonContentDescription: String? = null

    /** The action for click on secondary button. */
    var secondaryButtonAction: () -> Unit = {}

    /** The visibility of secondary button on tips card. The default value is `false`. */
    var secondaryButtonVisibility: Boolean = false

    var onClick: (() -> Unit)? = null

    /** The callback for click on card preference itself. */
    private var onDismiss: (() -> Unit)? = null

    /** Enable the dismiss button on tips card. */
    fun enableDismiss(enable: Boolean) =
        if (enable) onDismiss = { isVisible = false } else onDismiss = null

    /** Clear layout state if needed. */
    fun resetLayoutState() {
        primaryButtonVisibility = false
        secondaryButtonVisibility = false
        notifyChanged()
    }

    /** Build the tips card content to apply any changes of this card's property. */
    fun buildContent() {
        setContent {
            SettingsCard(
                CardModel(
                    title = title?.toString() ?: "",
                    text = summary?.toString() ?: "",
                    buttons = listOfNotNull(configPrimaryButton(), configSecondaryButton()),
                    onDismiss = onDismiss,
                    imageVector =
                    iconResId
                        ?.takeIf { it != Resources.ID_NULL }
                        ?.let { ImageVector.vectorResource(it) },
                    onClick = onClick,
                )
            )
        }
    }

    private fun configPrimaryButton(): CardButton? {
        return if (primaryButtonVisibility)
            CardButton(
                text = primaryButtonText,
                contentDescription = primaryButtonContentDescription,
                onClick = primaryButtonAction,
            )
        else null
    }

    private fun configSecondaryButton(): CardButton? {
        return if (secondaryButtonVisibility)
            CardButton(
                text = secondaryButtonText,
                contentDescription = secondaryButtonContentDescription,
                onClick = secondaryButtonAction,
            )
        else null
    }

    override fun notifyChanged() {
        buildContent()
        super.notifyChanged()
    }
}

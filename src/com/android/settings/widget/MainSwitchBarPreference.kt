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
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.preference.TwoStatePreference
import com.android.settings.SettingsActivity
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.widget.MainSwitchBar

/** Preference abstraction of the [MainSwitchBar] in settings activity. */
class MainSwitchBarPreference(context: Context, private val metadata: MainSwitchBarMetadata) :
    TwoStatePreference(context), OnCheckedChangeListener {

    private val mainSwitchBar: MainSwitchBar = (context as SettingsActivity).switchBar

    override fun setTitle(title: CharSequence?) {
        mainSwitchBar.setTitle(title)
    }

    override fun setSummary(summary: CharSequence?) {
        mainSwitchBar.setSummary(summary)
    }

    override fun setEnabled(enabled: Boolean) {
        mainSwitchBar.isEnabled = enabled
    }

    // Preference.setVisible is final, we cannot override it
    fun updateVisibility() {
        // always make preference invisible, the UI visibility is reflected on MainSwitchBar
        isVisible = false
        if ((metadata as? PreferenceAvailabilityProvider)?.isAvailable(context) != false) {
            mainSwitchBar.show()
        } else {
            mainSwitchBar.hide()
        }
    }

    override fun setChecked(checked: Boolean) {
        // remove listener to update UI only
        mainSwitchBar.removeOnSwitchChangeListener(this)
        mainSwitchBar.isChecked = checked
        mainSwitchBar.addOnSwitchChangeListener(this)
    }

    override fun onAttached() {
        super.onAttached()
        mainSwitchBar.addOnSwitchChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // prevent user from toggling the switch before data store operation is done
        isEnabled = false
        // once data store is updated, isEnabled will be reset due to rebind
        persistBoolean(isChecked)
    }

    override fun onDetached() {
        mainSwitchBar.removeOnSwitchChangeListener(this)
        super.onDetached()
    }
}

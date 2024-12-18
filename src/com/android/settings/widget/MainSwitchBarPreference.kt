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
import android.os.Parcel
import android.os.Parcelable
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.preference.TwoStatePreference
import com.android.settings.SettingsActivity
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.widget.MainSwitchBar

/** Preference abstraction of the [MainSwitchBar] in settings activity. */
class MainSwitchBarPreference(context: Context, private val metadata: MainSwitchBarMetadata) :
    TwoStatePreference(context), OnCheckedChangeListener, MainSwitchBar.PreChangeListener {

    // main switch bar might be null when configuration is just changed
    private val mainSwitchBar: MainSwitchBar?
        get() = (context as SettingsActivity).switchBar

    override fun setTitle(title: CharSequence?) {
        mainSwitchBar?.setTitle(title)
        super.setTitle(title)
    }

    override fun setSummary(summary: CharSequence?) {
        mainSwitchBar?.setSummary(summary)
        super.setSummary(summary)
    }

    override fun setEnabled(enabled: Boolean) {
        mainSwitchBar?.isEnabled = enabled
        super.setEnabled(enabled)
    }

    // Preference.setVisible is final, we cannot override it
    fun updateVisibility() {
        // always make preference invisible, the UI visibility is reflected on MainSwitchBar
        isVisible = false
        val mainSwitchBar = mainSwitchBar ?: return
        if ((metadata as? PreferenceAvailabilityProvider)?.isAvailable(context) != false) {
            mainSwitchBar.show()
        } else {
            mainSwitchBar.hide()
        }
    }

    override fun setChecked(checked: Boolean) {
        val mainSwitchBar = mainSwitchBar ?: return
        // remove listener to update UI only
        mainSwitchBar.removeOnSwitchChangeListener(this)
        mainSwitchBar.isChecked = checked
        mainSwitchBar.addOnSwitchChangeListener(this)
    }

    override fun onAttached() {
        super.onAttached()
        val mainSwitchBar = mainSwitchBar!!
        mainSwitchBar.setPreChangeListener(this)
        mainSwitchBar.addOnSwitchChangeListener(this)
    }

    override fun preChange(isCheck: Boolean) = callChangeListener(isCheck)

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // prevent user from toggling the switch before data store operation is done
        isEnabled = false
        // once data store is updated, isEnabled will be reset due to rebind
        persistBoolean(isChecked)
    }

    override fun onDetached() {
        val mainSwitchBar = mainSwitchBar!!
        mainSwitchBar.removeOnSwitchChangeListener(this)
        mainSwitchBar.setPreChangeListener(null)
        super.onDetached()
    }

    override fun onSaveInstanceState(): Parcelable =
        SavedState(super.onSaveInstanceState()!!).also {
            it.isEnabled = isEnabled
            it.title = title
            it.summary = summary
            it.mainSwitchBarState = mainSwitchBar?.onSaveInstanceState()
        }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        isEnabled = savedState.isEnabled
        title = savedState.title
        summary = savedState.summary
        mainSwitchBar?.onRestoreInstanceState(savedState.mainSwitchBarState!!)
    }

    private class SavedState : BaseSavedState {
        var isEnabled: Boolean = false
        var title: CharSequence? = null
        var summary: CharSequence? = null
        var mainSwitchBarState: Parcelable? = null

        constructor(source: Parcel) : super(source) {
            isEnabled = source.readBoolean()
            title = source.readCharSequence()
            summary = source.readCharSequence()
            val stateClass = MainSwitchBar.SavedState::class.java
            mainSwitchBarState = source.readParcelable(stateClass.classLoader, stateClass)
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeBoolean(isEnabled)
            dest.writeCharSequence(title)
            dest.writeCharSequence(summary)
            dest.writeParcelable(mainSwitchBarState, flags)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object : Parcelable.Creator<SavedState> {
                    override fun createFromParcel(parcel: Parcel): SavedState {
                        return SavedState(parcel)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint2.ui.settings.fragment

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.settingslib.widget.TwoTargetPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "FingerprintSettingsPreference"

class FingerprintSettingsPreference(
    context: Context,
    val fingerprintViewModel: FingerprintData,
    val fragment: FingerprintSettingsV2Fragment,
    val isLastFingerprint: Boolean
) : TwoTargetPreference(context) {
  private lateinit var myView: View

  init {
    key = "FINGERPRINT_" + fingerprintViewModel.fingerId
    Log.d(TAG, "FingerprintPreference $this with frag $fragment $key")
    title = fingerprintViewModel.name
    isPersistent = false
    setIcon(R.drawable.ic_fingerprint_24dp)
    setOnPreferenceClickListener {
      fragment.lifecycleScope.launch { fragment.onPrefClicked(fingerprintViewModel) }
      true
    }
  }

  override fun onBindViewHolder(view: PreferenceViewHolder) {
    super.onBindViewHolder(view)
    myView = view.itemView
    view.itemView.findViewById<View>(R.id.delete_button)?.setOnClickListener {
      fragment.lifecycleScope.launch { fragment.onDeletePrefClicked(fingerprintViewModel) }
    }
  }

  /** Highlights this dialog. */
  suspend fun highlight() {
    fragment.activity?.getDrawable(R.drawable.preference_highlight)?.let { highlight ->
      val centerX: Float = myView.width / 2.0f
      val centerY: Float = myView.height / 2.0f
      highlight.setHotspot(centerX, centerY)
      myView.background = highlight
      myView.isPressed = true
      myView.isPressed = false
      delay(300)
      myView.background = null
    }
  }

  override fun getSecondTargetResId(): Int {
    return R.layout.preference_widget_delete
  }

  suspend fun askUserToDeleteDialog(): Boolean {
    return FingerprintDeletionDialog.showInstance(fingerprintViewModel, isLastFingerprint, fragment)
  }

  suspend fun askUserToRenameDialog(): Pair<FingerprintData, String>? {
    return FingerprintSettingsRenameDialog.showInstance(fingerprintViewModel, fragment)
  }
}

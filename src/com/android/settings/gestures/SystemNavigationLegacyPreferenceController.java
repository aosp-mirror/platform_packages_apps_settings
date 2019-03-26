/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.ServiceManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.widget.RadioButtonPreference;

public class SystemNavigationLegacyPreferenceController extends
        SystemNavigationPreferenceController {
    static final String PREF_KEY_LEGACY = "gesture_legacy";

    public SystemNavigationLegacyPreferenceController(Context context, String key) {
        this(context, IOverlayManager.Stub.asInterface(ServiceManager.getService(
                Context.OVERLAY_SERVICE)), key);
    }

    @VisibleForTesting
    public SystemNavigationLegacyPreferenceController(Context context,
            IOverlayManager overlayManager, String key) {
        super(context, overlayManager, key);
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(PREF_KEY_LEGACY, getPreferenceKey());
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        setNavBarInteractionMode(mOverlayManager, NAV_BAR_MODE_3BUTTON_OVERLAY);
        selectRadioButtonInGroup(PREF_KEY_LEGACY, mPreferenceScreen);
    }

    @Override
    public boolean isChecked() {
        return !isEdgeToEdgeEnabled(mContext) && !isSwipeUpEnabled(mContext);
    }
}

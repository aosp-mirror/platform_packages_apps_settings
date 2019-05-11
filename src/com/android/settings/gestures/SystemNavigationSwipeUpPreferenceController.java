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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.ServiceManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.widget.RadioButtonPreference;

public class SystemNavigationSwipeUpPreferenceController extends
        SystemNavigationPreferenceController {
    static final String PREF_KEY_SWIPE_UP = "gesture_swipe_up";

    public SystemNavigationSwipeUpPreferenceController(Context context, String key) {
        this(context, IOverlayManager.Stub.asInterface(ServiceManager.getService(
                Context.OVERLAY_SERVICE)), key);
    }

    @VisibleForTesting
    public SystemNavigationSwipeUpPreferenceController(Context context,
            IOverlayManager overlayManager, String key) {
        super(context, overlayManager, key, NAV_BAR_MODE_2BUTTON_OVERLAY);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        setNavBarInteractionMode(mOverlayManager, NAV_BAR_MODE_2BUTTON_OVERLAY);
        selectRadioButtonInGroup(PREF_KEY_SWIPE_UP, mPreferenceScreen);
    }

    @Override
    public boolean isChecked() {
        return isSwipeUpEnabled(mContext);
    }
}

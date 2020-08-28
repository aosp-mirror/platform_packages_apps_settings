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

package com.android.settings.core;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.development.OverlaySettingsPreferenceController;


/**
 * A mixin that adds window flag to prevent non-system overlays showing on top of Settings
 * activities.
 */
public class HideNonSystemOverlayMixin implements LifecycleObserver {

    private final Activity mActivity;

    public HideNonSystemOverlayMixin(Activity activity) {
        mActivity = activity;
    }

    @VisibleForTesting
    boolean isEnabled() {
        return !OverlaySettingsPreferenceController.isOverlaySettingsEnabled(mActivity);
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (mActivity == null || !isEnabled()) {
            return;
        }
        mActivity.getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        android.util.EventLog.writeEvent(0x534e4554, "120484087", -1, "");
    }


    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (mActivity == null || !isEnabled()) {
            return;
        }
        final Window window = mActivity.getWindow();
        final WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.privateFlags &= ~SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(attrs);
    }
}

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

package com.android.settings.accessibility;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Arrays;
import java.util.List;

/** Preference controller for captioning custom visibility. */
public class CaptioningCustomController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @Nullable
    private Preference mCustom;
    private final CaptionHelper mCaptionHelper;
    private final ContentResolver mContentResolver;
    @VisibleForTesting
    AccessibilitySettingsContentObserver mSettingsContentObserver;
    @VisibleForTesting
    static final List<String> CAPTIONING_FEATURE_KEYS = Arrays.asList(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET
    );

    public CaptioningCustomController(Context context, String preferenceKey) {
        this(context, preferenceKey, new CaptionHelper(context),
                new AccessibilitySettingsContentObserver(new Handler(Looper.getMainLooper())));
    }

    @VisibleForTesting
    CaptioningCustomController(
            Context context, String preferenceKey, CaptionHelper captionHelper,
            AccessibilitySettingsContentObserver contentObserver) {
        super(context, preferenceKey);
        mCaptionHelper = new CaptionHelper(context);
        mContentResolver = context.getContentResolver();
        mSettingsContentObserver = contentObserver;
        mSettingsContentObserver.registerKeysToObserverCallback(CAPTIONING_FEATURE_KEYS, key -> {
            if (mCustom != null) {
                mCustom.setVisible(shouldShowPreference());
            }
        });
    }

    @Override
    public int getAvailabilityStatus() {
        if (com.android.settings.accessibility.Flags.fixA11ySettingsSearch()) {
            return (shouldShowPreference()) ? AVAILABLE : AVAILABLE_UNSEARCHABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCustom = screen.findPreference(getPreferenceKey());
        if (mCustom != null) {
            mCustom.setVisible(shouldShowPreference());
        }
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(mContentResolver);
    }

    private boolean shouldShowPreference() {
        return mCaptionHelper.getRawUserStyle() == CaptioningManager.CaptionStyle.PRESET_CUSTOM;
    }
}

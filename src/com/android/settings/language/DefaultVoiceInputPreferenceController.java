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

package com.android.settings.language;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/** Controller of the Voice Input preference. */
public class DefaultVoiceInputPreferenceController extends DefaultAppPreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String KEY_VOICE_INPUT = "voice_input_settings";

    private VoiceInputHelper mHelper;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private Context mContext;

    public DefaultVoiceInputPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mHelper = new VoiceInputHelper(context);
        mHelper.buildUi();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_VOICE_RECOGNIZERS);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VOICE_INPUT;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
        updatePreference();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(mPreference);
        updatePreference();
    }

    @Override
    public void onPause() {}

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final String defaultKey = getDefaultAppKey();
        if (defaultKey == null) {
            return null;
        }

        for (VoiceInputHelper.RecognizerInfo info : mHelper.mAvailableRecognizerInfos) {
            if (TextUtils.equals(defaultKey, info.mKey)) {
                return new DefaultVoiceInputPicker.VoiceInputDefaultAppInfo(mContext,
                        mPackageManager, mUserId, info, true /* enabled */);
            }
        }
        return null;
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        final DefaultAppInfo appInfo = getDefaultAppInfo();
        if (appInfo == null
                || !(appInfo instanceof DefaultVoiceInputPicker.VoiceInputDefaultAppInfo)) {
            return null;
        }
        return ((DefaultVoiceInputPicker.VoiceInputDefaultAppInfo) appInfo).getSettingIntent();
    }

    private void updatePreference() {
        if (mPreference == null) {
            return;
        }
        mHelper.buildUi();
        if (isAvailable()) {
            if (mScreen.findPreference(getPreferenceKey()) == null) {
                // add it if it's not on scree
                mScreen.addPreference(mPreference);
            }
        } else {
            mScreen.removePreference(mPreference);
        }
    }

    private String getDefaultAppKey() {
        final ComponentName currentService = DefaultVoiceInputPicker.getCurrentService(mHelper);
        if (currentService == null) {
            return null;
        }
        return currentService.flattenToShortString();
    }
}

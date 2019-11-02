/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.RadioButtonPreference;

public class PreventRingingGesturePreferenceController extends AbstractPreferenceController
        implements RadioButtonPreference.OnClickListener, LifecycleObserver,
        OnResume, OnPause, PreferenceControllerMixin {

    @VisibleForTesting
    static final String KEY_VIBRATE = "prevent_ringing_option_vibrate";

    @VisibleForTesting
    static final String KEY_MUTE = "prevent_ringing_option_mute";

    private final String PREF_KEY_VIDEO = "gesture_prevent_ringing_video";
    private final String KEY = "gesture_prevent_ringing_category";
    private final Context mContext;

    private VideoPreference mVideoPreference;

    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    RadioButtonPreference mVibratePref;
    @VisibleForTesting
    RadioButtonPreference mMutePref;

    private SettingObserver mSettingObserver;

    public PreventRingingGesturePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            return;
        }
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mVibratePref = makeRadioPreference(KEY_VIBRATE, R.string.prevent_ringing_option_vibrate);
        mMutePref = makeRadioPreference(KEY_MUTE, R.string.prevent_ringing_option_mute);

        if (mPreferenceCategory != null) {
            mSettingObserver = new SettingObserver(mPreferenceCategory);
        }

        mVideoPreference = screen.findPreference(getVideoPrefKey());
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    public String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int preventRingingSetting = keyToSetting(preference.getKey());
        if (preventRingingSetting != Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_VIBRATE)) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.VOLUME_HUSH_GESTURE, preventRingingSetting);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int preventRingingSetting = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_VIBRATE);
        final boolean isVibrate = preventRingingSetting == Settings.Secure.VOLUME_HUSH_VIBRATE;
        final boolean isMute = preventRingingSetting == Settings.Secure.VOLUME_HUSH_MUTE;
        if (mVibratePref != null && mVibratePref.isChecked() != isVibrate) {
            mVibratePref.setChecked(isVibrate);
        }
        if (mMutePref != null && mMutePref.isChecked() != isMute) {
            mMutePref.setChecked(isMute);
        }

        if (preventRingingSetting == Settings.Secure.VOLUME_HUSH_OFF) {
            mVibratePref.setEnabled(false);
            mMutePref.setEnabled(false);
        } else {
            mVibratePref.setEnabled(true);
            mMutePref.setEnabled(true);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }

        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible();
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }

        if (mVideoPreference != null) {
            mVideoPreference.onViewInvisible();
        }
    }

    private int keyToSetting(String key) {
        switch (key) {
            case KEY_MUTE:
                return Settings.Secure.VOLUME_HUSH_MUTE;
            case KEY_VIBRATE:
                return Settings.Secure.VOLUME_HUSH_VIBRATE;
            default:
                return Settings.Secure.VOLUME_HUSH_OFF;
        }
    }

    private RadioButtonPreference makeRadioPreference(String key, int titleId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private class SettingObserver extends ContentObserver {
        private final Uri VOLUME_HUSH_GESTURE = Settings.Secure.getUriFor(
                Settings.Secure.VOLUME_HUSH_GESTURE);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(VOLUME_HUSH_GESTURE, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || VOLUME_HUSH_GESTURE.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}

/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.android.settings.inputmethod.PhysicalKeyboardFragment.getHardKeyboards;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.keyboard.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.LabeledSeekBarPreference;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

public class KeyboardRepeatKeysMainFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {
    private static final String TAG = "RepeatKeysMainFragment";
    private static final String TIME_OUT_KEY = "repeat_keys_timeout_preference";
    private static final String DELAY_KEY = "repeat_keys_delay_preference";

    private final Uri mRepeatKeyUri = Settings.Secure.getUriFor(
            Settings.Secure.KEY_REPEAT_ENABLED);
    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mRepeatKeyUri.equals(uri)) {
                updatePreferencesState();
            }
        }
    };
    private InputManager mInputManager;
    private ContentResolver mContentResolver;
    @Nullable
    private LabeledSeekBarPreference mRepeatTimeoutPreference;
    @Nullable
    private LabeledSeekBarPreference mRepeatDelayPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PHYSICAL_KEYBOARD_REPEAT_KEYS;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mInputManager = Preconditions.checkNotNull(getActivity()
                .getSystemService(InputManager.class));
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        super.onCreatePreferences(bundle, s);
        mRepeatTimeoutPreference = findPreference(TIME_OUT_KEY);
        mRepeatDelayPreference = findPreference(DELAY_KEY);
        updatePreferencesState();
    }

    @Override
    public void onResume() {
        super.onResume();
        finishEarlyIfNeeded();
        mInputManager.registerInputDeviceListener(this, null);
        registerSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        mInputManager.unregisterInputDeviceListener(this);
        unregisterSettingsObserver();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.repeat_key_main_page;
    }

    private void updatePreferencesState() {
        boolean isRepeatKeyEnabled = InputSettings.isRepeatKeysEnabled(getContext());
        if (mRepeatTimeoutPreference != null && mRepeatDelayPreference != null) {
            mRepeatTimeoutPreference.setEnabled(isRepeatKeyEnabled);
            mRepeatDelayPreference.setEnabled(isRepeatKeyEnabled);
        }
    }

    private void registerSettingsObserver() {
        unregisterSettingsObserver();
        mContentResolver.registerContentObserver(
                mRepeatKeyUri,
                false,
                mContentObserver,
                UserHandle.myUserId());
    }

    private void unregisterSettingsObserver() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        finishEarlyIfNeeded();
    }

    private void finishEarlyIfNeeded() {
        final Context context = getContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            final List<PhysicalKeyboardFragment.HardKeyboardDeviceInfo> newHardKeyboards =
                    getHardKeyboards(context);
            if (newHardKeyboards.isEmpty()) {
                getActivity().finish();
            }
        });
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.repeat_key_main_page) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return Flags.keyboardAndTouchpadA11yNewPageEnabled()
                            && !getHardKeyboards(context).isEmpty();
                }
            };
}

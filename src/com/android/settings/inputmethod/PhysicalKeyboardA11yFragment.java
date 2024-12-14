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
import android.content.Context;
import android.hardware.input.InputManager;

import androidx.annotation.NonNull;

import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.keyboard.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

@SearchIndexable
public class PhysicalKeyboardA11yFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {
    private static final String TAG = "KeyboardAndTouchA11yFragment";

    private InputManager mInputManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PHYSICAL_KEYBOARD_A11Y;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mInputManager = Preconditions.checkNotNull(getActivity()
                .getSystemService(InputManager.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        finishEarlyIfNeeded();
        mInputManager.registerInputDeviceListener(this, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        mInputManager.unregisterInputDeviceListener(this);
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

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.physical_keyboard_a11y_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.physical_keyboard_a11y_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return Flags.keyboardAndTouchpadA11yNewPageEnabled()
                            && !getHardKeyboards(context).isEmpty();
                }
            };

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
}

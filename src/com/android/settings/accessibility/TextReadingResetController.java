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

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

/**
 * The controller of the reset button in the text and reading options page.
 */
class TextReadingResetController extends BasePreferenceController {
    private final View.OnClickListener mOnResetClickListener;

    TextReadingResetController(Context context, String preferenceKey,
            @Nullable View.OnClickListener listener) {
        super(context, preferenceKey);
        mOnResetClickListener = listener;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final LayoutPreference layoutPreference = screen.findPreference(getPreferenceKey());
        final View view = layoutPreference.findViewById(R.id.reset_button);
        view.setOnClickListener(v -> {
            if (mOnResetClickListener != null) {
                mOnResetClickListener.onClick(v);
            }
        });
    }

    /**
     * Interface for resetting to default state.
     */
    interface ResetStateListener {
        /**
         * Called when the reset button was clicked.
         */
        void resetState();
    }
}

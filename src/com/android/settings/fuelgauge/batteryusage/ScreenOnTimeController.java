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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Controller for screen on time in battery usage page. */
public class ScreenOnTimeController extends BasePreferenceController {
    private static final String TAG = "ScreenOnTimeController";
    private static final String ROOT_PREFERENCE_KEY = "screen_on_time_category";
    private static final String SCREEN_ON_TIME_TEXT_PREFERENCE_KEY = "screen_on_time_text";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[\\d]*[\\.,]?[\\d]+");
    private static final Locale IW_LOCALE = new Locale("iw");

    @VisibleForTesting Context mPrefContext;
    @VisibleForTesting PreferenceCategory mRootPreference;
    @VisibleForTesting TextViewPreference mScreenOnTimeTextPreference;

    public ScreenOnTimeController(Context context) {
        super(context, ROOT_PREFERENCE_KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mRootPreference = screen.findPreference(ROOT_PREFERENCE_KEY);
        mScreenOnTimeTextPreference = screen.findPreference(SCREEN_ON_TIME_TEXT_PREFERENCE_KEY);
    }

    void handleSceenOnTimeUpdated(Long screenOnTime, String slotTimestamp) {
        if (screenOnTime == null) {
            mRootPreference.setVisible(false);
            mScreenOnTimeTextPreference.setVisible(false);
            return;
        }
        showCategoryTitle(slotTimestamp);
        showScreenOnTimeText(screenOnTime);
    }

    @VisibleForTesting
    void showCategoryTitle(String slotTimestamp) {
        mRootPreference.setTitle(
                slotTimestamp == null
                        ? mPrefContext.getString(R.string.screen_time_category_last_full_charge)
                        : mPrefContext.getString(
                                R.string.screen_time_category_for_slot, slotTimestamp));
        mRootPreference.setVisible(true);
    }

    @VisibleForTesting
    void showScreenOnTimeText(Long screenOnTime) {
        final CharSequence timeSequence =
                BatteryUtils.formatElapsedTimeWithoutComma(
                        mPrefContext,
                        (double) screenOnTime,
                        /* withSeconds= */ false,
                        /* collapseTimeUnit= */ false);
        mScreenOnTimeTextPreference.setText(
                enlargeFontOfNumberIfNeeded(mPrefContext, timeSequence));
        mScreenOnTimeTextPreference.setVisible(true);
    }

    @VisibleForTesting
    static CharSequence enlargeFontOfNumberIfNeeded(Context context, CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }

        final Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        if (locale != null && IW_LOCALE.getLanguage().equals(locale.getLanguage())) {
            return text;
        }

        final SpannableString spannableText = new SpannableString(text);
        final Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            spannableText.setSpan(
                    new AbsoluteSizeSpan(36, true /* dip */),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableText;
    }
}

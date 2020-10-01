/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.suggestions;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.StringRes;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollSuggestionActivity;
import com.android.settings.biometrics.fingerprint.FingerprintSuggestionActivity;
import com.android.settings.notification.zen.ZenSuggestionActivity;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settings.wifi.calling.WifiCallingSuggestionActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SettingsSuggestionsTest {

    @Test
    public void wallpaperSuggestion_isValid() {
        assertSuggestionEquals(
                WallpaperSuggestionActivity.class.getName(),
                R.string.wallpaper_suggestion_title,
                R.string.wallpaper_suggestion_summary);
    }

    @Test
    public void fingerprintSuggestion_isValid() {
        assertSuggestionEquals(
                FingerprintSuggestionActivity.class.getName(),
                R.string.suggestion_additional_fingerprints,
                R.string.suggestion_additional_fingerprints_summary);
    }

    @Test
    public void fingerprintEnrollSuggestion_isValid() {
        assertSuggestionEquals(
                FingerprintEnrollSuggestionActivity.class.getName(),
                R.string.suggested_fingerprint_lock_settings_title,
                R.string.suggested_fingerprint_lock_settings_summary);
    }

    @Test
    public void wifiCallingSuggestion_isValid() {
        assertSuggestionEquals(
                WifiCallingSuggestionActivity.class.getName(),
                R.string.wifi_calling_suggestion_title,
                R.string.wifi_calling_suggestion_summary);
    }

    @Test
    public void nightDisplaySuggestion_isValid() {
        assertSuggestionEquals(
                Settings.NightDisplaySuggestionActivity.class.getName(),
                R.string.night_display_suggestion_title,
                R.string.night_display_suggestion_summary);
    }

    @Test
    public void zenSuggestion_isValid() {
        assertSuggestionEquals(
                ZenSuggestionActivity.class.getName(),
                R.string.zen_suggestion_title,
                R.string.zen_suggestion_summary);
    }

    private void assertSuggestionEquals(String activityName, @StringRes int titleRes,
            @StringRes int summaryRes) {

        final Context context = RuntimeEnvironment.application;
        final PackageManager pm = context.getPackageManager();
        final ComponentName componentName = new ComponentName(context, activityName);
        final ActivityInfo info;
        try {
            info = pm.getActivityInfo(componentName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        final int titleResourceId = (int) info.metaData.get("com.android.settings.title");
        assertThat(titleResourceId).isEqualTo(titleRes);

        final int summaryResourceId = (int) info.metaData.get("com.android.settings.summary");
        assertThat(summaryResourceId).isEqualTo(summaryRes);
    }
}

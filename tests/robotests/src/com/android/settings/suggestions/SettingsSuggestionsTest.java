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

import static com.android.settings.TestConfig.MANIFEST_PATH;
import static com.google.common.truth.Truth.assertThat;

import android.annotation.StringRes;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.fingerprint.FingerprintSuggestionActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.ActivityData;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.manifest.IntentFilterData;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsSuggestionsTest {

    private static final String CATEGORY_FIRST_IMPRESSION =
            "com.android.settings.suggested.category.FIRST_IMPRESSION";

    private static final String CATEGORY_SETTINGS_ONLY =
            "com.android.settings.suggested.category.SETTINGS_ONLY";

    @Test
    public void wallpaperSuggestion_isValid() {
        assertSuggestionEquals("com.android.settings.wallpaper.WallpaperSuggestionActivity",
                CATEGORY_FIRST_IMPRESSION,
                R.string.wallpaper_suggestion_title, R.string.wallpaper_suggestion_summary);
    }

    @Test
    public void fingerprintSuggestion_isValid() {
        assertSuggestionEquals(
                FingerprintSuggestionActivity.class.getName(),
                CATEGORY_FIRST_IMPRESSION,
                R.string.suggestion_additional_fingerprints,
                R.string.suggestion_additional_fingerprints_summary);
    }

    @Test
    public void wifiCallingSuggestion_isValid() {
        assertSuggestionEquals("Settings$WifiCallingSuggestionActivity",
                CATEGORY_FIRST_IMPRESSION,
                R.string.wifi_calling_suggestion_title, R.string.wifi_calling_suggestion_summary);
    }

    @Test
    public void nightDisplaySuggestion_isValid() {
        assertSuggestionEquals("Settings$NightDisplaySuggestionActivity",
            CATEGORY_FIRST_IMPRESSION,
            R.string.night_display_suggestion_title, R.string.night_display_suggestion_summary);
    }

    private void assertSuggestionEquals(String activityName, String category, @StringRes int title,
            @StringRes int summary) {
        final AndroidManifest androidManifest = ShadowApplication.getInstance().getAppManifest();
        final ActivityData activityData = androidManifest.getActivityData(activityName);
        final Map<String, Object> metaData = activityData.getMetaData().getValueMap();
        final Context context = RuntimeEnvironment.application;
        final String expectedTitle = context.getString(title);
        final String expectedSummary = context.getString(summary);

        final String pName = context.getPackageName();
        final String actualTitle = context.getString(context.getResources().getIdentifier(
                ((String) metaData.get("com.android.settings.title")).substring(8), "string",
                pName));
        final String actualSummary = context.getString(context.getResources().getIdentifier(
                ((String) metaData.get("com.android.settings.summary")).substring(8), "string",
                pName));
        assertThat(actualTitle).isEqualTo(expectedTitle);
        assertThat(actualSummary).isEqualTo(expectedSummary);

        final List<IntentFilterData> intentFilters = activityData.getIntentFilters();
        final List<String> categories = new ArrayList<>();
        for (IntentFilterData intentFilter : intentFilters) {
            categories.addAll(intentFilter.getCategories());
        }

        assertThat(categories).contains(category);
    }
}

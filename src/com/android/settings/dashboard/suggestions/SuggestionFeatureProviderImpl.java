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

package com.android.settings.dashboard.suggestions;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Settings.NightDisplaySuggestionActivity;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.fingerprint.FingerprintEnrollSuggestionActivity;
import com.android.settings.fingerprint.FingerprintSuggestionActivity;
import com.android.settings.notification.ZenOnboardingActivity;
import com.android.settings.notification.ZenSuggestionActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ScreenLockSuggestionActivity;
import com.android.settings.support.NewDeviceIntroSuggestionActivity;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settings.wifi.calling.WifiCallingSuggestionActivity;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import java.util.List;

public class SuggestionFeatureProviderImpl implements SuggestionFeatureProvider {

    private static final String TAG = "SuggestionFeature";
    private static final int EXCLUSIVE_SUGGESTION_MAX_COUNT = 3;

    private static final String SHARED_PREF_FILENAME = "suggestions";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @Override
    public boolean isSuggestionEnabled(Context context) {
        final ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return !am.isLowRamDevice();
    }

    @Override
    public ComponentName getSuggestionServiceComponent() {
        return new ComponentName(
                "com.android.settings.intelligence",
                "com.android.settings.intelligence.suggestions.SuggestionService");
    }

    @Override
    public boolean isSmartSuggestionEnabled(Context context) {
        return false;
    }

    @Override
    public boolean isSuggestionComplete(Context context, @NonNull ComponentName component) {
        final String className = component.getClassName();
        if (className.equals(WallpaperSuggestionActivity.class.getName())) {
            return WallpaperSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(FingerprintSuggestionActivity.class.getName())) {
            return FingerprintSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(FingerprintEnrollSuggestionActivity.class.getName())) {
            return FingerprintEnrollSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(ScreenLockSuggestionActivity.class.getName())) {
            return ScreenLockSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(WifiCallingSuggestionActivity.class.getName())) {
            return WifiCallingSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(NightDisplaySuggestionActivity.class.getName())) {
            return NightDisplayPreferenceController.isSuggestionComplete(context);
        } else if (className.equals(NewDeviceIntroSuggestionActivity.class.getName())) {
            return NewDeviceIntroSuggestionActivity.isSuggestionComplete(context);
        } else if (className.equals(ZenSuggestionActivity.class.getName())) {
            return ZenOnboardingActivity.isSuggestionComplete(context);
        }
        return false;
    }

    @Override
    public SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
    }

    public SuggestionFeatureProviderImpl(Context context) {
        final Context appContext = context.getApplicationContext();
        mMetricsFeatureProvider = FeatureFactory.getFactory(appContext)
                .getMetricsFeatureProvider();
    }

    @Override
    public void filterExclusiveSuggestions(List<Tile> suggestions) {
        if (suggestions == null) {
            return;
        }
        for (int i = suggestions.size() - 1; i >= EXCLUSIVE_SUGGESTION_MAX_COUNT; i--) {
            Log.d(TAG, "Removing exclusive suggestion");
            suggestions.remove(i);
        }
    }

    @Override
    public void dismissSuggestion(Context context, SuggestionControllerMixin mixin,
            Suggestion suggestion) {
        if (mixin == null || suggestion == null || context == null) {
            return;
        }
        mMetricsFeatureProvider.action(
                context, MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION,
                suggestion.getId());
        mixin.dismissSuggestion(suggestion);
    }

    @Override
    public Pair<Integer, Object>[] getLoggingTaggedData(Context context) {
        final boolean isSmartSuggestionEnabled = isSmartSuggestionEnabled(context);
        return new Pair[] {Pair.create(
                MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED,
                isSmartSuggestionEnabled ? 1 : 0)};
    }
}

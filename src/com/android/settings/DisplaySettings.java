/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.AutoRotatePreferenceController;
import com.android.settings.display.CameraGesturePreferenceController;
import com.android.settings.display.DozePreferenceController;
import com.android.settings.display.FontSizePreferenceController;
import com.android.settings.display.LiftToWakePreferenceController;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ScreenSaverPreferenceController;
import com.android.settings.display.TapToWakePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.display.VrDisplayPreferenceController;
import com.android.settings.display.WallpaperPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

public class DisplaySettings extends DashboardFragment {
    private static final String TAG = "DisplaySettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_DISPLAY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (mDashboardFeatureProvider.isEnabled()) {
            return R.xml.ia_display_settings;
        } else {
            return R.xml.display_settings;
        }
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new AutoBrightnessPreferenceController(context));
        controllers.add(new AutoRotatePreferenceController(context));
        controllers.add(new CameraGesturePreferenceController(context));
        controllers.add(new DozePreferenceController(context));
        controllers.add(new FontSizePreferenceController(context));
        controllers.add(new LiftToWakePreferenceController(context));
        controllers.add(new NightDisplayPreferenceController(context));
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ScreenSaverPreferenceController(context));
        controllers.add(new TapToWakePreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context));
        controllers.add(new VrDisplayPreferenceController(context));
        controllers.add(new WallpaperPreferenceController(context));
        return controllers;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                updateSummary();
            }
        }

        private void updateSummary() {
            final long currentTimeout = Settings.System.getLong(mContext.getContentResolver(),
                    SCREEN_OFF_TIMEOUT, TimeoutPreferenceController.FALLBACK_SCREEN_TIMEOUT_VALUE);
            final CharSequence[] entries =
                    mContext.getResources().getTextArray(R.array.screen_timeout_entries);
            final CharSequence[] values =
                    mContext.getResources().getTextArray(R.array.screen_timeout_values);
            final CharSequence timeoutDescription = TimeoutPreferenceController
                    .getTimeoutDescription(currentTimeout, entries, values);
            final String summary = timeoutDescription == null ? ""
                    : mContext.getString(R.string.display_summary, timeoutDescription);
            mLoader.setSummary(this, summary);
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    final boolean useNewIA = FeatureFactory.getFactory(context)
                            .getDashboardFeatureProvider(context)
                            .isEnabled();
                    if (useNewIA) {
                        sir.xmlResId = R.xml.ia_display_settings;
                    } else {
                        sir.xmlResId = R.xml.display_settings;
                    }
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<>();

                    new AutoBrightnessPreferenceController(context).updateNonIndexableKeys(result);
                    new AutoRotatePreferenceController(context).updateNonIndexableKeys(result);
                    new CameraGesturePreferenceController(context).updateNonIndexableKeys(result);
                    new DozePreferenceController(context).updateNonIndexableKeys(result);
                    new FontSizePreferenceController(context).updateNonIndexableKeys(result);
                    new LiftToWakePreferenceController(context).updateNonIndexableKeys(result);
                    new NightDisplayPreferenceController(context).updateNonIndexableKeys(result);
                    new NightModePreferenceController(context).updateNonIndexableKeys(result);
                    new ScreenSaverPreferenceController(context).updateNonIndexableKeys(result);
                    new TapToWakePreferenceController(context).updateNonIndexableKeys(result);
                    new TimeoutPreferenceController(context).updateNonIndexableKeys(result);
                    new VrDisplayPreferenceController(context).updateNonIndexableKeys(result);
                    new WallpaperPreferenceController(context).updateNonIndexableKeys(result);

                    return result;
                }
            };
}

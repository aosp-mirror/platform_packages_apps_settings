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
package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.assist.DefaultAssistPreferenceController;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPaymentSettingsPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultAppSettings extends DashboardFragment {

    static final String TAG = "DefaultAppSettings";

    private static final String KEY_ASSIST_VOICE_INPUT = "assist_and_voice_input";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_default_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_ADVANCED;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new DefaultAssistPreferenceController(context, KEY_ASSIST_VOICE_INPUT,
                false /* showSetting */));
        controllers.add(new DefaultBrowserPreferenceController(context));
        controllers.add(new DefaultWorkBrowserPreferenceController(context));
        controllers.add(new DefaultPhonePreferenceController(context));
        controllers.add(new DefaultWorkPhonePreferenceController(context));
        controllers.add(new DefaultSmsPreferenceController(context));
        controllers.add(new DefaultEmergencyPreferenceController(context));
        controllers.add(new DefaultHomePreferenceController(context));
        controllers.add(new DefaultPaymentSettingsPreferenceController(context));
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.app_default_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_ASSIST_VOICE_INPUT);
                    // TODO (b/38230148) Remove these keys when we can differentiate work results
                    keys.add((new DefaultWorkPhonePreferenceController(context))
                            .getPreferenceKey());
                    keys.add((new DefaultWorkBrowserPreferenceController(context))
                            .getPreferenceKey());
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };

    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final DefaultSmsPreferenceController mDefaultSmsPreferenceController;
        private final DefaultBrowserPreferenceController mDefaultBrowserPreferenceController;
        private final DefaultPhonePreferenceController mDefaultPhonePreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mDefaultSmsPreferenceController = new DefaultSmsPreferenceController(mContext);
            mDefaultBrowserPreferenceController = new DefaultBrowserPreferenceController(mContext);
            mDefaultPhonePreferenceController = new DefaultPhonePreferenceController(mContext);
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            CharSequence summary = concatSummaryText(
                    mDefaultSmsPreferenceController.getDefaultAppLabel(),
                    mDefaultBrowserPreferenceController.getDefaultAppLabel());
            summary = concatSummaryText(summary,
                    mDefaultPhonePreferenceController.getDefaultAppLabel());
            if (!TextUtils.isEmpty(summary)) {
                mSummaryLoader.setSummary(this, summary);
            }
        }

        private CharSequence concatSummaryText(CharSequence summary1, CharSequence summary2) {
            if (TextUtils.isEmpty(summary1)) {
                return summary2;
            }
            if (TextUtils.isEmpty(summary2)) {
                return summary1;
            }
            return mContext.getString(R.string.join_many_items_middle, summary1, summary2);
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY =
            new SummaryLoader.SummaryProviderFactory() {
                @Override
                public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                        SummaryLoader summaryLoader) {
                    return new DefaultAppSettings.SummaryProvider(activity, summaryLoader);
                }
            };
}

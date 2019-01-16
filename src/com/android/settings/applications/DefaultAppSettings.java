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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.applications.assist.DefaultAssistPreferenceController;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPaymentSettingsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkPhonePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class DefaultAppSettings extends DashboardFragment {

    static final String TAG = "DefaultAppSettings";

    private static final String KEY_DEFAULT_WORK_CATEGORY = "work_app_defaults";
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_ADVANCED;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final List<AbstractPreferenceController> workControllers = new ArrayList<>();
        workControllers.add(new DefaultWorkPhonePreferenceController(context));
        workControllers.add(new DefaultWorkBrowserPreferenceController(context));
        controllers.addAll(workControllers);
        controllers.add(new PreferenceCategoryController(
                context, KEY_DEFAULT_WORK_CATEGORY).setChildren(workControllers));
        controllers.add(new DefaultAssistPreferenceController(context, KEY_ASSIST_VOICE_INPUT,
                false /* showSetting */));
        controllers.add(new DefaultBrowserPreferenceController(context));
        controllers.add(new DefaultPhonePreferenceController(context));
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
                public List<AbstractPreferenceController> createPreferenceControllers(
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
            final List<CharSequence> summaries = new ArrayList<>();
            if(!TextUtils.isEmpty(mDefaultBrowserPreferenceController.getDefaultAppLabel())) {
                summaries.add(mDefaultBrowserPreferenceController.getDefaultAppLabel());
            }
            if(!TextUtils.isEmpty(mDefaultPhonePreferenceController.getDefaultAppLabel())) {
                summaries.add(mDefaultPhonePreferenceController.getDefaultAppLabel());
            }
            if(!TextUtils.isEmpty(mDefaultSmsPreferenceController.getDefaultAppLabel())) {
                summaries.add(mDefaultSmsPreferenceController.getDefaultAppLabel());
            }

            CharSequence summary = ListFormatter.getInstance().format(summaries);
            if (!TextUtils.isEmpty(summary)) {
                mSummaryLoader.setSummary(this, summary);
            }
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

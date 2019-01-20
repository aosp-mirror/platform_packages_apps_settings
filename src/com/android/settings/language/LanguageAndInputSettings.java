/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.language;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.inputmethod.PhysicalKeyboardPreferenceController;
import com.android.settings.inputmethod.SpellCheckerPreferenceController;
import com.android.settings.inputmethod.VirtualKeyboardPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class LanguageAndInputSettings extends DashboardFragment {

    private static final String TAG = "LangAndInputSettings";

    private static final String KEY_KEYBOARDS_CATEGORY = "keyboards_category";
    private static final String KEY_TEXT_TO_SPEECH = "tts_settings_summary";
    private static final String KEY_POINTER_AND_TTS_CATEGORY = "pointer_and_tts_category";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_LANGUAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hack to update action bar title. It's necessary to refresh title because this page user
        // can change locale from here and fragment won't relaunch. Once language changes, title
        // must display in the new language.
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.setTitle(R.string.language_settings);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.language_and_input;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        // Language
        controllers.add(new PhoneLanguagePreferenceController(context));

        // Input
        final VirtualKeyboardPreferenceController virtualKeyboardPreferenceController =
                new VirtualKeyboardPreferenceController(context);
        final PhysicalKeyboardPreferenceController physicalKeyboardPreferenceController =
                new PhysicalKeyboardPreferenceController(context, lifecycle);
        controllers.add(virtualKeyboardPreferenceController);
        controllers.add(physicalKeyboardPreferenceController);
        controllers.add(new PreferenceCategoryController(context,
                KEY_KEYBOARDS_CATEGORY).setChildren(
                Arrays.asList(virtualKeyboardPreferenceController,
                        physicalKeyboardPreferenceController)));

        // Pointer and Tts
        final TtsPreferenceController ttsPreferenceController =
                new TtsPreferenceController(context, KEY_TEXT_TO_SPEECH);
        controllers.add(ttsPreferenceController);
        final PointerSpeedController pointerController = new PointerSpeedController(context);
        controllers.add(pointerController);
        controllers.add(new PreferenceCategoryController(context,
                KEY_POINTER_AND_TTS_CATEGORY).setChildren(
                Arrays.asList(pointerController, ttsPreferenceController)));

        // Input Assistance
        controllers.add(new SpellCheckerPreferenceController(context));

        return controllers;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            final ContentResolver contentResolver = mContext.getContentResolver();
            if (listening) {
                final String flattenComponent = Settings.Secure.getString(
                        contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD);
                if (!TextUtils.isEmpty(flattenComponent)) {
                    final PackageManager packageManage = mContext.getPackageManager();
                    final String pkg = ComponentName.unflattenFromString(flattenComponent)
                            .getPackageName();
                    final InputMethodManager imm = (InputMethodManager)
                            mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    final List<InputMethodInfo> imis = imm.getInputMethodList();
                    for (InputMethodInfo imi : imis) {
                        if (TextUtils.equals(imi.getPackageName(), pkg)) {
                            mSummaryLoader.setSummary(this, imi.loadLabel(packageManage));
                            return;
                        }
                    }
                }
                mSummaryLoader.setSummary(this, "");
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = (activity, summaryLoader) -> new SummaryProvider(activity, summaryLoader);

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.language_and_input;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}

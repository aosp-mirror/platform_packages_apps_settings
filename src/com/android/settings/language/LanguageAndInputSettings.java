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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.speech.tts.TtsEngines;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAutofillPreferenceController;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.gestures.AssistGesturePreferenceController;
import com.android.settings.gestures.DoubleTapPowerPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.DoubleTwistPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.gestures.SwipeToNotificationPreferenceController;
import com.android.settings.inputmethod.GameControllerPreferenceController;
import com.android.settings.inputmethod.PhysicalKeyboardPreferenceController;
import com.android.settings.inputmethod.SpellCheckerPreferenceController;
import com.android.settings.inputmethod.VirtualKeyboardPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LanguageAndInputSettings extends DashboardFragment {

    private static final String TAG = "LangAndInputSettings";

    private static final String KEY_TEXT_TO_SPEECH = "tts_settings_summary";
    private static final String KEY_ASSIST = "gesture_assist_input_summary";
    private static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_input_summary";
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power_input_summary";
    private static final String KEY_DOUBLE_TWIST = "gesture_double_twist_input_summary";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen_input_summary";
    private static final String KEY_PICK_UP = "gesture_pick_up_input_summary";

    private AmbientDisplayConfiguration mAmbientDisplayConfig;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_LANGUAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(2);

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
        activity.setTitle(R.string.language_input_gesture_title);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.language_and_input;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        if (mAmbientDisplayConfig == null) {
            mAmbientDisplayConfig = new AmbientDisplayConfiguration(context);
        }

        return buildPreferenceControllers(context, getLifecycle(), mAmbientDisplayConfig);
    }

    private static List<PreferenceController> buildPreferenceControllers(@NonNull Context context,
            @Nullable Lifecycle lifecycle,
            @NonNull AmbientDisplayConfiguration ambientDisplayConfiguration) {
        final List<PreferenceController> controllers = new ArrayList<>();
        // Language
        controllers.add(new PhoneLanguagePreferenceController(context));
        controllers.add(new SpellCheckerPreferenceController(context));
        controllers.add(new UserDictionaryPreferenceController(context));
        controllers.add(new TtsPreferenceController(context, new TtsEngines(context)));
        // Input
        controllers.add(new VirtualKeyboardPreferenceController(context));
        controllers.add(new PhysicalKeyboardPreferenceController(context, lifecycle));
        final GameControllerPreferenceController gameControllerPreferenceController
                = new GameControllerPreferenceController(context);
        if (lifecycle != null) {
            lifecycle.addObserver(gameControllerPreferenceController);
        }

        controllers.add(gameControllerPreferenceController);
        // Gestures
        controllers.add(new AssistGesturePreferenceController(context, lifecycle, KEY_ASSIST,
                false /* assistOnly */));
        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle,
                KEY_SWIPE_DOWN));
        controllers.add(new DoubleTwistPreferenceController(context, lifecycle, KEY_DOUBLE_TWIST));
        controllers.add(new DoubleTapPowerPreferenceController(context, lifecycle,
                KEY_DOUBLE_TAP_POWER));
        controllers.add(new PickupGesturePreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_PICK_UP));
        controllers.add(new DoubleTapScreenPreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_DOUBLE_TAP_SCREEN));
        controllers.add(new DefaultAutofillPreferenceController(context));
        return controllers;
    }

    @VisibleForTesting
    void setAmbientDisplayConfig(AmbientDisplayConfiguration ambientConfig) {
        mAmbientDisplayConfig = ambientConfig;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final AssistGestureFeatureProvider mFeatureProvider;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
        }

        @Override
        public void setListening(boolean listening) {
            final ContentResolver contentResolver = mContext.getContentResolver();
            if (listening) {
                if (mFeatureProvider.isSensorAvailable(mContext)) {
                    final boolean assistGestureEnabled = Settings.Secure.getInt(
                            contentResolver, Settings.Secure.ASSIST_GESTURE_ENABLED, 1) != 0;
                    final boolean assistGestureSilenceEnabled = Settings.Secure.getInt(
                            contentResolver, Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED,
                            1) != 0;
                    String summary;
                    if (mFeatureProvider.isSupported(mContext) && assistGestureEnabled) {
                        summary = mContext.getString(
                                R.string.language_input_gesture_summary_on_with_assist);
                    } else if (assistGestureSilenceEnabled) {
                        summary = mContext.getString(
                                R.string.language_input_gesture_summary_on_non_assist);
                    } else {
                        summary = mContext.getString(R.string.language_input_gesture_summary_off);
                    }
                    mSummaryLoader.setSummary(this, summary);
                } else {
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
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null,
                            new AmbientDisplayConfiguration(context));
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Duplicates in summary and details pages.
                    keys.add(KEY_TEXT_TO_SPEECH);
                    keys.add(KEY_ASSIST);
                    keys.add(KEY_SWIPE_DOWN);
                    keys.add(KEY_DOUBLE_TAP_POWER);
                    keys.add(KEY_DOUBLE_TWIST);
                    keys.add(KEY_DOUBLE_TAP_SCREEN);
                    keys.add(KEY_PICK_UP);

                    return keys;
                }
            };
}

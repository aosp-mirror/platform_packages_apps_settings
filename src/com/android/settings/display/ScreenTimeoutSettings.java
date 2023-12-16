/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.app.admin.DevicePolicyResources.Strings.Settings.OTHER_OPTIONS_DISABLED_BY_ADMIN;
import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.SensorPrivacyManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that is used to control screen timeout.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ScreenTimeoutSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {
    private static final String TAG = "ScreenTimeout";
    /** If there is no setting in the provider, use this. */
    public static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final int DEFAULT_ORDER_OF_LOWEST_PREFERENCE = Integer.MAX_VALUE - 1;

    private CharSequence[] mInitialEntries;
    private CharSequence[] mInitialValues;
    private FooterPreference mPrivacyPreference;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private SensorPrivacyManager mPrivacyManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAdaptiveSleepBatterySaverPreferenceController.updateVisibility();
            mAdaptiveSleepController.updatePreference();
        }
    };

    private DevicePolicyManager mDevicePolicyManager;
    private SensorPrivacyManager.OnSensorPrivacyChangedListener mPrivacyChangedListener;

    @VisibleForTesting
    Context mContext;

    @VisibleForTesting
    RestrictedLockUtils.EnforcedAdmin mAdmin;

    @VisibleForTesting
    FooterPreference mDisableOptionsPreference;

    @VisibleForTesting
    FooterPreference mPowerConsumptionPreference;

    @VisibleForTesting
    AdaptiveSleepPermissionPreferenceController mAdaptiveSleepPermissionController;

    @VisibleForTesting
    AdaptiveSleepCameraStatePreferenceController mAdaptiveSleepCameraStatePreferenceController;

    @VisibleForTesting
    AdaptiveSleepPreferenceController mAdaptiveSleepController;

    @VisibleForTesting
    AdaptiveSleepBatterySaverPreferenceController mAdaptiveSleepBatterySaverPreferenceController;

    public ScreenTimeoutSettings() {
        super();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mDevicePolicyManager = mContext.getSystemService(DevicePolicyManager.class);
        mInitialEntries = getResources().getStringArray(R.array.screen_timeout_entries);
        mInitialValues = getResources().getStringArray(R.array.screen_timeout_values);
        mAdaptiveSleepController = new AdaptiveSleepPreferenceController(context);
        mAdaptiveSleepPermissionController = new AdaptiveSleepPermissionPreferenceController(
                context);
        mAdaptiveSleepCameraStatePreferenceController =
                new AdaptiveSleepCameraStatePreferenceController(context, getLifecycle());
        mAdaptiveSleepBatterySaverPreferenceController =
                new AdaptiveSleepBatterySaverPreferenceController(context);
        mPrivacyPreference = new FooterPreference(context);
        mPrivacyPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        mPrivacyPreference.setTitle(R.string.adaptive_sleep_privacy);
        mPrivacyPreference.setSelectable(false);
        mPrivacyPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPrivacyChangedListener = (sensor, enabled) -> mAdaptiveSleepController.updatePreference();
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();
        final long maxTimeout = getMaxScreenTimeout(getContext());
        if (mInitialValues != null) {
            for (int i = 0; i < mInitialValues.length; ++i) {
                if (Long.parseLong(mInitialValues[i].toString()) <= maxTimeout) {
                    candidates.add(new TimeoutCandidateInfo(mInitialEntries[i],
                            mInitialValues[i].toString(), true));
                }
            }
        } else {
            Log.e(TAG, "Screen timeout options do not exist.");
        }
        return candidates;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdaptiveSleepPermissionController.updateVisibility();
        mAdaptiveSleepCameraStatePreferenceController.updateVisibility();
        mAdaptiveSleepBatterySaverPreferenceController.updateVisibility();
        mAdaptiveSleepController.updatePreference();
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        mPrivacyManager.addSensorPrivacyListener(CAMERA, mPrivacyChangedListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(mReceiver);
        mPrivacyManager.removeSensorPrivacyListener(CAMERA, mPrivacyChangedListener);
    }

    @Override
    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }

        for (CandidateInfo info : candidateList) {
            SelectorWithWidgetPreference pref =
                    new SelectorWithWidgetPreference(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            screen.addPreference(pref);
        }

        final long selectedTimeout = Long.parseLong(defaultKey);
        final long maxTimeout = getMaxScreenTimeout(getContext());
        if (!candidateList.isEmpty() && (selectedTimeout > maxTimeout)) {
            // The selected time out value is longer than the max timeout allowed by the admin.
            // Select the largest value from the list by default.
            final SelectorWithWidgetPreference preferenceWithLargestTimeout =
                    (SelectorWithWidgetPreference) screen.getPreference(candidateList.size() - 1);
            preferenceWithLargestTimeout.setChecked(true);
        }

        mPrivacyPreference = new FooterPreference(mContext);
        mPrivacyPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        mPrivacyPreference.setTitle(R.string.adaptive_sleep_privacy);
        mPrivacyPreference.setSelectable(false);
        mPrivacyPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);

        if (isScreenAttentionAvailable(getContext())) {
            mAdaptiveSleepPermissionController.addToScreen(screen);
            mAdaptiveSleepCameraStatePreferenceController.addToScreen(screen);
            mAdaptiveSleepController.addToScreen(screen);
            mAdaptiveSleepBatterySaverPreferenceController.addToScreen(screen);
            screen.addPreference(mPrivacyPreference);
        }

        if (mAdmin != null) {
            setupDisabledFooterPreference();
            screen.addPreference(mDisableOptionsPreference);
        } else {
            setupPowerConsumptionFooterPreference();
            screen.addPreference(mPowerConsumptionPreference);
        }
    }

    @VisibleForTesting
    void setupDisabledFooterPreference() {
        final String textDisabledByAdmin = mDevicePolicyManager.getResources().getString(
                OTHER_OPTIONS_DISABLED_BY_ADMIN, () -> getResources().getString(
                        R.string.admin_disabled_other_options));
        final String textMoreDetails = getResources().getString(R.string.admin_more_details);

        mDisableOptionsPreference = new FooterPreference(getContext());
        mDisableOptionsPreference.setTitle(textDisabledByAdmin);
        mDisableOptionsPreference.setSelectable(false);
        mDisableOptionsPreference.setLearnMoreText(textMoreDetails);
        mDisableOptionsPreference.setLearnMoreAction(v -> {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), mAdmin);
        });
        mDisableOptionsPreference.setIcon(R.drawable.ic_info_outline_24dp);

        // The 'disabled by admin' preference should always be at the end of the setting page.
        mPrivacyPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE - 1);
        mDisableOptionsPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE);
    }

    @VisibleForTesting
    void setupPowerConsumptionFooterPreference() {
        mPowerConsumptionPreference = new FooterPreference(getContext());
        mPowerConsumptionPreference.setTitle(R.string.power_consumption_footer_summary);
        mPowerConsumptionPreference.setSelectable(false);
        mPowerConsumptionPreference.setIcon(R.drawable.ic_info_outline_24dp);

        // The 'Longer screen timeout' preference should always be at the end of the setting page.
        mPrivacyPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE - 1);
        mPowerConsumptionPreference.setOrder(DEFAULT_ORDER_OF_LOWEST_PREFERENCE);
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentSystemScreenTimeout(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        setCurrentSystemScreenTimeout(getContext(), key);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_TIMEOUT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_timeout_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    private Long getMaxScreenTimeout(Context context) {
        if (context == null) {
            return Long.MAX_VALUE;
        }
        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return Long.MAX_VALUE;
        }
        mAdmin = RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(context);
        if (mAdmin != null) {
            return dpm.getMaximumTimeToLock(null /* admin */, UserHandle.myUserId());
        }
        return Long.MAX_VALUE;
    }

    private String getCurrentSystemScreenTimeout(Context context) {
        if (context == null) {
            return Long.toString(FALLBACK_SCREEN_TIMEOUT_VALUE);
        } else {
            return Long.toString(Settings.System.getLong(context.getContentResolver(),
                    SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
        }
    }

    private void setCurrentSystemScreenTimeout(Context context, String key) {
        try {
            if (context != null) {
                final long value = Long.parseLong(key);
                mMetricsFeatureProvider.action(context, SettingsEnums.ACTION_SCREEN_TIMEOUT_CHANGED,
                        (int) value);
                Settings.System.putLong(context.getContentResolver(), SCREEN_OFF_TIMEOUT, value);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
    }

    private static boolean isScreenAttentionAvailable(Context context) {
        return AdaptiveSleepPreferenceController.isAdaptiveSleepSupported(context);
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_timeout_settings) {
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    if (!isScreenAttentionAvailable(context)) {
                        return null;
                    }
                    final Resources res = context.getResources();
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.adaptive_sleep_title);
                    data.key = AdaptiveSleepPreferenceController.PREFERENCE_KEY;
                    data.keywords = res.getString(R.string.adaptive_sleep_title);

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }
            };
}

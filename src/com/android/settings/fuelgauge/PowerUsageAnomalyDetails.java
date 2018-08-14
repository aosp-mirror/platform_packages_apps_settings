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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.IconDrawableFactory;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment;
import com.android.settings.fuelgauge.anomaly.AnomalyPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * Fragment to show a list of anomaly apps, where user could handle these anomalies
 */
public class PowerUsageAnomalyDetails extends DashboardFragment implements
        AnomalyDialogFragment.AnomalyDialogListener {

    public static final String TAG = "PowerAbnormalUsageDetail";
    @VisibleForTesting
    static final String EXTRA_ANOMALY_LIST = "anomaly_list";
    private static final int REQUEST_ANOMALY_ACTION = 0;
    private static final String KEY_PREF_ANOMALY_LIST = "app_abnormal_list";

    @VisibleForTesting
    List<Anomaly> mAnomalies;
    @VisibleForTesting
    PreferenceGroup mAbnormalListGroup;
    @VisibleForTesting
    PackageManager mPackageManager;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    IconDrawableFactory mIconDrawableFactory;

    public static void startBatteryAbnormalPage(SettingsActivity caller,
            InstrumentedPreferenceFragment fragment, List<Anomaly> anomalies) {
        Bundle args = new Bundle();
        args.putParcelableList(EXTRA_ANOMALY_LIST, anomalies);

        new SubSettingLauncher(caller)
                .setDestination(PowerUsageAnomalyDetails.class.getName())
                .setTitle(R.string.battery_abnormal_details_title)
                .setArguments(args)
                .setSourceMetricsCategory(fragment.getMetricsCategory())
                .launch();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getContext();

        mAnomalies = getArguments().getParcelableArrayList(EXTRA_ANOMALY_LIST);
        mAbnormalListGroup = (PreferenceGroup) findPreference(KEY_PREF_ANOMALY_LIST);
        mPackageManager = context.getPackageManager();
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshUi();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof AnomalyPreference) {
            AnomalyPreference anomalyPreference = (AnomalyPreference) preference;
            final Anomaly anomaly = anomalyPreference.getAnomaly();

            AnomalyDialogFragment dialogFragment = AnomalyDialogFragment.newInstance(anomaly,
                    MetricsProto.MetricsEvent.FUELGAUGE_ANOMALY_DETAIL);
            dialogFragment.setTargetFragment(this, REQUEST_ANOMALY_ACTION);
            dialogFragment.show(getFragmentManager(), TAG);

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_abnormal_detail;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FUELGAUGE_ANOMALY_DETAIL;
    }

    void refreshUi() {
        mAbnormalListGroup.removeAll();
        for (int i = 0, size = mAnomalies.size(); i < size; i++) {
            final Anomaly anomaly = mAnomalies.get(i);
            Preference pref = new AnomalyPreference(getPrefContext(), anomaly);
            pref.setSummary(mBatteryUtils.getSummaryResIdFromAnomalyType(anomaly.type));
            Drawable icon = getBadgedIcon(anomaly.packageName, UserHandle.getUserId(anomaly.uid));
            if (icon != null) {
                pref.setIcon(icon);
            }

            mAbnormalListGroup.addPreference(pref);
        }
    }

    @Override
    public void onAnomalyHandled(Anomaly anomaly) {
        mAnomalies.remove(anomaly);
        refreshUi();
    }

    @VisibleForTesting
    Drawable getBadgedIcon(String packageName, int userId) {
        return Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager, packageName, userId);
    }
}

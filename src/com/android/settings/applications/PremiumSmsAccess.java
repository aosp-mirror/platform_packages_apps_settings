/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.annotation.Nullable;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.settings.DividerPreference;
import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.applications.AppStateSmsPremBridge.SmsState;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;

public class PremiumSmsAccess extends EmptyTextSettings implements Callback, Callbacks, OnPreferenceChangeListener {

    private ApplicationsState mApplicationsState;
    private AppStateSmsPremBridge mSmsBackend;
    private Session mSession;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mApplicationsState = ApplicationsState.getInstance((Application)
                getContext().getApplicationContext());
        mSession = mApplicationsState.newSession(this);
        mSmsBackend = new AppStateSmsPremBridge(getContext(), mApplicationsState, this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        mSmsBackend.resume();
    }

    @Override
    public void onPause() {
        mSmsBackend.pause();
        mSession.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mSmsBackend.release();
        mSession.release();
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PREMIUM_SMS_ACCESS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PremiumSmsPreference pref = (PremiumSmsPreference) preference;
        int smsState = Integer.parseInt((String) newValue);
        logSpecialPermissionChange(smsState, pref.mAppEntry.info.packageName);
        mSmsBackend.setSmsState(pref.mAppEntry.info.packageName, smsState);
        return true;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(int smsState, String packageName) {
        int category = SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN;
        switch (smsState) {
            case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER:
                category = MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ASK;
                break;
            case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW:
                category = MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_DENY;
                break;
            case SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW:
                category = MetricsProto.MetricsEvent.
                        APP_SPECIAL_PERMISSION_PREMIUM_SMS_ALWAYS_ALLOW;
                break;
        }
        if (category != SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN) {
            FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(
                    getContext(), category, packageName);
        }
    }

    private void updatePrefs(ArrayList<AppEntry> apps) {
        if (apps == null) return;
        setEmptyText(R.string.premium_sms_none);
        setLoading(false, true);
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(
                getPrefContext());
        screen.setOrderingAsAdded(true);

        for (int i = 0; i < apps.size(); i++) {
            final PremiumSmsPreference smsPreference =
                    new PremiumSmsPreference(apps.get(i), getPrefContext());
            smsPreference.setOnPreferenceChangeListener(this);
            screen.addPreference(smsPreference);
        }
        if (apps.size() != 0) {
            DividerPreference summary = new DividerPreference(getPrefContext());
            summary.setSelectable(false);
            summary.setSummary(R.string.premium_sms_warning);
            summary.setDividerAllowedAbove(true);
            screen.addPreference(summary);
        }

        setPreferenceScreen(screen);
    }

    private void update() {
        updatePrefs(mSession.rebuild(AppStateSmsPremBridge.FILTER_APP_PREMIUM_SMS,
                ApplicationsState.ALPHA_COMPARATOR));
    }

    @Override
    public void onExtraInfoUpdated() {
        update();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        updatePrefs(apps);
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }

    @Override
    public void onPackageIconChanged() {

    }

    @Override
    public void onPackageSizeChanged(String packageName) {

    }

    @Override
    public void onAllSizesComputed() {

    }

    @Override
    public void onLauncherInfoChanged() {

    }

    @Override
    public void onLoadEntriesCompleted() {

    }

    private class PremiumSmsPreference extends DropDownPreference {
        private final AppEntry mAppEntry;

        public PremiumSmsPreference(AppEntry appEntry, Context context) {
            super(context);
            mAppEntry = appEntry;
            mAppEntry.ensureLabel(context);
            setTitle(mAppEntry.label);
            if (mAppEntry.icon != null) {
                setIcon(mAppEntry.icon);
            }
            setEntries(R.array.security_settings_premium_sms_values);
            setEntryValues(new CharSequence[] {
                    String.valueOf(SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ASK_USER),
                    String.valueOf(SmsUsageMonitor.PREMIUM_SMS_PERMISSION_NEVER_ALLOW),
                    String.valueOf(SmsUsageMonitor.PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW),
            });
            setValue(String.valueOf(getCurrentValue()));
            setSummary("%s");
        }

        private int getCurrentValue() {
            return mAppEntry.extraInfo instanceof SmsState
                    ? ((SmsState) mAppEntry.extraInfo).smsState
                    : SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            if (getIcon() == null) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        mApplicationsState.ensureIcon(mAppEntry);
                        setIcon(mAppEntry.icon);
                    }
                });
            }
            super.onBindViewHolder(holder);
        }
    }
}

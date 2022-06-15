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

package com.android.settings.applications.specialaccess.premiumsms;

import android.annotation.Nullable;
import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.applications.AppStateSmsPremBridge;
import com.android.settings.applications.AppStateSmsPremBridge.SmsState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;

@SearchIndexable
public class PremiumSmsAccess extends EmptyTextSettings
        implements Callback, Callbacks, OnPreferenceChangeListener {

    private ApplicationsState mApplicationsState;
    private AppStateSmsPremBridge mSmsBackend;
    private Session mSession;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mApplicationsState = ApplicationsState.getInstance((Application)
                getContext().getApplicationContext());
        mSession = mApplicationsState.newSession(this, getSettingsLifecycle());
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
        mSmsBackend.resume();
    }

    @Override
    public void onPause() {
        mSmsBackend.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mSmsBackend.release();
        super.onDestroy();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.premium_sms_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PREMIUM_SMS_ACCESS;
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
        int category = SmsManager.PREMIUM_SMS_CONSENT_UNKNOWN;
        switch (smsState) {
            case SmsManager.PREMIUM_SMS_CONSENT_ASK_USER:
                category = SettingsEnums.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ASK;
                break;
            case SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW:
                category = SettingsEnums.APP_SPECIAL_PERMISSION_PREMIUM_SMS_DENY;
                break;
            case SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW:
                category = SettingsEnums.
                        APP_SPECIAL_PERMISSION_PREMIUM_SMS_ALWAYS_ALLOW;
                break;
        }
        if (category != SmsManager.PREMIUM_SMS_CONSENT_UNKNOWN) {
            // TODO(117860032): Category is wrong. It should be defined in SettingsEnums.
            final MetricsFeatureProvider metricsFeatureProvider =
                    FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider();
            metricsFeatureProvider.action(
                    metricsFeatureProvider.getAttribution(getActivity()),
                    category,
                    getMetricsCategory(),
                    packageName,
                    smsState);
        }
    }

    private void updatePrefs(ArrayList<AppEntry> apps) {
        if (apps == null) return;
        setEmptyText(R.string.premium_sms_none);
        setLoading(false, true);
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        screen.setOrderingAsAdded(true);

        for (int i = 0; i < apps.size(); i++) {
            final PremiumSmsPreference smsPreference =
                    new PremiumSmsPreference(apps.get(i), getPrefContext());
            smsPreference.setOnPreferenceChangeListener(this);
            screen.addPreference(smsPreference);
        }
        if (apps.size() != 0) {
            FooterPreference footer = new FooterPreference(getPrefContext());
            footer.setTitle(R.string.premium_sms_warning);
            screen.addPreference(footer);
        }
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
            setEntryValues(new CharSequence[]{
                    String.valueOf(SmsManager.PREMIUM_SMS_CONSENT_ASK_USER),
                    String.valueOf(SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW),
                    String.valueOf(SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW),
            });
            setValue(String.valueOf(getCurrentValue()));
            setSummary("%s");
        }

        private int getCurrentValue() {
            return mAppEntry.extraInfo instanceof SmsState
                    ? ((SmsState) mAppEntry.extraInfo).smsState
                    : SmsManager.PREMIUM_SMS_CONSENT_UNKNOWN;
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.premium_sms_settings);
}

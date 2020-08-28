/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.service.notification.NotificationAssistantService;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.applications.ServiceListing;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class NotificationAssistantPicker extends DefaultAppPickerFragment implements
        ServiceListing.Callback {

    private static final String TAG = "NotiAssistantPicker";

    @VisibleForTesting
    protected NotificationBackend mNotificationBackend;
    private List<CandidateInfo> mCandidateInfos = new ArrayList<>();
    @VisibleForTesting
    protected Context mContext;
    private ServiceListing mServiceListing;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mNotificationBackend = new NotificationBackend();
        mServiceListing = new ServiceListing.Builder(context)
                .setTag(TAG)
                .setSetting(Settings.Secure.ENABLED_NOTIFICATION_ASSISTANT)
                .setIntentAction(NotificationAssistantService.SERVICE_INTERFACE)
                .setPermission(android.Manifest.permission.BIND_NOTIFICATION_ASSISTANT_SERVICE)
                .setNoun("notification assistant")
                .build();
        mServiceListing.addCallback(this);
        mServiceListing.reload();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mServiceListing.removeCallback(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_assistant_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        return mCandidateInfos;
    }

    @Override
    protected String getDefaultKey() {
        ComponentName cn = mNotificationBackend.getAllowedNotificationAssistant();
        return (cn != null) ? cn.flattenToString() : "";
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return mNotificationBackend.setNotificationAssistantGranted(
                ComponentName.unflattenFromString(key));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_NOTIFICATION_ASSISTANT;
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo info) {
        if (TextUtils.isEmpty(info.getKey())) {
            return null;
        }
        return mContext.getString(R.string.notification_assistant_security_warning_summary,
                info.loadLabel());
    }

    @Override
    public void onServicesReloaded(List<ServiceInfo> services) {
        List<CandidateInfo> list = new ArrayList<>();
        services.sort(new PackageItemInfo.DisplayNameComparator(mPm));
        for (ServiceInfo service : services) {
            if (mContext.getPackageManager().checkPermission(
                    android.Manifest.permission.REQUEST_NOTIFICATION_ASSISTANT_SERVICE,
                    service.packageName) == PackageManager.PERMISSION_GRANTED) {
                final ComponentName cn = new ComponentName(service.packageName, service.name);
                list.add(new DefaultAppInfo(mContext, mPm, mUserId, cn));
            }
        }
        list.add(new CandidateNone(mContext));
        mCandidateInfos = list;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.notification_assistant_settings);

    public static class CandidateNone extends CandidateInfo {

        public Context mContext;

        public CandidateNone(Context context) {
            super(true);
            mContext = context;
        }

        @Override
        public CharSequence loadLabel() {
            return mContext.getString(R.string.no_notification_assistant);
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return "";
        }
    }
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.mobilebundledapps;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.mobilebundledapps.ApplicationMetadataUtils.MbaDeveloper;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * A fragment for retrieving the transparency metadata and PSL in the in-APK XML file and displaying
 * them.
 */
public class MobileBundledAppsDetailsFragment extends AppInfoWithHeader {
    private static final String METADATA_PREF_KEY = "metadata";

    protected PackageManager mPackageManager;
    private Context mContext;
    private LayoutPreference mMetadataPreferenceView;
    private ApplicationsState mApplicationState;
    private boolean mCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPackageManager = mContext.getPackageManager();
        addPreferencesFromResource(R.xml.mobile_bundled_apps_details_preference);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mCreated) {
            return;
        }
        super.onActivityCreated(savedInstanceState);
        final ApplicationMetadataUtils appUtil = ApplicationMetadataUtils.newInstance(
                mPackageManager,
                mPackageName);
        if (mAppEntry == null) {
            mApplicationState =
                    ApplicationsState.getInstance((Application) (mContext.getApplicationContext()));
            mAppEntry = mApplicationState.getEntry(mPackageName, mContext.getUserId());
        }
        mMetadataPreferenceView = findPreference(METADATA_PREF_KEY);
        createView(appUtil);
        mCreated = true;
    }

    private void createView(final ApplicationMetadataUtils appUtil) {
        final LinearLayout devListLayout =
                mMetadataPreferenceView.findViewById(R.id.developer_list);
        populateDeveloperList(appUtil.getDevelopers(), devListLayout);

        ((TextView) mMetadataPreferenceView.findViewById(R.id.contains_ads))
                .setText(Boolean.toString(appUtil.getContainsAds()));

        ((TextView) mMetadataPreferenceView.findViewById(R.id.contact_url))
                .setText(appUtil.getContactUrl());
        ((TextView) mMetadataPreferenceView.findViewById(R.id.contact_email))
                .setText(appUtil.getContactEmail());

        ((TextView) mMetadataPreferenceView.findViewById(R.id.privacy_policy_url))
                .setText(appUtil.getPrivacyPolicyUrl());

        ((TextView) mMetadataPreferenceView.findViewById(R.id.description))
                .setText(appUtil.getDescription());

        ((TextView) mMetadataPreferenceView.findViewById(R.id.category))
                .setText(appUtil.getCategoryName());
    }

    private void populateDeveloperList(List<MbaDeveloper> developersDetails, ViewGroup parent) {
        for (MbaDeveloper dev : developersDetails) {
            View itemView = LayoutInflater.from(mContext)
                    .inflate(R.layout.mobile_bundled_apps_developer_fragment_row, parent, false);

            ((TextView) itemView.findViewById(R.id.developer_name)).setText(dev.name);
            ((TextView) itemView.findViewById(R.id.developer_relationship))
                    .setText(dev.relationship);
            ((TextView) itemView.findViewById(R.id.developer_email)).setText(dev.email);
            ((TextView) itemView.findViewById(R.id.developer_country)).setText(dev.country);

            parent.addView(itemView);
        }
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    protected boolean refreshUi() {
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRANSPARENCY_METADATA;
    }

}

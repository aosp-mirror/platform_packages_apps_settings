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

package com.android.settings.applications.appinfo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.applications.PermissionsSummaryHelper;

import java.util.ArrayList;
import java.util.List;

public class AppPermissionPreferenceController extends AppInfoPreferenceControllerBase {

    private static final String TAG = "PermissionPrefControl";
    private static final String EXTRA_HIDE_INFO_BUTTON = "hideInfoButton";

    private String mPackageName;

    @VisibleForTesting
    final PermissionsSummaryHelper.PermissionsResultCallback mPermissionCallback
            = new PermissionsSummaryHelper.PermissionsResultCallback() {
        @Override
        public void onPermissionSummaryResult(int standardGrantedPermissionCount,
                int requestedPermissionCount, int additionalGrantedPermissionCount,
                List<CharSequence> grantedGroupLabels) {
            final Resources res = mContext.getResources();
            CharSequence summary;

            if (requestedPermissionCount == 0) {
                summary = res.getString(
                        R.string.runtime_permissions_summary_no_permissions_requested);
                mPreference.setEnabled(false);
            } else {
                final ArrayList<CharSequence> list = new ArrayList<>(grantedGroupLabels);
                if (additionalGrantedPermissionCount > 0) {
                    // N additional permissions.
                    list.add(res.getQuantityString(
                            R.plurals.runtime_permissions_additional_count,
                            additionalGrantedPermissionCount, additionalGrantedPermissionCount));
                }
                if (list.size() == 0) {
                    summary = res.getString(
                            R.string.runtime_permissions_summary_no_permissions_granted);
                } else {
                    summary = ListFormatter.getInstance().format(list);
                }
                mPreference.setEnabled(true);
            }
            mPreference.setSummary(summary);
        }
    };

    public AppPermissionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        PermissionsSummaryHelper.getPermissionSummary(mContext, mPackageName, mPermissionCallback);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            startManagePermissionsActivity();
            return true;
        }
        return false;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    private void startManagePermissionsActivity() {
        // start new activity to manage app permissions
        final Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mParent.getAppEntry().info.packageName);
        intent.putExtra(EXTRA_HIDE_INFO_BUTTON, true);
        try {
            mParent.getActivity().startActivityForResult(intent, mParent.SUB_INFO_FRAGMENT);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }
}

/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AppSecurityPermissions;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

public class AppPermissionSettings extends AppInfoWithHeader {

    private ISms mSmsManager;
    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSmsManager = ISms.Stub.asInterface(ServiceManager.getService("isms"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.permission_settings, container, false);

        final ViewGroup allDetails = (ViewGroup) view.findViewById(R.id.all_details);
        Utils.forceCustomPadding(allDetails, true /* additive padding */);

        mRootView = view;
        return view;
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        // Security permissions section
        LinearLayout permsView = (LinearLayout) mRootView.findViewById(R.id.permissions_section);
        AppSecurityPermissions asp = new AppSecurityPermissions(getActivity(), mPackageName);
        int premiumSmsPermission = getPremiumSmsPermission(mPackageName);
        // Premium SMS permission implies the app also has SEND_SMS permission, so the original
        // application permissions list doesn't have to be shown/hidden separately. The premium
        // SMS subsection should only be visible if the app has tried to send to a premium SMS.
        if (asp.getPermissionCount() > 0
                || premiumSmsPermission != SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN) {
            permsView.setVisibility(View.VISIBLE);
        } else {
            permsView.setVisibility(View.GONE);
        }
        // Premium SMS permission subsection
        TextView securityBillingDesc = (TextView) permsView.findViewById(
                R.id.security_settings_billing_desc);
        LinearLayout securityBillingList = (LinearLayout) permsView.findViewById(
                R.id.security_settings_billing_list);
        if (premiumSmsPermission != SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN) {
            // Show the premium SMS permission selector
            securityBillingDesc.setVisibility(View.VISIBLE);
            securityBillingList.setVisibility(View.VISIBLE);
            Spinner spinner = (Spinner) permsView.findViewById(
                    R.id.security_settings_premium_sms_list);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.security_settings_premium_sms_values,
                    android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            // List items are in the same order as SmsUsageMonitor constants, offset by 1.
            spinner.setSelection(premiumSmsPermission - 1);
            spinner.setOnItemSelectedListener(new PremiumSmsSelectionListener(
                    mPackageName, mSmsManager));
        } else {
            // Hide the premium SMS permission selector
            securityBillingDesc.setVisibility(View.GONE);
            securityBillingList.setVisibility(View.GONE);
        }
        // App permissions subsection
        if (asp.getPermissionCount() > 0) {
            // Make the security sections header visible
            LinearLayout securityList = (LinearLayout) permsView.findViewById(
                    R.id.security_settings_list);
            securityList.removeAllViews();
            securityList.addView(asp.getPermissionsViewWithRevokeButtons());
            // If this app is running under a shared user ID with other apps,
            // update the description to explain this.
            String[] packages = mPm.getPackagesForUid(mPackageInfo.applicationInfo.uid);
            if (packages != null && packages.length > 1) {
                ArrayList<CharSequence> pnames = new ArrayList<CharSequence>();
                for (int i=0; i<packages.length; i++) {
                    String pkg = packages[i];
                    if (mPackageInfo.packageName.equals(pkg)) {
                        continue;
                    }
                    try {
                        ApplicationInfo ainfo = mPm.getApplicationInfo(pkg, 0);
                        pnames.add(ainfo.loadLabel(mPm));
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                final int N = pnames.size();
                if (N > 0) {
                    final Resources res = getActivity().getResources();
                    String appListStr;
                    if (N == 1) {
                        appListStr = pnames.get(0).toString();
                    } else if (N == 2) {
                        appListStr = res.getString(R.string.join_two_items, pnames.get(0),
                                pnames.get(1));
                    } else {
                        appListStr = pnames.get(N-2).toString();
                        for (int i=N-3; i>=0; i--) {
                            appListStr = res.getString(i == 0 ? R.string.join_many_items_first
                                    : R.string.join_many_items_middle, pnames.get(i), appListStr);
                        }
                        appListStr = res.getString(R.string.join_many_items_last,
                                appListStr, pnames.get(N-1));
                    }
                    TextView descr = (TextView) mRootView.findViewById(
                            R.id.security_settings_desc);
                    descr.setText(res.getString(R.string.security_settings_desc_multi,
                            mPackageInfo.applicationInfo.loadLabel(mPm), appListStr));
                }
            }
        }
        return true;
    }

    private int getPremiumSmsPermission(String packageName) {
        try {
            if (mSmsManager != null) {
                return mSmsManager.getPremiumSmsPermission(packageName);
            }
        } catch (RemoteException ex) {
            // ignored
        }
        return SmsUsageMonitor.PREMIUM_SMS_PERMISSION_UNKNOWN;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for Permissions screen.
        return null;
    }

    public static CharSequence getSummary(AppEntry appEntry, Context context) {
        if (appEntry.info.targetSdkVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            AppPermissions appPerms = new AppPermissions(context, appEntry.info.packageName);
            int count = appPerms.getPermissionCount();
            int grantedCount = appPerms.getGrantedPermissionsCount();
            return context.getResources().getQuantityString(R.plurals.runtime_permissions_summary,
                    count, grantedCount, count);
        }
        AppSecurityPermissions asp = new AppSecurityPermissions(context,
                appEntry.info.packageName);
        int count = asp.getPermissionCount();
        return context.getResources().getQuantityString(R.plurals.permissions_summary,
                count, count);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_APP_PERMISSION;
    }

    private static class PremiumSmsSelectionListener implements AdapterView.OnItemSelectedListener {
        private final String mPackageName;
        private final ISms mSmsManager;

        PremiumSmsSelectionListener(String packageName, ISms smsManager) {
            mPackageName = packageName;
            mSmsManager = smsManager;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position,
                long id) {
            if (position >= 0 && position < 3) {
                if (localLOGV) Log.d(TAG, "Selected premium SMS policy " + position);
                setPremiumSmsPermission(mPackageName, (position + 1));
            } else {
                Log.e(TAG, "Error: unknown premium SMS policy " + position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Ignored
        }

        private void setPremiumSmsPermission(String packageName, int permission) {
            try {
                if (mSmsManager != null) {
                    mSmsManager.setPremiumSmsPermission(packageName, permission);
                }
            } catch (RemoteException ex) {
                // ignored
            }
        }
    }

}
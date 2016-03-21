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
package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.RestrictedPreference;
import com.android.settings.Utils;

import java.util.List;

import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

public class AppManagementFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "AppManagementFragment";

    private static final String ARG_PACKAGE_NAME = "package";

    private static final String KEY_VERSION = "version";
    private static final String KEY_ALWAYS_ON_VPN = "always_on_vpn";
    private static final String KEY_FORGET_VPN = "forget_vpn";

    private AppOpsManager mAppOpsManager;
    private PackageManager mPackageManager;
    private ConnectivityManager mConnectivityManager;

    // VPN app info
    private final int mUserId = UserHandle.myUserId();
    private int mPackageUid;
    private String mPackageName;
    private PackageInfo mPackageInfo;
    private String mVpnLabel;

    // UI preference
    private Preference mPreferenceVersion;
    private RestrictedSwitchPreference mPreferenceAlwaysOn;
    private RestrictedPreference mPreferenceForget;

    // Listener
    private final AppDialogFragment.Listener mForgetVpnDialogFragmentListener =
            new AppDialogFragment.Listener() {
        @Override
        public void onForget() {
            // Unset always-on-vpn when forgetting the VPN
            if (isVpnAlwaysOn()) {
                setAlwaysOnVpn(false);
            }
            // Also dismiss and go back to VPN list
            finish();
        }

        @Override
        public void onCancel() {
            // do nothing
        }
    };

    public static void show(Context context, AppPreference pref) {
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pref.getPackageName());
        Utils.startWithFragmentAsUser(context, AppManagementFragment.class.getName(), args, -1,
                pref.getLabel(), false, new UserHandle(pref.getUserId()));
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        addPreferencesFromResource(R.xml.vpn_app_management);

        mPackageManager = getContext().getPackageManager();
        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
        mConnectivityManager = getContext().getSystemService(ConnectivityManager.class);

        mPreferenceVersion = findPreference(KEY_VERSION);
        mPreferenceAlwaysOn = (RestrictedSwitchPreference) findPreference(KEY_ALWAYS_ON_VPN);
        mPreferenceForget = (RestrictedPreference) findPreference(KEY_FORGET_VPN);

        mPreferenceAlwaysOn.setOnPreferenceClickListener(this);
        mPreferenceForget.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isInfoLoaded = loadInfo();
        if (isInfoLoaded) {
            mPreferenceVersion.setTitle(
                    getPrefContext().getString(R.string.vpn_version, mPackageInfo.versionName));
            updateUI();
        } else {
            finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_FORGET_VPN:
                return onForgetVpnClick();
            case KEY_ALWAYS_ON_VPN:
                return onAlwaysOnVpnClick();
            default:
                Log.w(TAG, "unknown key is clicked: " + key);
                return false;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.VPN;
    }

    private boolean onForgetVpnClick() {
        AppDialogFragment.show(this, mForgetVpnDialogFragmentListener, mPackageInfo, mVpnLabel,
                true /* editing */, true);
        return true;
    }

    private boolean onAlwaysOnVpnClick() {
        final boolean isChecked = mPreferenceAlwaysOn.isChecked();
        if (isChecked && isLegacyVpnLockDownOrAnotherPackageAlwaysOn()) {
            // Show dialog if user replace always-on-vpn package and show not checked first
            mPreferenceAlwaysOn.setChecked(false);
            ReplaceExistingVpnFragment.show(this);
        } else {
            setAlwaysOnVpn(isChecked);
        }
        return true;
    }

    private void setAlwaysOnVpn(boolean isEnabled) {
        // Only clear legacy lockdown vpn in system user.
        if (mUserId == UserHandle.USER_SYSTEM) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        mConnectivityManager.setAlwaysOnVpnPackageForUser(mUserId, isEnabled ? mPackageName : null);
        updateUI();
    }

    private void updateUI() {
        if (isAdded()) {
            mPreferenceAlwaysOn.setChecked(isVpnAlwaysOn());
        }
    }

    private String getAlwaysOnVpnPackage() {
        return mConnectivityManager.getAlwaysOnVpnPackageForUser(mUserId);
    }

    private boolean isVpnAlwaysOn() {
        return mPackageName.equals(getAlwaysOnVpnPackage());
    }

    /**
     * @return false if the intent doesn't contain an existing package or can't retrieve activated
     * vpn info.
     */
    private boolean loadInfo() {
        final Bundle args = getArguments();
        if (args == null) {
            Log.e(TAG, "empty bundle");
            return false;
        }

        mPackageName = args.getString(ARG_PACKAGE_NAME);
        if (mPackageName == null) {
            Log.e(TAG, "empty package name");
            return false;
        }

        try {
            mPackageUid = mPackageManager.getPackageUid(mPackageName, /* PackageInfoFlags */ 0);
            mPackageInfo = mPackageManager.getPackageInfo(mPackageName, /* PackageInfoFlags */ 0);
            mVpnLabel = VpnConfig.getVpnLabel(getPrefContext(), mPackageName).toString();
        } catch (NameNotFoundException nnfe) {
            Log.e(TAG, "package not found", nnfe);
            return false;
        }

        if (!isVpnActivated()) {
            Log.e(TAG, "package didn't register VPN profile");
            return false;
        }

        return true;
    }

    private boolean isVpnActivated() {
        final List<AppOpsManager.PackageOps> apps = mAppOpsManager.getOpsForPackage(mPackageUid,
                mPackageName, new int[]{OP_ACTIVATE_VPN});
        return apps != null && apps.size() > 0 && apps.get(0) != null;
    }

    private boolean isLegacyVpnLockDownOrAnotherPackageAlwaysOn() {
        if (mUserId == UserHandle.USER_SYSTEM) {
            String lockdownKey = VpnUtils.getLockdownVpn();
            if (lockdownKey != null) {
                return true;
            }
        }

        return getAlwaysOnVpnPackage() != null && !isVpnAlwaysOn();
    }

    public static class ReplaceExistingVpnFragment extends DialogFragment
            implements DialogInterface.OnClickListener {

        public static void show(AppManagementFragment parent) {
            final ReplaceExistingVpnFragment frag = new ReplaceExistingVpnFragment();
            frag.setTargetFragment(parent, 0);
            frag.show(parent.getFragmentManager(), null);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.vpn_replace_always_on_vpn_title)
                    .setMessage(getActivity().getString(R.string.vpn_replace_always_on_vpn_message))
                    .setNegativeButton(getActivity().getString(R.string.vpn_cancel), null)
                    .setPositiveButton(getActivity().getString(R.string.vpn_continue), this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (getTargetFragment() instanceof AppManagementFragment) {
                ((AppManagementFragment) getTargetFragment()).setAlwaysOnVpn(true);
            }
        }
    }
}

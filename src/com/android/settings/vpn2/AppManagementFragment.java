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
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.RestrictedPreference;

import java.util.List;

import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

public class AppManagementFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

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

        mPreferenceAlwaysOn.setOnPreferenceChangeListener(this);
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
            default:
                Log.w(TAG, "unknown key is clicked: " + key);
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_ALWAYS_ON_VPN:
                return onAlwaysOnVpnClick((Boolean) newValue);
            default:
                Log.w(TAG, "unknown key is clicked: " + preference.getKey());
                return false;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.VPN;
    }

    private boolean onForgetVpnClick() {
        updateRestrictedViews();
        if (!mPreferenceForget.isEnabled()) {
            return false;
        }
        AppDialogFragment.show(this, mForgetVpnDialogFragmentListener, mPackageInfo, mVpnLabel,
                true /* editing */, true);
        return true;
    }

    private boolean onAlwaysOnVpnClick(final boolean isChecked) {
        if (isChecked && isLegacyVpnLockDownOrAnotherPackageAlwaysOn()) {
            // Show dialog if user replace always-on-vpn package and show not checked first
            ReplaceExistingVpnFragment.show(this);
            return false;
        } else {
            return setAlwaysOnVpnByUI(isChecked);
        }
    }

    private boolean setAlwaysOnVpnByUI(boolean isEnabled) {
        updateRestrictedViews();
        if (!mPreferenceAlwaysOn.isEnabled()) {
            return false;
        }
        // Only clear legacy lockdown vpn in system user.
        if (mUserId == UserHandle.USER_SYSTEM) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        final boolean success = setAlwaysOnVpn(isEnabled);
        if (isEnabled && (!success || !isVpnAlwaysOn())) {
            CannotConnectFragment.show(this, mVpnLabel);
        }
        return success;
    }

    private boolean setAlwaysOnVpn(boolean isEnabled) {
         return mConnectivityManager.setAlwaysOnVpnPackageForUser(mUserId,
                isEnabled ? mPackageName : null, /* lockdownEnabled */ false);
    }

    private boolean checkTargetVersion() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return true;
        }
        final int targetSdk = mPackageInfo.applicationInfo.targetSdkVersion;
        if (targetSdk >= Build.VERSION_CODES.N) {
            return true;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Package " + mPackageName + " targets SDK version " + targetSdk + "; must"
                    + " target at least " + Build.VERSION_CODES.N + " to use always-on.");
        }
        return false;
    }

    private void updateUI() {
        if (isAdded()) {
            mPreferenceAlwaysOn.setChecked(isVpnAlwaysOn());
            updateRestrictedViews();
        }
    }

    private void updateRestrictedViews() {
        if (isAdded()) {
            mPreferenceAlwaysOn.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN,
                    mUserId);
            mPreferenceForget.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN,
                    mUserId);

            if (checkTargetVersion()) {
                // setSummary doesn't override the admin message when user restriction is applied
                mPreferenceAlwaysOn.setSummary(null);
                // setEnabled is not required here, as checkRestrictionAndSetDisabled
                // should have refreshed the enable state.
            } else {
                mPreferenceAlwaysOn.setEnabled(false);
                mPreferenceAlwaysOn.setSummary(R.string.vpn_not_supported_by_this_app);
            }
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

    public static class CannotConnectFragment extends DialogFragment {
        private static final String TAG = "CannotConnect";
        private static final String ARG_VPN_LABEL = "label";

        public static void show(AppManagementFragment parent, String vpnLabel) {
            if (parent.getFragmentManager().findFragmentByTag(TAG) == null) {
                final Bundle args = new Bundle();
                args.putString(ARG_VPN_LABEL, vpnLabel);

                final DialogFragment frag = new CannotConnectFragment();
                frag.setArguments(args);
                frag.show(parent.getFragmentManager(), TAG);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String vpnLabel = getArguments().getString(ARG_VPN_LABEL);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getActivity().getString(R.string.vpn_cant_connect_title, vpnLabel))
                    .setMessage(getActivity().getString(R.string.vpn_cant_connect_message))
                    .setPositiveButton(R.string.okay, null)
                    .create();
        }
    }

    public static class ReplaceExistingVpnFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        private static final String TAG = "ReplaceExistingVpn";

        public static void show(AppManagementFragment parent) {
            if (parent.getFragmentManager().findFragmentByTag(TAG) == null) {
                final ReplaceExistingVpnFragment frag = new ReplaceExistingVpnFragment();
                frag.setTargetFragment(parent, 0);
                frag.show(parent.getFragmentManager(), TAG);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.vpn_replace_always_on_vpn_title)
                    .setMessage(getActivity().getString(R.string.vpn_replace_always_on_vpn_message))
                    .setNegativeButton(getActivity().getString(R.string.vpn_cancel), null)
                    .setPositiveButton(getActivity().getString(R.string.vpn_replace), this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (getTargetFragment() instanceof AppManagementFragment) {
                final AppManagementFragment target = (AppManagementFragment) getTargetFragment();
                if (target.setAlwaysOnVpnByUI(true)) {
                    target.updateUI();
                }
            }
        }
    }
}

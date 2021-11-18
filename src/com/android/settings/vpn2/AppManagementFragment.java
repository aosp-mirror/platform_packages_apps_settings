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

import static android.app.AppOpsManager.OP_ACTIVATE_PLATFORM_VPN;
import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

import android.annotation.NonNull;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.net.VpnConfig;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.List;

public class AppManagementFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        ConfirmLockdownFragment.ConfirmLockdownListener {

    private static final String TAG = "AppManagementFragment";

    private static final String ARG_PACKAGE_NAME = "package";

    private static final String KEY_VERSION = "version";
    private static final String KEY_ALWAYS_ON_VPN = "always_on_vpn";
    private static final String KEY_LOCKDOWN_VPN = "lockdown_vpn";
    private static final String KEY_FORGET_VPN = "forget_vpn";

    private PackageManager mPackageManager;
    private DevicePolicyManager mDevicePolicyManager;
    private ConnectivityManager mConnectivityManager;
    private IConnectivityManager mConnectivityService;

    // VPN app info
    private final int mUserId = UserHandle.myUserId();
    private String mPackageName;
    private PackageInfo mPackageInfo;
    private String mVpnLabel;

    // UI preference
    private RestrictedSwitchPreference mPreferenceAlwaysOn;
    private RestrictedSwitchPreference mPreferenceLockdown;
    private RestrictedPreference mPreferenceForget;

    // Listener
    private final AppDialogFragment.Listener mForgetVpnDialogFragmentListener =
            new AppDialogFragment.Listener() {
        @Override
        public void onForget() {
            // Unset always-on-vpn when forgetting the VPN
            if (isVpnAlwaysOn()) {
                setAlwaysOnVpn(false, false);
            }
            // Also dismiss and go back to VPN list
            finish();
        }

        @Override
        public void onCancel() {
            // do nothing
        }
    };

    public static void show(Context context, AppPreference pref, int sourceMetricsCategory) {
        final Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pref.getPackageName());
        new SubSettingLauncher(context)
                .setDestination(AppManagementFragment.class.getName())
                .setArguments(args)
                .setTitleText(pref.getLabel())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setUserHandle(new UserHandle(pref.getUserId()))
                .launch();
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        addPreferencesFromResource(R.xml.vpn_app_management);

        mPackageManager = getContext().getPackageManager();
        mDevicePolicyManager = getContext().getSystemService(DevicePolicyManager.class);
        mConnectivityManager = getContext().getSystemService(ConnectivityManager.class);
        mConnectivityService = IConnectivityManager.Stub
                .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

        mPreferenceAlwaysOn = (RestrictedSwitchPreference) findPreference(KEY_ALWAYS_ON_VPN);
        mPreferenceLockdown = (RestrictedSwitchPreference) findPreference(KEY_LOCKDOWN_VPN);
        mPreferenceForget = (RestrictedPreference) findPreference(KEY_FORGET_VPN);

        mPreferenceAlwaysOn.setOnPreferenceChangeListener(this);
        mPreferenceLockdown.setOnPreferenceChangeListener(this);
        mPreferenceForget.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isInfoLoaded = loadInfo();
        if (isInfoLoaded) {
            updateUI();

            Preference version = getPreferenceScreen().findPreference(KEY_VERSION);
            if (version != null) {
                // Version field has been added.
                return;
            }

            /**
             * Create version field at runtime, and set max height on the display area.
             *
             * When long length of text given within version field, a large text area
             * might be created and inconvenient to the user (User need to scroll
             * for a long time in order to get to the Preferences after this field.)
             */
            version = new Preference(getPrefContext()) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);

                    TextView titleView =
                            (TextView) holder.findViewById(android.R.id.title);
                    if (titleView != null) {
                        titleView.setTextAppearance(R.style.vpn_app_management_version_title);
                    }

                    TextView summaryView =
                            (TextView) holder.findViewById(android.R.id.summary);
                    if (summaryView != null) {
                        summaryView.setTextAppearance(R.style.vpn_app_management_version_summary);

                        // Set max height in summary area.
                        int versionMaxHeight = getListView().getHeight();
                        summaryView.setMaxHeight(versionMaxHeight);
                        summaryView.setVerticalScrollBarEnabled(false);
                        summaryView.setHorizontallyScrolling(false);
                    }
                }
            };
            version.setOrder(0);            // Set order to 0 in order to be placed
                                            // in front of other Preference(s).
            version.setKey(KEY_VERSION);    // Set key to avoid from creating multi instance.
            version.setTitle(R.string.vpn_version);
            version.setSummary(mPackageInfo.versionName);
            version.setSelectable(false);
            getPreferenceScreen().addPreference(version);
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
                return onAlwaysOnVpnClick((Boolean) newValue, mPreferenceLockdown.isChecked());
            case KEY_LOCKDOWN_VPN:
                return onAlwaysOnVpnClick(mPreferenceAlwaysOn.isChecked(), (Boolean) newValue);
            default:
                Log.w(TAG, "unknown key is clicked: " + preference.getKey());
                return false;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VPN;
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

    private boolean onAlwaysOnVpnClick(final boolean alwaysOnSetting, final boolean lockdown) {
        final boolean replacing = isAnotherVpnActive();
        final boolean wasLockdown = VpnUtils.isAnyLockdownActive(getActivity());
        if (ConfirmLockdownFragment.shouldShow(replacing, wasLockdown, lockdown)) {
            // Place a dialog to confirm that traffic should be locked down.
            final Bundle options = null;
            ConfirmLockdownFragment.show(
                    this, replacing, alwaysOnSetting, wasLockdown, lockdown, options);
            return false;
        }
        // No need to show the dialog. Change the setting straight away.
        return setAlwaysOnVpnByUI(alwaysOnSetting, lockdown);
    }

    @Override
    public void onConfirmLockdown(Bundle options, boolean isEnabled, boolean isLockdown) {
        setAlwaysOnVpnByUI(isEnabled, isLockdown);
    }

    private boolean setAlwaysOnVpnByUI(boolean isEnabled, boolean isLockdown) {
        updateRestrictedViews();
        if (!mPreferenceAlwaysOn.isEnabled()) {
            return false;
        }
        // Only clear legacy lockdown vpn in system user.
        if (mUserId == UserHandle.USER_SYSTEM) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        final boolean success = setAlwaysOnVpn(isEnabled, isLockdown);
        if (isEnabled && (!success || !isVpnAlwaysOn())) {
            CannotConnectFragment.show(this, mVpnLabel);
        } else {
            updateUI();
        }
        return success;
    }

    private boolean setAlwaysOnVpn(boolean isEnabled, boolean isLockdown) {
        return mConnectivityManager.setAlwaysOnVpnPackageForUser(mUserId,
                isEnabled ? mPackageName : null, isLockdown, /* lockdownWhitelist */ null);
    }

    private void updateUI() {
        if (isAdded()) {
            final boolean alwaysOn = isVpnAlwaysOn();
            final boolean lockdown = alwaysOn
                    && VpnUtils.isAnyLockdownActive(getActivity());

            mPreferenceAlwaysOn.setChecked(alwaysOn);
            mPreferenceLockdown.setChecked(lockdown);
            updateRestrictedViews();
        }
    }

    private void updateRestrictedViews() {
        if (isAdded()) {
            mPreferenceAlwaysOn.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN,
                    mUserId);
            mPreferenceLockdown.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN,
                    mUserId);
            mPreferenceForget.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_VPN,
                    mUserId);

            if (mPackageName.equals(mDevicePolicyManager.getAlwaysOnVpnPackage())) {
                EnforcedAdmin admin = RestrictedLockUtils.getProfileOrDeviceOwner(
                        getContext(), UserHandle.of(mUserId));
                mPreferenceAlwaysOn.setDisabledByAdmin(admin);
                mPreferenceForget.setDisabledByAdmin(admin);
                if (mDevicePolicyManager.isAlwaysOnVpnLockdownEnabled()) {
                    mPreferenceLockdown.setDisabledByAdmin(admin);
                }
            }
            if (mConnectivityManager.isAlwaysOnVpnPackageSupportedForUser(mUserId, mPackageName)) {
                // setSummary doesn't override the admin message when user restriction is applied
                mPreferenceAlwaysOn.setSummary(R.string.vpn_always_on_summary);
                // setEnabled is not required here, as checkRestrictionAndSetDisabled
                // should have refreshed the enable state.
            } else {
                mPreferenceAlwaysOn.setEnabled(false);
                mPreferenceLockdown.setEnabled(false);
                mPreferenceAlwaysOn.setSummary(R.string.vpn_always_on_summary_not_supported);
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
            mPackageInfo = mPackageManager.getPackageInfo(mPackageName, /* PackageInfoFlags */ 0);
            mVpnLabel = VpnConfig.getVpnLabel(getPrefContext(), mPackageName).toString();
        } catch (NameNotFoundException nnfe) {
            Log.e(TAG, "package not found", nnfe);
            return false;
        }

        if (mPackageInfo.applicationInfo == null) {
            Log.e(TAG, "package does not include an application");
            return false;
        }
        if (!appHasVpnPermission(getContext(), mPackageInfo.applicationInfo)) {
            Log.e(TAG, "package didn't register VPN profile");
            return false;
        }

        return true;
    }

    @VisibleForTesting
    static boolean appHasVpnPermission(Context context, @NonNull ApplicationInfo application) {
        final AppOpsManager service =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        final List<AppOpsManager.PackageOps> ops = service.getOpsForPackage(application.uid,
                application.packageName, new int[]{OP_ACTIVATE_VPN, OP_ACTIVATE_PLATFORM_VPN});
        return !ArrayUtils.isEmpty(ops);
    }

    /**
     * @return {@code true} if another VPN (VpnService or legacy) is connected or set as always-on.
     */
    private boolean isAnotherVpnActive() {
        try {
            final VpnConfig config = mConnectivityService.getVpnConfig(mUserId);
            return config != null && !TextUtils.equals(config.user, mPackageName);
        } catch (RemoteException e) {
            Log.w(TAG, "Failure to look up active VPN", e);
            return false;
        }
    }

    public static class CannotConnectFragment extends InstrumentedDialogFragment {
        private static final String TAG = "CannotConnect";
        private static final String ARG_VPN_LABEL = "label";

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_VPN_CANNOT_CONNECT;
        }

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
}

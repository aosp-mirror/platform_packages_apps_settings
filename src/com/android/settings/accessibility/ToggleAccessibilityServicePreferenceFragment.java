/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import static com.android.settings.accessibility.AccessibilityStatsLogUtils.logAccessibilityServiceEnabled;
import static com.android.settings.accessibility.PreferredShortcuts.retrieveUserShortcutType;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fragment for providing toggle bar and basic accessibility service setup. */
public class ToggleAccessibilityServicePreferenceFragment extends
        ToggleFeaturePreferenceFragment {

    private static final String TAG = "ToggleAccessibilityServicePreferenceFragment";
    private static final int ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION = 1;
    private LockPatternUtils mLockPatternUtils;
    private AtomicBoolean mIsDialogShown = new AtomicBoolean(/* initialValue= */ false);

    private static final String EMPTY_STRING = "";

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateSwitchBarToggleSwitch();
                }
            };

    private Dialog mDialog;
    private BroadcastReceiver mPackageRemovedReceiver;

    @Override
    public int getMetricsCategory() {
        // Retrieve from getArguments() directly because this function will be executed from
        // onAttach(), but variable mComponentName only available after onProcessArguments()
        // which comes from onCreateView().
        final ComponentName componentName = getArguments().getParcelable(
                AccessibilitySettings.EXTRA_COMPONENT_NAME);

        return FeatureFactory.getFactory(getActivity().getApplicationContext())
                .getAccessibilityMetricsFeatureProvider()
                .getDownloadedFeatureMetricsCategory(componentName);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater infalter) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(getPrefContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        final AccessibilityServiceInfo serviceInfo = getAccessibilityServiceInfo();
        if (serviceInfo == null) {
            getActivity().finishAndRemoveTask();
        } else if (!AccessibilityUtil.isSystemApp(serviceInfo)) {
            registerPackageRemoveReceiver();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBarToggleSwitch();
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
        logAccessibilityServiceEnabled(toggledService, enabled);
        AccessibilityUtils.setAccessibilityServiceState(getPrefContext(), toggledService, enabled);
    }

    // IMPORTANT: Refresh the info since there are dynamically changing
    // capabilities. For
    // example, before JellyBean MR2 the user was granting the explore by touch
    // one.
    @Nullable
    AccessibilityServiceInfo getAccessibilityServiceInfo() {
        final List<AccessibilityServiceInfo> infos = AccessibilityManager.getInstance(
                getPrefContext()).getInstalledAccessibilityServiceList();

        for (int i = 0, count = infos.size(); i < count; i++) {
            AccessibilityServiceInfo serviceInfo = infos.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if (mComponentName.getPackageName().equals(resolveInfo.serviceInfo.packageName)
                    && mComponentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                return serviceInfo;
            }
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.ENABLE_WARNING_FROM_TOGGLE: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getPrefContext(), info,
                                this::onDialogButtonFromEnableToggleClicked,
                                this::onDialogButtonFromUninstallClicked);
                break;
            }
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getPrefContext(), info,
                                this::onDialogButtonFromShortcutToggleClicked,
                                this::onDialogButtonFromUninstallClicked);
                break;
            }
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getPrefContext(), info,
                                this::onDialogButtonFromShortcutClicked,
                                this::onDialogButtonFromUninstallClicked);
                break;
            }
            case DialogEnums.DISABLE_WARNING_FROM_TOGGLE: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createDisableDialog(getPrefContext(), info,
                                this::onDialogButtonFromDisableToggleClicked);
                break;
            }
            default: {
                mDialog = super.onCreateDialog(dialogId);
            }
        }
        return mDialog;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogEnums.ENABLE_WARNING_FROM_TOGGLE:
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT:
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_ENABLE;
            case DialogEnums.DISABLE_WARNING_FROM_TOGGLE:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_DISABLE;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return SettingsEnums.DIALOG_ACCESSIBILITY_TUTORIAL;
            default:
                return super.getDialogMetricsCategory(dialogId);
        }
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    @Override
    protected void updateSwitchBarToggleSwitch() {
        final boolean checked = isAccessibilityServiceEnabled();
        if (mToggleServiceSwitchPreference.isChecked() == checked) {
            return;
        }
        mToggleServiceSwitchPreference.setChecked(checked);
    }

    private boolean isAccessibilityServiceEnabled() {
        return AccessibilityUtils.getEnabledServicesFromSettings(getPrefContext())
                .contains(mComponentName);
    }

    /**
     * Return whether the device is encrypted with legacy full disk encryption. Newer devices
     * should be using File Based Encryption.
     *
     * @return true if device is encrypted
     */
    private boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION) {
            if (resultCode == Activity.RESULT_OK) {
                handleConfirmServiceEnabled(/* confirmed= */ true);
                // The user confirmed that they accept weaker encryption when
                // enabling the accessibility service, so change encryption.
                // Since we came here asynchronously, check encryption again.
                if (isFullDiskEncrypted()) {
                    mLockPatternUtils.clearEncryptionPassword();
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, 0);
                }
            } else {
                handleConfirmServiceEnabled(/* confirmed= */ false);
            }
        }
    }

    private void registerPackageRemoveReceiver() {
        if (mPackageRemovedReceiver != null || getContext() == null) {
            return;
        }
        mPackageRemovedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                if (TextUtils.equals(mComponentName.getPackageName(), packageName)) {
                    getActivity().finishAndRemoveTask();
                }
            }
        };
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getContext().registerReceiver(mPackageRemovedReceiver, filter);
    }

    private void unregisterPackageRemoveReceiver() {
        if (mPackageRemovedReceiver == null || getContext() == null) {
            return;
        }
        getContext().unregisterReceiver(mPackageRemovedReceiver);
        mPackageRemovedReceiver = null;
    }

    private boolean isServiceSupportAccessibilityButton() {
        final AccessibilityManager ams = getPrefContext().getSystemService(
                AccessibilityManager.class);
        final List<AccessibilityServiceInfo> services = ams.getInstalledAccessibilityServiceList();

        for (AccessibilityServiceInfo info : services) {
            if ((info.flags & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0) {
                ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
                if (serviceInfo != null && TextUtils.equals(serviceInfo.name,
                        getAccessibilityServiceInfo().getResolveInfo().serviceInfo.name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleConfirmServiceEnabled(boolean confirmed) {
        getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, confirmed);
        onPreferenceToggled(mPreferenceKey, confirmed);
    }

    private String createConfirmCredentialReasonMessage() {
        int resId = R.string.enable_service_password_reason;
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING: {
                resId = R.string.enable_service_pattern_reason;
            }
            break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX: {
                resId = R.string.enable_service_pin_reason;
            }
            break;
        }
        return getString(resId, getAccessibilityServiceInfo().getResolveInfo()
                .loadLabel(getPackageManager()));
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked != isAccessibilityServiceEnabled()) {
            onPreferenceClick(isChecked);
        }
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        final int shortcutTypes = retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        if (preference.isChecked()) {
            if (!mToggleServiceSwitchPreference.isChecked()) {
                preference.setChecked(false);
                showPopupDialog(DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE);
            } else {
                AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes,
                        mComponentName);
                showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
            }
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        final boolean isServiceOnOrShortcutAdded = mShortcutPreference.isChecked()
                || mToggleServiceSwitchPreference.isChecked();
        showPopupDialog(isServiceOnOrShortcutAdded ? DialogEnums.EDIT_SHORTCUT
                : DialogEnums.ENABLE_WARNING_FROM_SHORTCUT);
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        super.onProcessArguments(arguments);
        // Settings title and intent.
        String settingsTitle = arguments.getString(AccessibilitySettings.EXTRA_SETTINGS_TITLE);
        String settingsComponentName = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(settingsComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                mSettingsTitle = settingsTitle;
                mSettingsIntent = settingsIntent;
                setHasOptionsMenu(true);
            }
        }

        mComponentName = arguments.getParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME);

        // Settings animated image.
        final int animatedImageRes = arguments.getInt(
                AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES);
        if (animatedImageRes > 0) {
            mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(mComponentName.getPackageName())
                    .appendPath(String.valueOf(animatedImageRes))
                    .build();
        }

        // Get Accessibility service name.
        mPackageName = getAccessibilityServiceInfo().getResolveInfo().loadLabel(
                getPackageManager());
    }

    private void onDialogButtonFromDisableToggleClicked(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                handleConfirmServiceEnabled(/* confirmed= */ false);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                handleConfirmServiceEnabled(/* confirmed= */ true);
                break;
            default:
                throw new IllegalArgumentException("Unexpected button identifier");
        }
    }

    private void onDialogButtonFromEnableToggleClicked(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.permission_enable_allow_button) {
            onAllowButtonFromEnableToggleClicked();
        } else if (viewId == R.id.permission_enable_deny_button) {
            onDenyButtonFromEnableToggleClicked();
        } else {
            throw new IllegalArgumentException("Unexpected view id");
        }
    }

    private void onDialogButtonFromUninstallClicked() {
        mDialog.dismiss();
        final Intent uninstallIntent = createUninstallPackageActivityIntent();
        if (uninstallIntent == null) {
            return;
        }
        startActivity(uninstallIntent);
    }

    @Nullable
    private Intent createUninstallPackageActivityIntent() {
        final AccessibilityServiceInfo a11yServiceInfo = getAccessibilityServiceInfo();
        if (a11yServiceInfo == null) {
            Log.w(TAG, "createUnInstallIntent -- invalid a11yServiceInfo");
            return null;
        }
        final ApplicationInfo appInfo =
                a11yServiceInfo.getResolveInfo().serviceInfo.applicationInfo;
        final Uri packageUri = Uri.parse("package:" + appInfo.packageName);
        final Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        return uninstallIntent;
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterPackageRemoveReceiver();
    }

    private void onAllowButtonFromEnableToggleClicked() {
        if (isFullDiskEncrypted()) {
            final String title = createConfirmCredentialReasonMessage();
            final Intent intent = ConfirmDeviceCredentialActivity.createIntent(title, /* details= */
                    null);
            startActivityForResult(intent,
                    ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION);
        } else {
            handleConfirmServiceEnabled(/* confirmed= */ true);
            if (isServiceSupportAccessibilityButton()) {
                mIsDialogShown.set(false);
                showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
            }
        }

        mDialog.dismiss();
    }

    private void onDenyButtonFromEnableToggleClicked() {
        handleConfirmServiceEnabled(/* confirmed= */ false);
        mDialog.dismiss();
    }

    void onDialogButtonFromShortcutToggleClicked(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.permission_enable_allow_button) {
            onAllowButtonFromShortcutToggleClicked();
        } else if (viewId == R.id.permission_enable_deny_button) {
            onDenyButtonFromShortcutToggleClicked();
        } else {
            throw new IllegalArgumentException("Unexpected view id");
        }
    }

    private void onAllowButtonFromShortcutToggleClicked() {
        mShortcutPreference.setChecked(true);

        final int shortcutTypes = retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes, mComponentName);

        mIsDialogShown.set(false);
        showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);

        mDialog.dismiss();

        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    private void onDenyButtonFromShortcutToggleClicked() {
        mShortcutPreference.setChecked(false);

        mDialog.dismiss();
    }

    void onDialogButtonFromShortcutClicked(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.permission_enable_allow_button) {
            onAllowButtonFromShortcutClicked();
        } else if (viewId == R.id.permission_enable_deny_button) {
            onDenyButtonFromShortcutClicked();
        } else {
            throw new IllegalArgumentException("Unexpected view id");
        }
    }

    private void onAllowButtonFromShortcutClicked() {
        mIsDialogShown.set(false);
        showPopupDialog(DialogEnums.EDIT_SHORTCUT);

        mDialog.dismiss();
    }

    private void onDenyButtonFromShortcutClicked() {
        mDialog.dismiss();
    }

    private boolean onPreferenceClick(boolean isChecked) {
        if (isChecked) {
            mToggleServiceSwitchPreference.setChecked(false);
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    /* disableService */ false);
            if (!mShortcutPreference.isChecked()) {
                showPopupDialog(DialogEnums.ENABLE_WARNING_FROM_TOGGLE);
            } else {
                handleConfirmServiceEnabled(/* confirmed= */ true);
                if (isServiceSupportAccessibilityButton()) {
                    showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
                }
            }
        } else {
            mToggleServiceSwitchPreference.setChecked(true);
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    /* enableService */ true);
            showDialog(DialogEnums.DISABLE_WARNING_FROM_TOGGLE);
        }
        return true;
    }

    private void showPopupDialog(int dialogId) {
        if (mIsDialogShown.compareAndSet(/* expect= */ false, /* update= */ true)) {
            showDialog(dialogId);
            setOnDismissListener(
                    dialog -> mIsDialogShown.compareAndSet(/* expect= */ true, /* update= */
                            false));
        }
    }
}

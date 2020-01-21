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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Fragment for providing toggle bar and basic accessibility service setup. */
public class ToggleAccessibilityServicePreferenceFragment extends
        ToggleFeaturePreferenceFragment implements ShortcutPreference.OnClickListener {

    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private int mUserShortcutType = UserShortcutType.DEFAULT;
    // Used to restore the edit dialog status.
    private int mUserShortcutTypeCache = UserShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

    public static final int ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION = 1;
    private CharSequence mDialogTitle;
    private LockPatternUtils mLockPatternUtils;
    private AtomicBoolean mIsDialogShown = new AtomicBoolean(/* initialValue= */ false);

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateSwitchBarToggleSwitch();
                }
            };

    private Dialog mDialog;

    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogType {
        int ENABLE_WARNING_FROM_TOGGLE = 1;
        int ENABLE_WARNING_FROM_SHORTCUT = 2;
        int LAUNCH_ACCESSIBILITY_TUTORIAL = 3;
        int EDIT_SHORTCUT = 4;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SERVICE;
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
        mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Restore the user shortcut type.
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SHORTCUT_TYPE)) {
            mUserShortcutTypeCache = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    UserShortcutType.DEFAULT);
        }
        initShortcutPreference();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutTypeCache);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsContentObserver.register(getContentResolver());
        updateSwitchBarToggleSwitch();
        updateShortcutPreferenceData();
        updateShortcutPreference();
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
        AccessibilityUtils.setAccessibilityServiceState(getActivity(), toggledService, enabled);
    }

    // IMPORTANT: Refresh the info since there are dynamically changing
    // capabilities. For
    // example, before JellyBean MR2 the user was granting the explore by touch
    // one.
    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(
                getActivity()).getInstalledAccessibilityServiceList();
        final int serviceInfoCount = serviceInfos.size();
        for (int i = 0; i < serviceInfoCount; i++) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
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
            case DialogType.ENABLE_WARNING_FROM_TOGGLE: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getActivity(), info,
                                this::onDialogButtonFromToggleClicked);
                break;
            }
            case DialogType.ENABLE_WARNING_FROM_SHORTCUT: {
                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                mDialog = AccessibilityServiceWarning
                        .createCapabilitiesDialog(getActivity(), info,
                                this::onDialogButtonFromShortcutClicked);
                break;
            }
            case DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL: {
                if (AccessibilityUtil.isGestureNavigateEnabled(getContext())) {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .showGestureNavigationTutorialDialog(getActivity());
                } else {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .showAccessibilityButtonTutorialDialog(getActivity());
                }
                break;
            }
            case DialogType.EDIT_SHORTCUT: {
                final CharSequence dialogTitle = getActivity().getString(
                        R.string.accessibility_shortcut_edit_dialog_title, mDialogTitle);
                mDialog = AccessibilityEditDialogUtils.showEditShortcutDialog(getActivity(),
                        dialogTitle, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(mDialog);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
            }
        }
        return mDialog;
    }

    private void initializeDialogCheckBox(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        updateAlertDialogCheckState();
        updateAlertDialogEnableState();
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
    }

    private void updateAlertDialogEnableState() {
        if (!mSoftwareTypeCheckBox.isChecked()) {
            mHardwareTypeCheckBox.setEnabled(false);
        } else if (!mHardwareTypeCheckBox.isChecked()) {
            mSoftwareTypeCheckBox.setEnabled(false);
        } else {
            mSoftwareTypeCheckBox.setEnabled(true);
            mHardwareTypeCheckBox.setEnabled(true);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutTypeCache & type) == type);
        checkBox.setOnClickListener(v -> {
            updateUserShortcutType(/* saveChanges= */ false);
            updateAlertDialogEnableState();
        });
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutTypeCache = UserShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.HARDWARE;
        }
        if (saveChanges) {
            mUserShortcutType = mUserShortcutTypeCache;
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        }
    }

    private void setUserShortcutType(Context context, int type) {
        Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        final String componentName = getComponentName().flattenToString();
        if (info.isEmpty()) {
            info = new HashSet<>();
        } else {
            final Set<String> filtered = info.stream().filter(
                    str -> str.contains(componentName)).collect(Collectors.toSet());
            info.removeAll(filtered);
        }
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                getComponentName().flattenToString(), type);
        info.add(shortcut.flattenToString());
        SharedPreferenceUtils.setUserShortcutType(context, info);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getUserShortcutType(context, UserShortcutType.SOFTWARE);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);
        return AccessibilityUtil.capitalize(joinStrings);
    }

    private int getUserShortcutType(Context context, @UserShortcutType int defaultValue) {
        final Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        final String componentName = getComponentName().flattenToString();
        final Set<String> filtered = info.stream().filter(
                str -> str.contains(componentName)).collect(
                Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getUserShortcutType();
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateUserShortcutType(/* saveChanges= */ true);
        if (mShortcutPreference.getChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getContext(), mUserShortcutType,
                    mComponentName);
            AccessibilityUtil.optOutAllValuesFromSettings(getContext(), ~mUserShortcutType,
                    mComponentName);
        }
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogType.ENABLE_WARNING_FROM_TOGGLE:
            case DialogType.ENABLE_WARNING_FROM_SHORTCUT:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_ENABLE;
            case DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return AccessibilityUtil.isGestureNavigateEnabled(getContext())
                        ? SettingsEnums.DIALOG_TOGGLE_SCREEN_GESTURE_NAVIGATION
                        : SettingsEnums.DIALOG_TOGGLE_SCREEN_ACCESSIBILITY_BUTTON;
            case DialogType.EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT;
            default:
                return 0;
        }
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        final String switchBarText = (info == null) ? "" :
                getString(R.string.accessibility_service_master_switch_title,
                info.getResolveInfo().loadLabel(getPackageManager()));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
    }

    private void updateShortcutPreferenceData() {
        // Get the user shortcut type from settings provider.
        mUserShortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                getComponentName());
        if (mUserShortcutType != UserShortcutType.DEFAULT) {
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        } else {
            //  Get the user shortcut type from shared_prefs if cannot get from settings provider.
            mUserShortcutType = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        }
    }

    @Override
    protected void updateFooterTitle(PreferenceCategory category) {
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        final String titleText = (info == null) ? "" :
                getString(R.string.accessibility_footer_title,
                        info.getResolveInfo().loadLabel(getPackageManager()));
        category.setTitle(titleText);
    }

    private void initShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        mShortcutPreference.setOnClickListener(this);
    }

    private void updateShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        ShortcutPreference shortcutPreference = preferenceScreen.findPreference(
                getShortcutPreferenceKey());

        if (shortcutPreference != null) {
            final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
            shortcutPreference.setChecked(
                    AccessibilityUtil.hasValuesInSettings(getContext(), shortcutTypes,
                            mComponentName));
            shortcutPreference.setSummary(getShortcutTypeSummary(getContext()));
        }
    }

    protected String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    private void updateSwitchBarToggleSwitch() {
        final boolean checked = AccessibilityUtils.getEnabledServicesFromSettings(getActivity())
                .contains(mComponentName);
        mSwitchBar.setCheckedInternal(checked);
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
                handleConfirmServiceEnabled(true);
                // The user confirmed that they accept weaker encryption when
                // enabling the accessibility service, so change encryption.
                // Since we came here asynchronously, check encryption again.
                if (isFullDiskEncrypted()) {
                    mLockPatternUtils.clearEncryptionPassword();
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, 0);
                }
            } else {
                handleConfirmServiceEnabled(false);
            }
        }
    }

    private boolean isServiceSupportAccessibilityButton() {
        final AccessibilityManager ams = (AccessibilityManager) getContext().getSystemService(
                Context.ACCESSIBILITY_SERVICE);
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
        mSwitchBar.setCheckedInternal(confirmed);
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
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(this::onBeforeCheckedChanged);
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
        if (preference.getChecked()) {
            if (!getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED)) {
                preference.setChecked(false);
                showPopupDialog(DialogType.ENABLE_WARNING_FROM_SHORTCUT);
            } else {
                AccessibilityUtil.optInAllValuesToSettings(getContext(), shortcutTypes,
                        mComponentName);
            }
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getContext(), shortcutTypes,
                    mComponentName);
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, false);
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mUserShortcutTypeCache = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        showPopupDialog(DialogType.EDIT_SHORTCUT);
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
        int animatedImageRes = arguments.getInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES);
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mComponentName.getPackageName())
                .appendPath(String.valueOf(animatedImageRes))
                .build();

        // Settings html description.
        mHtmlDescription = arguments.getCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION);

        // Get Accessibility service name.
        mDialogTitle = getAccessibilityServiceInfo().getResolveInfo().loadLabel(
                getPackageManager());
    }

    private void onDialogButtonFromToggleClicked(View view) {
        if (view.getId() == R.id.permission_enable_allow_button) {
            onAllowButtonFromToggleClicked();
        } else if (view.getId() == R.id.permission_enable_deny_button) {
            onDenyButtonFromToggleClicked();
        } else {
            throw new IllegalArgumentException("Unexpected view id");
        }
    }

    private void onAllowButtonFromToggleClicked() {
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
                showPopupDialog(DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL);
            }
        }

        mDialog.dismiss();
    }

    private void onDenyButtonFromToggleClicked() {
        handleConfirmServiceEnabled(/* confirmed= */ false);
        mDialog.dismiss();
    }

    void onDialogButtonFromShortcutClicked(View view) {
        if (view.getId() == R.id.permission_enable_allow_button) {
            onAllowButtonFromShortcutClicked();
        } else if (view.getId() == R.id.permission_enable_deny_button) {
            onDenyButtonFromShortcutClicked();
        } else {
            throw new IllegalArgumentException("Unexpected view id");
        }
    }

    private void onAllowButtonFromShortcutClicked() {
        final ShortcutPreference shortcutPreference = findPreference(getShortcutPreferenceKey());
        shortcutPreference.setChecked(true);

        final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
        AccessibilityUtil.optInAllValuesToSettings(getContext(), shortcutTypes, mComponentName);

        mDialog.dismiss();
    }

    private void onDenyButtonFromShortcutClicked() {
        final ShortcutPreference shortcutPreference = findPreference(getShortcutPreferenceKey());
        shortcutPreference.setChecked(false);

        mDialog.dismiss();
    }

    private boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
        if (checked) {
            mSwitchBar.setCheckedInternal(false);
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, false);

            final ShortcutPreference shortcutPreference = findPreference(
                    getShortcutPreferenceKey());
            if (!shortcutPreference.getChecked()) {
                showPopupDialog(DialogType.ENABLE_WARNING_FROM_TOGGLE);
            } else {
                handleConfirmServiceEnabled(/* confirmed= */ true);
                if (isServiceSupportAccessibilityButton()) {
                    showPopupDialog(DialogType.LAUNCH_ACCESSIBILITY_TUTORIAL);
                }
            }
        } else {
            handleConfirmServiceEnabled(/* confirmed= */ false);
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

    ComponentName getComponentName() {
        return mComponentName;
    }
}

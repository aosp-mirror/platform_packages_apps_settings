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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ConfirmDeviceCredentialActivity;
import com.android.settings.R;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;

public class ToggleAccessibilityServicePreferenceFragment
        extends ToggleFeaturePreferenceFragment implements DialogInterface.OnClickListener {

    private static final int DIALOG_ID_ENABLE_WARNING = 1;
    private static final int DIALOG_ID_DISABLE_WARNING = 2;

    public static final int ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION = 1;

    private LockPatternUtils mLockPatternUtils;

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
            @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateSwitchBarToggleSwitch();
                }
            };

    private ComponentName mComponentName;

    private int mShownDialogId;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_SERVICE;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater infalter) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.

        // We still want to show the "Settings" menu.
        if (mSettingsTitle != null && mSettingsIntent != null) {
            MenuItem menuItem = menu.add(mSettingsTitle);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menuItem.setIntent(mSettingsIntent);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override
    public void onResume() {
        mSettingsContentObserver.register(getContentResolver());
        updateSwitchBarToggleSwitch();
        super.onResume();
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
            case DIALOG_ID_ENABLE_WARNING: {
                mShownDialogId = DIALOG_ID_ENABLE_WARNING;

                final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }

                final AlertDialog ad = new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.enable_service_title,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setView(createEnableDialogContentView(info))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();

                final View.OnTouchListener filterTouchListener = new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // Filter obscured touches by consuming them.
                        if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) {
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                Toast.makeText(v.getContext(), R.string.touch_filtered_warning,
                                        Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        }
                        return false;
                    }
                };

                ad.create();
                ad.getButton(AlertDialog.BUTTON_POSITIVE).setOnTouchListener(filterTouchListener);
                return ad;
            }
            case DIALOG_ID_DISABLE_WARNING: {
                mShownDialogId = DIALOG_ID_DISABLE_WARNING;
                AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.disable_service_title,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setMessage(getString(R.string.disable_service_message,
                                info.getResolveInfo().loadLabel(getPackageManager())))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    private void updateSwitchBarToggleSwitch() {
        final String settingValue = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        final boolean checked = settingValue != null
                && settingValue.contains(mComponentName.flattenToString());
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

    private View createEnableDialogContentView(AccessibilityServiceInfo info) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                null);

        TextView encryptionWarningView = (TextView) content.findViewById(
                R.id.encryption_warning);
        if (isFullDiskEncrypted()) {
            String text = getString(R.string.enable_service_encryption_warning,
                    info.getResolveInfo().loadLabel(getPackageManager()));
            encryptionWarningView.setText(text);
            encryptionWarningView.setVisibility(View.VISIBLE);
        } else {
            encryptionWarningView.setVisibility(View.GONE);
        }

        TextView capabilitiesHeaderView = (TextView) content.findViewById(
                R.id.capabilities_header);
        capabilitiesHeaderView.setText(getString(R.string.capabilities_list_title,
                info.getResolveInfo().loadLabel(getPackageManager())));

        LinearLayout capabilitiesView = (LinearLayout) content.findViewById(R.id.capabilities);

        // This capability is implicit for all services.
        View capabilityView = inflater.inflate(
                com.android.internal.R.layout.app_permission_item_old, null);

        ImageView imageView = (ImageView) capabilityView.findViewById(
                com.android.internal.R.id.perm_icon);
        imageView.setImageDrawable(getActivity().getDrawable(
                com.android.internal.R.drawable.ic_text_dot));

        TextView labelView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_group);
        labelView.setText(getString(R.string.capability_title_receiveAccessibilityEvents));

        TextView descriptionView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_list);
        descriptionView.setText(getString(R.string.capability_desc_receiveAccessibilityEvents));

        List<AccessibilityServiceInfo.CapabilityInfo> capabilities =
                info.getCapabilityInfos();

        capabilitiesView.addView(capabilityView);

        // Service specific capabilities.
        final int capabilityCount = capabilities.size();
        for (int i = 0; i < capabilityCount; i++) {
            AccessibilityServiceInfo.CapabilityInfo capability = capabilities.get(i);

            capabilityView = inflater.inflate(
                    com.android.internal.R.layout.app_permission_item_old, null);

            imageView = (ImageView) capabilityView.findViewById(
                    com.android.internal.R.id.perm_icon);
            imageView.setImageDrawable(getActivity().getDrawable(
                    com.android.internal.R.drawable.ic_text_dot));

            labelView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_group);
            labelView.setText(getString(capability.titleResId));

            descriptionView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_list);
            descriptionView.setText(getString(capability.descResId));

            capabilitiesView.addView(capabilityView);
        }

        return content;
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final boolean checked;
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mShownDialogId == DIALOG_ID_ENABLE_WARNING) {
                    if (isFullDiskEncrypted()) {
                        String title = createConfirmCredentialReasonMessage();
                        Intent intent = ConfirmDeviceCredentialActivity.createIntent(title, null);
                        startActivityForResult(intent,
                                ACTIVITY_REQUEST_CONFIRM_CREDENTIAL_FOR_WEAKER_ENCRYPTION);
                    } else {
                        handleConfirmServiceEnabled(true);
                    }
                } else {
                    handleConfirmServiceEnabled(false);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                checked = (mShownDialogId == DIALOG_ID_DISABLE_WARNING);
                handleConfirmServiceEnabled(checked);
                break;
            default:
                throw new IllegalArgumentException();
        }
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
            } break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX: {
                resId = R.string.enable_service_pin_reason;
            } break;
        }
        return getString(resId, getAccessibilityServiceInfo().getResolveInfo()
                .loadLabel(getPackageManager()));
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    mSwitchBar.setCheckedInternal(false);
                    getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, false);
                    showDialog(DIALOG_ID_ENABLE_WARNING);
                } else {
                    mSwitchBar.setCheckedInternal(true);
                    getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, true);
                    showDialog(DIALOG_ID_DISABLE_WARNING);
                }
                return true;
            }
        });
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
    }
}

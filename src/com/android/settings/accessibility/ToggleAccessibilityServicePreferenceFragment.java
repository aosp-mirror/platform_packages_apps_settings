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
import android.app.Dialog;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fragment for providing toggle bar and basic accessibility service setup. */
public class ToggleAccessibilityServicePreferenceFragment extends
        ToggleFeaturePreferenceFragment {

    private static final String TAG = "ToggleAccessibilityServicePreferenceFragment";
    private static final String KEY_HAS_LOGGED = "has_logged";
    private final AtomicBoolean mIsDialogShown = new AtomicBoolean(/* initialValue= */ false);

    private Dialog mWarningDialog;
    private ComponentName mTileComponentName;
    private BroadcastReceiver mPackageRemovedReceiver;
    private boolean mDisabledStateLogged = false;
    private long mStartTimeMillsForLogging = 0;

    @Override
    public int getMetricsCategory() {
        return getArguments().getInt(AccessibilitySettings.EXTRA_METRICS_CATEGORY);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_HAS_LOGGED)) {
                mDisabledStateLogged = savedInstanceState.getBoolean(KEY_HAS_LOGGED);
            }
        }
    }

    @Override
    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        super.registerKeysToObserverCallback(contentObserver);
        contentObserver.registerObserverCallback(key -> updateSwitchBarToggleSwitch());
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mStartTimeMillsForLogging > 0) {
            outState.putBoolean(KEY_HAS_LOGGED, mDisabledStateLogged);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
        logAccessibilityServiceEnabled(toggledService, enabled);
        if (!enabled) {
            logDisabledState(toggledService.getPackageName());
        }
        AccessibilityUtils.setAccessibilityServiceState(getPrefContext(), toggledService, enabled);
    }

    // IMPORTANT: Refresh the info since there are dynamically changing capabilities. For
    // example, before JellyBean MR2 the user was granting the explore by touch one.
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
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        switch (dialogId) {
            case DialogEnums.ENABLE_WARNING_FROM_TOGGLE:
                if (info == null) {
                    return null;
                }
                if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
                    mWarningDialog =
                            com.android.internal.accessibility.dialog.AccessibilityServiceWarning
                                    .createAccessibilityServiceWarningDialog(getPrefContext(), info,
                                            v -> onAllowButtonFromEnableToggleClicked(),
                                            v -> onDenyButtonFromEnableToggleClicked(),
                                            v -> onDialogButtonFromUninstallClicked());
                } else {
                    mWarningDialog = AccessibilityServiceWarning
                            .createCapabilitiesDialog(getPrefContext(), info,
                                    this::onDialogButtonFromEnableToggleClicked,
                                    this::onDialogButtonFromUninstallClicked);
                }
                return mWarningDialog;
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE:
                if (info == null) {
                    return null;
                }
                if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
                    mWarningDialog =
                            com.android.internal.accessibility.dialog.AccessibilityServiceWarning
                                    .createAccessibilityServiceWarningDialog(getPrefContext(), info,
                                            v -> onAllowButtonFromShortcutToggleClicked(),
                                            v -> onDenyButtonFromShortcutToggleClicked(),
                                            v -> onDialogButtonFromUninstallClicked());
                } else {
                    mWarningDialog = AccessibilityServiceWarning
                            .createCapabilitiesDialog(getPrefContext(), info,
                                    this::onDialogButtonFromShortcutToggleClicked,
                                    this::onDialogButtonFromUninstallClicked);
                }
                return mWarningDialog;
            case DialogEnums.ENABLE_WARNING_FROM_SHORTCUT:
                if (info == null) {
                    return null;
                }
                if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
                    mWarningDialog =
                            com.android.internal.accessibility.dialog.AccessibilityServiceWarning
                                    .createAccessibilityServiceWarningDialog(getPrefContext(), info,
                                            v -> onAllowButtonFromShortcutClicked(),
                                            v -> onDenyButtonFromShortcutClicked(),
                                            v -> onDialogButtonFromUninstallClicked());
                } else {
                    mWarningDialog = AccessibilityServiceWarning
                            .createCapabilitiesDialog(getPrefContext(), info,
                                    this::onDialogButtonFromShortcutClicked,
                                    this::onDialogButtonFromUninstallClicked);
                }
                return mWarningDialog;
            case DialogEnums.DISABLE_WARNING_FROM_TOGGLE:
                if (info == null) {
                    return null;
                }
                mWarningDialog = AccessibilityServiceWarning
                        .createDisableDialog(getPrefContext(), info,
                                this::onDialogButtonFromDisableToggleClicked);
                return mWarningDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
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
    ComponentName getTileComponentName() {
        return mTileComponentName;
    }

    @Override
    CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
        final ComponentName componentName = getTileComponentName();
        if (componentName == null) {
            return null;
        }

        final CharSequence tileName = loadTileLabel(getPrefContext(), componentName);
        if (tileName == null) {
            return null;
        }

        final int titleResId = type == QuickSettingsTooltipType.GUIDE_TO_EDIT
                ? R.string.accessibility_service_qs_tooltip_content
                : R.string.accessibility_service_auto_added_qs_tooltip_content;
        return getString(titleResId, tileName);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    boolean serviceSupportsAccessibilityButton() {
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        return info != null
                && (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0;
    }

    private void handleConfirmServiceEnabled(boolean confirmed) {
        getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, confirmed);
        onPreferenceToggled(mPreferenceKey, confirmed);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isAccessibilityServiceEnabled()) {
            onPreferenceClick(isChecked);
        }
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        final int shortcutTypes = retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        if (preference.isChecked()) {
            final boolean isWarningRequired;
            if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
                isWarningRequired = getPrefContext().getSystemService(AccessibilityManager.class)
                        .isAccessibilityServiceWarningRequired(getAccessibilityServiceInfo());
            } else {
                isWarningRequired = !mToggleServiceSwitchPreference.isChecked();
            }
            if (isWarningRequired) {
                preference.setChecked(false);
                showPopupDialog(DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE);
            } else {
                onAllowButtonFromShortcutToggleClicked();
            }
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), shortcutTypes,
                    mComponentName);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        final boolean isWarningRequired;
        if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
            isWarningRequired = getPrefContext().getSystemService(AccessibilityManager.class)
                    .isAccessibilityServiceWarningRequired(getAccessibilityServiceInfo());
        } else {
            isWarningRequired = !(mShortcutPreference.isChecked()
                    || mToggleServiceSwitchPreference.isChecked());
        }

        if (isWarningRequired) {
            showPopupDialog(DialogEnums.ENABLE_WARNING_FROM_SHORTCUT);
        } else {
            onAllowButtonFromShortcutClicked();
        }
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

        if (arguments.containsKey(AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME)) {
            final String tileServiceComponentName = arguments.getString(
                    AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME);
            mTileComponentName = ComponentName.unflattenFromString(tileServiceComponentName);
        }

        mStartTimeMillsForLogging = arguments.getLong(AccessibilitySettings.EXTRA_TIME_FOR_LOGGING);
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
        mWarningDialog.dismiss();
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

    @Override
    protected int getPreferenceScreenResId() {
        // TODO(b/171272809): Add back when controllers move to static type
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private void onAllowButtonFromEnableToggleClicked() {
        handleConfirmServiceEnabled(/* confirmed= */ true);
        if (serviceSupportsAccessibilityButton()) {
            mIsDialogShown.set(false);
            showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        if (mWarningDialog != null) {
            mWarningDialog.dismiss();
        }
    }

    private void onDenyButtonFromEnableToggleClicked() {
        handleConfirmServiceEnabled(/* confirmed= */ false);
        mWarningDialog.dismiss();
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

    void onAllowButtonFromShortcutToggleClicked() {
        mShortcutPreference.setChecked(true);

        final int shortcutTypes = retrieveUserShortcutType(getPrefContext(),
                mComponentName.flattenToString(), UserShortcutType.SOFTWARE);
        AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes, mComponentName);

        mIsDialogShown.set(false);
        showPopupDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);

        if (mWarningDialog != null) {
            mWarningDialog.dismiss();
        }

        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    private void onDenyButtonFromShortcutToggleClicked() {
        mShortcutPreference.setChecked(false);

        mWarningDialog.dismiss();
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

        if (mWarningDialog != null) {
            mWarningDialog.dismiss();
        }
    }

    private void onDenyButtonFromShortcutClicked() {
        mWarningDialog.dismiss();
    }

    private boolean onPreferenceClick(boolean isChecked) {
        if (isChecked) {
            mToggleServiceSwitchPreference.setChecked(false);
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                    /* disableService */ false);
            final boolean isWarningRequired;
            if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
                isWarningRequired = getPrefContext().getSystemService(AccessibilityManager.class)
                        .isAccessibilityServiceWarningRequired(getAccessibilityServiceInfo());
            } else {
                isWarningRequired = !mShortcutPreference.isChecked();
            }
            if (isWarningRequired) {
                showPopupDialog(DialogEnums.ENABLE_WARNING_FROM_TOGGLE);
            } else {
                onAllowButtonFromEnableToggleClicked();
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

    private void logDisabledState(String packageName) {
        if (mStartTimeMillsForLogging > 0 && !mDisabledStateLogged) {
            AccessibilityStatsLogUtils.logDisableNonA11yCategoryService(
                    packageName,
                    SystemClock.elapsedRealtime() - mStartTimeMillsForLogging);
            mDisabledStateLogged = true;
        }
    }
}

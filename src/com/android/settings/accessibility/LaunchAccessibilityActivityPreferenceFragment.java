/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityStatsLogUtils.logAccessibilityServiceEnabled;

import android.accessibilityservice.AccessibilityShortcutInfo;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import java.util.List;

/** Fragment for providing open activity button. */
public class LaunchAccessibilityActivityPreferenceFragment extends
        ToggleFeaturePreferenceFragment {
    private static final String TAG = "LaunchA11yActivity";
    private static final String EMPTY_STRING = "";

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToggleServiceDividerSwitchPreference.setSwitchVisibility(View.GONE);
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        logAccessibilityServiceEnabled(mComponentName, enabled);
        launchShortcutTargetActivity(getPrefContext().getDisplayId(), mComponentName);
    }

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        super.onInstallSwitchPreferenceToggleSwitch();
        mToggleServiceDividerSwitchPreference.setOnPreferenceClickListener((preference) -> {
            final boolean checked = ((DividerSwitchPreference) preference).isChecked();
            onPreferenceToggled(mPreferenceKey, checked);
            return false;
        });
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        super.onProcessArguments(arguments);

        mComponentName = arguments.getParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME);
        final ActivityInfo info = getAccessibilityShortcutInfo().getActivityInfo();
        mPackageName = info.loadLabel(getPackageManager()).toString();

        // Settings animated image.
        final int animatedImageRes = arguments.getInt(
                AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES);
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mComponentName.getPackageName())
                .appendPath(String.valueOf(animatedImageRes))
                .build();

        // Settings html description.
        mHtmlDescription = arguments.getCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION);

        // Settings title and intent.
        final String settingsTitle = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_TITLE);
        mSettingsIntent = TextUtils.isEmpty(settingsTitle) ? null : getSettingsIntent(arguments);
        mSettingsTitle = (mSettingsIntent == null) ? null : settingsTitle;
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        super.onSettingsClicked(preference);
        showDialog(DialogEnums.EDIT_SHORTCUT);
    }

    @Override
    int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                mComponentName);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.
    }

    @Override
    protected void updateToggleServiceTitle(SwitchPreference switchPreference) {
        final AccessibilityShortcutInfo info = getAccessibilityShortcutInfo();
        final String switchBarText = (info == null) ? EMPTY_STRING : getString(
                R.string.accessibility_service_master_open_title,
                info.getActivityInfo().loadLabel(getPackageManager()));

        switchPreference.setTitle(switchBarText);
    }

    // IMPORTANT: Refresh the info since there are dynamically changing capabilities.
    private AccessibilityShortcutInfo getAccessibilityShortcutInfo() {
        final List<AccessibilityShortcutInfo> infos = AccessibilityManager.getInstance(
                getPrefContext()).getInstalledAccessibilityShortcutListAsUser(getPrefContext(),
                UserHandle.myUserId());

        for (int i = 0, count = infos.size(); i < count; i++) {
            AccessibilityShortcutInfo shortcutInfo = infos.get(i);
            ActivityInfo activityInfo = shortcutInfo.getActivityInfo();
            if (mComponentName.getPackageName().equals(activityInfo.packageName)
                    && mComponentName.getClassName().equals(activityInfo.name)) {
                return shortcutInfo;
            }
        }
        return null;
    }

    private void launchShortcutTargetActivity(int displayId, ComponentName name) {
        final Intent intent = new Intent();
        final Bundle bundle = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle();

        intent.setComponent(name);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            final int userId = UserHandle.myUserId();
            getPrefContext().startActivityAsUser(intent, bundle, UserHandle.of(userId));
        } catch (ActivityNotFoundException ignore) {
            // ignore the exception
            Log.w(TAG, "Target activity not found.");
        }
    }

    @Nullable
    private Intent getSettingsIntent(Bundle arguments) {
        final String settingsComponentName = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME);
        if (TextUtils.isEmpty(settingsComponentName)) {
            return null;
        }

        final Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName.unflattenFromString(settingsComponentName));
        if (getPackageManager().queryIntentActivities(settingsIntent, /* flags= */ 0).isEmpty()) {
            return null;
        }

        return settingsIntent;
    }
}

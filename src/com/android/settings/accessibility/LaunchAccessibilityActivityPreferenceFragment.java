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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;

/** Fragment for providing open activity button. */
public class LaunchAccessibilityActivityPreferenceFragment extends ToggleFeaturePreferenceFragment {
    private static final String TAG = "LaunchA11yActivity";
    private static final String EMPTY_STRING = "";
    protected static final String KEY_LAUNCH_PREFERENCE = "launch_preference";
    private ComponentName mTileComponentName;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        // Init new preference to replace the switch preference instead.
        initLaunchPreference();
        removePreference(KEY_USE_SERVICE_PREFERENCE);
        return view;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        // Do nothing.
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
        if (animatedImageRes > 0) {
            mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(mComponentName.getPackageName())
                    .appendPath(String.valueOf(animatedImageRes))
                    .build();
        }

        // Settings html description.
        mHtmlDescription = arguments.getCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION);

        // Settings title and intent.
        final String settingsTitle = arguments.getString(
                AccessibilitySettings.EXTRA_SETTINGS_TITLE);
        mSettingsIntent = TextUtils.isEmpty(settingsTitle) ? null : getSettingsIntent(arguments);
        mSettingsTitle = (mSettingsIntent == null) ? null : settingsTitle;

        // Tile service.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME)) {
            final String tileServiceComponentName = arguments.getString(
                    AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME);
            mTileComponentName = ComponentName.unflattenFromString(tileServiceComponentName);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do not call super. We don't want to see the "Help & feedback" option on this page so as
        // not to confuse users who think they might be able to send feedback about a specific
        // accessibility service from this page.
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

    /** Customizes the order by preference key. */
    protected List<String> getPreferenceOrderList() {
        final List<String> lists = new ArrayList<>();
        lists.add(KEY_ANIMATED_IMAGE);
        lists.add(KEY_LAUNCH_PREFERENCE);
        lists.add(KEY_GENERAL_CATEGORY);
        lists.add(KEY_HTML_DESCRIPTION_PREFERENCE);
        return lists;
    }

    private void initLaunchPreference() {
        final Preference launchPreference = new Preference(getPrefContext());
        launchPreference.setLayoutResource(R.layout.accessibility_launch_activity_preference);
        launchPreference.setKey(KEY_LAUNCH_PREFERENCE);

        final AccessibilityShortcutInfo info = getAccessibilityShortcutInfo();
        final String switchBarText = (info == null) ? EMPTY_STRING : getString(
                R.string.accessibility_service_primary_open_title,
                info.getActivityInfo().loadLabel(getPackageManager()));
        launchPreference.setTitle(switchBarText);

        launchPreference.setOnPreferenceClickListener(preference -> {
            logAccessibilityServiceEnabled(mComponentName, /* enabled= */ true);
            launchShortcutTargetActivity(getPrefContext().getDisplayId(), mComponentName);
            return true;
        });
        getPreferenceScreen().addPreference(launchPreference);
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

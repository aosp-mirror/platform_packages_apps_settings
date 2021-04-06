/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications.specialaccess.notificationaccess;

import static com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

public class BridgedAppsSettings extends DashboardFragment {

    private static final String TAG = "BridgedAppsSettings";

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 42;
    private static final String EXTRA_SHOW_SYSTEM = "show_system";

    private boolean mShowSystem;
    private AppFilter mFilter;

    private int mUserId;
    private ComponentName mComponentName;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mShowSystem = icicle != null && icicle.getBoolean(EXTRA_SHOW_SYSTEM);

        use(BridgedAppsPreferenceController.class).setNm(new NotificationBackend());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_SYSTEM:
                mShowSystem = !mShowSystem;
                item.setTitle(mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
                mFilter = mShowSystem ? ApplicationsState.FILTER_NOT_HIDE
                        : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;

                use(BridgedAppsPreferenceController.class).setFilter(mFilter).rebuild();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;

        final Bundle args = getArguments();
        Intent intent = (args == null) ?
                getIntent() : (Intent) args.getParcelable("intent");
        String cn = args.getString(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME);
        if (cn != null) {
            mComponentName = ComponentName.unflattenFromString(cn);
        }
        if (intent != null && intent.hasExtra(Intent.EXTRA_USER_HANDLE)) {
            mUserId = ((UserHandle) intent.getParcelableExtra(
                    Intent.EXTRA_USER_HANDLE)).getIdentifier();
        } else {
            mUserId = UserHandle.myUserId();
        }

        use(BridgedAppsPreferenceController.class)
                .setAppState(ApplicationsState.getInstance(
                        (Application) context.getApplicationContext()))
                .setCn(mComponentName)
                .setUserId(mUserId)
                .setSession(getSettingsLifecycle())
                .setFilter(mFilter)
                .rebuild();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ACCESS_BRIDGED_APPS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_access_bridged_apps_settings;
    }
}

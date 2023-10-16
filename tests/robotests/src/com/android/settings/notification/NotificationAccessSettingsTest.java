/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.notification.NotificationAccessSettings.ALLOWED_KEY;
import static com.android.settings.notification.NotificationAccessSettings.NOT_ALLOWED_KEY;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.base.Strings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class NotificationAccessSettingsTest {

    private Context mContext;
    private NotificationAccessSettings mAccessSettings;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mAccessSettings = new NotificationAccessSettings();
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        activity.getSupportFragmentManager().beginTransaction().add(mAccessSettings, null).commit();

        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt())).then(
                (Answer<ApplicationInfo>) invocation -> {
                    ApplicationInfo appInfo = mock(ApplicationInfo.class);
                    when(appInfo.loadLabel(any())).thenReturn(invocation.getArgument(0));
                    return appInfo;
                });

        mAccessSettings.mNm = mNotificationManager;
        mAccessSettings.mPm = mPackageManager;
        ShadowBluetoothUtils.sLocalBluetoothManager = mock(LocalBluetoothManager.class);
    }

    @Test
    public void updateList_enabledLongName_shown() {
        ComponentName longCn = new ComponentName("test.pkg1",
                Strings.repeat("Blah", 200) + "Service");
        ComponentName shortCn = new ComponentName("test.pkg2", "ReasonableService");
        ArrayList<ServiceInfo> services = new ArrayList<>();
        services.add(newServiceInfo(longCn.getPackageName(), longCn.getClassName(), 1));
        services.add(newServiceInfo(shortCn.getPackageName(), shortCn.getClassName(), 2));
        when(mNotificationManager.isNotificationListenerAccessGranted(any())).thenReturn(true);

        mAccessSettings.updateList(services);

        PreferenceScreen screen = mAccessSettings.getPreferenceScreen();
        PreferenceCategory allowed = checkNotNull(screen.findPreference(ALLOWED_KEY));
        PreferenceCategory notAllowed = checkNotNull(screen.findPreference(NOT_ALLOWED_KEY));
        assertThat(allowed.getPreferenceCount()).isEqualTo(2);
        assertThat(allowed.getPreference(0).getKey()).isEqualTo(longCn.flattenToString());
        assertThat(allowed.getPreference(1).getKey()).isEqualTo(shortCn.flattenToString());
        assertThat(notAllowed.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void updateList_disabledLongName_notShown() {
        ComponentName longCn = new ComponentName("test.pkg1",
                Strings.repeat("Blah", 200) + "Service");
        ComponentName shortCn = new ComponentName("test.pkg2", "ReasonableService");
        ArrayList<ServiceInfo> services = new ArrayList<>();
        services.add(newServiceInfo(longCn.getPackageName(), longCn.getClassName(), 1));
        services.add(newServiceInfo(shortCn.getPackageName(), shortCn.getClassName(), 2));
        when(mNotificationManager.isNotificationListenerAccessGranted(any())).thenReturn(false);

        mAccessSettings.updateList(services);

        PreferenceScreen screen = mAccessSettings.getPreferenceScreen();
        PreferenceCategory allowed = checkNotNull(screen.findPreference(ALLOWED_KEY));
        PreferenceCategory notAllowed = checkNotNull(screen.findPreference(NOT_ALLOWED_KEY));
        assertThat(allowed.getPreferenceCount()).isEqualTo(0);
        assertThat(notAllowed.getPreferenceCount()).isEqualTo(1);
        assertThat(notAllowed.getPreference(0).getKey()).isEqualTo(shortCn.flattenToString());
    }

    private static ServiceInfo newServiceInfo(String packageName, String serviceName, int uid) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = packageName;
        serviceInfo.name = serviceName;
        serviceInfo.applicationInfo = new ApplicationInfo();
        serviceInfo.applicationInfo.uid = uid;
        return serviceInfo;
    }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.role.RoleManager;
import android.app.usage.UsageEvents;
import android.bluetooth.BluetoothAdapter;
import android.companion.ICompanionDeviceManager;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;

import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NotificationBackendTest {

    @Mock
    LocalBluetoothManager mBm;
    @Mock
    ICompanionDeviceManager mCdm;
    @Mock
    CachedBluetoothDeviceManager mCbm;
    ComponentName mCn = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mBm.getCachedDeviceManager()).thenReturn(mCbm);
    }

    @Test
    public void testMarkAppRow_unblockablePackage() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = packageName;
        nonBlockablePkgs[1] = "some.other.package";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        // This package has a package lock but no locked channels
        assertTrue(appRow.lockedImportance);
    }

    @Test
    public void testMarkAppRow_defaultPackage() {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        List<String> roles = new ArrayList<>();
        roles.add(RoleManager.ROLE_DIALER);
        RoleManager rm = mock(RoleManager.class);
        when(rm.getHeldRolesFromController(anyString())).thenReturn(roles);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), rm, pi);

        assertTrue(appRow.systemApp);
    }

    @Test
    public void testMarkAppRow_notDefaultPackage() {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        List<String> roles = new ArrayList<>();
        roles.add(RoleManager.ROLE_HOME);
        RoleManager rm = mock(RoleManager.class);
        when(rm.getHeldRolesFromController(anyString())).thenReturn(roles);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), rm, pi);

        assertFalse(appRow.systemApp);
    }

    @Test
    public void testGetAggregatedUsageEvents_multipleEventsAgg() {
        List<UsageEvents.Event> events = new ArrayList<>();
        UsageEvents.Event good = new UsageEvents.Event();
        good.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good.mPackage = "pkg";
        good.mNotificationChannelId = "channel1";
        good.mTimeStamp = 2;
        events.add(good);
        UsageEvents.Event good2 = new UsageEvents.Event();
        good2.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good2.mPackage = "pkg";
        good2.mNotificationChannelId = "channel2";
        good2.mTimeStamp = 3;
        events.add(good2);
        UsageEvents.Event good1 = new UsageEvents.Event();
        good1.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good1.mPackage = "pkg";
        good1.mNotificationChannelId = "channel1";
        good1.mTimeStamp = 6;
        events.add(good1);
        NotificationBackend backend = new NotificationBackend();

        AppRow appRow = new AppRow();
        appRow.pkg = "pkg";
        backend.recordAggregatedUsageEvents(getUsageEvents(events), appRow);

        assertThat(appRow.sentByChannel.get("channel1").sentCount).isEqualTo(2);
        assertThat(appRow.sentByChannel.get("channel1").lastSent).isEqualTo(6);
        assertThat(appRow.sentByChannel.get("channel1").avgSentWeekly).isEqualTo(2);
        assertThat(appRow.sentByChannel.get("channel2").sentCount).isEqualTo(1);
        assertThat(appRow.sentByChannel.get("channel2").lastSent).isEqualTo(3);
        assertThat(appRow.sentByChannel.get("channel2").avgSentWeekly).isEqualTo(1);
        assertThat(appRow.sentByApp.sentCount).isEqualTo(3);
        assertThat(appRow.sentByApp.lastSent).isEqualTo(6);
        assertThat(appRow.sentByApp.avgSentWeekly).isEqualTo(3);
    }

    private UsageEvents getUsageEvents(List<UsageEvents.Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"pkg"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    @Test
    public void getDeviceList_noAssociations() throws Exception {
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(null);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn("00:00:00:00:00:10");
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        BluetoothAdapter.getDefaultAdapter().enable();

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEmpty();
    }

    @Test
    public void getDeviceList_associationsButNoDevice() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        when(mCbm.getCachedDevicesCopy()).thenReturn(new ArrayList<>());

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEmpty();
    }

    @Test
    public void getDeviceList_singleDevice() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs.get(0));
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEqualTo("Device 1");
    }

    @Test
    public void getDeviceList_multipleDevices() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs.get(0));
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);

        CachedBluetoothDevice cbd2 = mock(CachedBluetoothDevice.class);
        when(cbd2.getAddress()).thenReturn(macs.get(1));
        when(cbd2.getName()).thenReturn("Device 2");
        cachedDevices.add(cbd2);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEqualTo("Device 1, Device 2");
    }
}

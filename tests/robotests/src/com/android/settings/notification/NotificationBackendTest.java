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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.INotificationManager;
import android.app.role.RoleManager;
import android.app.usage.UsageEvents;
import android.bluetooth.BluetoothAdapter;
import android.companion.AssociationInfo;
import android.companion.ICompanionDeviceManager;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Build;
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
    @Mock
    INotificationManager mInm;
    NotificationBackend mNotificationBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mBm.getCachedDeviceManager()).thenReturn(mCbm);
        mNotificationBackend = new NotificationBackend();
        mNotificationBackend.setNm(mInm);
    }

    @Test
    public void testMarkAppRow_fixedImportance() throws Exception {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        pi.applicationInfo.uid = 123;

        when(mInm.isImportanceLocked(pi.packageName, 123)).thenReturn(true);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), pi);

        assertTrue(appRow.systemApp);
        assertTrue(appRow.lockedImportance);
    }

    @Test
    public void testMarkAppRow_notFixedPermission() throws Exception {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        pi.applicationInfo.uid = 123;

        when(mInm.isImportanceLocked(anyString(), anyInt())).thenReturn(false);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), pi);

        assertFalse(appRow.systemApp);
        assertFalse(appRow.lockedImportance);
    }

    @Test
    public void testMarkAppRow_targetsT_noPermissionRequest() throws Exception {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        pi.applicationInfo.uid = 123;
        pi.applicationInfo.targetSdkVersion= Build.VERSION_CODES.TIRAMISU;
        pi.requestedPermissions = new String[] {"something"};

        when(mInm.isPermissionFixed(pi.packageName, 0)).thenReturn(false);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), pi);

        assertFalse(appRow.systemApp);
        assertTrue(appRow.lockedImportance);
    }

    @Test
    public void testMarkAppRow_targetsT_permissionRequest() throws Exception {
        PackageInfo pi = new PackageInfo();
        pi.packageName = "test";
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = "test";
        pi.applicationInfo.uid = 123;
        pi.applicationInfo.targetSdkVersion= Build.VERSION_CODES.TIRAMISU;
        pi.requestedPermissions = new String[] {"something",
                android.Manifest.permission.POST_NOTIFICATIONS};

        when(mInm.isPermissionFixed(pi.packageName, 0)).thenReturn(false);

        AppRow appRow = new NotificationBackend().loadAppRow(RuntimeEnvironment.application,
                mock(PackageManager.class), pi);

        assertFalse(appRow.systemApp);
        assertFalse(appRow.lockedImportance);
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
        List<AssociationInfo> associations =
                mockAssociations("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(associations);

        when(mCbm.getCachedDevicesCopy()).thenReturn(new ArrayList<>());

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEmpty();
    }

    @Test
    public void getDeviceList_singleDevice() throws Exception {
        String[] macs = { "00:00:00:00:00:10", "00:00:00:00:00:20" };
        List<AssociationInfo> associations = mockAssociations(macs);
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(associations);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs[0]);
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEqualTo("Device 1");
    }

    @Test
    public void getDeviceList_multipleDevices() throws Exception {
        String[] macs = { "00:00:00:00:00:10", "00:00:00:00:00:20" };
        List<AssociationInfo> associations = mockAssociations(macs);
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(associations);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs[0]);
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);

        CachedBluetoothDevice cbd2 = mock(CachedBluetoothDevice.class);
        when(cbd2.getAddress()).thenReturn(macs[1]);
        when(cbd2.getName()).thenReturn("Device 2");
        cachedDevices.add(cbd2);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(new NotificationBackend().getDeviceList(
                mCdm, mBm, mCn.getPackageName(), 0).toString()).isEqualTo("Device 1, Device 2");
    }

    private ImmutableList<AssociationInfo> mockAssociations(String... macAddresses) {
        final AssociationInfo[] associations = new AssociationInfo[macAddresses.length];
        for (int index = 0; index < macAddresses.length; index++) {
            final AssociationInfo association = mock(AssociationInfo.class);
            when(association.isSelfManaged()).thenReturn(false);
            when(association.getDeviceMacAddress())
                    .thenReturn(MacAddress.fromString(macAddresses[index]));
            associations[index] = association;
        }
        return ImmutableList.copyOf(associations);
    }
}

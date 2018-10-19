/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.IconDrawableFactory;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class UtilsTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final int USER_ID = 1;

    @Mock
    private WifiManager wifiManager;
    @Mock
    private Network network;
    @Mock
    private ConnectivityManager connectivityManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IconDrawableFactory mIconDrawableFactory;
    @Mock
    private ApplicationInfo mApplicationInfo;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(wifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
    }

    @Test
    public void testGetWifiIpAddresses_succeeds() throws Exception {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        LinkAddress address = new LinkAddress(InetAddress.getByName("127.0.0.1"), 0);
        LinkProperties lp = new LinkProperties();
        lp.addLinkAddress(address);
        when(connectivityManager.getLinkProperties(network)).thenReturn(lp);

        assertThat(Utils.getWifiIpAddresses(mContext)).isEqualTo("127.0.0.1");
    }

    @Test
    public void testGetWifiIpAddresses_nullLinkProperties() {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        // Explicitly set the return value to null for readability sake.
        when(connectivityManager.getLinkProperties(network)).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void testGetWifiIpAddresses_nullNetwork() {
        // Explicitly set the return value to null for readability sake.
        when(wifiManager.getCurrentNetwork()).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void testInitializeVolumeDoesntBreakOnNullVolume() {
        VolumeInfo info = new VolumeInfo("id", 0, new DiskInfo("id", 0), "");
        StorageManager storageManager = mock(StorageManager.class, RETURNS_DEEP_STUBS);
        when(storageManager.findVolumeById(anyString())).thenReturn(info);

        Utils.maybeInitializeVolume(storageManager, new Bundle());
    }

    @Test
    public void testGetInstallationStatus_notInstalled_shouldReturnUninstalled() {
        assertThat(Utils.getInstallationStatus(new ApplicationInfo()))
                .isEqualTo(R.string.not_installed);
    }

    @Test
    public void testGetInstallationStatus_enabled_shouldReturnInstalled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.installed);
    }

    @Test
    public void testGetInstallationStatus_disabled_shouldReturnDisabled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = false;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.disabled);
    }

    @Test
    public void testIsProfileOrDeviceOwner_deviceOwnerApp_returnTrue() {
        when(mDevicePolicyManager.isDeviceOwnerAppOnAnyUser(PACKAGE_NAME)).thenReturn(true);

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager, PACKAGE_NAME))
            .isTrue();
    }

    @Test
    public void testIsProfileOrDeviceOwner_profileOwnerApp_returnTrue() {
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo());

        when(mUserManager.getUsers()).thenReturn(userInfos);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userInfos.get(0).id))
            .thenReturn(new ComponentName(PACKAGE_NAME, ""));

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager, PACKAGE_NAME))
            .isTrue();
    }

    @Test
    public void testSetEditTextCursorPosition_shouldGetExpectedEditTextLenght() {
        final EditText editText = new EditText(mContext);
        final CharSequence text = "test";
        editText.setText(text, TextView.BufferType.EDITABLE);
        final int length = editText.getText().length();
        Utils.setEditTextCursorPosition(editText);

        assertThat(editText.getSelectionEnd()).isEqualTo(length);
    }

    @Test
    public void testGetBadgedIcon_usePackageNameAndUserId()
        throws PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfoAsUser(
                PACKAGE_NAME, PackageManager.GET_META_DATA, USER_ID);

        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager, PACKAGE_NAME, USER_ID);

        // Verify that it uses the correct user id
        verify(mPackageManager).getApplicationInfoAsUser(eq(PACKAGE_NAME), anyInt(), eq(USER_ID));
        verify(mIconDrawableFactory).getBadgedIcon(mApplicationInfo, USER_ID);
    }
}

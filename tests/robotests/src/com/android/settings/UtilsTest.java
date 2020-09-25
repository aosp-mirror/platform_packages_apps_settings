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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActionBar;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.VectorDrawable;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2Manager;
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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowBinder;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
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
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void getWifiIpAddresses_succeeds() throws Exception {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        LinkAddress address = new LinkAddress(InetAddress.getByName("127.0.0.1"), 0);
        LinkProperties lp = new LinkProperties();
        lp.addLinkAddress(address);
        when(connectivityManager.getLinkProperties(network)).thenReturn(lp);

        assertThat(Utils.getWifiIpAddresses(mContext)).isEqualTo("127.0.0.1");
    }

    @Test
    public void getWifiIpAddresses_nullLinkProperties() {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        // Explicitly set the return value to null for readability sake.
        when(connectivityManager.getLinkProperties(network)).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void getWifiIpAddresses_nullNetwork() {
        // Explicitly set the return value to null for readability sake.
        when(wifiManager.getCurrentNetwork()).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void initializeVolumeDoesntBreakOnNullVolume() {
        VolumeInfo info = new VolumeInfo("id", 0, new DiskInfo("id", 0), "");
        StorageManager storageManager = mock(StorageManager.class, RETURNS_DEEP_STUBS);
        when(storageManager.findVolumeById(anyString())).thenReturn(info);

        Utils.maybeInitializeVolume(storageManager, new Bundle());
    }

    @Test
    public void getInstallationStatus_notInstalled_shouldReturnUninstalled() {
        assertThat(Utils.getInstallationStatus(new ApplicationInfo()))
                .isEqualTo(R.string.not_installed);
    }

    @Test
    public void getInstallationStatus_enabled_shouldReturnInstalled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.installed);
    }

    @Test
    public void getInstallationStatus_disabled_shouldReturnDisabled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = false;

        assertThat(Utils.getInstallationStatus(info)).isEqualTo(R.string.disabled);
    }

    @Test
    public void isProfileOrDeviceOwner_deviceOwnerApp_returnTrue() {
        when(mDevicePolicyManager.isDeviceOwnerAppOnAnyUser(PACKAGE_NAME)).thenReturn(true);

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager, PACKAGE_NAME))
            .isTrue();
    }

    @Test
    public void isProfileOrDeviceOwner_profileOwnerApp_returnTrue() {
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo());

        when(mUserManager.getUsers()).thenReturn(userInfos);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userInfos.get(0).id))
            .thenReturn(new ComponentName(PACKAGE_NAME, ""));

        assertThat(Utils.isProfileOrDeviceOwner(mUserManager, mDevicePolicyManager, PACKAGE_NAME))
            .isTrue();
    }

    @Test
    public void setEditTextCursorPosition_shouldGetExpectedEditTextLenght() {
        final EditText editText = new EditText(mContext);
        final CharSequence text = "test";
        editText.setText(text, TextView.BufferType.EDITABLE);
        final int length = editText.getText().length();
        Utils.setEditTextCursorPosition(editText);

        assertThat(editText.getSelectionEnd()).isEqualTo(length);
    }

    @Test
    public void createIconWithDrawable_BitmapDrawable() {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        final BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void createIconWithDrawable_ColorDrawable() {
        final ColorDrawable drawable = new ColorDrawable(Color.BLACK);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void createIconWithDrawable_VectorDrawable() {
        final VectorDrawable drawable = VectorDrawable.create(mContext.getResources(),
                R.drawable.ic_settings_accent);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void getBadgedIcon_usePackageNameAndUserId()
        throws PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfoAsUser(
                PACKAGE_NAME, PackageManager.GET_META_DATA, USER_ID);

        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager, PACKAGE_NAME, USER_ID);

        // Verify that it uses the correct user id
        verify(mPackageManager).getApplicationInfoAsUser(eq(PACKAGE_NAME), anyInt(), eq(USER_ID));
        verify(mIconDrawableFactory).getBadgedIcon(mApplicationInfo, USER_ID);
    }

    @Test
    public void isPackageEnabled_appEnabled_returnTrue()
            throws PackageManager.NameNotFoundException{
        mApplicationInfo.enabled = true;
        when(mPackageManager.getApplicationInfo(PACKAGE_NAME, 0)).thenReturn(mApplicationInfo);

        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isPackageEnabled_appDisabled_returnTrue()
            throws PackageManager.NameNotFoundException{
        mApplicationInfo.enabled = false;
        when(mPackageManager.getApplicationInfo(PACKAGE_NAME, 0)).thenReturn(mApplicationInfo);

        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void isPackageEnabled_noApp_returnFalse() {
        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void setActionBarShadowAnimation_nullParameters_shouldNotCrash() {
        // no crash here
        Utils.setActionBarShadowAnimation(null, null, null);
    }

    @Test
    public void setActionBarShadowAnimation_shouldSetElevationToZero() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final ActionBar actionBar = activity.getActionBar();

        Utils.setActionBarShadowAnimation(activity, activity.getLifecycle(),
                new ScrollView(mContext));

        assertThat(actionBar.getElevation()).isEqualTo(0.f);
    }

    @Test
    public void isSettingsIntelligence_IsSI_returnTrue() {
        final String siPackageName = mContext.getString(
                R.string.config_settingsintelligence_package_name);
        ShadowBinder.setCallingUid(USER_ID);
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{siPackageName});

        assertThat(Utils.isSettingsIntelligence(mContext)).isTrue();
    }

    @Test
    public void isSettingsIntelligence_IsNotSI_returnFalse() {
        ShadowBinder.setCallingUid(USER_ID);
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{PACKAGE_NAME});

        assertThat(Utils.isSettingsIntelligence(mContext)).isFalse();
    }

    @Test
    public void isMediaOutputDisabled_infosSizeEqual1_returnsTrue() {
        final MediaRouter2Manager router2Manager = mock(MediaRouter2Manager.class);
        final MediaRoute2Info info = mock(MediaRoute2Info.class);
        final List<MediaRoute2Info> infos = new ArrayList<>();
        infos.add(info);

        when(router2Manager.getAvailableRoutes(anyString())).thenReturn(infos);
        when(info.getType()).thenReturn(0);

        assertThat(Utils.isMediaOutputDisabled(router2Manager, "test")).isTrue();
    }

    @Test
    public void isMediaOutputDisabled_infosSizeOverThan1_returnsFalse() {
        final MediaRouter2Manager router2Manager = mock(MediaRouter2Manager.class);
        final MediaRoute2Info info = mock(MediaRoute2Info.class);
        final MediaRoute2Info info2 = mock(MediaRoute2Info.class);
        final List<MediaRoute2Info> infos = new ArrayList<>();
        infos.add(info);
        infos.add(info2);

        when(router2Manager.getAvailableRoutes(anyString())).thenReturn(infos);
        when(info.getType()).thenReturn(0);
        when(info2.getType()).thenReturn(0);

        assertThat(Utils.isMediaOutputDisabled(router2Manager, "test")).isFalse();
    }
}

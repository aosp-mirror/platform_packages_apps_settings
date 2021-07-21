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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(AndroidJUnit4.class)
public class WifiDppQrCodeGeneratorFragmentTest {

    private WifiDppConfiguratorActivity mActivity;
    private WifiDppQrCodeGeneratorFragment mFragment;
    private Context mContext;


    @Before
    public void setUp() {
        Intent intent =
                new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WPA");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "\\012345678,");

        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(WifiDppConfiguratorActivity.class);
        mActivity.setWifiNetworkConfig(WifiNetworkConfig.getValidConfigOrNull(intent));
        mActivity.startActivity(intent);

        mFragment = spy(new WifiDppQrCodeGeneratorFragment());
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.add(mFragment, null);
        ft.commit();

        mContext = spy(InstrumentationRegistry.getTargetContext());
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @Test
    public void rotateScreen_shouldNotCrash() {
        mActivity.setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivity.setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void createNearbyButton_returnsNull() {
        assertThat(mFragment.createNearbyButton(new Intent(), v -> {
        })).isNull();
    }

    private static ResolveInfo createResolveInfo(int userId) {
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = createActivityInfo();
        resolveInfo.targetUserId = userId;
        return resolveInfo;
    }

    private static ActivityInfo createActivityInfo() {
        ActivityInfo ai = new ActivityInfo();
        ai.name = "activity_name";
        ai.packageName = "foo_bar";
        ai.enabled = true;
        ai.exported = true;
        ai.permission = null;
        ai.applicationInfo = new ApplicationInfo();
        ai.applicationInfo.packageName = "com.google.android.gms";

        Bundle metadata = mock(Bundle.class);
        when(metadata.getInt(anyString())).thenReturn(1);
        ai.metaData = metadata;
        return ai;
    }

    @Test
    public void createNearbyButtonFromSetting_notNull()
            throws PackageManager.NameNotFoundException {
        doReturn(ComponentName.unflattenFromString(
                "com.google.android.gms/com.google.android.gms.nearby.sharing.ShareSheetActivity"))
                .when(mFragment).getNearbySharingComponent();
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(createResolveInfo(0)).when(packageManager).resolveActivity(any(), anyInt());

        Resources resources = mock(Resources.class);
        when(resources.getString(anyInt())).thenReturn("Nearby");
        Drawable drawable = mock(Drawable.class);
        when(resources.getDrawable(anyInt())).thenReturn(drawable);

        when(packageManager.getResourcesForActivity(any())).thenReturn(resources);

        when(mContext.getPackageManager()).thenReturn(packageManager);


        assertThat(mFragment.createNearbyButton(new Intent(), v -> {
        })).isNotNull();
    }

    @Test
    public void createNearbyButtonFromConfig_notNull() throws PackageManager.NameNotFoundException {
        doReturn(
                "com.google.android.gms/com.google.android.gms.nearby.sharing.ShareSheetActivity")
                .when(mFragment).getString(anyInt());
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(createResolveInfo(0)).when(packageManager).resolveActivity(any(), anyInt());

        Resources resources = mock(Resources.class);
        when(resources.getString(anyInt())).thenReturn("Nearby");
        Drawable drawable = mock(Drawable.class);
        when(resources.getDrawable(anyInt())).thenReturn(drawable);

        when(packageManager.getResourcesForActivity(any())).thenReturn(resources);

        when(mContext.getPackageManager()).thenReturn(packageManager);


        assertThat(mFragment.createNearbyButton(new Intent(), v -> {
        })).isNotNull();
    }
}

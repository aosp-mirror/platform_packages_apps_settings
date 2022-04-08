/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.DeviceConfig;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowDeviceConfig.class})
public class AdvancedBluetoothDetailsHeaderControllerTest {
    private static final int BATTERY_LEVEL_MAIN = 30;
    private static final int BATTERY_LEVEL_LEFT = 25;
    private static final int BATTERY_LEVEL_RIGHT = 45;
    private static final int LOW_BATTERY_LEVEL = 15;
    private static final int CASE_LOW_BATTERY_LEVEL = 19;
    private static final String ICON_URI = "content://test.provider/icon.png";
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";

    private Context mContext;

    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private Bitmap mBitmap;
    @Mock
    private ImageView mImageView;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    private AdvancedBluetoothDetailsHeaderController mController;
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new AdvancedBluetoothDetailsHeaderController(mContext, "pref_Key");
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        mController.init(mCachedDevice);
        mLayoutPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.advanced_bt_entity_header, null));
        mController.mLayoutPreference = mLayoutPreference;
        mController.mBluetoothAdapter = mBluetoothAdapter;
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
    }

    @Test
    public void createBatteryIcon_hasCorrectInfo() {
        final Drawable drawable = mController.createBtBatteryIcon(mContext, BATTERY_LEVEL_MAIN,
                true /* charging */);
        assertThat(drawable).isInstanceOf(BatteryMeterView.BatteryMeterDrawable.class);

        final BatteryMeterView.BatteryMeterDrawable iconDrawable =
                (BatteryMeterView.BatteryMeterDrawable) drawable;
        assertThat(iconDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL_MAIN);
        assertThat(iconDrawable.getCharging()).isTrue();
    }

    @Test
    public void refresh_connected_updateCorrectInfo() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_LEFT).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_RIGHT).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY)).thenReturn(
                String.valueOf(BATTERY_LEVEL_MAIN).getBytes());

        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.refresh();

        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_left), BATTERY_LEVEL_LEFT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_right), BATTERY_LEVEL_RIGHT);
        assertBatteryLevel(mLayoutPreference.findViewById(R.id.layout_middle), BATTERY_LEVEL_MAIN);
    }

    @Test
    public void refresh_disconnected_updateCorrectInfo() {
        when(mCachedDevice.isConnected()).thenReturn(false);

        mController.refresh();

        final LinearLayout layout = mLayoutPreference.findViewById(R.id.layout_middle);

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(layout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(layout.findViewById(R.id.header_title).getVisibility()).isEqualTo(View.GONE);
        assertThat(layout.findViewById(R.id.bt_battery_summary).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(layout.findViewById(R.id.bt_battery_icon).getVisibility()).isEqualTo(View.GONE);
        assertThat(layout.findViewById(R.id.header_icon).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void refresh_withLowBatteryAndUncharged_showAlertIcon() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY)).thenReturn(
                String.valueOf(LOW_BATTERY_LEVEL).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY)).thenReturn(
                String.valueOf(LOW_BATTERY_LEVEL).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY)).thenReturn(
                String.valueOf(CASE_LOW_BATTERY_LEVEL).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING)).thenReturn(
                String.valueOf(false).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING)).thenReturn(
                String.valueOf(true).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING)).thenReturn(
                String.valueOf(false).getBytes());
        when(mCachedDevice.isConnected()).thenReturn(true);

        mController.refresh();

        assertBatteryIcon(mLayoutPreference.findViewById(R.id.layout_left),
                R.drawable.ic_battery_alert_24dp);
        assertBatteryIcon(mLayoutPreference.findViewById(R.id.layout_right), /* resId= */-1);
        assertBatteryIcon(mLayoutPreference.findViewById(R.id.layout_middle),
                R.drawable.ic_battery_alert_24dp);
    }

    @Test
    public void getAvailabilityStatus_untetheredHeadsetWithConfigOn_returnAvailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "true", true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_untetheredHeadsetWithConfigOff_returnUnavailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "false", true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notUntetheredHeadsetWithConfigOn_returnUnavailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "true", true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notUntetheredHeadsetWithConfigOff_returnUnavailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "false", true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateIcon_existInCache_setImageBitmap() {
        mController.mIconCache.put(ICON_URI, mBitmap);

        mController.updateIcon(mImageView, ICON_URI);

        verify(mImageView).setImageBitmap(mBitmap);
    }

    @Test
    public void onStart_isAvailable_registerCallback() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "true", true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        mController.onStart();

        verify(mBluetoothAdapter).addOnMetadataChangedListener(mBluetoothDevice,
                mContext.getMainExecutor(), mController.mMetadataListener);
    }

    @Test
    public void onStop_isRegisterCallback_unregisterCallback() {
        mController.mIsRegisterCallback = true;

        mController.onStop();

        verify(mBluetoothAdapter).removeOnMetadataChangedListener(mBluetoothDevice,
                mController.mMetadataListener);
    }

    @Test
    public void onStart_notAvailable_registerCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        mController.onStart();

        verify(mBluetoothAdapter, never()).addOnMetadataChangedListener(mBluetoothDevice,
                mContext.getMainExecutor(), mController.mMetadataListener);
    }

    @Test
    public void onStop_notRegisterCallback_unregisterCallback() {
        mController.mIsRegisterCallback = false;

        mController.onStop();

        verify(mBluetoothAdapter, never()).removeOnMetadataChangedListener(mBluetoothDevice,
                mController.mMetadataListener);
    }

    @Test
    public void onDestroy_recycleBitmap() {
        mController.mIconCache.put(ICON_URI, mBitmap);

        mController.onDestroy();

        assertThat(mController.mIconCache).isEmpty();
        verify(mBitmap).recycle();
    }

    private void assertBatteryLevel(LinearLayout linearLayout, int batteryLevel) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
        assertThat(textView.getText().toString()).isEqualTo(
                com.android.settings.Utils.formatPercentage(batteryLevel));
    }

    private void assertBatteryIcon(LinearLayout linearLayout, int resId) {
        final ImageView imageView = linearLayout.findViewById(R.id.bt_battery_icon);
        assertThat(shadowOf(imageView.getDrawable()).getCreatedFromResId())
                .isEqualTo(resId);
    }

}

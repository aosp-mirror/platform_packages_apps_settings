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
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowDeviceConfig.class})
public class AdvancedBluetoothDetailsHeaderControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int BATTERY_LEVEL_MAIN = 30;
    private static final int BATTERY_LEVEL_LEFT = 25;
    private static final int BATTERY_LEVEL_RIGHT = 45;
    private static final int LOW_BATTERY_LEVEL = 15;
    private static final int CASE_LOW_BATTERY_LEVEL = 19;
    private static final int LOW_BATTERY_LEVEL_THRESHOLD = 15;
    private static final int BATTERY_LEVEL_5 = 5;
    private static final int BATTERY_LEVEL_50 = 50;
    private static final String ICON_URI = "content://test.provider/icon.png";
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String DEVICE_SUMMARY = "test summary";

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
    @Mock
    private PreferenceFragmentCompat mFragment;
    private AdvancedBluetoothDetailsHeaderController mController;
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = Robolectric.buildActivity(SettingsActivity.class).get();
        mController = new AdvancedBluetoothDetailsHeaderController(mContext, "pref_Key");
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        mController.init(mCachedDevice, mFragment);
        mLayoutPreference = new LayoutPreference(mContext,
                LayoutInflater.from(mContext).inflate(R.layout.advanced_bt_entity_header, null));
        mController.mLayoutPreference = mLayoutPreference;
        mController.mBluetoothAdapter = mBluetoothAdapter;
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice.getIdentityAddress()).thenReturn(MAC_ADDRESS);
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
    public void refresh_connectedWatch_behaveAsExpected() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_WATCH.getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(false).getBytes());
        when(mCachedDevice.isConnected()).thenReturn(true);

        mController.refresh();

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_middle).getVisibility()).isEqualTo(
                View.VISIBLE);
        // TODO (b/188954766) : clarify settings design
    }

    @Test
    public void refresh_connectedWatch_unknownBatteryLevel_shouldNotShowBatteryLevel() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_WATCH.getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(false).getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_MAIN_BATTERY)).thenReturn(
                String.valueOf(BluetoothUtils.META_INT_ERROR).getBytes());
        when(mBluetoothDevice.getBatteryLevel()).thenReturn(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        when(mCachedDevice.isConnected()).thenReturn(true);

        mController.refresh();

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_middle).getVisibility()).isEqualTo(
                View.VISIBLE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_middle)
                .requireViewById(R.id.bt_battery_summary).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void refresh_connectedStylus_behaveAsExpected() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_STYLUS.getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(false).getBytes());
        when(mCachedDevice.isConnected()).thenReturn(true);

        mController.refresh();

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_middle).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void refresh_connectedUnknownType_behaveAsExpected() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                "UNKNOWN_TYPE".getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(false).getBytes());
        when(mCachedDevice.isConnected()).thenReturn(true);

        mController.refresh();

        assertThat(mLayoutPreference.findViewById(R.id.layout_left).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_right).getVisibility()).isEqualTo(
                View.GONE);
        assertThat(mLayoutPreference.findViewById(R.id.layout_middle).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void refresh_connectedUntetheredHeadset_behaveAsExpected() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET.getBytes());
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(false).getBytes());
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
    public void refresh_connected_updateCorrectInfo() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(true).getBytes());
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
        assertThat(layout.findViewById(R.id.battery_ring).getVisibility()).isEqualTo(View.GONE);
    }

    @Ignore
    @Test
    public void refresh_connectedWatch_checkSummary() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_DEVICE_TYPE)).thenReturn(
                BluetoothDevice.DEVICE_TYPE_WATCH.getBytes());
        when(mCachedDevice.isConnected()).thenReturn(true);
        when(mCachedDevice.getConnectionSummary(/* shortSummary= */ true))
                .thenReturn(DEVICE_SUMMARY);

        mController.refresh();

        assertThat(((TextView) (mLayoutPreference.findViewById(R.id.entity_header_summary)))
                .getText()).isEqualTo(DEVICE_SUMMARY);
    }

    @Test
    public void refresh_withLowBatteryAndUncharged_showAlertIcon() {
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn(
                String.valueOf(true).getBytes());
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
    public void getAvailabilityStatus_untetheredHeadset_returnAvailable() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notUntetheredHeadset_returnUnavailable() {
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
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());
        Set<CachedBluetoothDevice> cacheBluetoothDevices = new HashSet<>();
        when(mCachedDevice.getMemberDevice()).thenReturn(cacheBluetoothDevices);
        when(mBluetoothAdapter.addOnMetadataChangedListener(
                mBluetoothDevice, mContext.getMainExecutor(), mController.mMetadataListener))
                .thenReturn(true);

        mController.onStart();

        verify(mCachedDevice).registerCallback(mController);
        verify(mBluetoothAdapter).addOnMetadataChangedListener(mBluetoothDevice,
                mContext.getMainExecutor(), mController.mMetadataListener);
    }

    @Test
    public void onStart_notAvailable_notNeedToRegisterCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        mController.onStart();

        verify(mCachedDevice, never()).registerCallback(mController);
        verify(mBluetoothAdapter, never()).addOnMetadataChangedListener(mBluetoothDevice,
                mContext.getMainExecutor(), mController.mMetadataListener);
    }

    @Test
    public void onStart_isAvailableButNoBluetoothDevice_notNeedToRegisterCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());
        when(mCachedDevice.getDevice()).thenReturn(null);
        Set<CachedBluetoothDevice> cacheBluetoothDevices = new HashSet<>();
        when(mCachedDevice.getMemberDevice()).thenReturn(cacheBluetoothDevices);

        mController.onStart();

        verify(mCachedDevice, never()).registerCallback(mController);
        verify(mBluetoothAdapter, never()).addOnMetadataChangedListener(mBluetoothDevice,
                mContext.getMainExecutor(), mController.mMetadataListener);
    }

    @Test
    public void onStop_availableAndHasBluetoothDevice_unregisterCallback() {
        onStart_isAvailable_registerCallback();

        mController.onStop();

        verify(mCachedDevice).unregisterCallback(mController);
        verify(mBluetoothAdapter).removeOnMetadataChangedListener(mBluetoothDevice,
                mController.mMetadataListener);
    }

    @Test
    public void onStop_noBluetoothDevice_noNeedToUnregisterCallback() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());
        when(mCachedDevice.getDevice()).thenReturn(null);

        mController.onStart();
        mController.onStop();

        verify(mCachedDevice, never()).unregisterCallback(mController);
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

    @Test
    public void estimateReadyIsBothAvailable_showsView() {
        mController.mIsLeftDeviceEstimateReady = true;
        mController.mIsRightDeviceEstimateReady = true;

        mController.showBothDevicesBatteryPredictionIfNecessary();

        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_left),
                View.VISIBLE);
        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_right),
                View.VISIBLE);
    }

    @Test
    public void leftDeviceEstimateIsReadyRightDeviceIsNotReady_notShowView() {
        mController.mIsLeftDeviceEstimateReady = true;
        mController.mIsRightDeviceEstimateReady = false;

        mController.showBothDevicesBatteryPredictionIfNecessary();

        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_left),
                View.GONE);
        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_right),
                View.GONE);
    }

    @Test
    public void leftDeviceEstimateIsNotReadyRightDeviceIsReady_notShowView() {
        mController.mIsLeftDeviceEstimateReady = false;
        mController.mIsRightDeviceEstimateReady = true;

        mController.showBothDevicesBatteryPredictionIfNecessary();

        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_left),
                View.GONE);
        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_right),
                View.GONE);
    }

    @Test
    public void bothDevicesEstimateIsNotReady_notShowView() {
        mController.mIsLeftDeviceEstimateReady = false;
        mController.mIsRightDeviceEstimateReady = false;

        mController.showBothDevicesBatteryPredictionIfNecessary();

        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_left),
                View.GONE);
        assertBatteryPredictionVisible(mLayoutPreference.findViewById(R.id.layout_right),
                View.GONE);
    }

    @Test
    public void showBatteryPredictionIfNecessary_estimateReadyIsAvailable_showCorrectValue() {
        final String leftBatteryPrediction =
                StringUtil.formatElapsedTime(mContext, 12000000, false, false).toString();
        final String rightBatteryPrediction =
                StringUtil.formatElapsedTime(mContext, 1200000, false, false).toString();

        mController.showBatteryPredictionIfNecessary(1, 12000000,
                mLayoutPreference.findViewById(R.id.layout_left));
        mController.showBatteryPredictionIfNecessary(1, 1200000,
                mLayoutPreference.findViewById(R.id.layout_right));

        assertBatteryPrediction(mLayoutPreference.findViewById(R.id.layout_left),
                leftBatteryPrediction);
        assertBatteryPrediction(mLayoutPreference.findViewById(R.id.layout_right),
                rightBatteryPrediction);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void enablePolishFlag_renameButtonShown() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());
        Set<CachedBluetoothDevice> cacheBluetoothDevices = new HashSet<>();
        when(mCachedDevice.getMemberDevice()).thenReturn(cacheBluetoothDevices);

        mController.onStart();

        ImageButton button = mLayoutPreference.findViewById(R.id.rename_button);
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void assertBatteryPredictionVisible(LinearLayout linearLayout, int visible) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_prediction);
        assertThat(textView.getVisibility()).isEqualTo(visible);
    }

    private void assertBatteryPrediction(LinearLayout linearLayout, String prediction) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_prediction);
        assertThat(textView.getText().toString()).isEqualTo(prediction);
    }

    private void assertBatteryLevel(LinearLayout linearLayout, int batteryLevel) {
        final TextView textView = linearLayout.findViewById(R.id.bt_battery_summary);
        assertThat(textView.getText().toString()).isEqualTo(
                com.android.settings.Utils.formatPercentage(batteryLevel));
        if (Flags.enableBluetoothDeviceDetailsPolish()) {
            final ProgressBar bar = linearLayout.findViewById(R.id.battery_ring);
            assertThat(bar.getProgress()).isEqualTo(batteryLevel);
        }
    }

    private void assertBatteryIcon(LinearLayout linearLayout, int resId) {
        final ImageView imageView = linearLayout.findViewById(R.id.bt_battery_icon);
        assertThat(shadowOf(imageView.getDrawable()).getCreatedFromResId())
                .isEqualTo(resId);
    }

}

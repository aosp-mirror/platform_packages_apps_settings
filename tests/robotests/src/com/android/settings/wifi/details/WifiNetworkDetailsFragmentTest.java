/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.wifi.details;

import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_CELLULAR;
import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_ETHERNET;
import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_UNKNOWN;
import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_WIFI;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_GREAT;

import static com.android.settings.network.NetworkProviderSettings.WIFI_DIALOG_ID;
import static com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_HOTSPOT_CONNECTION_CATEGORY;
import static com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_HOTSPOT_DEVICE_BATTERY;
import static com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_HOTSPOT_DEVICE_CATEGORY;
import static com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_HOTSPOT_DEVICE_INTERNET_SOURCE;
import static com.android.settingslib.Utils.formatPercentage;
import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MAX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.details2.WifiDetailPreferenceController2;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkDetailsFragmentTest {

    static final String TEST_PREFERENCE_KEY = "TEST_PREFERENCE_KEY";
    static final int BATTERY_PERCENTAGE_MAX = 100;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PreferenceManager mPreferenceManager;
    @Mock
    Preference mHotspotDeviceCategory;
    @Mock
    Preference mInternetSource;
    @Mock
    Preference mBattery;
    @Mock
    Preference mHotspotConnectionCategory;
    @Mock
    Menu mMenu;
    @Mock
    Drawable mDrawable;
    @Mock
    WifiDetailPreferenceController2 mWifiDetailPreferenceController2;
    @Mock
    WifiEntry mWifiEntry;
    @Mock
    NetworkDetailsTracker mNetworkDetailsTracker;
    @Mock
    WifiNetworkDetailsViewModel.HotspotNetworkData mHotspotNetworkData;

    FakeFragment mFragment;
    PreferenceScreen mScreen;

    @Before
    public void setUp() {
        doReturn(mWifiEntry).when(mNetworkDetailsTracker).getWifiEntry();
        doReturn(true).when(mWifiEntry).isSaved();
        doReturn(NETWORK_TYPE_WIFI).when(mHotspotNetworkData).getNetworkType();
        doReturn(WIFI_LEVEL_MAX).when(mHotspotNetworkData).getUpstreamConnectionStrength();
        doReturn(BATTERY_PERCENTAGE_MAX).when(mHotspotNetworkData).getBatteryPercentage();
        doReturn(true).when(mHotspotNetworkData).isBatteryCharging();

        mFragment = spy(new FakeFragment());
        doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();
        doReturn(mContext).when(mFragment).getContext();
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        doReturn(mPreferenceManager).when(mScreen).getPreferenceManager();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();

        doReturn(mHotspotDeviceCategory).when(mScreen).findPreference(KEY_HOTSPOT_DEVICE_CATEGORY);
        doReturn(mInternetSource).when(mScreen).findPreference(KEY_HOTSPOT_DEVICE_INTERNET_SOURCE);
        doReturn(mBattery).when(mScreen).findPreference(KEY_HOTSPOT_DEVICE_BATTERY);
        doReturn(mHotspotConnectionCategory).when(mScreen)
                .findPreference(KEY_HOTSPOT_CONNECTION_CATEGORY);
        mFragment.mNetworkDetailsTracker = mNetworkDetailsTracker;
    }

    @Test
    public void getMetricsCategory_shouldReturnWifiNetworkDetails() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.WIFI_NETWORK_DETAILS);
    }

    @Test
    public void getDialogMetricsCategory_withCorrectId_shouldReturnDialogWifiApEdit() {
        assertThat(mFragment.getDialogMetricsCategory(WIFI_DIALOG_ID)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_AP_EDIT);
    }

    @Test
    public void getDialogMetricsCategory_withWrongId_shouldReturnZero() {
        assertThat(mFragment.getDialogMetricsCategory(-1 /* dialogId */)).isEqualTo(0);
    }

    @Test
    public void onCreateOptionsMenu_shouldSetCorrectIcon() {
        MenuItem menuItem = mock(MenuItem.class);
        doReturn(menuItem).when(mMenu).add(anyInt(), eq(Menu.FIRST), anyInt(), anyInt());

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(menuItem).setIcon(com.android.internal.R.drawable.ic_mode_edit);
    }

    @Test
    public void onCreateOptionsMenu_isNotSavedNetwork_shouldNotAddEditMenu() {
        doReturn(false).when(mWifiEntry).isSaved();

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), eq(R.string.wifi_modify));
    }

    @Test
    public void onCreateOptionsMenu_uiRestricted_shouldNotAddEditMenu() {
        mFragment.mIsUiRestricted = true;

        mFragment.onCreateOptionsMenu(mMenu, mock(MenuInflater.class));

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), eq(R.string.wifi_modify));
    }

    @Test
    public void restrictUi_shouldShowRestrictedText() {
        TextView restrictedText = mock(TextView.class);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(false).when(mFragment).isUiRestrictedByOnlyAdmin();
        doReturn(restrictedText).when(mFragment).getEmptyTextView();

        mFragment.restrictUi();

        verify(restrictedText).setText(anyInt());
    }

    @Test
    public void restrictUi_shouldRemoveAllPreferences() {
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(true).when(mFragment).isUiRestrictedByOnlyAdmin();

        mFragment.restrictUi();

        verify(mScreen).removeAll();
    }

    @Test
    public void refreshPreferences_controllerShouldUpdateStateAndDisplayPreference() {
        Preference preference = mock(Preference.class);
        TestController controller = mock(TestController.class);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(preference).when(mScreen).findPreference(TEST_PREFERENCE_KEY);
        doReturn(TEST_PREFERENCE_KEY).when(controller).getPreferenceKey();
        mFragment.mControllers = new ArrayList<>();
        mFragment.mControllers.add(controller);
        mFragment.addPreferenceController(controller);

        mFragment.refreshPreferences();

        verify(controller).updateState(preference);
        verify(controller).displayPreference(mScreen);
    }

    @Test
    public void onHotspotNetworkChanged_dataNull_hotspotSetVisibleFalse() {
        mFragment.mWifiDetailPreferenceController2 = mWifiDetailPreferenceController2;

        mFragment.onHotspotNetworkChanged(null);

        verify(mHotspotDeviceCategory).setVisible(false);
        verify(mHotspotConnectionCategory).setVisible(false);
        verify(mWifiDetailPreferenceController2).setSignalStrengthTitle(R.string.wifi_signal);
    }

    @Test
    public void onHotspotNetworkChanged_dataNotNull_hotspotSetVisibleTrue() {
        mFragment.mWifiDetailPreferenceController2 = mWifiDetailPreferenceController2;

        mFragment.onHotspotNetworkChanged(mHotspotNetworkData);

        verify(mHotspotDeviceCategory).setVisible(true);
        verify(mFragment).updateInternetSource(mHotspotNetworkData.getNetworkType(),
                mHotspotNetworkData.getUpstreamConnectionStrength());
        verify(mFragment).updateBattery(mHotspotNetworkData.isBatteryCharging(),
                mHotspotNetworkData.getBatteryPercentage());
        verify(mHotspotConnectionCategory).setVisible(true);
        verify(mWifiDetailPreferenceController2)
                .setSignalStrengthTitle(R.string.hotspot_connection_strength);
    }

    @Test
    public void updateInternetSource_networkTypeWifi_setWifiResource() {
        doReturn(mDrawable).when(mContext)
                .getDrawable(WifiUtils.getInternetIconResource(WIFI_LEVEL_MAX, false));

        mFragment.updateInternetSource(NETWORK_TYPE_WIFI, WIFI_LEVEL_MAX);

        verify(mInternetSource).setSummary(R.string.internet_source_wifi);
        verify(mInternetSource).setIcon(mDrawable);
    }

    @Test
    public void updateInternetSource_networkTypeMobileData_setMobileDataResource() {
        doReturn(mDrawable).when(mFragment).getMobileDataIcon(SIGNAL_STRENGTH_GREAT);

        mFragment.updateInternetSource(NETWORK_TYPE_CELLULAR, SIGNAL_STRENGTH_GREAT);

        verify(mInternetSource).setSummary(R.string.internet_source_mobile_data);
        verify(mInternetSource).setIcon(mDrawable);
    }

    @Test
    public void updateInternetSource_networkTypeEthernet_setEthernetResource() {
        doReturn(mDrawable).when(mContext).getDrawable(R.drawable.ic_settings_ethernet);

        mFragment.updateInternetSource(NETWORK_TYPE_ETHERNET, 0 /* don't care */);

        verify(mInternetSource).setSummary(R.string.internet_source_ethernet);
        verify(mInternetSource).setIcon(mDrawable);
    }

    @Test
    public void updateInternetSource_networkTypeUnknown_setPlaceholderResource() {
        mFragment.updateInternetSource(NETWORK_TYPE_UNKNOWN, 0 /* don't care */);

        verify(mInternetSource).setSummary(R.string.summary_placeholder);
        verify(mInternetSource).setIcon(null);
    }

    @Test
    public void updateBattery_hiPercentageNoCharging_setSummaryCorrect() {
        mFragment.updateBattery(false /* isChanging */, BATTERY_PERCENTAGE_MAX);

        verify(mBattery).setSummary(formatPercentage(BATTERY_PERCENTAGE_MAX));
    }

    @Test
    public void updateBattery_lowPercentageWithCharging_setSummaryCorrect() {
        String summary = mContext.getString(R.string.hotspot_battery_charging_summary,
                formatPercentage(0));

        mFragment.updateBattery(true /* isChanging */, 0 /* percentage */);

        verify(mBattery).setSummary(summary);
    }

    // Fake WifiNetworkDetailsFragment to override the protected method as public.
    public static class FakeFragment extends WifiNetworkDetailsFragment {

        @Override
        public void addPreferenceController(AbstractPreferenceController controller) {
            super.addPreferenceController(controller);
        }

        @Override
        public boolean isUiRestrictedByOnlyAdmin() {
            return super.isUiRestrictedByOnlyAdmin();
        }
    }

    public static class TestController extends BasePreferenceController {

        public TestController() {
            super(RuntimeEnvironment.application, TEST_PREFERENCE_KEY);
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}

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
 * limitations under the License.
 */

package com.android.settings.network;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.TetheringManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class TetherPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private Preference mPreference;

    private TetherPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = spy(new TetherPreferenceController(mContext, /* lifecycle= */ null));
        ReflectionHelpers.setField(mController, "mContext", mContext);
        ReflectionHelpers.setField(mController, "mTetheringManager", mTetheringManager);
        ReflectionHelpers.setField(mController, "mBluetoothAdapter", mBluetoothAdapter);
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
    }

    @Test
    public void lifeCycle_onCreate_shouldInitBluetoothPan() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mController.onCreate(null);

        verify(mBluetoothAdapter).getState();
        verify(mBluetoothAdapter).getProfileProxy(mContext, mController.mBtProfileServiceListener,
                BluetoothProfile.PAN);
    }

    @Test
    public void lifeCycle_onCreate_shouldNotInitBluetoothPanWhenBluetoothOff() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_OFF);
        mController.onCreate(null);

        verify(mBluetoothAdapter).getState();
        verifyNoMoreInteractions(mBluetoothAdapter);
    }

    @Test
    public void goThroughLifecycle_shouldDestoryBluetoothProfile() {
        final BluetoothPan pan = mock(BluetoothPan.class);
        final AtomicReference<BluetoothPan> panRef =
                ReflectionHelpers.getField(mController, "mBluetoothPan");
        panRef.set(pan);

        mController.onDestroy();

        verify(mBluetoothAdapter).closeProfileProxy(BluetoothProfile.PAN, pan);
    }

    @Test
    public void updateSummary_noPreference_noInteractionWithTetheringManager() {
        ReflectionHelpers.setField(mController, "mPreference", null);
        mController.updateSummary();
        verifyNoMoreInteractions(mTetheringManager);
    }

    @Test
    public void updateSummary_wifiTethered_shouldShowHotspotMessage() {
        when(mTetheringManager.getTetheredIfaces()).thenReturn(new String[]{"123"});
        when(mTetheringManager.getTetherableWifiRegexs()).thenReturn(new String[]{"123"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_on_tether_off);
    }

    @Test
    public void updateSummary_btThetherOn_shouldShowTetherMessage() {
        when(mTetheringManager.getTetheredIfaces()).thenReturn(new String[]{"123"});
        when(mTetheringManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"123"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_off_tether_on);
    }

    @Ignore
    @Test
    public void updateSummary_tetherOff_shouldShowTetherOffMessage() {
        when(mTetheringManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"123"});
        when(mTetheringManager.getTetherableWifiRegexs()).thenReturn(new String[]{"456"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.switch_off_text);
    }

    @Test
    public void updateSummary_wifiBtTetherOn_shouldShowHotspotAndTetherMessage() {
        when(mTetheringManager.getTetheredIfaces()).thenReturn(new String[]{"123", "456"});
        when(mTetheringManager.getTetherableWifiRegexs()).thenReturn(new String[]{"456"});
        when(mTetheringManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"23"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_on_tether_on);
    }

    @Ignore
    @Test
    public void airplaneModeOn_shouldUpdateSummaryToOff() {
        final Context context = RuntimeEnvironment.application;
        ReflectionHelpers.setField(mController, "mContext", context);

        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        mController.onResume();

        verifyNoInteractions(mPreference);

        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        final ContentObserver observer =
            ReflectionHelpers.getField(mController, "mAirplaneModeObserver");
        observer.onChange(true, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        verify(mPreference).setSummary(R.string.switch_off_text);
    }

    @Test
    public void onResume_shouldRegisterTetherReceiver() {
        when(mContext.getContentResolver()).thenReturn(mock(ContentResolver.class));

        mController.onResume();

        verify(mContext).registerReceiver(
                any(TetherPreferenceController.TetherBroadcastReceiver.class),
                any(IntentFilter.class));
    }

    @Test
    public void onPause_shouldUnregisterTetherReceiver() {
        when(mContext.getContentResolver()).thenReturn(mock(ContentResolver.class));
        mController.onResume();

        mController.onPause();

        verify(mContext)
            .unregisterReceiver(any(TetherPreferenceController.TetherBroadcastReceiver.class));
    }

    @Test
    public void tetherStatesChanged_shouldUpdateSummary() {
        final Context context = RuntimeEnvironment.application;
        ReflectionHelpers.setField(mController, "mContext", context);
        mController.onResume();

        context.sendBroadcast(new Intent(TetheringManager.ACTION_TETHER_STATE_CHANGED));

        verify(mController).updateSummary();
    }
}

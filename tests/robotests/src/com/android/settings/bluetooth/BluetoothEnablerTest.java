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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
public class BluetoothEnablerTest {

    private static EnforcedAdmin sFakeEnforcedAdmin;
    private PreferenceViewHolder mHolder;
    private RestrictedSwitchPreference mRestrictedSwitchPreference;

    @BeforeClass
    public static void beforeClass() {
        sFakeEnforcedAdmin = new EnforcedAdmin(new ComponentName("test.package", "test.Class"), 10);
    }

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private LocalBluetoothAdapter mBluetoothAdapter;
    @Mock
    private SwitchWidgetController.OnSwitchChangeListener mCallback;

    private Context mContext;
    private SwitchWidgetController mSwitchController;
    private BluetoothEnabler mBluetoothEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mBluetoothManager.getBluetoothAdapter()).thenReturn(mBluetoothAdapter);

        mRestrictedSwitchPreference = new RestrictedSwitchPreference(mContext);
        mSwitchController = spy(new SwitchBarController(new SwitchBar(mContext)));
        mBluetoothEnabler = new BluetoothEnabler(
                mContext,
                mSwitchController,
                mMetricsFeatureProvider,
                mBluetoothManager,
                123,
                mRestrictionUtils);
        mHolder = PreferenceViewHolder.createInstanceForTests(mock(View.class));
        mRestrictedSwitchPreference.onBindViewHolder(mHolder);
        mBluetoothEnabler.setToggleCallback(mCallback);
    }

    @Test
    public void onSwitchToggled_shouldLogActionWithSuppliedEvent() {
        // WHEN the switch is toggled...
        mBluetoothEnabler.onSwitchToggled(false);

        // THEN the corresponding metrics action is logged.
        verify(mMetricsFeatureProvider).action(mContext, 123, false);
    }

    @Test
    public void onSwitchToggled_shouldTriggerCallback() {
        // WHEN the switch is toggled...
        mBluetoothEnabler.onSwitchToggled(false);

        // THEN the callback is triggered
        verify(mCallback).onSwitchToggled(false);
    }

    @Test
    public void maybeEnforceRestrictions_noRestrictions() {
        // GIVEN there are no restrictions set...
        when(mRestrictionUtils.checkIfRestrictionEnforced(any(Context.class), any(String.class)))
                .thenReturn(null);

        // WHEN the maybeEnforceRestrictions is called...
        // THEN false is returned to indicate there was no restriction to enforce
        assertThat(mBluetoothEnabler.maybeEnforceRestrictions()).isFalse();

        // THEN a null EnfoceAdmin is set.
        verify(mSwitchController).setDisabledByAdmin(null);
        // THEN the state of the switch isn't changed.
        verify(mSwitchController, never()).setChecked(anyBoolean());
    }

    @Test
    public void maybeEnforceRestrictions_disallowBluetoothRestrictionSet() {
        // GIVEN Bluetooth has been disallowed...
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH)).thenReturn(sFakeEnforcedAdmin);
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(null);

        // WHEN the maybeEnforceRestrictions is called...
        // THEN true is returned to indicate there was a restriction to enforce.
        assertThat(mBluetoothEnabler.maybeEnforceRestrictions()).isTrue();

        // THEN the expected EnfoceAdmin is set.
        verify(mSwitchController).setDisabledByAdmin(sFakeEnforcedAdmin);

        // THEN the switch is unchecked.
        verify(mSwitchController).setChecked(false);
    }

    @Test
    public void maybeEnforceRestrictions_disallowConfigBluetoothRestrictionSet() {
        // GIVEN configuring Bluetooth has been disallowed...
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH)).thenReturn(null);
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(sFakeEnforcedAdmin);

        // WHEN the maybeEnforceRestrictions is called...
        // THEN true is returned to indicate there was a restriction to enforce.
        assertThat(mBluetoothEnabler.maybeEnforceRestrictions()).isTrue();

        // THEN the expected EnfoceAdmin is set.
        verify(mSwitchController).setDisabledByAdmin(sFakeEnforcedAdmin);

        // THEN the switch is unchecked.
        verify(mSwitchController).setChecked(false);
    }

    @Test
    public void maybeEnforceRestrictions_disallowBluetoothNotOverriden() {
        // GIVEN Bluetooth has been disallowed...
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH)).thenReturn(sFakeEnforcedAdmin);
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(null);

        mBluetoothEnabler.resume(mContext);

        verify(mSwitchController, never()).setEnabled(true);
    }

    @Test
    public void startWithBluetoothOff_switchIsOff() {
        when(mBluetoothAdapter.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_OFF);
        verify(mSwitchController, never()).setChecked(anyBoolean());
        mBluetoothEnabler.resume(mContext);
        verify(mSwitchController, never()).setChecked(true);
    }

    @Test
    public void startWithBluetoothOn_switchIsOn() {
        when(mBluetoothAdapter.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_ON);
        verify(mSwitchController, never()).setChecked(anyBoolean());
        mBluetoothEnabler.resume(mContext);
        verify(mSwitchController, never()).setChecked(false);
        verify(mSwitchController).setChecked(true);
    }

    @Test
    public void bluetoothTurnsOff_switchTurnsOff() {
        // Start up with bluetooth turned on. The switch should get turned on.
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        when(mContext.registerReceiver(captor.capture(), any(IntentFilter.class))).thenReturn(null);
        when(mBluetoothAdapter.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_ON);
        verify(mSwitchController, never()).setChecked(anyBoolean());
        mBluetoothEnabler.resume(mContext);
        verify(mSwitchController, never()).setChecked(false);
        verify(mSwitchController).setChecked(true);

        // Now simulate bluetooth being turned off via an event.
        BroadcastReceiver receiver = captor.getValue();
        Intent turningOff = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        turningOff.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        receiver.onReceive(mContext, turningOff);
        Intent off = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        off.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        receiver.onReceive(mContext, off);

        // Make sure the switch was turned off.
        verify(mSwitchController).setChecked(false);
    }

    @Test
    public void bluetoothTurnsOn_switchTurnsOn() {
        // Start up with bluetooth turned on. The switch should be left off.
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        when(mContext.registerReceiver(captor.capture(), any(IntentFilter.class))).thenReturn(null);
        when(mBluetoothAdapter.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_OFF);
        verify(mSwitchController, never()).setChecked(anyBoolean());
        mBluetoothEnabler.resume(mContext);
        verify(mSwitchController, never()).setChecked(anyBoolean());

        // Now simulate bluetooth being turned on via an event.
        BroadcastReceiver receiver = captor.getValue();
        Intent turningOn = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        turningOn.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_ON);
        receiver.onReceive(mContext, turningOn);
        Intent on = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        on.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        receiver.onReceive(mContext, on);

        // Make sure the switch was turned on.
        verify(mSwitchController).setChecked(true);
    }
}

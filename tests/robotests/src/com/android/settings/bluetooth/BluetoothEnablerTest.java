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

import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.MasterSwitchController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothEnablerTest {

    private static final EnforcedAdmin FAKE_ENFORCED_ADMIN =
            new EnforcedAdmin(new ComponentName("test.package", "test.Class"), 10);

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private Context mContext;
    @Mock
    private MasterSwitchController mMasterSwitchController;
    @Mock
    private RestrictionUtils mRestrictionUtils;

    private BluetoothEnabler mBluetoothEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mBluetoothEnabler = new BluetoothEnabler(
                mContext,
                mMasterSwitchController,
                mMetricsFeatureProvider,
                mock(LocalBluetoothManager.class),
                123,
                mRestrictionUtils);
    }

    @Test
    public void onSwitchToggled_shouldLogActionWithSuppliedEvent() {
        // WHEN the switch is toggled...
        mBluetoothEnabler.onSwitchToggled(false);

        // THEN the corresponding metrics action is logged.
        verify(mMetricsFeatureProvider).action(mContext, 123, false);
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
        verify(mMasterSwitchController).setDisabledByAdmin(null);
        // THEN the state of the switch isn't changed.
        verify(mMasterSwitchController, never()).setChecked(anyBoolean());
    }

    @Test
    public void maybeEnforceRestrictions_disallowBluetoothRestrictionSet() {
        // GIVEN Bluetooth has been disallowed...
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH)).thenReturn(FAKE_ENFORCED_ADMIN);
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(null);

        // WHEN the maybeEnforceRestrictions is called...
        // THEN true is returned to indicate there was a restriction to enforce.
        assertThat(mBluetoothEnabler.maybeEnforceRestrictions()).isTrue();

        // THEN the expected EnfoceAdmin is set.
        verify(mMasterSwitchController).setDisabledByAdmin(FAKE_ENFORCED_ADMIN);

        // THEN the switch is unchecked.
        verify(mMasterSwitchController).setChecked(false);
    }

    @Test
    public void maybeEnforceRestrictions_disallowConfigBluetoothRestrictionSet() {
        // GIVEN configuring Bluetooth has been disallowed...
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH)).thenReturn(null);
        when(mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(FAKE_ENFORCED_ADMIN);

        // WHEN the maybeEnforceRestrictions is called...
        // THEN true is returned to indicate there was a restriction to enforce.
        assertThat(mBluetoothEnabler.maybeEnforceRestrictions()).isTrue();

        // THEN the expected EnfoceAdmin is set.
        verify(mMasterSwitchController).setDisabledByAdmin(FAKE_ENFORCED_ADMIN);

        // THEN the switch is unchecked.
        verify(mMasterSwitchController).setChecked(false);
    }

}

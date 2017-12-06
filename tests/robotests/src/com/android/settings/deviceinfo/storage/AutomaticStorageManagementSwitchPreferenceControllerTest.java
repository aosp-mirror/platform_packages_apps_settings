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

package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.deletionhelper.ActivationWarningFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.widget.MasterSwitchPreference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {SettingsShadowSystemProperties.class}
)
public class AutomaticStorageManagementSwitchPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MasterSwitchPreference mPreference;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentManager mFragmentManager;

    private Context mContext;
    private AutomaticStorageManagementSwitchPreferenceController mController;
    private MetricsFeatureProvider mMetricsFeature;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        FeatureFactory factory = FeatureFactory.getFactory(mContext);
        mMetricsFeature = factory.getMetricsFeatureProvider();

        mController = new AutomaticStorageManagementSwitchPreferenceController(
                mContext, mMetricsFeature, mFragmentManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_shouldReturnTrue_forHighRamDevice() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_shouldAlwaysReturnFalse_forLowRamDevice() {
        SettingsShadowSystemProperties.set("ro.config.low_ram", "true");
        assertThat(mController.isAvailable()).isFalse();
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void onResume_shouldReflectEnabledStatus() {
        mController.displayPreference(mScreen);
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 1);

        mController.onResume();

        verify(mPreference).setChecked(eq(true));
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mPreference).setOnPreferenceChangeListener(
                any(Preference.OnPreferenceChangeListener.class));
    }

    @Test
    public void togglingShouldCauseMetricsEvent() {
        // FakeFeatureFactory uses mock contexts, so this test scaffolds itself rather than using
        // the instance variables.
        FakeFeatureFactory.setupForTest(mMockContext);
        FakeFeatureFactory factory =
                (FakeFeatureFactory) FakeFeatureFactory.getFactory(mMockContext);
        AutomaticStorageManagementSwitchPreferenceController controller =
                new AutomaticStorageManagementSwitchPreferenceController(
                        mMockContext, factory.metricsFeatureProvider, mFragmentManager);

        controller.onSwitchToggled(true);

        verify(factory.metricsFeatureProvider, times(1)).action(
                any(Context.class), eq(MetricsEvent.ACTION_TOGGLE_STORAGE_MANAGER), eq(true));
    }

    @Test
    public void togglingShouldUpdateSettingsSecure() {
        mController.onSwitchToggled(true);

        ContentResolver resolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(
                resolver, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 0)).isNotEqualTo(0);
    }

    @Test
    public void togglingOnShouldTriggerWarningFragment() {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when (mFragmentManager.beginTransaction()).thenReturn(transaction);

        mController.onSwitchToggled(true);

        verify(transaction).add(any(), eq(ActivationWarningFragment.TAG));
    }

    @Test
    public void togglingOffShouldTriggerWarningFragment() {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when (mFragmentManager.beginTransaction()).thenReturn(transaction);

        mController.onSwitchToggled(false);

        verify(transaction, never()).add(any(), eq(ActivationWarningFragment.TAG));
    }


    @Test
    public void togglingOnShouldNotTriggerWarningFragmentIfEnabledByDefault() {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when (mFragmentManager.beginTransaction()).thenReturn(transaction);
        SettingsShadowSystemProperties.set(
                AutomaticStorageManagementSwitchPreferenceController
                        .STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY, "true");

        mController.onSwitchToggled(true);

        verify(transaction, never()).add(any(), eq(ActivationWarningFragment.TAG));
    }

    @Test
    public void togglingOnShouldTriggerWarningFragmentIfEnabledByDefaultAndDisabledByPolicy() {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(mFragmentManager.beginTransaction()).thenReturn(transaction);
        SettingsShadowSystemProperties.set(
                AutomaticStorageManagementSwitchPreferenceController
                        .STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY,
                "true");
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_TURNED_OFF_BY_POLICY,
                1);

        mController.onSwitchToggled(true);

        verify(transaction).add(any(), eq(ActivationWarningFragment.TAG));
    }
}

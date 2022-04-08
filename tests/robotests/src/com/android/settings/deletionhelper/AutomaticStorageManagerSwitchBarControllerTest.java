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
 * limitations under the License
 */

package com.android.settings.deletionhelper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutomaticStorageManagerSwitchBarControllerTest {

    private Context mContext;
    private SwitchBar mSwitchBar;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private Preference mPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentManager mFragmentManager;

    private AutomaticStorageManagerSwitchBarController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mSwitchBar = new SwitchBar(mContext);

        mMetricsFeatureProvider = FakeFeatureFactory.setupForTest().getMetricsFeatureProvider();
        mPreference = new Preference(mContext);

        mController =
                new AutomaticStorageManagerSwitchBarController(
                        mContext,
                        mSwitchBar,
                        mMetricsFeatureProvider,
                        mPreference,
                        mFragmentManager);
    }

    @Test
    public void onSwitchChanged_false_recordsAMetric() {
        mController.onSwitchChanged(null, false);

        verify(mMetricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(MetricsProto.MetricsEvent.ACTION_TOGGLE_STORAGE_MANAGER),
                        eq(false));
    }

    @Test
    public void onSwitchChanged_true_recordsAMetric() {
        mController.onSwitchChanged(null, true);

        verify(mMetricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(MetricsProto.MetricsEvent.ACTION_TOGGLE_STORAGE_MANAGER),
                        eq(true));
    }

    @Test
    public void onSwitchChanged_showWarningFragmentIfNotEnabledByDefault() {
        mController.onSwitchChanged(null, true);

        verify(mFragmentManager.beginTransaction())
                .add(any(Fragment.class), eq(ActivationWarningFragment.TAG));
    }

    @Test
    public void onSwitchChange_doNotShowWarningFragmentIfEnabledByDefault() {
        SystemProperties.set("ro.storage_manager.enabled", "true");

        mController.onSwitchChanged(null, true);

        verify(mFragmentManager.beginTransaction(), never())
                .add(any(Fragment.class), eq(ActivationWarningFragment.TAG));
    }

    @Test
    public void initializeSwitchOnConstruction() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                1);

        mController =
                new AutomaticStorageManagerSwitchBarController(
                        mContext,
                        mSwitchBar,
                        mMetricsFeatureProvider,
                        mPreference,
                        mFragmentManager);

        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void initializingSwitchDoesNotTriggerView() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                1);

        mController =
                new AutomaticStorageManagerSwitchBarController(
                        mContext,
                        mSwitchBar,
                        mMetricsFeatureProvider,
                        mPreference,
                        mFragmentManager);

        verify(mFragmentManager.beginTransaction(), never())
                .add(any(Fragment.class), eq(ActivationWarningFragment.TAG));
    }
}

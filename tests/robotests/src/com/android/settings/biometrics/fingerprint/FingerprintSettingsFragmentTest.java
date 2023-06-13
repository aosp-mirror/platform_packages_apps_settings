/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;

import static com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import static com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment.ADD_FINGERPRINT_REQUEST;
import static com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment.CHOOSE_LOCK_GENERIC_REQUEST;
import static com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment.KEY_FINGERPRINT_ADD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.biometrics.BiometricsSplitScreenDialog;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowSettingsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSettingsPreferenceFragment.class, ShadowUtils.class, ShadowFragment.class,
        ShadowUserManager.class, ShadowLockPatternUtils.class})
public class FingerprintSettingsFragmentTest {
    private FingerprintSettingsFragment mFragment;
    private Context mContext;
    private FragmentActivity mActivity;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;

    @Captor
    private ArgumentCaptor<CancellationSignal> mCancellationSignalArgumentCaptor =
            ArgumentCaptor.forClass(CancellationSignal.class);
    @Captor
    private ArgumentCaptor<FingerprintManager.AuthenticationCallback>
            mAuthenticationCallbackArgumentCaptor = ArgumentCaptor.forClass(
            FingerprintManager.AuthenticationCallback.class);

    private FingerprintAuthenticateSidecar mFingerprintAuthenticateSidecar;

    @Before
    public void setUp() {
        doReturn(true).when(mFingerprintManager).isHardwareDetected();
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void testAddFingerprint_inFullScreen_noDialog() {
        setUpFragment(false);
        // Click "Add Fingerprint"
        final Preference preference = new Preference(mContext);
        preference.setKey(KEY_FINGERPRINT_ADD);
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment).startActivityForResult(any(), eq(ADD_FINGERPRINT_REQUEST));
        verify(mFragmentTransaction, never()).add(any(),
                eq(BiometricsSplitScreenDialog.class.getName()));

    }

    @Test
    public void testAddFingerprint_inMultiWindow_showsDialog() {
        setUpFragment(false);

        doReturn(true).when(mActivity).isInMultiWindowMode();

        // Click "Add Fingerprint"
        final Preference preference = new Preference(mContext);
        preference.setKey(KEY_FINGERPRINT_ADD);
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment, times(0)).startActivityForResult(any(), eq(ADD_FINGERPRINT_REQUEST));
        verify(mFragmentTransaction).add(any(), eq(BiometricsSplitScreenDialog.class.getName()));
    }

    @Test
    public void testChooseLockKeyForFingerprint() {
        setUpFragment(true);
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(
                Intent.class);
        verify(mFragment).startActivityForResult(intentArgumentCaptor.capture(),
                eq(CHOOSE_LOCK_GENERIC_REQUEST));

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT,
                false)).isTrue();
    }

    // Test the case when FingerprintAuthenticateSidecar receives an error callback from the
    // framework or from another authentication client. The cancellation signal should not be set
    // to null because there may exist a running authentication client.
    // The signal can only be cancelled from the caller in FingerprintSettings.
    @Test
    public void testCancellationSignalLifeCycle() {
        setUpFragment(false);

        mFingerprintAuthenticateSidecar.setFingerprintManager(mFingerprintManager);

        doNothing().when(mFingerprintManager).authenticate(any(),
                mCancellationSignalArgumentCaptor.capture(),
                mAuthenticationCallbackArgumentCaptor.capture(), any(), anyInt());

        mFingerprintAuthenticateSidecar.startAuthentication(1);

        assertThat(mAuthenticationCallbackArgumentCaptor.getValue()).isNotNull();
        assertThat(mCancellationSignalArgumentCaptor.getValue()).isNotNull();

        // Authentication error callback should not cancel the signal.
        mAuthenticationCallbackArgumentCaptor.getValue().onAuthenticationError(0, "");
        assertThat(mFingerprintAuthenticateSidecar.isCancelled()).isFalse();

        // The signal should be cancelled when caller stops the authentication.
        mFingerprintAuthenticateSidecar.stopAuthentication();
        assertThat(mFingerprintAuthenticateSidecar.isCancelled()).isTrue();
    }

    private void setUpFragment(boolean showChooseLock) {
        Intent intent = new Intent();
        if (!showChooseLock) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L);
        }

        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class, intent).get());
        mContext = spy(ApplicationProvider.getApplicationContext());

        mFragment = spy(new FingerprintSettingsFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();

        FragmentManager fragmentManager = mock(FragmentManager.class);
        doReturn(mFragmentTransaction).when(fragmentManager).beginTransaction();
        doReturn(mFragmentTransaction).when(mFragmentTransaction).add(any(), anyString());
        doReturn(fragmentManager).when(mFragment).getFragmentManager();
        doReturn(fragmentManager).when(mActivity).getSupportFragmentManager();

        mFingerprintAuthenticateSidecar = new FingerprintAuthenticateSidecar();
        doReturn(mFingerprintAuthenticateSidecar).when(fragmentManager).findFragmentByTag(
                "authenticate_sidecar");

        doNothing().when(mFragment).startActivityForResult(any(Intent.class), anyInt());

        setSensor();

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();
    }

    private void setSensor() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                1 /* maxEnrollmentsPerUser */,
                new ArrayList<ComponentInfoInternal>(),
                TYPE_UDFPS_OPTICAL,
                true /* resetLockoutRequiresHardwareAuthToken */));
        doReturn(props).when(mFingerprintManager).getSensorPropertiesInternal();
    }
}

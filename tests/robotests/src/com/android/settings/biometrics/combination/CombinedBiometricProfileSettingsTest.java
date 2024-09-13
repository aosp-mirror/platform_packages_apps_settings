/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.combination;

import static com.android.settings.biometrics.combination.BiometricsSettingsBase.CONFIRM_REQUEST;
import static com.android.settings.password.ChooseLockPattern.RESULT_FINISHED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.Flags;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.biometrics.face.FaceStatusPreferenceController;
import com.android.settings.biometrics.fingerprint.FingerprintStatusPreferenceController;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.testutils.shadow.ShadowSettingsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.core.AbstractPreferenceController;

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
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSettingsPreferenceFragment.class, ShadowUtils.class, ShadowFragment.class})
public class CombinedBiometricProfileSettingsTest {

    private TestCombinedBiometricProfileSettings mFragment;
    private Context mContext;
    private FragmentActivity mActivity;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Captor
    private ArgumentCaptor<Preference> mPreferenceCaptor;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private BiometricSettingsAppPreferenceController mBiometricSettingsAppPreferenceController;
    @Mock
    private FingerprintStatusPreferenceController mFingerprintStatusPreferenceController;
    @Mock
    private FaceStatusPreferenceController mFaceStatusPreferenceController;
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private BiometricManager mBiometricManager;

    @Before
    public void setUp() {
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        ShadowUtils.setFaceManager(mFaceManager);
        FakeFeatureFactory.setupForTest();

        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class,
                new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L)).get());
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new TestCombinedBiometricProfileSettings(mContext));
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mBiometricManager).when(mActivity).getSystemService(BiometricManager.class);
        when(mBiometricManager.canAuthenticate(anyInt(),
                eq(BiometricManager.Authenticators.MANDATORY_BIOMETRICS)))
                .thenReturn(BiometricManager.BIOMETRIC_ERROR_MANDATORY_NOT_ACTIVE);

        ReflectionHelpers.setField(mFragment, "mDashboardFeatureProvider",
                FakeFeatureFactory.setupForTest().dashboardFeatureProvider);

        final Map<Class<?>, List<AbstractPreferenceController>> preferenceControllers =
                ReflectionHelpers.getField(mFragment, "mPreferenceControllers");
        List<AbstractPreferenceController> controllerList = new ArrayList<>();
        controllerList.add(mBiometricSettingsAppPreferenceController);
        preferenceControllers.put(BiometricSettingsAppPreferenceController.class, controllerList);
        controllerList.add(mFingerprintStatusPreferenceController);
        preferenceControllers.put(FingerprintStatusPreferenceController.class, controllerList);
        controllerList.add(mFaceStatusPreferenceController);
        preferenceControllers.put(FaceStatusPreferenceController.class, controllerList);

        doAnswer(invocation -> {
            final Preference preference = invocation.getArgument(0);
            return preference.getKey().equals(mFragment.getFingerprintPreferenceKey());
        }).when(mFingerprintStatusPreferenceController)
                .setPreferenceTreeClickLauncher(any(), any());
        doAnswer(invocation -> {
            final Preference preference = invocation.getArgument(0);
            return preference.getKey().equals(mFragment.getFingerprintPreferenceKey());
        }).when(mFingerprintStatusPreferenceController).handlePreferenceTreeClick(any());
        doAnswer(invocation -> {
            final Preference preference = invocation.getArgument(0);
            return preference.getKey().equals(mFragment.getFacePreferenceKey());
        }).when(mFaceStatusPreferenceController)
                .setPreferenceTreeClickLauncher(any(), any());
        doAnswer(invocation -> {
            final Preference preference = invocation.getArgument(0);
            return preference.getKey().equals(mFragment.getFacePreferenceKey());
        }).when(mFaceStatusPreferenceController).handlePreferenceTreeClick(any());

        doAnswer(invocation -> {
            final CharSequence key = invocation.getArgument(0);
            final Preference preference = new Preference(mContext);
            preference.setKey(key.toString());
            return preference;
        }).when(mFragment).findPreference(any());
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testLaunchBiometricPrompt_onCreateFragment() {
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mFragment).startActivityForResult(any(), anyInt());
        when(mBiometricManager.canAuthenticate(anyInt(),
                eq(BiometricManager.Authenticators.MANDATORY_BIOMETRICS)))
                .thenReturn(BiometricManager.BIOMETRIC_SUCCESS);

        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onActivityResult(CONFIRM_REQUEST, RESULT_FINISHED,
                new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));

        verify(mFragment).startActivityForResult(intentArgumentCaptor.capture(),
                eq(BiometricsSettingsBase.BIOMETRIC_AUTH_REQUEST));

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(
                ConfirmDeviceCredentialActivity.InternalActivity.class.getName());
    }

    @Test
    public void testClickFingerprintUnlockWithValidGkPwHandle() {
        doAnswer(invocation -> {
            final FingerprintManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onChallengeGenerated(0, 0, 1L);
            return null;
        }).when(mFingerprintManager).generateChallenge(anyInt(), any());
        doReturn(new byte[] { 1 }).when(mFragment).requestGatekeeperHat(any(), anyLong(), anyInt(),
                anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Fingerprint Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFingerprintPreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFingerprintStatusPreferenceController).handlePreferenceTreeClick(
                mPreferenceCaptor.capture());
        List<Preference> capturedPreferences = mPreferenceCaptor.getAllValues();

        assertThat(capturedPreferences.size()).isEqualTo(1);
        assertThat(capturedPreferences.get(0).getKey())
                .isEqualTo(mFragment.getFingerprintPreferenceKey());
    }

    @Test
    public void testClickFingerprintUnlockIfGkPwHandleTimeout() {
        doAnswer(invocation -> {
            final FingerprintManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onChallengeGenerated(0, 0, 1L);
            return null;
        }).when(mFingerprintManager).generateChallenge(anyInt(), any());
        doThrow(new IllegalStateException("Test")).when(mFragment).requestGatekeeperHat(any(),
                anyLong(), anyInt(), anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Fingerprint Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFingerprintPreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment).launchChooseOrConfirmLock();
    }

    @Test
    public void testActivityResultLaunchFingerprintUnlock() {
        doAnswer(invocation -> {
            final FingerprintManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onChallengeGenerated(0, 0, 1L);
            return null;
        }).when(mFingerprintManager).generateChallenge(anyInt(), any());
        doReturn(new byte[] { 1 }).when(mFragment).requestGatekeeperHat(any(), anyLong(), anyInt(),
                anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        final Bundle bundle = new Bundle();
        bundle.putString(BiometricsSettingsBase.RETRY_PREFERENCE_KEY,
                mFragment.getFingerprintPreferenceKey());
        final Bundle retryBundle = new Bundle();
        bundle.putBundle(BiometricsSettingsBase.RETRY_PREFERENCE_BUNDLE, retryBundle);
        mFragment.onCreate(bundle);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // onActivityResult
        mFragment.onActivityResult(CONFIRM_REQUEST, RESULT_FINISHED,
                new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));

        verify(mFingerprintStatusPreferenceController).handlePreferenceTreeClick(
                mPreferenceCaptor.capture());
        List<Preference> capturedPreferences = mPreferenceCaptor.getAllValues();
        assertThat(capturedPreferences.size()).isEqualTo(1);
        assertThat(capturedPreferences.get(0).getKey())
                .isEqualTo(mFragment.getFingerprintPreferenceKey());
    }

    @Test
    public void testClickFaceUnlock() {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        doReturn(new byte[] { 1 }).when(mFragment).requestGatekeeperHat(any(), anyLong(), anyInt(),
                anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Face Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFacePreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFaceStatusPreferenceController).handlePreferenceTreeClick(
                mPreferenceCaptor.capture());
        List<Preference> capturedPreferences = mPreferenceCaptor.getAllValues();
        assertThat(capturedPreferences.size()).isEqualTo(1);
        assertThat(capturedPreferences.get(0).getKey()).isEqualTo(mFragment.getFacePreferenceKey());
    }

    @Test
    public void testClickFaceUnlockIfGkPwHandleTimeout() {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        doThrow(new IllegalStateException("Test")).when(mFragment).requestGatekeeperHat(any(),
                anyLong(), anyInt(), anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Face Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFacePreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment).launchChooseOrConfirmLock();
    }

    @Test
    public void testActivityResultLaunchFaceUnlock() {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        doReturn(new byte[] { 1 }).when(mFragment).requestGatekeeperHat(any(), anyLong(), anyInt(),
                anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        final Bundle bundle = new Bundle();
        bundle.putString(BiometricsSettingsBase.RETRY_PREFERENCE_KEY,
                mFragment.getFingerprintPreferenceKey());
        final Bundle retryBundle = new Bundle();
        bundle.putBundle(BiometricsSettingsBase.RETRY_PREFERENCE_BUNDLE, retryBundle);
        mFragment.onCreate(bundle);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Face Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFacePreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFaceStatusPreferenceController).handlePreferenceTreeClick(
                mPreferenceCaptor.capture());
        List<Preference> capturedPreferences = mPreferenceCaptor.getAllValues();
        assertThat(capturedPreferences.size()).isEqualTo(1);
        assertThat(capturedPreferences.get(0).getKey()).isEqualTo(mFragment.getFacePreferenceKey());
    }

    @Test
    public void testNoCrashIfDetachActivityDuringGeneratingChallengeThroughFaceManager() {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            mFragment.onPause();
            mFragment.onStop();
            mFragment.onDestroy();
            mFragment.onDetach();
            doReturn(null).when(mFragment).getActivity();
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        doThrow(new IllegalStateException("Test")).when(mFragment).requestGatekeeperHat(
                any(), anyLong(), anyInt(), anyLong());
        FragmentManager fragmentManager = mock(FragmentManager.class);

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Face Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFacePreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment, never()).launchChooseOrConfirmLock();
    }

    @Test
    public void testNoCrashIfDetachActivityDuringGeneratingChallengeThroughFingerprintManager() {
        doAnswer(invocation -> {
            final FingerprintManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            mFragment.onPause();
            mFragment.onStop();
            mFragment.onDestroy();
            mFragment.onDetach();
            doReturn(null).when(mFragment).getActivity();
            callback.onChallengeGenerated(0, 0, 1L);
            return null;
        }).when(mFingerprintManager).generateChallenge(anyInt(), any());
        doThrow(new IllegalStateException("Test")).when(mFragment).requestGatekeeperHat(
                any(), anyLong(), anyInt(), anyLong());

        // Start fragment
        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onCreateView(LayoutInflater.from(mContext), mock(ViewGroup.class), Bundle.EMPTY);
        mFragment.onResume();

        // User clicks on "Fingerprint Unlock"
        final Preference preference = new Preference(mContext);
        preference.setKey(mFragment.getFingerprintPreferenceKey());
        mFragment.onPreferenceTreeClick(preference);

        verify(mFragment, never()).launchChooseOrConfirmLock();
    }

    /**
     * a test fragment that initializes PreferenceScreen for testing.
     */
    static class TestCombinedBiometricProfileSettings extends CombinedBiometricProfileSettings {

        private final Context mContext;
        private final PreferenceManager mPreferenceManager;

        TestCombinedBiometricProfileSettings(Context context) {
            super();
            mContext = context;
            mPreferenceManager = new PreferenceManager(context);
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context));
            setArguments(new Bundle());
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceManager.getPreferenceScreen();
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            // do nothing
        }

        @Override
        public void addPreferencesFromResource(@XmlRes int preferencesResId) {
            // do nothing
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        protected void launchChooseOrConfirmLock() {
            if (getActivity() == null) {
                throw new AndroidRuntimeException("TestFailed");
            }
        }
    }
}

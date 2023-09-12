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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.os.Looper;
import android.os.RemoteException;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FaceSettingsFooterPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String PREF_KEY = "security_face_footer";
    @Mock
    private FaceManager mFaceManager;
    @Mock
    PackageManager mPackageManager;
    @Captor
    private ArgumentCaptor<IFaceAuthenticatorsRegisteredCallback> mCaptor;
    private Preference mPreference;
    private Context mContext;
    private FaceSettingsFooterPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
    }

    private void setupHasFaceFeature() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
    }

    private void setupNoFaceFeature() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(false);
    }

    private void displayFaceSettingsFooterPreferenceController() {
        ShadowApplication.getInstance().setSystemService(Context.FACE_SERVICE, mFaceManager);
        mController = new FaceSettingsFooterPreferenceController(mContext, PREF_KEY);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreference = new FooterPreference(mContext);
        mPreference.setKey(PREF_KEY);
        screen.addPreference(mPreference);

        mController.displayPreference(screen);
    }

    private void createFaceSettingsFooterPreferenceController() {
        ShadowApplication.getInstance().setSystemService(Context.FACE_SERVICE, mFaceManager);
        mController = new FaceSettingsFooterPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isSliceable_returnFalse() {
        setupHasFaceFeature();
        displayFaceSettingsFooterPreferenceController();

        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void testString_faceNotClass3() throws RemoteException {
        setupHasFaceFeature();
        displayFaceSettingsFooterPreferenceController();

        verify(mFaceManager).addAuthenticatorsRegisteredCallback(mCaptor.capture());
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_settings_footer));

        List<FaceSensorPropertiesInternal> props = List.of(new FaceSensorPropertiesInternal(
                0 /* id */,
                FaceSensorProperties.STRENGTH_WEAK,
                1 /* maxTemplatesAllowed */,
                new ArrayList<>() /* componentInfo */,
                FaceSensorProperties.TYPE_UNKNOWN,
                true /* supportsFaceDetection */,
                true /* supportsSelfIllumination */,
                false /* resetLockoutRequiresChallenge */));
        mCaptor.getValue().onAllAuthenticatorsRegistered(props);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_settings_footer));
    }

    @Test
    public void testString_faceClass3() throws RemoteException {
        setupHasFaceFeature();
        displayFaceSettingsFooterPreferenceController();

        verify(mFaceManager).addAuthenticatorsRegisteredCallback(mCaptor.capture());
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_settings_footer));

        List<FaceSensorPropertiesInternal> props = List.of(new FaceSensorPropertiesInternal(
                0 /* id */,
                FaceSensorProperties.STRENGTH_STRONG,
                1 /* maxTemplatesAllowed */,
                new ArrayList<>() /* componentInfo */,
                FaceSensorProperties.TYPE_UNKNOWN,
                true /* supportsFaceDetection */,
                true /* supportsSelfIllumination */,
                false /* resetLockoutRequiresChallenge */));
        mCaptor.getValue().onAllAuthenticatorsRegistered(props);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_settings_footer_class3));
    }

    @Test
    public void testSupportFaceFeature_shouldAddAuthenticatorsRegisteredCallback() {
        setupHasFaceFeature();
        displayFaceSettingsFooterPreferenceController();

        verify(mFaceManager).addAuthenticatorsRegisteredCallback(any());
    }

    @Test
    public void testNoFaceFeature_shouldNotAddAuthenticatorsRegisteredCallback() {
        setupNoFaceFeature();
        displayFaceSettingsFooterPreferenceController();

        verify(mContext, never()).getSystemService(FaceManager.class);
        verify(mFaceManager, never()).addAuthenticatorsRegisteredCallback(any());
    }

    @Test
    public void testHasFaceFeature_shouldNotAddAuthenticatorsRegisteredCallback_inCtor() {
        setupHasFaceFeature();
        createFaceSettingsFooterPreferenceController();

        verify(mContext, never()).getSystemService(FaceManager.class);
        verify(mFaceManager, never()).addAuthenticatorsRegisteredCallback(any());
    }
}

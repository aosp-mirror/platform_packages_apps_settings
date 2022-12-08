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

import static android.util.DisplayMetrics.DENSITY_DEFAULT;
import static android.util.DisplayMetrics.DENSITY_XXXHIGH;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_NEXT_LAUNCHED;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_LAUNCHED_POSTURE_GUIDANCE;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_CLOSED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_OPENED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.face.FaceManager;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class FaceEnrollEducationTest {
    @Mock
    private FaceManager mFaceManager;

    private Context mContext;
    private ActivityController<FaceEnrollEducation> mActivityController;
    private FaceEnrollEducation mActivity;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFaceManager(mFaceManager);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void testFaceEnrollEducation_hasHeader() {
        buildActivity();
        CharSequence headerText = getGlifLayout().getHeaderText();

        assertThat(headerText).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_title));
    }

    @Test
    public void testFaceEnrollEducation_hasDescription() {
        buildActivity();
        CharSequence desc = getGlifLayout().getDescriptionText();

        assertThat(desc).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_message));
    }

    @Test
    public void testFaceEnrollEducation_showFooterPrimaryButton() {
        buildActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_start));
    }

    @Test
    public void testFaceEnrollEducation_showFooterSecondaryButton() {
        buildActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(mContext.getString(
                R.string.security_settings_face_enroll_introduction_cancel));
    }

    @Test
    public void testFaceEnrollEducation_defaultNeverLaunchPostureGuidance() {
        buildActivity();

        assertThat(mActivity.launchPostureGuidance()).isFalse();
        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollEducation_onStartNeverRegisterPostureChangeCallback() {
        buildActivity();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNull();
        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onStartRegisteredPostureChangeCallback() {
        buildActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNotNull();
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_unFolded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_XXXHIGH;
        buildActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNotNull();

        mActivity.onConfigurationChanged(newConfig);

        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_OPENED);
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_folded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_DEFAULT;
        buildActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNotNull();

        mActivity.onConfigurationChanged(newConfig);

        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_CLOSED);
    }

    private void buildActivityForPosture() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
        testIntent.putExtra(EXTRA_KEY_NEXT_LAUNCHED, false);
        testIntent.putExtra(EXTRA_LAUNCHED_POSTURE_GUIDANCE, false);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                testIntent);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mActivityController = Robolectric.buildActivity(
                FaceEnrollEducation.class, testIntent);
        mActivity = spy(mActivityController.create().get());

        when(mContext.getSystemService(eq(Context.FACE_SERVICE))).thenReturn(mFaceManager);
    }

    private void buildActivity() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                null /* Simulate no posture intent */);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mActivityController = Robolectric.buildActivity(
                FaceEnrollEducation.class, testIntent);
        mActivity = spy(mActivityController.create().get());

        when(mContext.getSystemService(eq(Context.FACE_SERVICE))).thenReturn(mFaceManager);
    }

    private GlifLayout getGlifLayout() {
        return mActivity.findViewById(R.id.setup_wizard_layout);
    }
}

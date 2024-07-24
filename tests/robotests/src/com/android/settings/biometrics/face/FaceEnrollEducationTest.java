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
import org.junit.Ignore;
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
    private ActivityController<TestFaceEnrollEducation> mActivityController;
    private TestFaceEnrollEducation mActivity;
    private FakeFeatureFactory mFakeFeatureFactory;

    public static class TestFaceEnrollEducation extends FaceEnrollEducation {

        @Override
        protected boolean launchPostureGuidance() {
            return super.launchPostureGuidance();
        }
    }

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

    private void setupActivityForPosture() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
        testIntent.putExtra(EXTRA_KEY_NEXT_LAUNCHED, false);
        testIntent.putExtra(EXTRA_LAUNCHED_POSTURE_GUIDANCE, false);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                testIntent);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mActivityController = Robolectric.buildActivity(
                TestFaceEnrollEducation.class, testIntent);
        mActivity = spy(mActivityController.create().get());

        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
    }

    private void setupActivity() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                null /* Simulate no posture intent */);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mActivityController = Robolectric.buildActivity(
                TestFaceEnrollEducation.class, testIntent);
        mActivity = spy(mActivityController.create().get());

        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
    }

    private GlifLayout getGlifLayout() {
        return mActivity.findViewById(R.id.setup_wizard_layout);
    }

    @Test
    @Ignore("b/295325503")
    public void testFaceEnrollEducation_hasHeader() {
        setupActivity();
        CharSequence headerText = getGlifLayout().getHeaderText();

        assertThat(headerText.toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_title));
    }

    @Test
    public void testFaceEnrollEducation_hasDescription() {
        setupActivity();
        CharSequence desc = getGlifLayout().getDescriptionText();

        assertThat(desc.toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_message));
    }

    @Test
    public void testFaceEnrollEducation_showFooterPrimaryButton() {
        setupActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_start));
    }

    @Test
    public void testFaceEnrollEducation_showFooterSecondaryButton() {
        setupActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText().toString()).isEqualTo(mContext.getString(
                R.string.security_settings_face_enroll_introduction_cancel));
    }

    @Test
    public void testFaceEnrollEducation_defaultNeverLaunchPostureGuidance() {
        setupActivity();

        assertThat(mActivity.launchPostureGuidance()).isFalse();
        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollEducation_onStartNeverRegisterPostureChangeCallback() {
        setupActivity();
        mActivity.onStart();

        assertThat(mActivity.getPostureGuidanceIntent()).isNull();
        assertThat(mActivity.getPostureCallback()).isNull();
        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onStartRegisteredPostureChangeCallback() {
        setupActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mActivity.getPostureCallback()).isNotNull();
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_unFolded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_XXXHIGH;
        setupActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mActivity.getPostureCallback()).isNotNull();

        mActivity.onConfigurationChanged(newConfig);

        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_OPENED);
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_folded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_DEFAULT;
        setupActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mActivity.getPostureCallback()).isNotNull();

        mActivity.onConfigurationChanged(newConfig);

        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_CLOSED);
    }
}

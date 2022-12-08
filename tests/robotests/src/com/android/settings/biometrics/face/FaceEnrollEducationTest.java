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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.airbnb.lottie.LottieAnimationView;
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
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private ActivityController<FaceEnrollEducation> mActivityController;
    private FaceEnrollEducation mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFaceManager(mFaceManager);
        FakeFeatureFactory.setupForTest();

        mActivityController = Robolectric.buildActivity(FaceEnrollEducation.class);
        mActivity = spy(mActivityController.get());
        mContext = spy(ApplicationProvider.getApplicationContext());

        when(mContext.getSystemService(eq(Context.FACE_SERVICE))).thenReturn(mFaceManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }


    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Ignore
    @Test
    public void testLaunchFaceEnrollConfirmation_hasHeader() {
        CharSequence headerText = getGlifLayout().getHeaderText();

        assertThat(headerText).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_title));
    }

    @Ignore
    @Test
    public void testLaunchFaceEnrollConfirmation_hasDescription() {
        CharSequence desc = getGlifLayout().getDescriptionText();

        assertThat(desc).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_message));
    }

    @Ignore
    @Test
    public void testLaunchFaceEnrollConfirmation_showLottieImage() {
        LottieAnimationView illustration = getGlifLayout().findViewById(R.id.illustration_lottie);

        assertThat(illustration.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Ignore
    @Test
    public void testLaunchFaceEnrollConfirmation_showFooterPrimaryButton() {
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_start));
    }

    @Ignore
    @Test
    public void testLaunchFaceEnrollConfirmation_showFooterSecondaryButton() {
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(mContext.getString(
                R.string.security_settings_face_enroll_introduction_cancel));
    }

    private GlifLayout getGlifLayout() {
        return getRootView().findViewById(R.id.setup_wizard_layout);
    }

    private View getRootView() {
        return LayoutInflater.from(mActivity)
                .inflate(R.layout.face_enroll_education, null, false);
    }
}

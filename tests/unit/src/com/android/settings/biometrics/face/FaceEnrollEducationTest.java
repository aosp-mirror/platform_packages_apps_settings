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

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresDevice;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.android.settings.R;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RequiresDevice
@RunWith(AndroidJUnit4.class)
public class FaceEnrollEducationTest {

    private static final ComponentName COMPONENT_NAME =
            new ComponentName("package", "class");
    private static final int USER_ID = UserHandle.myUserId();
    private static final UserHandle USER_HANDLE = new UserHandle(USER_ID);

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FaceManager mFaceManager;

    private List<Face> mFaces = new ArrayList();
    private Context mContext;

    @Rule
    public ActivityTestRule<FaceEnrollEducation> mActivityTestRule =
            new ActivityTestRule<>(
                    FaceEnrollEducation.class,
                    true /* enable touch at launch */,
                    false /* don't launch at every test */);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.getEnrolledFaces(anyInt())).thenReturn(mFaces);
        when(mFaceManager.hasEnrolledTemplates(anyInt())).thenReturn(true);
    }

    @RequiresDevice
    @Test
    public void testLaunchFaceEnrollConfirmation_hasHeader() {
        FaceEnrollEducation activity = mActivityTestRule.launchActivity(null);

        GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
        CharSequence headerText = glifLayout.getHeaderText();

        assertThat(headerText).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_title));
    }

    @RequiresDevice
    @Test
    public void testLaunchFaceEnrollConfirmation_hasDescription() {
        FaceEnrollEducation activity = mActivityTestRule.launchActivity(null);

        GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
        CharSequence desc = glifLayout.getDescriptionText();

        assertThat(desc).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_message));
    }

    @RequiresDevice
    @Test
    public void testLaunchFaceEnrollConfirmation_showLottieImage() {
        FaceEnrollEducation activity = mActivityTestRule.launchActivity(null);

        GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
        LottieAnimationView illustration = glifLayout.findViewById(R.id.illustration_lottie);

        assertThat(illustration.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @RequiresDevice
    @Test
    public void testLaunchFaceEnrollConfirmation_showFooterPrimaryButton() {
        FaceEnrollEducation activity = mActivityTestRule.launchActivity(null);

        GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
        FooterBarMixin footer = glifLayout.getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_education_start));
    }

    @RequiresDevice
    @Test
    public void testLaunchFaceEnrollConfirmation_showFooterSecondaryButton() {
        FaceEnrollEducation activity = mActivityTestRule.launchActivity(null);

        GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
        FooterBarMixin footer = glifLayout.getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_introduction_cancel));
    }
}

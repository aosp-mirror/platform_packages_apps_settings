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

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.util.DisplayMetrics.DENSITY_DEFAULT;
import static android.util.DisplayMetrics.DENSITY_XXXHIGH;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_NEXT_LAUNCHED;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_LAUNCHED_POSTURE_GUIDANCE;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_CLOSED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_OPENED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.view.BottomScrollView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUserManager.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowSensorPrivacyManager.class
})
public class FaceEnrollIntroductionTest {

    @Mock
    private FaceManager mFaceManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    private Context mContext;
    private ActivityController<? extends Activity> mController;
    private TestFaceEnrollIntroduction mActivity;
    private FaceEnrollIntroduction mSpyActivity;
    private FakeFeatureFactory mFakeFeatureFactory;
    private ShadowUserManager mUserManager;

    enum GateKeeperAction {CALL_SUPER, RETURN_BYTE_ARRAY, THROW_CREDENTIAL_NOT_MATCH}

    public static class TestFaceEnrollIntroduction extends FaceEnrollIntroduction {

        private int mRecreateCount = 0;

        public int getRecreateCount() {
            return mRecreateCount;
        }

        @Override
        public void recreate() {
            mRecreateCount++;
            // Do nothing
        }

        public boolean getConfirmingCredentials() {
            return mConfirmingCredentials;
        }

        public FaceManager mOverrideFaceManager = null;
        @NonNull
        public GateKeeperAction mGateKeeperAction = GateKeeperAction.CALL_SUPER;

        @Nullable
        @Override
        public byte[] requestGatekeeperHat(long challenge) {
            switch (mGateKeeperAction) {
                case RETURN_BYTE_ARRAY:
                    return new byte[]{1};
                case THROW_CREDENTIAL_NOT_MATCH:
                    throw new BiometricUtils.GatekeeperCredentialNotMatchException("test");
                case CALL_SUPER:
                default:
                    return super.requestGatekeeperHat(challenge);
            }
        }

        @Nullable
        @Override
        protected FaceManager getFaceManager() {
            return mOverrideFaceManager;
        }

        @Override
        protected boolean launchPostureGuidance() {
            return super.launchPostureGuidance();
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFaceManager(mFaceManager);
        mUserManager = ShadowUserManager.getShadow();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();

        when(mFakeFeatureFactory.securityFeatureProvider.getLockPatternUtils(any(Context.class)))
                .thenReturn(mLockPatternUtils);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    private void setupActivity() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                null /* Simulate no posture intent */);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mUserManager.addUserProfile(new UserHandle(0));
        mController = Robolectric.buildActivity(
                TestFaceEnrollIntroduction.class, testIntent);
        mActivity = (TestFaceEnrollIntroduction) spy(mController.get());
        mActivity.mOverrideFaceManager = mFaceManager;
        when(mActivity.getPostureGuidanceIntent()).thenReturn(null);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mLockPatternUtils.getActivePasswordQuality(Mockito.anyInt())).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        mController.create();
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
        mUserManager.addUserProfile(new UserHandle(0));
        mController = Robolectric.buildActivity(TestFaceEnrollIntroduction.class, testIntent);
        mSpyActivity = (FaceEnrollIntroduction) spy(mController.get());
        when(mSpyActivity.getPostureGuidanceIntent()).thenReturn(testIntent);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mLockPatternUtils.getActivePasswordQuality(Mockito.anyInt())).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        mController.create();
    }

    private void setupActivityWithGenerateChallenge(@NonNull Intent intent) {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        mController = Robolectric.buildActivity(TestFaceEnrollIntroduction.class, intent);
        mActivity = (TestFaceEnrollIntroduction) mController.get();
        mActivity.mOverrideFaceManager = mFaceManager;
    }

    private GlifLayout getGlifLayout(Activity activity) {
        return activity.findViewById(R.id.setup_wizard_layout);
    }

    private void setFaceManagerToHave(int numEnrollments) {
        List<Face> faces = new ArrayList<>();
        for (int i = 0; i < numEnrollments; i++) {
            faces.add(new Face("Face " + i /* name */, 1 /*faceId */, 1 /* deviceId */));
        }
        when(mFaceManager.getEnrolledFaces(anyInt())).thenReturn(faces);
    }

    @Test
    public void intro_CheckCanEnroll() {
        setFaceManagerToHave(0 /* numEnrollments */);
        setupActivityWithGenerateChallenge(new Intent());
        mController.create();
        int result = mActivity.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolled() {
        setFaceManagerToHave(1 /* numEnrollments */);
        setupActivityWithGenerateChallenge(new Intent());
        mController.create();
        int result = mActivity.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.face_intro_error_max);
    }

    @Test
    public void testOnCreate() {
        setupActivityWithGenerateChallenge(new Intent());
        mController.create();
    }

    @Test
    public void testOnCreateToGenerateChallenge() {
        setupActivityWithGenerateChallenge(
                new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));
        mActivity.mGateKeeperAction = GateKeeperAction.RETURN_BYTE_ARRAY;
        mController.create();
    }

    @Test
    public void testGenerateChallengeFailThenRecreate() {
        setupActivityWithGenerateChallenge(
                new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));
        mActivity.mGateKeeperAction = GateKeeperAction.THROW_CREDENTIAL_NOT_MATCH;
        mController.create();

        // Make sure recreate() is called on original activity
        assertThat(mActivity.getRecreateCount()).isEqualTo(1);

        // Simulate recreate() action
        setupActivityWithGenerateChallenge(mActivity.getIntent());
        mController.create();

        // Verify confirmLock()
        assertThat(mActivity.getConfirmingCredentials()).isTrue();
        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        ShadowActivity.IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertWithMessage("Next activity 1").that(startedActivity).isNotNull();
    }

    @Test
    public void testFaceEnrollIntroduction_hasHeader() {
        setupActivity();
        TextView headerTextView = getGlifLayout(mActivity).findViewById(R.id.suc_layout_title);

        assertThat(headerTextView).isNotNull();
        assertThat(headerTextView.getText().toString()).isNotEmpty();
    }

    @Test
    public void testFaceEnrollIntroduction_hasDescription() {
        setupActivity();
        CharSequence desc = getGlifLayout(mActivity).getDescriptionText();

        assertThat(desc.toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_introduction_message));
    }

    @Test
    public void testFaceEnrollEducation_hasBottomScrollView() {
        setupActivity();
        BottomScrollView scrollView = getGlifLayout(mActivity).findViewById(R.id.sud_scroll_view);

        assertThat(scrollView).isNotNull();
        assertThat(scrollView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testFaceEnrollIntroduction_showFooterPrimaryButton() {
        setupActivity();
        FooterBarMixin footer = getGlifLayout(mActivity).getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton).isNotNull();
        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText().toString()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_introduction_agree));
    }

    @Test
    public void testFaceEnrollIntroduction_notShowFooterSecondaryButton() {
        setupActivity();
        FooterBarMixin footer = getGlifLayout(mActivity).getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void testFaceEnrollIntroduction_defaultNeverLaunchPostureGuidance() {
        setupActivity();

        assertThat(mActivity.launchPostureGuidance()).isFalse();
        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollIntroduction_onStartNeverRegisterPostureChangeCallback() {
        setupActivity();
        mActivity.onStart();

        assertThat(mActivity.getPostureGuidanceIntent()).isNull();
        assertThat(mActivity.getPostureCallback()).isNull();
        assertThat(mActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollIntroduction_onStartRegisteredPostureChangeCallback() {
        setupActivityForPosture();
        mSpyActivity.onStart();

        assertThat(mSpyActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mSpyActivity.getPostureCallback()).isNotNull();
    }

    @Test
    public void testFaceEnrollIntroduction_onFoldedUpdated_unFolded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_XXXHIGH;
        setupActivityForPosture();
        mSpyActivity.onStart();

        assertThat(mSpyActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mSpyActivity.getPostureCallback()).isNotNull();

        mSpyActivity.onConfigurationChanged(newConfig);

        assertThat(mSpyActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_OPENED);
    }

    @Test
    public void testFaceEnrollEducation_onFoldedUpdated_folded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_DEFAULT;
        setupActivityForPosture();
        mSpyActivity.onStart();

        assertThat(mSpyActivity.getPostureGuidanceIntent()).isNotNull();
        assertThat(mSpyActivity.getPostureCallback()).isNotNull();

        mSpyActivity.onConfigurationChanged(newConfig);

        assertThat(mSpyActivity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_CLOSED);
    }
}

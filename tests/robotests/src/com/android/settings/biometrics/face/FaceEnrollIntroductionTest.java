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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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

import junit.framework.TestCase;

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

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUserManager.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowSensorPrivacyManager.class
})
public class FaceEnrollIntroductionTest extends TestCase {

    @Mock
    private FaceManager mFaceManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    private Context mContext;
    private ActivityController<TestFaceEnrollIntroduction> mController;
    private TestFaceEnrollIntroduction mActivity;
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

    private void setupActivity(@NonNull Intent intent) {
        doAnswer(invocation -> {
            final FaceManager.GenerateChallengeCallback callback =
                    invocation.getArgument(1);
            callback.onGenerateChallengeResult(0, 0, 1L);
            return null;
        }).when(mFaceManager).generateChallenge(anyInt(), any());
        mController = Robolectric.buildActivity(TestFaceEnrollIntroduction.class, intent);
        mActivity = mController.get();
        mActivity.mOverrideFaceManager = mFaceManager;
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void testOnCreate() {
        setupActivity(new Intent());
        mController.create();
    }

    @Test
    public void testOnCreateToGenerateChallenge() {
        setupActivity(new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));
        mActivity.mGateKeeperAction = GateKeeperAction.RETURN_BYTE_ARRAY;
        mController.create();
    }

    @Test
    public void testGenerateChallengeFailThenRecreate() {
        setupActivity(new Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L));
        mActivity.mGateKeeperAction = GateKeeperAction.THROW_CREDENTIAL_NOT_MATCH;
        mController.create();

        // Make sure recreate() is called on original activity
        assertThat(mActivity.getRecreateCount()).isEqualTo(1);

        // Simulate recreate() action
        setupActivity(mActivity.getIntent());
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
        buildActivity();
        TextView headerTextView = getGlifLayout().findViewById(R.id.suc_layout_title);

        assertThat(headerTextView).isNotNull();
        assertThat(headerTextView.getText().toString()).isNotEmpty();
    }

    @Test
    public void testFaceEnrollIntroduction_hasDescription() {
        buildActivity();
        CharSequence desc = getGlifLayout().getDescriptionText();

        assertThat(desc).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_introduction_message));
    }

    @Test
    public void testFaceEnrollEducation_hasBottomScrollView() {
        buildActivity();
        BottomScrollView scrollView = getGlifLayout().findViewById(R.id.sud_scroll_view);

        assertThat(scrollView).isNotNull();
        assertThat(scrollView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testFaceEnrollIntroduction_showFooterPrimaryButton() {
        buildActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getPrimaryButton();

        assertThat(footerButton).isNotNull();
        assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(footerButton.getText()).isEqualTo(
                mContext.getString(R.string.security_settings_face_enroll_introduction_agree));
    }

    @Test
    public void testFaceEnrollIntroduction_notShowFooterSecondaryButton() {
        buildActivity();
        FooterBarMixin footer = getGlifLayout().getMixin(FooterBarMixin.class);
        FooterButton footerButton = footer.getSecondaryButton();

        assertThat(footerButton.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void testFaceEnrollIntroduction_defaultNeverLaunchPostureGuidance() {
        buildActivity();

        assertThat(mActivity.launchPostureGuidance()).isFalse();
        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollIntroduction_onStartNeverRegisterPostureChangeCallback() {
        buildActivity();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNull();
        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_UNKNOWN);
    }

    @Test
    public void testFaceEnrollIntroduction_onStartRegisteredPostureChangeCallback() {
        buildActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNotNull();
    }

    @Test
    public void testFaceEnrollIntroduction_onFoldedUpdated_unFolded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_XXXHIGH;
        buildActivityForPosture();
        mActivity.onStart();

        assertThat(mActivity.mFoldCallback).isNotNull();

        mActivity.onConfigurationChanged(newConfig);

        assertThat(mActivity.mDevicePostureState).isEqualTo(DEVICE_POSTURE_OPENED);
    }

    @Test
    public void testFaceEnrollEducation_onFoldedUpdated_folded() {
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
        mUserManager.addUserProfile(new UserHandle(0));
        mController = Robolectric.buildActivity(TestFaceEnrollIntroduction.class, testIntent);

        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mLockPatternUtils.getActivePasswordQuality(Mockito.anyInt())).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        mActivity = spy(mController.create().get());
    }

    private void buildActivity() {
        final Intent testIntent = new Intent();
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                null /* Simulate no posture intent */);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mUserManager.addUserProfile(new UserHandle(0));
        mController = Robolectric.buildActivity(
                TestFaceEnrollIntroduction.class, testIntent);

        doReturn(mContext).when(mContext).getApplicationContext();
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mLockPatternUtils.getActivePasswordQuality(Mockito.anyInt())).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        mActivity = spy(mController.create().get());
        mActivity.mOverrideFaceManager = mFaceManager;
    }

    private GlifLayout getGlifLayout() {
        return mActivity.findViewById(R.id.setup_wizard_layout);
    }
}

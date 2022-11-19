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
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowSensorPrivacyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
        ShadowSensorPrivacyManager.class
})
public class FaceEnrollIntroductionTest {

    @Mock private FaceManager mFaceManager;

    private ActivityController<TestFaceEnrollIntroduction> mController;
    private TestFaceEnrollIntroduction mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
        setupActivity(new Intent());
        mController.create();
        int result = mActivity.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolled() {
        setFaceManagerToHave(1 /* numEnrollments */);
        setupActivity(new Intent());
        mController.create();
        int result = mActivity.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.face_intro_error_max);
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

    enum GateKeeperAction { CALL_SUPER, RETURN_BYTE_ARRAY, THROW_CREDENTIAL_NOT_MATCH }

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
        @NonNull public GateKeeperAction mGateKeeperAction = GateKeeperAction.CALL_SUPER;

        @Nullable
        @Override
        public byte[] requestGatekeeperHat(long challenge) {
            switch (mGateKeeperAction) {
                case RETURN_BYTE_ARRAY:
                    return new byte[] { 1 };
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
}

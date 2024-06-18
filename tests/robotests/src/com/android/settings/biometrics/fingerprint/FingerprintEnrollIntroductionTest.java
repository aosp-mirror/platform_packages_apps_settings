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

package com.android.settings.biometrics.fingerprint;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
import static android.content.Intent.EXTRA_USER_ID;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintEnrollOptions;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.UserManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.GatekeeperPasswordProvider;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.RequireScrollMixin;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FingerprintEnrollIntroductionTest {

    @Mock private LockPatternUtils mLockPatternUtils;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private UserManager mUserManager;

    private GatekeeperPasswordProvider mGatekeeperPasswordProvider;

    private Context mContext;

    private TestFingerprintEnrollIntroduction mFingerprintEnrollIntroduction;
    private ActivityController<TestFingerprintEnrollIntroduction> mController;

    private static final int MAX_ENROLLMENTS = 5;
    private static final byte[] EXPECTED_TOKEN = new byte[] { 10, 20, 30, 40 };
    private static final long EXPECTED_CHALLENGE = 9876L;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mGatekeeperPasswordProvider = new GatekeeperPasswordProvider(mLockPatternUtils);

        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        final List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop =
                new FingerprintSensorPropertiesInternal(
                        0 /* sensorId */,
                        SensorProperties.STRENGTH_STRONG,
                        MAX_ENROLLMENTS /* maxEnrollmentsPerUser */,
                        componentInfo,
                        FingerprintSensorProperties.TYPE_REAR,
                        true /* resetLockoutRequiresHardwareAuthToken */);
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(prop);
        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);

        when(mUserManager.getCredentialOwnerProfile(anyInt())).thenAnswer(
                (Answer<Integer>) invocation -> (int) invocation.getArgument(0));

        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(anyLong(), anyLong(), anyInt()))
                .thenAnswer((Answer<VerifyCredentialResponse>) invocation ->
                        newGoodCredential(invocation.getArgument(0), EXPECTED_TOKEN));
        doNothing().when(mLockPatternUtils).removeGatekeeperPasswordHandle(anyLong());
    }

    void setupFingerprintEnrollIntroWith(@NonNull Intent intent) {

        mController = Robolectric.buildActivity(TestFingerprintEnrollIntroduction.class, intent);
        mFingerprintEnrollIntroduction = mController.get();
        mFingerprintEnrollIntroduction.mMockedFingerprintManager = mFingerprintManager;
        mFingerprintEnrollIntroduction.mMockedGatekeeperPasswordProvider =
                mGatekeeperPasswordProvider;
        mFingerprintEnrollIntroduction.mMockedLockPatternUtils = mLockPatternUtils;
        mFingerprintEnrollIntroduction.mMockedUserManager = mUserManager;

        mFingerprintEnrollIntroduction.mNewSensorId = 1;
        mFingerprintEnrollIntroduction.mNewChallenge = EXPECTED_CHALLENGE;

        final int userId = intent.getIntExtra(EXTRA_USER_ID, 0);
        when(mLockPatternUtils.getActivePasswordQuality(userId))
                .thenReturn(PASSWORD_QUALITY_SOMETHING);

        mController.create();
    }

    void setFingerprintManagerToHave(int numEnrollments) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        for (int i = 0; i < numEnrollments; i++) {
            fingerprints.add(
                    new Fingerprint(
                            "Fingerprint " + i /* name */, 1 /*fingerId */, 1 /* deviceId */));
        }
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(fingerprints);
    }

    @Test
    public void intro_CheckCanEnrollNormal() {
        setupFingerprintEnrollIntroWith(newTokenOnlyIntent());
        setFingerprintManagerToHave(3 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledNormal() {
        setupFingerprintEnrollIntroWith(newTokenOnlyIntent());
        setFingerprintManagerToHave(7 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckCanEnrollDuringSUW() {
        // This code path should depend on suw_max_fingerprints_enrollable versus
        // FingerprintManager.getSensorProperties...maxEnrollmentsPerUser()
        Resources resources = mock(Resources.class);
        when(resources.getInteger(anyInt())).thenReturn(5);
        when(mContext.getResources()).thenReturn(resources);

        setupFingerprintEnrollIntroWith(newFirstSuwIntent());
        setFingerprintManagerToHave(0 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);

        final RequireScrollMixin requireScrollMixin =
                ((GlifLayout) mFingerprintEnrollIntroduction.findViewById(
                        R.id.setup_wizard_layout)).getMixin(RequireScrollMixin.class);
        requireScrollMixin.getOnRequireScrollStateChangedListener().onRequireScrollStateChanged(
                false);
        Assert.assertEquals(View.VISIBLE,
                mFingerprintEnrollIntroduction.getSecondaryFooterButton().getVisibility());
    }

    @Test
    public void intro_CheckMaxEnrolledDuringSUW() {
        // This code path should depend on suw_max_fingerprints_enrollable versus
        // FingerprintManager.getSensorProperties...maxEnrollmentsPerUser()
        Resources resources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getInteger(anyInt())).thenReturn(1);

        setupFingerprintEnrollIntroWith(newFirstSuwIntent());
        setFingerprintManagerToHave(1 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);

        final RequireScrollMixin requireScrollMixin =
                ((GlifLayout) mFingerprintEnrollIntroduction.findViewById(
                        R.id.setup_wizard_layout)).getMixin(RequireScrollMixin.class);
        requireScrollMixin.getOnRequireScrollStateChangedListener().onRequireScrollStateChanged(
                false);
        Assert.assertEquals(View.INVISIBLE,
                mFingerprintEnrollIntroduction.getSecondaryFooterButton().getVisibility());
    }

    @Test
    public void intro_CheckCanEnrollDuringDeferred() {
        setupFingerprintEnrollIntroWith(newDeferredSuwIntent());
        setFingerprintManagerToHave(2 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledDuringDeferred() {
        setupFingerprintEnrollIntroWith(newDeferredSuwIntent());
        setFingerprintManagerToHave(6 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckCanEnrollDuringPortal() {
        setupFingerprintEnrollIntroWith(newPortalSuwIntent());
        setFingerprintManagerToHave(2 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledDuringPortal() {
        setupFingerprintEnrollIntroWith(newPortalSuwIntent());
        setFingerprintManagerToHave(6 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckGenerateChallenge() {
        setupFingerprintEnrollIntroWith(newGkPwHandleAndFromSettingsIntent());

        final long challengeField = mFingerprintEnrollIntroduction.getChallengeField();
        assertThat(challengeField).isEqualTo(EXPECTED_CHALLENGE);

        final byte[] token = mFingerprintEnrollIntroduction.getTokenField();
        assertThat(token).isNotNull();
        assertThat(token.length).isEqualTo(EXPECTED_TOKEN.length);
        for (int i = 0; i < token.length; ++i) {
            assertWithMessage("token[" + i + "] is " + token[i] + " not " + EXPECTED_TOKEN[i])
                    .that(token[i]).isEqualTo(EXPECTED_TOKEN[i]);
        }

        final Intent resultIntent = mFingerprintEnrollIntroduction.getSetResultIntentExtra(null);
        assertThat(resultIntent).isNotNull();
        assertThat(resultIntent.getLongExtra(EXTRA_KEY_CHALLENGE, -1L)).isEqualTo(challengeField);
        final byte[] token2 = resultIntent.getByteArrayExtra(EXTRA_KEY_CHALLENGE_TOKEN);
        assertThat(token2).isNotNull();
        assertThat(token2.length).isEqualTo(EXPECTED_TOKEN.length);
        for (int i = 0; i < token2.length; ++i) {
            assertWithMessage("token2[" + i + "] is " + token2[i] + " not " + EXPECTED_TOKEN[i])
                    .that(token2[i]).isEqualTo(EXPECTED_TOKEN[i]);
        }
    }

    @Test
    public void clickNext_onActivityResult_pause_shouldFinish() {
        setupFingerprintEnrollIntroWith(newTokenOnlyIntent());
        mController.resume();
        mFingerprintEnrollIntroduction.clickNextBtn();
        mController.pause().stop();
        assertThat(mFingerprintEnrollIntroduction.shouldFinishWhenBackgrounded()).isEqualTo(false);

        mController.resume().pause().stop();
        assertThat(mFingerprintEnrollIntroduction.shouldFinishWhenBackgrounded()).isEqualTo(true);
    }

    @Test
    public void testFingerprintEnrollIntroduction_forwardsEnrollOptions() {
        final Intent intent = newTokenOnlyIntent();
        intent.putExtra(BiometricUtils.EXTRA_ENROLL_REASON,
                FingerprintEnrollOptions.ENROLL_REASON_SETTINGS);
        setupFingerprintEnrollIntroWith(intent);

        final Intent enrollingIntent = mFingerprintEnrollIntroduction.getEnrollingIntent();
        assertThat(enrollingIntent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1))
                .isEqualTo(FingerprintEnrollOptions.ENROLL_REASON_SETTINGS);
    }

    private Intent newTokenOnlyIntent() {
        return new Intent()
                .putExtra(EXTRA_KEY_CHALLENGE_TOKEN, new byte[] { 1 });
    }

    private Intent newFirstSuwIntent() {
        return newTokenOnlyIntent()
                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
    }

    private Intent newDeferredSuwIntent() {
        return newTokenOnlyIntent()
                .putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true);
    }

    private Intent newPortalSuwIntent() {
        return newTokenOnlyIntent()
                .putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, true);
    }

    private Intent newGkPwHandleAndFromSettingsIntent() {
        return new Intent()
                .putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true)
                .putExtra(EXTRA_KEY_GK_PW_HANDLE, 1L);
    }

    private VerifyCredentialResponse newGoodCredential(long gkPwHandle, @NonNull byte[] hat) {
        return new VerifyCredentialResponse.Builder()
                .setGatekeeperPasswordHandle(gkPwHandle)
                .setGatekeeperHAT(hat)
                .build();
    }

    public static class TestFingerprintEnrollIntroduction
            extends FingerprintEnrollIntroduction {

        public FingerprintManager mMockedFingerprintManager;
        public GatekeeperPasswordProvider mMockedGatekeeperPasswordProvider;
        public LockPatternUtils mMockedLockPatternUtils;
        public UserManager mMockedUserManager;
        public int mNewSensorId;
        public long mNewChallenge;

        @Nullable
        public byte[] getTokenField() {
            return mToken;
        }

        public long getChallengeField() {
            return mChallenge;
        }

        @Override
        protected boolean isDisabledByAdmin() {
            return false;
        }

        @Nullable
        @Override
        protected FingerprintManager getFingerprintManager() {
            return mMockedFingerprintManager;
        }

        @Override
        protected UserManager getUserManager() {
            return mMockedUserManager;
        }

        @NonNull
        @Override
        protected GatekeeperPasswordProvider getGatekeeperPasswordProvider() {
            return mMockedGatekeeperPasswordProvider;
        }

        @NonNull
        @Override
        protected LockPatternUtils getLockPatternUtils() {
            return mMockedLockPatternUtils;
        }

        @Override
        protected void getChallenge(GenerateChallengeCallback callback) {
            callback.onChallengeGenerated(mNewSensorId, mUserId, mNewChallenge);
        }

        @Override
        protected boolean shouldFinishWhenBackgrounded() {
            return super.shouldFinishWhenBackgrounded();
        }

        //mock click next btn
        public void clickNextBtn() {
            super.onNextButtonClick(null);
        }

    }
}

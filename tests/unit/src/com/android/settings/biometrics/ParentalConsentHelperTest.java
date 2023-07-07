/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_NONE;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_MODALITY;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_DENIED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_GRANTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.hardware.biometrics.BiometricAuthenticator;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics.face.FaceEnrollParentalConsent;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollParentalConsent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class ParentalConsentHelperTest {

    private static final int REQUEST_CODE = 12;

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();

    @Mock
    private Activity mRootActivity;
    @Mock
    private Intent mRootActivityIntent;
    @Captor
    ArgumentCaptor<Intent> mLastStarted;

    @Before
    public void setup() {
        when(mRootActivity.getIntent()).thenAnswer(invocation -> mRootActivityIntent);
        when(mRootActivityIntent.getBundleExtra(any())).thenAnswer(invocation -> null);
        when(mRootActivityIntent.getStringExtra(any())).thenAnswer(invocation -> null);
        when(mRootActivityIntent.getBooleanExtra(any(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArguments()[1]);
    }

    @Test
    public void testLaunchNext__fingerprint_all_consent() {
        testLaunchNext(
                true /* requireFace */, true /* grantFace */,
                true /* requireFingerprint */, true /* grantFace */,
                90 /* gkpw */);
    }

    @Test
    public void testLaunchNext_nothing_to_consent() {
        testLaunchNext(
                false /* requireFace */, false /* grantFace */,
                false /* requireFingerprint */, false /* grantFace */,
                80 /* gkpw */);
    }

    @Test
    public void testLaunchNext_face_and_fingerprint_no_consent() {
        testLaunchNext(
                true /* requireFace */, false /* grantFace */,
                true /* requireFingerprint */, false /* grantFace */,
                70 /* gkpw */);
    }

    @Test
    public void testLaunchNext_face_and_fingerprint_only_face_consent() {
        testLaunchNext(
                true /* requireFace */, true /* grantFace */,
                true /* requireFingerprint */, false /* grantFace */,
                60 /* gkpw */);
    }

    @Test
    public void testLaunchNext_face_and_fingerprint_only_fingerprint_consent() {
        testLaunchNext(
                true /* requireFace */, false /* grantFace */,
                true /* requireFingerprint */, true /* grantFace */,
                50 /* gkpw */);
    }

    @Test
    public void testLaunchNext_face_with_consent() {
        testLaunchNext(
                true /* requireFace */, true /* grantFace */,
                false /* requireFingerprint */, false /* grantFace */,
                40 /* gkpw */);
    }

    @Test
    public void testLaunchNext_face_without_consent() {
        testLaunchNext(
                true /* requireFace */, false /* grantFace */,
                false /* requireFingerprint */, false /* grantFace */,
                30 /* gkpw */);
    }

    @Test
    public void testLaunchNext_fingerprint_with_consent() {
        testLaunchNext(
                false /* requireFace */, false /* grantFace */,
                true /* requireFingerprint */, true /* grantFace */,
                20 /* gkpw */);
    }

    @Test
    public void testLaunchNext_fingerprint_without_consent() {
        testLaunchNext(
                false /* requireFace */, false /* grantFace */,
                true /* requireFingerprint */, false /* grantFace */,
                10 /* gkpw */);
    }

    private void testLaunchNext(
            boolean requireFace, boolean grantFace,
            boolean requireFingerprint, boolean grantFingerprint,
            long gkpw) {
        final List<Pair<String, Boolean>> expectedLaunches = new ArrayList<>();
        if (requireFingerprint) {
            expectedLaunches.add(
                    new Pair(FingerprintEnrollParentalConsent.class.getName(), grantFingerprint));
        }
        if (requireFace) {
            expectedLaunches.add(new Pair(FaceEnrollParentalConsent.class.getName(), grantFace));
        }

        // initial consent status
        final ParentalConsentHelper helper = new ParentalConsentHelper(gkpw);
        helper.setConsentRequirement(requireFace, requireFingerprint);
        assertThat(ParentalConsentHelper.hasFaceConsent(helper.getConsentResult()))
                .isFalse();
        assertThat(ParentalConsentHelper.hasFingerprintConsent(helper.getConsentResult()))
                .isFalse();

        // check expected launches
        for (int i = 0; i <= expectedLaunches.size(); i++) {
            final Pair<String, Boolean> expected = i > 0 ? expectedLaunches.get(i - 1) : null;
            final boolean launchedNext = i == 0
                    ? helper.launchNext(mRootActivity, REQUEST_CODE)
                    : helper.launchNext(mRootActivity, REQUEST_CODE,
                            expected.second ? RESULT_CONSENT_GRANTED : RESULT_CONSENT_DENIED,
                            getResultIntent(getStartedModality(expected.first)));
            assertThat(launchedNext).isEqualTo(i < expectedLaunches.size());
        }
        verify(mRootActivity, times(expectedLaunches.size()))
                .startActivityForResult(mLastStarted.capture(), eq(REQUEST_CODE));
        assertThat(mLastStarted.getAllValues()
                .stream().map(i -> i.getComponent().getClassName()).collect(Collectors.toList()))
                .containsExactlyElementsIn(
                        expectedLaunches.stream().map(i -> i.first).collect(Collectors.toList()))
                .inOrder();
        if (!expectedLaunches.isEmpty()) {
            assertThat(mLastStarted.getAllValues()
                    .stream().map(BiometricUtils::getGatekeeperPasswordHandle).distinct()
                    .collect(Collectors.toList()))
                    .containsExactly(gkpw);
        }

        // final consent status
        assertThat(ParentalConsentHelper.hasFaceConsent(helper.getConsentResult()))
                .isEqualTo(requireFace && grantFace);
        assertThat(ParentalConsentHelper.hasFingerprintConsent(helper.getConsentResult()))
                .isEqualTo(requireFingerprint && grantFingerprint);
    }

    private static Intent getResultIntent(@BiometricAuthenticator.Modality int modality) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_MODALITY, modality);
        return intent;
    }

    @BiometricAuthenticator.Modality
    private static int getStartedModality(String name) {
        if (name.equals(FaceEnrollParentalConsent.class.getName())) {
            return TYPE_FACE;
        }
        if (name.equals(FingerprintEnrollParentalConsent.class.getName())) {
            return TYPE_FINGERPRINT;
        }
        return TYPE_NONE;
    }
}

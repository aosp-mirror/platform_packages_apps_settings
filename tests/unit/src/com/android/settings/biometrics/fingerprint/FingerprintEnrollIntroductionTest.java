/*
 * Copyright (C) 2016 The Android Open Source Project
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


import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.test.ActivityUnitTestCase;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class FingerprintEnrollIntroductionTest
        extends ActivityUnitTestCase<FingerprintEnrollIntroduction> {

    private TestContext mContext;

    @Mock
    private FingerprintManager mFingerprintManager;

    private FingerprintEnrollIntroduction mActivity;

    public FingerprintEnrollIntroductionTest() {
        super(FingerprintEnrollIntroduction.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        mContext = new TestContext(getInstrumentation().getTargetContext());
        setActivityContext(mContext);

        getInstrumentation().runOnMainSync(() -> {
            final Intent intent = new Intent();
            mActivity = startActivity(intent,
                    null /* savedInstanceState */, null /* lastNonConfigurationInstance */);
        });
    }

    public void testMaxFingerprint_shouldShowErrorMessage() {
        final int max = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
        doReturn(generateFingerprintList(max)).when(mFingerprintManager)
                .getEnrolledFingerprints(anyInt());

        getInstrumentation().runOnMainSync(() -> {
            getInstrumentation().callActivityOnCreate(mActivity, null);
            getInstrumentation().callActivityOnResume(mActivity);
        });

        final TextView errorTextView = (TextView) mActivity.findViewById(R.id.error_text);
        assertNotNull(errorTextView.getText().toString());

        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button nextButton = layout.getMixin(FooterBarMixin.class).getPrimaryButtonView();
        assertEquals(View.GONE, nextButton.getVisibility());
    }

    private List<Fingerprint> generateFingerprintList(int num) {
        ArrayList<Fingerprint> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            list.add(new Fingerprint("Fingerprint " + i, 0, i, 0));
        }
        return list;
    }

    public class TestContext extends ContextWrapper {

        public TestContext(Context base) {
            super(base);
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.FINGERPRINT_SERVICE.equals(name)) {
                return mFingerprintManager;
            }
            return super.getSystemService(name);
        }
    }
}

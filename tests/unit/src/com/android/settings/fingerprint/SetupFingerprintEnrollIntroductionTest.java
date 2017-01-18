/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fingerprint;


import static org.mockito.Mockito.doReturn;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SetupFingerprintEnrollIntroductionTest
        extends ActivityUnitTestCase<SetupFingerprintEnrollIntroduction> {

    private TestContext mContext;

    @Mock
    private KeyguardManager mKeyguardManager;

    private SetupFingerprintEnrollIntroduction mActivity;

    public SetupFingerprintEnrollIntroductionTest() {
        super(SetupFingerprintEnrollIntroduction.class);
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

    public void testKeyguardNotSecure_shouldShowSkipDialog() {
        doReturn(false).when(mKeyguardManager).isKeyguardSecure();

        getInstrumentation().runOnMainSync(() -> {
            getInstrumentation().callActivityOnCreate(mActivity, null);
            getInstrumentation().callActivityOnResume(mActivity);

            final Button skipButton =
                    (Button) mActivity.findViewById(R.id.fingerprint_cancel_button);
            assertEquals(View.VISIBLE, skipButton.getVisibility());
            skipButton.performClick();
        });

        assertFalse(isFinishCalled());
    }

    public void testKeyguardSecure_shouldNotShowSkipDialog() {
        doReturn(true).when(mKeyguardManager).isKeyguardSecure();

        getInstrumentation().runOnMainSync(() -> {
            getInstrumentation().callActivityOnCreate(mActivity, null);
            getInstrumentation().callActivityOnResume(mActivity);

            final Button skipButton =
                    (Button) mActivity.findViewById(R.id.fingerprint_cancel_button);
            assertEquals(View.VISIBLE, skipButton.getVisibility());
            skipButton.performClick();
        });

        assertTrue(isFinishCalled());
    }

    public class TestContext extends ContextWrapper {

        public TestContext(Context base) {
            super(base);
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.KEYGUARD_SERVICE.equals(name)) {
                return mKeyguardManager;
            }
            return super.getSystemService(name);
        }
    }
}

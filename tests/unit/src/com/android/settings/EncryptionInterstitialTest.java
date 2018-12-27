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

package com.android.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.ButtonFooterMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class EncryptionInterstitialTest {

    private Instrumentation mInstrumentation;
    private Context mContext;
    private TestActivityMonitor mActivityMonitor;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mActivityMonitor = new TestActivityMonitor();
        mInstrumentation.addMonitor(mActivityMonitor);
    }

    @After
    public void tearDown() {
        mInstrumentation.removeMonitor(mActivityMonitor);
    }

    @Test
    public void clickYes_shouldRequirePassword() {
        final Activity activity = mInstrumentation.startActivitySync(
                new Intent(mContext, EncryptionInterstitial.class)
                        .putExtra("extra_unlock_method_intent", new Intent("test.unlock.intent")));
        final PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(ButtonFooterMixin.class).getPrimaryButtonView().performClick();

        mActivityMonitor.waitForActivityWithTimeout(1000);
        assertEquals(1, mActivityMonitor.getHits());

        assertTrue(mActivityMonitor.mMatchedIntent.getBooleanExtra(
                EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, false));
    }

    @Test
    public void clickNo_shouldNotRequirePassword() {
        final Activity activity = mInstrumentation.startActivitySync(
                new Intent(mContext, EncryptionInterstitial.class)
                        .putExtra("extra_unlock_method_intent", new Intent("test.unlock.intent")));
        final PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(ButtonFooterMixin.class).getSecondaryButtonView().performClick();

        mActivityMonitor.waitForActivityWithTimeout(1000);
        assertEquals(1, mActivityMonitor.getHits());

        assertFalse(mActivityMonitor.mMatchedIntent.getBooleanExtra(
                EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true));
    }

    private static class TestActivityMonitor extends ActivityMonitor {

        Intent mMatchedIntent = null;

        @Override
        public ActivityResult onStartActivity(Intent intent) {
            if ("test.unlock.intent".equals(intent.getAction())) {
                mMatchedIntent = intent;
                return new ActivityResult(Activity.RESULT_OK, null);
            }
            return null;
        }
    }
}

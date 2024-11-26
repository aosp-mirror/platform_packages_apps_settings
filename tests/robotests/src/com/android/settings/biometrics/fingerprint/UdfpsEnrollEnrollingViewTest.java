/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.android.settings.R;
import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.LooperMode;


@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class UdfpsEnrollEnrollingViewTest {

    private Context mThemeContext;
    private TestFingerprintEnrollEnrolling mFingerprintEnrollEnrolling;
    private ActivityController<UdfpsEnrollEnrollingViewTest.TestFingerprintEnrollEnrolling>
            mController;
    private AttributeSet mAttributeSet;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = Robolectric.buildActivity(TestFingerprintEnrollEnrolling.class);
        mFingerprintEnrollEnrolling = mController.create().get();
        mThemeContext = new ContextThemeWrapper(mFingerprintEnrollEnrolling,
                R.style.SudThemeGlif_Light);
        mAttributeSet = Robolectric.buildAttributeSet().build();
    }

    private void assertDefaultTemplate(TestUdfpsEnrollEnrollingView layout) {
        final View title = layout.findViewById(
                com.google.android.setupdesign.R.id.suc_layout_title);
        assertThat(title).isNotNull();

        final View subTitle = layout.findViewById(
                com.google.android.setupdesign.R.id.sud_layout_subtitle);
        assertThat(subTitle).isNotNull();

        final View icon = layout.findViewById(com.google.android.setupdesign.R.id.sud_layout_icon);
        assertThat(icon).isNotNull();

        final View scrollView = layout.findViewById(
                com.google.android.setupdesign.R.id.sud_scroll_view);
        assertThat(scrollView).isNotNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENROLL_LAYOUT_TRUNCATE_IMPROVEMENT)
    public void testDefaultTemplate() {
        TestUdfpsEnrollEnrollingView layout = new TestUdfpsEnrollEnrollingView(mThemeContext,
                mAttributeSet);
        assertDefaultTemplate(layout);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENROLL_LAYOUT_TRUNCATE_IMPROVEMENT)
    public void testGlifHeaderScrollView() {
        TestUdfpsEnrollEnrollingView layout = new TestUdfpsEnrollEnrollingView(mThemeContext,
                mAttributeSet);
        final View headerScrollView = layout.findViewById(
                R.id.sud_header_scroll_view);

        assertThat(headerScrollView).isNotNull();
    }

    public static class TestFingerprintEnrollEnrolling extends FingerprintEnrollEnrolling {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            final TestUdfpsEnrollEnrollingView layout =
                    (TestUdfpsEnrollEnrollingView) getLayoutInflater().inflate(
                            R.layout.test_udfps_enroll_enrolling, null, false);
            setContentView(layout);
        }
    }
}

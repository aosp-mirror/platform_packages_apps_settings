/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.utils;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class AnnotationSpanTest {

    private Intent mTestIntent;
    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mTestIntent = new Intent("test_action");
    }

    @Test
    public void newLinkInfo_validIntent_isActionable() {
        mPackageManager.addResolveInfoForIntent(mTestIntent, new ResolveInfo());
        assertThat(new AnnotationSpan.LinkInfo(mContext, "annotation", mTestIntent).isActionable())
                .isTrue();
    }

    @Test
    public void newLinkInfo_invalidIntent_isNotActionable() {
        assertThat(new AnnotationSpan.LinkInfo(mContext, "annotation", mTestIntent).isActionable())
                .isFalse();
    }
}

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
 * limitations under the License
 */

package com.android.settings.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ZenAccessSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void logSpecialPermissionChange() {
        ZenAccessSettings.logSpecialPermissionChange(true, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_ALLOW),
                eq("app"));

        ZenAccessSettings.logSpecialPermissionChange(false, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_DENY),
                eq("app"));
    }
}

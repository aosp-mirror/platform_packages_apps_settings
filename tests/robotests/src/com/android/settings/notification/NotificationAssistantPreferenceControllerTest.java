/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification;

import static junit.framework.TestCase.assertEquals;

import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Debug;

import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotificationAssistantPreferenceControllerTest {

    private static final String KEY = "TEST_KEY";
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    private NotificationAssistantPreferenceController mPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreferenceController = new TestPreferenceController(mContext, mBackend);
    }

    @Test
    public void testGetSummary_noAssistant() {
        when(mBackend.getAllowedNotificationAssistant()).thenReturn(null);
        CharSequence noneLabel = new NotificationAssistantPicker.CandidateNone(mContext)
                .loadLabel();
        assertEquals(noneLabel, mPreferenceController.getSummary());
    }

    @Test
    public void testGetSummary_TestAssistant() {
        String testName = "test_pkg/test_cls";
        when(mBackend.getAllowedNotificationAssistant()).thenReturn(
                ComponentName.unflattenFromString(testName));
        assertEquals(testName, mPreferenceController.getSummary());
    }

    private final class TestPreferenceController extends NotificationAssistantPreferenceController {

        private TestPreferenceController(Context context, NotificationBackend backend) {
            super(context, KEY);
            mNotificationBackend = backend;
        }

        @Override
        public String getPreferenceKey() {
            return KEY;
        }

        @Override
        protected CandidateInfo createCandidateInfo(ComponentName cn) {
            return new CandidateInfo(true) {
                @Override
                public CharSequence loadLabel() { return cn.flattenToString(); }

                @Override
                public Drawable loadIcon() { return null; }

                @Override
                public String getKey() { return null; }
            };
        }
    }

}

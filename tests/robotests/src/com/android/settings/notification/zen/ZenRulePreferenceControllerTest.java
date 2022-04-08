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

package com.android.settings.notification.zen;

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.notification.zen.AbstractZenCustomRulePreferenceController;
import com.android.settings.notification.zen.ZenCustomRadioButtonPreference;
import com.android.settings.notification.zen.ZenModeBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ZenRulePreferenceControllerTest {

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private ZenCustomRadioButtonPreference mockPref;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private TestablePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mController = new TestablePreferenceController(mContext,"test", mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onResumeTest() {
        final String id = "testid";
        final AutomaticZenRule rule = new AutomaticZenRule("test", null, null,
                null, null, NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);

        assertTrue(mController.mRule == null);
        assertTrue(mController.mId == null);

        mController.onResume(rule, id);

        assertEquals(mController.mId, id);
        assertEquals(mController.mRule, rule);
    }

    class TestablePreferenceController extends AbstractZenCustomRulePreferenceController {
        TestablePreferenceController(Context context, String key,
                Lifecycle lifecycle) {
            super(context, key, lifecycle);
        }
    }
}

/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.notification.app;

import static android.service.notification.Adjustment.KEY_IMPORTANCE;
import static android.service.notification.Adjustment.KEY_SUMMARIZATION;
import static android.service.notification.Adjustment.KEY_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AdjustmentKeyPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private NotificationBackend.AppRow mAppRow;
    @Mock
    private NotificationBackend mBackend;
    private RestrictedSwitchPreference mSwitch;

    private AdjustmentKeyPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mSwitch = new RestrictedSwitchPreference(mContext);
        new PreferenceManager(mContext).createPreferenceScreen(mContext).addPreference(mSwitch);
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(true);

        mPrefController = new AdjustmentKeyPreferenceController(mContext, mBackend, KEY_TYPE);

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg.name";
        mAppRow.uid = 12345;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);
    }

    @Test
    @DisableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_flagOff() {
        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_flagOn() {
        assertThat(mPrefController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_summarization_notMsgApp() {
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(false);

        mPrefController = new AdjustmentKeyPreferenceController(
                mContext, mBackend, KEY_SUMMARIZATION);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testChecked_adjustmentAllowed() {
        when(mBackend.getAllowedAssistantAdjustments(mAppRow.pkg)).thenReturn(
                List.of(KEY_TYPE, KEY_IMPORTANCE));
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.isChecked()).isTrue();

        when(mBackend.getAllowedAssistantAdjustments(mAppRow.pkg)).thenReturn(
                List.of(KEY_SUMMARIZATION, KEY_IMPORTANCE));
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);
        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.isChecked()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testOnPreferenceChange_changeOnAndOff() {
        when(mBackend.getAllowedAssistantAdjustments(mAppRow.pkg)).thenReturn(
                List.of(KEY_TYPE, KEY_IMPORTANCE));
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        // when the switch value changes to false
        mPrefController.onPreferenceChange(mSwitch, false);

        verify(mBackend, times(1))
                .setAdjustmentSupportedForPackage(eq(KEY_TYPE), eq(mAppRow.pkg), eq(false));

        // same as above but now from false -> true
        mPrefController.onPreferenceChange(mSwitch, true);
        verify(mBackend, times(1))
                .setAdjustmentSupportedForPackage(eq(KEY_TYPE), eq(mAppRow.pkg), eq(true));
    }
}

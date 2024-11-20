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
package com.android.settings.notification.app;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

@RunWith(RobolectricTestRunner.class)
public class PromotedNotificationsPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private NotificationBackend.AppRow mAppRow;
    @Mock
    private NotificationBackend mBackend;
    private RestrictedSwitchPreference mSwitch;

    private PromotedNotificationsPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mSwitch = new RestrictedSwitchPreference(mContext);
        new PreferenceManager(mContext).createPreferenceScreen(mContext).addPreference(mSwitch);
        mPrefController = new PromotedNotificationsPreferenceController(mContext, mBackend);

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg.name";
        mAppRow.uid = 12345;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);
    }

    @Test
    @DisableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testIsAvailable_flagOff() {
        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testIsAvailable_flagOn() {
        assertThat(mPrefController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testChecked_canBePromoted() {
        mAppRow.canBePromoted = true;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.isChecked()).isTrue();

        mAppRow.canBePromoted = false;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);
        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testOnPreferenceChange_noChange() {
        mAppRow.canBePromoted = true;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        // No change means no backend call
        mPrefController.onPreferenceChange(mSwitch, true);
        verify(mBackend, never()).setCanBePromoted(any(), anyInt(), anyBoolean());
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testOnPreferenceChange_changeOnAndOff() {
        mAppRow.canBePromoted = true;
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        // when the switch value changes to false
        mPrefController.onPreferenceChange(mSwitch, false);

        // then updates the app row data in the preference controller
        assertThat(mPrefController.mAppRow.canBePromoted).isFalse();
        // and also updates the backend
        verify(mBackend, times(1)).setCanBePromoted(eq(mAppRow.pkg), eq(mAppRow.uid), eq(false));

        // same as above but now from false -> true
        mPrefController.onPreferenceChange(mSwitch, true);
        assertThat(mPrefController.mAppRow.canBePromoted).isTrue();
        verify(mBackend, times(1)).setCanBePromoted(eq(mAppRow.pkg), eq(mAppRow.uid), eq(true));
    }
}

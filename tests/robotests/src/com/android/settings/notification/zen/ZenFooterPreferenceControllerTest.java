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

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.notification.zen.ZenFooterPreferenceController;
import com.android.settings.notification.zen.ZenModeBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;

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
public class ZenFooterPreferenceControllerTest {
    private ZenFooterPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private FooterPreference mockPref;
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    NotificationManager mNotificationManager;

    private static final String PREF_KEY = "main_pref";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        when(mNotificationManager.getNotificationPolicy()).thenReturn(
                mock(NotificationManager.Policy.class));
        mController = new ZenFooterPreferenceController(
                mContext, mock(Lifecycle.class), PREF_KEY);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
    }

    @Test
    public void isAvailable_noVisEffects() {
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, 0);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_someVisEffects() {
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, 2);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_allVisEffects() {
        int allSuppressed = SUPPRESSED_EFFECT_SCREEN_OFF
                | SUPPRESSED_EFFECT_SCREEN_ON
                | SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                | SUPPRESSED_EFFECT_AMBIENT
                | SUPPRESSED_EFFECT_STATUS_BAR
                | SUPPRESSED_EFFECT_BADGE
                | SUPPRESSED_EFFECT_LIGHTS
                | SUPPRESSED_EFFECT_PEEK
                | SUPPRESSED_EFFECT_NOTIFICATION_LIST;
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, allSuppressed);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateSummary_noVisEffects() {
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, 0);
        mController.updateState(mockPref);
        verify(mockPref).setTitle(R.string.zen_mode_restrict_notifications_mute_footer);
    }

    @Test
    public void getSummary_someVisEffects() {
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, 2);
        mController.updateState(mockPref);
        verify(mockPref).setTitle(null);
    }

    @Test
    public void getSummary_allVisEffects() {
        int allSuppressed = SUPPRESSED_EFFECT_SCREEN_OFF
                | SUPPRESSED_EFFECT_SCREEN_ON
                | SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                | SUPPRESSED_EFFECT_AMBIENT
                | SUPPRESSED_EFFECT_STATUS_BAR
                | SUPPRESSED_EFFECT_BADGE
                | SUPPRESSED_EFFECT_LIGHTS
                | SUPPRESSED_EFFECT_PEEK
                | SUPPRESSED_EFFECT_NOTIFICATION_LIST;
        mBackend.mPolicy = new NotificationManager.Policy(0, 0, 0, allSuppressed);
        mController.updateState(mockPref);
        verify(mockPref).setTitle(R.string.zen_mode_restrict_notifications_hide_footer);
    }
}

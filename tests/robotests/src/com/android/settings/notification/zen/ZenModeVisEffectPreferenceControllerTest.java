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

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.PreferenceScreen;

import com.android.settings.notification.zen.ZenModeBackend;
import com.android.settings.notification.zen.ZenModeVisEffectPreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.DisabledCheckBoxPreference;
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
public class ZenModeVisEffectPreferenceControllerTest {
    private ZenModeVisEffectPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private DisabledCheckBoxPreference mockPref;
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    NotificationManager mNotificationManager;

    private static final String PREF_KEY = "main_pref";
    private static final int PREF_METRICS = 1;
    private static final int PARENT_EFFECT1 = SUPPRESSED_EFFECT_BADGE;
    private static final int PARENT_EFFECT2 = SUPPRESSED_EFFECT_NOTIFICATION_LIST;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        when(mNotificationManager.getNotificationPolicy()).thenReturn(
                mock(NotificationManager.Policy.class));
        mController = new ZenModeVisEffectPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY, SUPPRESSED_EFFECT_PEEK, PREF_METRICS, null);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable() {
        // SUPPRESSED_EFFECT_PEEK is always available:
        assertTrue(mController.isAvailable());

        // SUPPRESSED_EFFECT_LIGHTS is only available if the device has an LED:
        Context mockContext = mock(Context.class);
        mController = new ZenModeVisEffectPreferenceController(mockContext, mock(Lifecycle.class),
                PREF_KEY, SUPPRESSED_EFFECT_LIGHTS, PREF_METRICS, null);
        Resources mockResources = mock(Resources.class);
        when(mockContext.getResources()).thenReturn(mockResources);

        when(mockResources.getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(false); // no light
        assertFalse(mController.isAvailable());

        when(mockResources.getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(true); // has light
        assertTrue(mController.isAvailable());
    }

    @Test
    public void updateState_notChecked() {
        when(mBackend.isVisualEffectSuppressed(SUPPRESSED_EFFECT_PEEK)).thenReturn(false);
        mController.updateState(mockPref);

        verify(mockPref).setChecked(false);
        verify(mockPref).enableCheckbox(true);
    }

    @Test
    public void updateState_checked() {
        when(mBackend.isVisualEffectSuppressed(SUPPRESSED_EFFECT_PEEK)).thenReturn(true);
        mController.updateState(mockPref);

        verify(mockPref).setChecked(true);
        verify(mockPref).enableCheckbox(true);
    }

    @Test
    public void updateState_checkedFalse_parentChecked() {
        mController = new ZenModeVisEffectPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY, SUPPRESSED_EFFECT_PEEK, PREF_METRICS,
                new int[]{PARENT_EFFECT1, PARENT_EFFECT2});
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mBackend.isVisualEffectSuppressed(SUPPRESSED_EFFECT_PEEK)).thenReturn(false);
        when(mBackend.isVisualEffectSuppressed(PARENT_EFFECT1)).thenReturn(false);
        when(mBackend.isVisualEffectSuppressed(PARENT_EFFECT2)).thenReturn(true);
        mController.updateState(mockPref);

        verify(mockPref).setChecked(true);
        verify(mockPref).enableCheckbox(false);
        verify(mBackend, times(1)).saveVisualEffectsPolicy(SUPPRESSED_EFFECT_PEEK, true);
    }

    @Test
    public void updateState_checkedFalse_parentNotChecked() {
        mController = new ZenModeVisEffectPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY, SUPPRESSED_EFFECT_PEEK, PREF_METRICS,
                new int[]{PARENT_EFFECT1, PARENT_EFFECT2});
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mBackend.isVisualEffectSuppressed(SUPPRESSED_EFFECT_PEEK)).thenReturn(false);
        when(mBackend.isVisualEffectSuppressed(PARENT_EFFECT1)).thenReturn(false);
        when(mBackend.isVisualEffectSuppressed(PARENT_EFFECT2)).thenReturn(false);
        mController.updateState(mockPref);

        verify(mockPref).setChecked(false);
        verify(mockPref).enableCheckbox(true);
        verify(mBackend, never()).saveVisualEffectsPolicy(SUPPRESSED_EFFECT_PEEK, true);
    }

    @Test
    public void onPreferenceChanged_checkedFalse() {
        mController.onPreferenceChange(mockPref, false);

        verify(mBackend).saveVisualEffectsPolicy(SUPPRESSED_EFFECT_PEEK, false);
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(PREF_METRICS),
                eq(false));
    }

    @Test
    public void onPreferenceChanged_checkedTrue() {
        mController.onPreferenceChange(mockPref, true);
        verify(mBackend).saveVisualEffectsPolicy(SUPPRESSED_EFFECT_PEEK, true);
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(PREF_METRICS),
                eq(true));
    }
}

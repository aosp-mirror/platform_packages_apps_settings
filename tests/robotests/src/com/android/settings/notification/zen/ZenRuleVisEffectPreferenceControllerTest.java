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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.service.notification.ZenPolicy;

import androidx.preference.PreferenceScreen;

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
public class ZenRuleVisEffectPreferenceControllerTest extends ZenRuleCustomPrefContrTestBase {

    private static final @ZenPolicy.VisualEffect int EFFECT_PEEK = ZenPolicy.VISUAL_EFFECT_PEEK;
    private static final @ZenPolicy.VisualEffect int PARENT_EFFECT1 = ZenPolicy.VISUAL_EFFECT_BADGE;
    private static final @ZenPolicy.VisualEffect int PARENT_EFFECT2 =
            ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST;
    private static final int PREF_METRICS = 1;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private DisabledCheckBoxPreference mockPref;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    NotificationManager mNotificationManager;

    private Context mContext;
    private ZenRuleVisEffectPreferenceController mController;

    @Override
    AbstractZenCustomRulePreferenceController getController() {
        return mController;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        when(mNotificationManager.getNotificationPolicy()).thenReturn(
                mock(NotificationManager.Policy.class));

        mContext = RuntimeEnvironment.application;
        mController = new ZenRuleVisEffectPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY, EFFECT_PEEK, PREF_METRICS, null);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mBackend.getAutomaticZenRule(RULE_ID)).thenReturn(mRule);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable() {
        // VISUAL_EFFECT_PEEK isn't available until after onResume is called
        assertFalse(mController.isAvailable());
        updateControllerZenPolicy(new ZenPolicy()); // calls onResume
        assertTrue(mController.isAvailable());

        // VISUAL_EFFECT_LIGHTS is only available if the device has an LED:
        Context mockContext = mock(Context.class);
        mController = new ZenRuleVisEffectPreferenceController(mockContext, mock(Lifecycle.class),
                PREF_KEY, ZenPolicy.VISUAL_EFFECT_LIGHTS, PREF_METRICS, null);
        updateControllerZenPolicy(new ZenPolicy()); // calls onResume

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
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showPeeking(true)
                .build());
        mController.updateState(mockPref);

        verify(mockPref).setChecked(false);
        verify(mockPref).enableCheckbox(true);
    }

    @Test
    public void updateState_checked() {
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showPeeking(false)
                .build());
        mController.updateState(mockPref);

        verify(mockPref).setChecked(true);
        verify(mockPref).enableCheckbox(true);
    }

    @Test
    public void updateState_checkedFalse_parentChecked() {
        mController.mParentSuppressedEffects = new int[]{PARENT_EFFECT1, PARENT_EFFECT2};
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showVisualEffect(EFFECT_PEEK, true)
                .showVisualEffect(PARENT_EFFECT1, true)
                .showVisualEffect(PARENT_EFFECT2, false)
                .build());
        mController.updateState(mockPref);

        verify(mockPref).setChecked(true);
        verify(mockPref).enableCheckbox(false);
    }

    @Test
    public void updateState_checkedFalse_parentNotChecked() {
        mController.mParentSuppressedEffects = new int[]{PARENT_EFFECT1, PARENT_EFFECT2};
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showVisualEffect(EFFECT_PEEK, true)
                .showVisualEffect(PARENT_EFFECT1, true)
                .showVisualEffect(PARENT_EFFECT2, true)
                .build());
        mController.updateState(mockPref);

        verify(mockPref).setChecked(false);
        verify(mockPref).enableCheckbox(true);
    }

    @Test
    public void onPreferenceChanged_checkedFalse() {
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showPeeking(false)
                .build());
        mController.onPreferenceChange(mockPref, false);
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .showPeeking(true)
                .build());
        verify(mBackend).updateZenRule(RULE_ID, mRule);
    }

    @Test
    public void onPreferenceChanged_checkedTrue() {
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .showPeeking(true)
                .build());
        mController.onPreferenceChange(mockPref, true);
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .showPeeking(false)
                .build());
        verify(mBackend).updateZenRule(RULE_ID, mRule);
    }
}

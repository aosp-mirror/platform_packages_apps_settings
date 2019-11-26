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
 * limitations under the License.
 */

package com.android.settings.notification.zen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.zen.ZenModeAutomaticRulesPreferenceController;
import com.android.settings.notification.zen.ZenModeBackend;
import com.android.settings.notification.zen.ZenRulePreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ZenModeAutomaticRulesPreferenceControllerTest {

    private ZenModeAutomaticRulesPreferenceController mController;
    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private PreferenceCategory mockPref;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ZenRulePreference mZenRulePreference;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new ZenModeAutomaticRulesPreferenceController(mContext, mock(Fragment.class),
            null));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
            mockPref);
        mController.displayPreference(mPreferenceScreen);
        doReturn(mZenRulePreference).when(mController).createZenRulePreference(any());
    }

    @Test
    public void testUpdateState_clearsPreferencesWhenAddingNewPreferences() {
        final int NUM_RULES = 3;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();

        String ruleId1 = "test1_id";
        String ruleId2 = "test2_id";
        String ruleId3 = "test3_id";

        AutomaticZenRule autoRule1 = new AutomaticZenRule("test_rule_1", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        AutomaticZenRule autoRule2 = new AutomaticZenRule("test_rule_2", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20);
        AutomaticZenRule autoRule3 = new AutomaticZenRule("test_rule_3", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 30);

        rMap.put(ruleId1, autoRule1);
        rMap.put(ruleId2, autoRule2);
        rMap.put(ruleId3, autoRule3);

        // should add 3 new preferences to mockPref
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.updateState(mockPref);
        verify(mockPref, times(1)).removeAll();
        verify(mockPref, times(NUM_RULES)).addPreference(any());
    }

    @Test
    public void testUpdateState_clearsPreferencesWhenRemovingPreferences(){
        final int NUM_RULES = 2;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();

        String ruleId1 = "test1_id";
        String ruleId2 = "test2_id";

        AutomaticZenRule autoRule1 = new AutomaticZenRule("test_rule_1", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        AutomaticZenRule autoRule2 = new AutomaticZenRule("test_rule_2", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20);

        rMap.put(ruleId1, autoRule1);
        rMap.put(ruleId2, autoRule2);

        // update state should re-add all preferences since a preference was deleted
        when(mockPref.getPreferenceCount()).thenReturn(NUM_RULES + 2);
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.updateState(mockPref);
        verify(mockPref, times(1)).removeAll();
        verify(mockPref, times(NUM_RULES)).addPreference(any());
    }

    @Test
    public void testUpdateState_updateEnableState() throws NoSuchFieldException {
        final int NUM_RULES = 1;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();
        String testId = "test1_id";
        AutomaticZenRule rule = new AutomaticZenRule("rule_name", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        rMap.put(testId, rule);

        when(mockPref.getPreferenceCount()).thenReturn(NUM_RULES);
        when(mockPref.getPreference(anyInt())).thenReturn(mZenRulePreference);

        // update state should NOT re-add all the preferences, should only update enable state
        rule.setEnabled(false);
        rMap.put(testId, rule);
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        FieldSetter.setField(mZenRulePreference, ZenRulePreference.class.getDeclaredField("mId"), testId);
        mController.updateState(mockPref);
        verify(mZenRulePreference, times(1)).updatePreference(any());
        verify(mController, never()).reloadAllRules(any());
    }

    private void mockGetAutomaticZenRules(int numRules, Map<String, AutomaticZenRule> rules) {
        Map.Entry<String, AutomaticZenRule>[] arr = new Map.Entry[numRules];
        rules.entrySet().toArray(arr);
        when(mBackend.getAutomaticZenRules()).thenReturn(arr);
    }
}
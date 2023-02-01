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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;
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
        mContext = ApplicationProvider.getApplicationContext();
        mController = spy(new ZenModeAutomaticRulesPreferenceController(mContext, mock(Fragment.class),
            null));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
            mockPref);
        mController.displayPreference(mPreferenceScreen);
        doReturn(mZenRulePreference).when(mController).createZenRulePreference(any());
    }

    @Test
    public void testDisplayPreference_resetsPreferencesWhenCategoryEmpty() {
        // when the PreferenceCategory is empty (no preferences), make sure we clear out any
        // stale state in the cached set of zen rule preferences
        mController.mZenRulePreferences.put("test1_id", mZenRulePreference);
        when(mockPref.getPreferenceCount()).thenReturn(0);
        mController.displayPreference(mPreferenceScreen);
        assertTrue(mController.mZenRulePreferences.isEmpty());
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
        assertEquals(NUM_RULES, mController.mZenRulePreferences.size());
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

        // Add three preferences to the set of previously-known-about ZenRulePreferences; in this
        // case, test3_id is "deleted"
        mController.mZenRulePreferences.put("test1_id", mZenRulePreference);
        mController.mZenRulePreferences.put("test2_id", mZenRulePreference);
        mController.mZenRulePreferences.put("test3_id", mZenRulePreference);

        // update state should re-add all preferences since a preference was deleted
        when(mockPref.getPreferenceCount()).thenReturn(NUM_RULES + 1);
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.updateState(mockPref);
        verify(mockPref, times(1)).removeAll();
        verify(mockPref, times(NUM_RULES)).addPreference(any());
        assertEquals(NUM_RULES, mController.mZenRulePreferences.size());
    }

    @Test
    public void testUpdateState_clearsPreferencesWhenSameNumberButDifferentPrefs() {
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

        // Add two preferences to the set of previously-known-about ZenRulePreferences; in this
        // case, test3_id is "deleted" but test2_id is "added"
        mController.mZenRulePreferences.put("test1_id", mZenRulePreference);
        mController.mZenRulePreferences.put("test3_id", mZenRulePreference);

        // update state should re-add all preferences since a preference was deleted
        when(mockPref.getPreferenceCount()).thenReturn(NUM_RULES);
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.updateState(mockPref);
        verify(mockPref, times(1)).removeAll();
        verify(mockPref, times(NUM_RULES)).addPreference(any());
        assertEquals(NUM_RULES, mController.mZenRulePreferences.size());
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
        mController.mZenRulePreferences.put("test1_id", mZenRulePreference);

        // update state should NOT re-add all the preferences, should only update enable state
        rule.setEnabled(false);
        rMap.put(testId, rule);
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        setZenRulePreferenceField("mId", testId);
        mController.updateState(mockPref);
        verify(mZenRulePreference, times(1)).updatePreference(any());
        verify(mockPref, never()).removeAll();
        assertEquals(NUM_RULES, mController.mZenRulePreferences.size());
    }

    private void setZenRulePreferenceField(String name, Object value) {
        try {
            Field field = ZenRulePreference.class.getDeclaredField("mId");
            field.setAccessible(true);
            field.set(mZenRulePreference, value);
        } catch (ReflectiveOperationException e) {
            fail("Unable to set mZenRulePreference field: " + name);
        }
    }

    private void mockGetAutomaticZenRules(int numRules, Map<String, AutomaticZenRule> rules) {
        Map.Entry<String, AutomaticZenRule>[] arr = new Map.Entry[numRules];
        rules.entrySet().toArray(arr);
        when(mBackend.getAutomaticZenRules()).thenReturn(arr);
    }
}

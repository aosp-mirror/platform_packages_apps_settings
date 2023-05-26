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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ZenModeAutomaticRulesPreferenceControllerTest {

    private ZenModeAutomaticRulesPreferenceController mController;
    @Mock
    private ZenModeBackend mBackend;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    @Mock
    PackageManager mPm;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPm);
        when(mPm.queryIntentActivities(any(), any())).thenReturn(
                ImmutableList.of(mock(ResolveInfo.class)));
        when(mBackend.getAutomaticZenRules()).thenReturn(new Map.Entry[0]);
        mController = spy(new ZenModeAutomaticRulesPreferenceController(
                mContext, mock(Fragment.class), null, mBackend));
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        mPreferenceCategory.setKey(mController.getPreferenceKey());
        mPreferenceScreen.addPreference(mPreferenceCategory);
    }

    @Test
    public void testDisplayPreference_notPersistent() {
        mController.displayPreference(mPreferenceScreen);
        assertFalse(mPreferenceCategory.isPersistent());
    }

    @Test
    public void testDisplayThenUpdateState_onlyAddsOnceRulesUnchanged() {
        final int NUM_RULES = 1;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();

        String ruleId1 = "test1_id";

        AutomaticZenRule autoRule1 = new AutomaticZenRule("test_rule_1", null, null,
                null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);

        rMap.put(ruleId1, autoRule1);

        // should add 1 new preferences to mockPref
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.displayPreference(mPreferenceScreen);
        mController.updateState(mPreferenceCategory);
        assertEquals(NUM_RULES, mPreferenceCategory.getPreferenceCount());
        verify(mPreferenceCategory, times(1)).addPreference(any());
    }

    @Test
    public void testDisplayThenUpdateState_addsIfRulesChange() {
        Map<String, AutomaticZenRule> rMap = new HashMap<>();

        String ruleId1 = "test1_id";
        AutomaticZenRule autoRule1 = new AutomaticZenRule("test_rule_1", null, null,
                null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        rMap.put(ruleId1, autoRule1);
        mockGetAutomaticZenRules(1, rMap);
        // adds one
        mController.displayPreference(mPreferenceScreen);

        String ruleId2 = "test2_id";
        AutomaticZenRule autoRule2 = new AutomaticZenRule("test_rule_2", null, null,
                null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20);
        rMap.put(ruleId2, autoRule2);
        mockGetAutomaticZenRules(2, rMap);

        mController.updateState(mPreferenceCategory);
        assertEquals(2, mPreferenceCategory.getPreferenceCount());
        verify(mPreferenceCategory, times(2)).addPreference(any());
    }

    @Test
    public void testUpdateState_addingNewPreferences() {
        mController.displayPreference(mPreferenceScreen);
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
        mController.updateState(mPreferenceCategory);
        assertEquals(NUM_RULES, mPreferenceCategory.getPreferenceCount());
    }

    @Test
    public void testUpdateState_addsAndRemoves(){
        mController.displayPreference(mPreferenceScreen);
        final int NUM_RULES = 2;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();

        String FAKE_1 = "fake key 1";
        String FAKE_2 = "fake 2";
        mPreferenceCategory.addPreference(new ZenRulePreference(mContext,
                new AbstractMap.SimpleEntry<>(FAKE_1, mock(AutomaticZenRule.class)),
                null, null, mBackend));
        mPreferenceCategory.addPreference(new ZenRulePreference(mContext,
                new AbstractMap.SimpleEntry<>(FAKE_2, mock(AutomaticZenRule.class)),
                null, null, mBackend));
        assertNotNull(mPreferenceCategory.findPreference(FAKE_1));
        assertNotNull(mPreferenceCategory.findPreference(FAKE_2));

        String ruleId1 = "test1_id";
        String ruleId2 = "test2_id";

        AutomaticZenRule autoRule1 = new AutomaticZenRule("test_rule_1", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        AutomaticZenRule autoRule2 = new AutomaticZenRule("test_rule_2", null, null,
            null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20);

        rMap.put(ruleId1, autoRule1);
        rMap.put(ruleId2, autoRule2);

        // update state should re-add all preferences since a preference was deleted
        mockGetAutomaticZenRules(NUM_RULES, rMap);
        mController.updateState(mPreferenceCategory);
        assertNull(mPreferenceCategory.findPreference(FAKE_1));
        assertNull(mPreferenceCategory.findPreference(FAKE_2));
        assertNotNull(mPreferenceCategory.findPreference(ruleId1));
        assertNotNull(mPreferenceCategory.findPreference(ruleId2));
    }

    @Test
    public void testUpdateState_updateEnableState() {
        mController.displayPreference(mPreferenceScreen);
        String testId = "test1_id";
        AutomaticZenRule rule = new AutomaticZenRule("rule_name", null, null,
                null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);

        mPreferenceCategory.addPreference(new ZenRulePreference(mContext,
                new AbstractMap.SimpleEntry<>(testId, rule),
                null, null, mBackend));

        final int NUM_RULES = 1;
        Map<String, AutomaticZenRule> rMap = new HashMap<>();
        AutomaticZenRule ruleUpdated = new AutomaticZenRule("rule_name", null, null,
                null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10);
        ruleUpdated.setEnabled(false);
        rMap.put(testId, ruleUpdated);
        mockGetAutomaticZenRules(NUM_RULES, rMap);

        mController.updateState(mPreferenceCategory);
        assertFalse(mPreferenceCategory.findPreference(testId).isEnabled());
        assertEquals(NUM_RULES, mPreferenceCategory.getPreferenceCount());
    }

    private void mockGetAutomaticZenRules(int numRules, Map<String, AutomaticZenRule> rules) {
        Map.Entry<String, AutomaticZenRule>[] arr = new Map.Entry[numRules];
        rules.entrySet().toArray(arr);
        when(mBackend.getAutomaticZenRules()).thenReturn(arr);
    }
}

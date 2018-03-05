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

package com.android.settings.notification;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenModeAutomaticRulesPreferenceControllerTest {

    private static final String GENERIC_RULE_NAME = "test";
    private static final String DEFAULT_ID_1 = "DEFAULT_1";
    private static final String DEFAULT_ID_2 = "DEFAULT_2";

    private ZenModeAutomaticRulesPreferenceController mController;
    private final List<String> mDefaultIds = Arrays.asList(DEFAULT_ID_1, DEFAULT_ID_2);

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private PreferenceCategory mockPref;
    @Mock
    private NotificationManager.Policy mPolicy;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);
        mController = new ZenModeAutomaticRulesPreferenceController(mContext, mock(Fragment.class),
                mock(Lifecycle.class));

        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        ReflectionHelpers.setField(mController, "mDefaultRuleIds", mDefaultIds);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_checkRuleOrderingDescending() {
        final int NUM_RULES = 4;
        when(mNotificationManager.getAutomaticZenRules()).thenReturn(
                mockAutoZenRulesDecreasingCreationTime(NUM_RULES));

        Map.Entry<String, AutomaticZenRule>[] rules = mController.sortedRules();
        assertEquals(NUM_RULES, rules.length);

        // check ordering, most recent should be at the bottom/end (ie higher creation time)
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + (NUM_RULES - 1 - i), rules[i].getKey());
        }
    }

    @Test
    public void updateState_checkRuleOrderingAscending() {
        final int NUM_RULES = 4;
        when(mNotificationManager.getAutomaticZenRules()).thenReturn(
                mockAutoZenRulesAscendingCreationTime(NUM_RULES));

        Map.Entry<String, AutomaticZenRule>[] rules = mController.sortedRules();
        assertEquals(NUM_RULES, rules.length);

        // check ordering, most recent should be at the bottom/end (ie higher creation time)
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + i, rules[i].getKey());
        }
    }

    @Test
    public void updateState_checkRuleOrderingDescending_withDefaultRules() {
        final int NUM_RULES = 4;

        Map<String, AutomaticZenRule> ruleMap = mockAutoZenRulesDecreasingCreationTime(NUM_RULES);
        ruleMap.put(DEFAULT_ID_2, new AutomaticZenRule("DEFAULT_1_NAME", null,
                null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20));
        ruleMap.put(DEFAULT_ID_1, new AutomaticZenRule("DEFAULT_1_NAME", null,
                null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10));
        when(mNotificationManager.getAutomaticZenRules()).thenReturn(ruleMap);

        Map.Entry<String, AutomaticZenRule>[] rules = mController.sortedRules();
        assertEquals(NUM_RULES + 2, rules.length);

        assertEquals(rules[0].getKey(), DEFAULT_ID_1);
        assertEquals(rules[1].getKey(), DEFAULT_ID_2);
        // NON-DEFAULT RULES check ordering, most recent at the bottom/end
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + (NUM_RULES - 1 - i), rules[i + 2].getKey());
        }
    }

    @Test
    public void updateState_checkRuleOrderingMix() {
        final int NUM_RULES = 4;
        // map with creation times: 0, 2, 4, 6
        Map<String,AutomaticZenRule> rMap = mockAutoZenRulesAscendingCreationTime(NUM_RULES);

        final String insertedRule1 = "insertedRule1";
        rMap.put(insertedRule1, new AutomaticZenRule(insertedRule1, null, null,
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 5));

        final String insertedRule2 = "insertedRule2";
        rMap.put(insertedRule2, new AutomaticZenRule(insertedRule2, null, null,
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 3));

        // rule map with rule creation times, 0, 2, 4, 6, 5, 3
        // sort should create ordering based on creation times: 0, 2, 3, 4, 5, 6
        when(mNotificationManager.getAutomaticZenRules()).thenReturn(rMap);

        Map.Entry<String, AutomaticZenRule>[] rules = mController.sortedRules();
        assertEquals(NUM_RULES + 2, rules.length); // inserted 2 rules

        // check ordering of inserted rules
        assertEquals(insertedRule1, rules[4].getKey());
        assertEquals(insertedRule2, rules[2].getKey());
    }

    private Map<String, AutomaticZenRule> mockAutoZenRulesAscendingCreationTime(int numRules) {
        Map<String, AutomaticZenRule> ruleMap = new HashMap<>();

        for (int i = 0; i < numRules; i++) {
            ruleMap.put(GENERIC_RULE_NAME + i, new AutomaticZenRule(GENERIC_RULE_NAME + i, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, i * 2));
        }

        return ruleMap;
    }

    private Map<String, AutomaticZenRule> mockAutoZenRulesDecreasingCreationTime(int numRules) {
        Map<String, AutomaticZenRule> ruleMap = new HashMap<>();

        for (int i = 0; i < numRules; i++) {
            ruleMap.put(GENERIC_RULE_NAME + i, new AutomaticZenRule(GENERIC_RULE_NAME + i, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, numRules - i));
        }

        return ruleMap;
    }
}

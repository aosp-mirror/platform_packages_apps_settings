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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenAdapters;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;

import com.android.settings.R;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.time.Duration;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModesBackendTest {

    private static final AutomaticZenRule ZEN_RULE =
            new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                    .setType(AutomaticZenRule.TYPE_DRIVING)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                    .build();

    private static final ZenMode MANUAL_DND_MODE = ZenMode.manualDndMode(
            new AutomaticZenRule.Builder("Do Not Disturb", Uri.EMPTY)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                    .build());

    private static final ZenMode ZEN_RULE_MODE = new ZenMode("rule", ZEN_RULE);

    @Mock
    private NotificationManager mNm;

    private Context mContext;
    private ZenModesBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);

        mContext = RuntimeEnvironment.application;
        mBackend = new ZenModesBackend(mContext);
    }

    @Test
    public void getModes_containsManualDndAndZenRules() {
        AutomaticZenRule rule2 = new AutomaticZenRule.Builder("Bedtime", Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().disallowAllSounds().build())
                .build();
        Policy dndPolicy = new Policy(Policy.PRIORITY_CATEGORY_ALARMS,
                Policy.PRIORITY_SENDERS_CONTACTS, Policy.PRIORITY_SENDERS_CONTACTS);
        when(mNm.getAutomaticZenRules()).thenReturn(
                ImmutableMap.of("rule1", ZEN_RULE, "rule2", rule2));
        when(mNm.getNotificationPolicy()).thenReturn(dndPolicy);

        List<ZenMode> modes = mBackend.getModes();

        assertThat(modes).containsExactly(
                ZenMode.manualDndMode(
                        new AutomaticZenRule.Builder(
                                mContext.getString(R.string.zen_mode_settings_title), Uri.EMPTY)
                                .setType(AutomaticZenRule.TYPE_OTHER)
                                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                                .setZenPolicy(ZenAdapters.notificationPolicyToZenPolicy(dndPolicy))
                                .setTriggerDescription(
                                        mContext.getString(R.string.zen_mode_settings_summary))
                                .setManualInvocationAllowed(true)
                                .build()),
                new ZenMode("rule1", ZEN_RULE),
                new ZenMode("rule2", rule2))
                .inOrder();
    }

    @Test
    public void getMode_manualDnd_returnsMode() {
        Policy dndPolicy = new Policy(Policy.PRIORITY_CATEGORY_ALARMS,
                Policy.PRIORITY_SENDERS_CONTACTS, Policy.PRIORITY_SENDERS_CONTACTS);
        when(mNm.getNotificationPolicy()).thenReturn(dndPolicy);

        ZenMode mode = mBackend.getMode(ZenMode.MANUAL_DND_MODE_ID);

        assertThat(mode).isEqualTo(
                ZenMode.manualDndMode(
                        new AutomaticZenRule.Builder(
                                mContext.getString(R.string.zen_mode_settings_title), Uri.EMPTY)
                                .setType(AutomaticZenRule.TYPE_OTHER)
                                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                                .setZenPolicy(ZenAdapters.notificationPolicyToZenPolicy(dndPolicy))
                                .setTriggerDescription(
                                        mContext.getString(R.string.zen_mode_settings_summary))
                                .setManualInvocationAllowed(true)
                                .build()));
    }

    @Test
    public void getMode_zenRule_returnsMode() {
        when(mNm.getAutomaticZenRule(eq("rule"))).thenReturn(ZEN_RULE);

        ZenMode mode = mBackend.getMode("rule");

        assertThat(mode).isEqualTo(new ZenMode("rule", ZEN_RULE));
    }

    @Test
    public void getMode_missingRule_returnsNull() {
        when(mNm.getAutomaticZenRule(any())).thenReturn(null);

        ZenMode mode = mBackend.getMode("rule");

        assertThat(mode).isNull();
        verify(mNm).getAutomaticZenRule(eq("rule"));
    }

    @Test
    public void updateMode_manualDnd_setsNotificationPolicy() {
        ZenMode manualDnd = ZenMode.manualDndMode(
                new AutomaticZenRule.Builder("DND", Uri.EMPTY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .build());

        mBackend.updateMode(manualDnd);

        verify(mNm).setNotificationPolicy(eq(new ZenModeConfig().toNotificationPolicy(
                new ZenPolicy.Builder().allowAllSounds().build())), eq(true));
    }

    @Test
    public void updateMode_zenRule_updatesRule() {
        ZenMode ruleMode = new ZenMode("rule", ZEN_RULE);

        mBackend.updateMode(ruleMode);

        verify(mNm).updateAutomaticZenRule(eq("rule"), eq(ZEN_RULE), eq(true));
    }

    @Test
    public void activateMode_manualDnd_setsZenModeImportant() {
        mBackend.activateMode(MANUAL_DND_MODE, null);

        verify(mNm).setZenMode(eq(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS), eq(null),
                any(), eq(true));
    }

    @Test
    public void activateMode_manualDndWithDuration_setsZenModeImportantWithCondition() {
        mBackend.activateMode(MANUAL_DND_MODE, Duration.ofMinutes(30));

        verify(mNm).setZenMode(eq(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS),
                eq(ZenModeConfig.toTimeCondition(mContext, 30, 0, true).id),
                any(),
                eq(true));
    }

    @Test
    public void activateMode_zenRule_setsRuleStateActive() {
        mBackend.activateMode(ZEN_RULE_MODE, null);

        verify(mNm).setAutomaticZenRuleState(eq(ZEN_RULE_MODE.getId()),
                eq(new Condition(ZEN_RULE.getConditionId(), "", Condition.STATE_TRUE,
                        Condition.SOURCE_USER_ACTION)));
    }

    @Test
    public void activateMode_zenRuleWithDuration_fails() {
        assertThrows(IllegalArgumentException.class,
                () -> mBackend.activateMode(ZEN_RULE_MODE, Duration.ofMinutes(30)));
    }

    @Test
    public void deactivateMode_manualDnd_setsZenModeOff() {
        mBackend.deactivateMode(MANUAL_DND_MODE);

        verify(mNm).setZenMode(eq(Settings.Global.ZEN_MODE_OFF), eq(null), any(), eq(true));
    }

    @Test
    public void deactivateMode_zenRule_setsRuleStateInactive() {
        mBackend.deactivateMode(ZEN_RULE_MODE);

        verify(mNm).setAutomaticZenRuleState(eq(ZEN_RULE_MODE.getId()),
                eq(new Condition(ZEN_RULE.getConditionId(), "", Condition.STATE_FALSE,
                        Condition.SOURCE_USER_ACTION)));
    }

    @Test
    public void removeMode_zenRule_deletesRule() {
        mBackend.removeMode(ZEN_RULE_MODE);

        verify(mNm).removeAutomaticZenRule(ZEN_RULE_MODE.getId(), true);
    }

    @Test
    public void removeMode_manualDnd_fails() {
        assertThrows(IllegalArgumentException.class, () -> mBackend.removeMode(MANUAL_DND_MODE));
    }
}

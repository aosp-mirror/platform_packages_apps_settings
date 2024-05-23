package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.database.Cursor;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ZenModeBackendTest {
    @Mock
    private NotificationManager mNotificationManager;

    private static final String GENERIC_RULE_NAME = "test";
    private static final String DEFAULT_ID_1 = ZenModeConfig.EVENTS_DEFAULT_RULE_ID;
    private static final String DEFAULT_ID_2 = ZenModeConfig.EVERY_NIGHT_DEFAULT_RULE_ID;

    private Context mContext;
    private ZenModeBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mBackend = new ZenModeBackend(mContext);
    }

    private Cursor createMockCursor(int size) {
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.moveToFirst()).thenReturn(true);

        doAnswer(new Answer<Boolean>() {
            int count = 0;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (count < size) {
                    count++;
                    return true;
                }
                return false;
            }

        }).when(mockCursor).moveToNext();

        return mockCursor;
    }

    private Map.Entry<String, AutomaticZenRule>[] populateAutoZenRulesAscendingCreationTime(
            int numRules, boolean addDefaultRules) {
        Map<String, AutomaticZenRule> ruleMap = new HashMap<>();

        for (int i = 0; i < numRules; i++) {
            ruleMap.put(GENERIC_RULE_NAME + i, new AutomaticZenRule(GENERIC_RULE_NAME + i, null,
                    null, null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true,
                    i * 2));
        }

        if (addDefaultRules) {
            ruleMap.put(DEFAULT_ID_1, new AutomaticZenRule("DEFAULT_1_NAME", null, null, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20));
            ruleMap.put(DEFAULT_ID_2, new AutomaticZenRule("DEFAULT_2_NAME", null, null, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10));
        }

        Map.Entry<String, AutomaticZenRule>[] toReturn = new Map.Entry[ruleMap.size()];
        ruleMap.entrySet().toArray(toReturn);
        return toReturn;
    }

    private Map.Entry<String, AutomaticZenRule>[] populateAutoZenRulesDescendingCreationTime(
            int numRules, boolean addDefaultRules) {
        Map<String, AutomaticZenRule> ruleMap = new HashMap<>();

        for (int i = 0; i < numRules; i++) {
            ruleMap.put(GENERIC_RULE_NAME + i, new AutomaticZenRule(GENERIC_RULE_NAME + i, null,
                    null, null, null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true,
                    numRules - i));
        }

        if (addDefaultRules) {
            ruleMap.put(DEFAULT_ID_1, new AutomaticZenRule("DEFAULT_1_NAME", null, null, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 10));
            ruleMap.put(DEFAULT_ID_2, new AutomaticZenRule("DEFAULT_2_NAME", null, null, null,
                    null, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, true, 20));
        }

        Map.Entry<String, AutomaticZenRule>[] toReturn = new Map.Entry[ruleMap.size()];
        ruleMap.entrySet().toArray(toReturn);
        return toReturn;
    }

    @Test
    public void updateState_checkRuleOrderingDescending() {
        final int NUM_RULES = 4;
        Map.Entry<String, AutomaticZenRule>[] rules = populateAutoZenRulesDescendingCreationTime(
                NUM_RULES, false);
        Arrays.sort(rules, ZenModeBackend.RULE_COMPARATOR);

        // check ordering, most recent should be at the end
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + (NUM_RULES - 1 - i), rules[i].getKey());
        }
    }

    @Test
    public void updateState_checkRuleOrderingAscending() {
        final int NUM_RULES = 4;
        Map.Entry<String, AutomaticZenRule>[] rules = populateAutoZenRulesAscendingCreationTime(
                NUM_RULES, false);
        Arrays.sort(rules, ZenModeBackend.RULE_COMPARATOR);

        // check ordering, most recent should be at the end
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + i, rules[i].getKey());
        }
    }

    @Test
    public void updateState_checkRuleOrderingDescending_withDefaultRules() {
        final int NUM_RULES = 4;

        Map.Entry<String, AutomaticZenRule>[] rules = populateAutoZenRulesDescendingCreationTime(NUM_RULES,
                true);
        Arrays.sort(rules, ZenModeBackend.RULE_COMPARATOR);

        assertEquals(rules[0].getKey(), DEFAULT_ID_1);
        assertEquals(rules[1].getKey(), DEFAULT_ID_2);
        // NON-DEFAULT RULES check ordering, most recent at the bottom/end
        for (int i = 0; i < NUM_RULES; i++) {
            assertEquals(GENERIC_RULE_NAME + (NUM_RULES - 1 - i), rules[i + 2].getKey());
        }
    }

    @Test
    public void updateSummary_nullCursorValues() {
        Cursor testCursorWithNullValues = createMockCursor(3);
        when(testCursorWithNullValues.getString(0)).thenReturn(null);

        // expected - no null values
        List<String> contacts = mBackend.getStarredContacts(testCursorWithNullValues);
        for (String contact : contacts) {
            assertThat(contact).isNotNull();
        }
    }

    @Test
    public void saveConversationSenders_importantToNone() {
        when(mNotificationManager.getNotificationPolicy()).thenReturn(
                new Policy(PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_CONVERSATIONS
                        | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                        PRIORITY_SENDERS_CONTACTS,
                        PRIORITY_SENDERS_STARRED,
                        SUPPRESSED_EFFECT_AMBIENT,
                        CONVERSATION_SENDERS_IMPORTANT));
        mBackend = new ZenModeBackend(mContext);

        mBackend.saveConversationSenders(CONVERSATION_SENDERS_NONE);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        if (android.app.Flags.modesApi()) {
            verify(mNotificationManager).setNotificationPolicy(captor.capture(), eq(true));
        } else {
            verify(mNotificationManager).setNotificationPolicy(captor.capture());
        }

        Policy expected = new Policy(
                PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                PRIORITY_SENDERS_CONTACTS,
                PRIORITY_SENDERS_STARRED,
                SUPPRESSED_EFFECT_AMBIENT,
                CONVERSATION_SENDERS_NONE);
        assertEquals(expected, captor.getValue());
    }

    @Test
    public void saveConversationSenders_noneToAll() {
        when(mNotificationManager.getNotificationPolicy()).thenReturn(new Policy(
                PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                PRIORITY_SENDERS_CONTACTS,
                PRIORITY_SENDERS_STARRED,
                SUPPRESSED_EFFECT_AMBIENT,
                CONVERSATION_SENDERS_NONE));
        mBackend = new ZenModeBackend(mContext);

        mBackend.saveConversationSenders(CONVERSATION_SENDERS_ANYONE);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        if (android.app.Flags.modesApi()) {
            verify(mNotificationManager).setNotificationPolicy(captor.capture(), eq(true));
        } else {
            verify(mNotificationManager).setNotificationPolicy(captor.capture());
        }

        Policy expected = new Policy(PRIORITY_CATEGORY_CONVERSATIONS
                | PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                PRIORITY_SENDERS_CONTACTS,
                PRIORITY_SENDERS_STARRED,
                SUPPRESSED_EFFECT_AMBIENT,
                CONVERSATION_SENDERS_ANYONE);
        assertEquals(expected, captor.getValue());
    }

    @Test
    public void saveSenders_doesNotChangeConversations() {
        when(mNotificationManager.getNotificationPolicy()).thenReturn(
                new Policy(PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_CONVERSATIONS
                        | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                        PRIORITY_SENDERS_CONTACTS,
                        PRIORITY_SENDERS_STARRED,
                        SUPPRESSED_EFFECT_AMBIENT,
                        CONVERSATION_SENDERS_ANYONE));
        mBackend = new ZenModeBackend(mContext);

        mBackend.saveSenders(PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_ANY);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        if (android.app.Flags.modesApi()) {
            verify(mNotificationManager).setNotificationPolicy(captor.capture(), eq(true));
        } else {
            verify(mNotificationManager).setNotificationPolicy(captor.capture());
        }

        Policy expected = new Policy(PRIORITY_CATEGORY_CONVERSATIONS
                | PRIORITY_CATEGORY_CALLS | PRIORITY_CATEGORY_MESSAGES | PRIORITY_CATEGORY_ALARMS,
                PRIORITY_SENDERS_ANY,
                PRIORITY_SENDERS_STARRED,
                SUPPRESSED_EFFECT_AMBIENT,
                CONVERSATION_SENDERS_ANYONE);
        assertEquals(expected, captor.getValue());
    }

}

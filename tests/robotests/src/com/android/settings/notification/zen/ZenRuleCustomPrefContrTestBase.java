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

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.service.notification.ZenPolicy;

import com.android.settings.notification.zen.AbstractZenCustomRulePreferenceController;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
abstract class ZenRuleCustomPrefContrTestBase {
    public static final String RULE_ID = "test_rule_id";
    public static final String PREF_KEY = "main_pref";

    AutomaticZenRule mRule = new AutomaticZenRule("test", null, null, null, null,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);

    abstract AbstractZenCustomRulePreferenceController getController();

    void updateControllerZenPolicy(ZenPolicy policy) {
        mRule.setZenPolicy(policy);
        getController().onResume(mRule, RULE_ID);
    }
}

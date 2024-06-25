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

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;

import androidx.annotation.Nullable;

import com.android.settingslib.notification.modes.ZenMode;

import java.util.Random;

class TestModeBuilder {

    private String mId;
    private AutomaticZenRule mRule;
    private ZenModeConfig.ZenRule mConfigZenRule;

    public static final ZenMode EXAMPLE = new TestModeBuilder().build();

    TestModeBuilder() {
        // Reasonable defaults
        int id = new Random().nextInt(1000);
        mId = "rule_" + id;
        mRule = new AutomaticZenRule.Builder("Test Rule #" + id, Uri.parse("rule://" + id))
                .setPackage("some_package")
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().disallowAllSounds().build())
                .build();
        mConfigZenRule = new ZenModeConfig.ZenRule();
        mConfigZenRule.enabled = true;
        mConfigZenRule.pkg = "some_package";
    }

    TestModeBuilder setId(String id) {
        mId = id;
        return this;
    }

    TestModeBuilder setAzr(AutomaticZenRule rule) {
        mRule = rule;
        mConfigZenRule.pkg = rule.getPackageName();
        mConfigZenRule.conditionId = rule.getConditionId();
        mConfigZenRule.enabled = rule.isEnabled();
        return this;
    }

    TestModeBuilder setConfigZenRule(ZenModeConfig.ZenRule configZenRule) {
        mConfigZenRule = configZenRule;
        return this;
    }

    public TestModeBuilder setName(String name) {
        mRule.setName(name);
        mConfigZenRule.name = name;
        return this;
    }

    public TestModeBuilder setPackage(String pkg) {
        mRule.setPackageName(pkg);
        mConfigZenRule.pkg = pkg;
        return this;
    }

    TestModeBuilder setConditionId(Uri conditionId) {
        mRule.setConditionId(conditionId);
        mConfigZenRule.conditionId = conditionId;
        return this;
    }

    TestModeBuilder setType(@AutomaticZenRule.Type int type) {
        mRule.setType(type);
        mConfigZenRule.type = type;
        return this;
    }

    TestModeBuilder setInterruptionFilter(
            @NotificationManager.InterruptionFilter int interruptionFilter) {
        mRule.setInterruptionFilter(interruptionFilter);
        mConfigZenRule.zenMode = NotificationManager.zenModeFromInterruptionFilter(
                interruptionFilter, NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        return this;
    }

    TestModeBuilder setZenPolicy(@Nullable ZenPolicy policy) {
        mRule.setZenPolicy(policy);
        mConfigZenRule.zenPolicy = policy;
        return this;
    }

    TestModeBuilder setDeviceEffects(@Nullable ZenDeviceEffects deviceEffects) {
        mRule.setDeviceEffects(deviceEffects);
        mConfigZenRule.zenDeviceEffects = deviceEffects;
        return this;
    }

    public TestModeBuilder setEnabled(boolean enabled) {
        mRule.setEnabled(enabled);
        mConfigZenRule.enabled = enabled;
        return this;
    }

    TestModeBuilder setManualInvocationAllowed(boolean allowed) {
        mRule.setManualInvocationAllowed(allowed);
        mConfigZenRule.allowManualInvocation = allowed;
        return this;
    }

    public TestModeBuilder setTriggerDescription(@Nullable String triggerDescription) {
        mRule.setTriggerDescription(triggerDescription);
        mConfigZenRule.triggerDescription = triggerDescription;
        return this;
    }

    TestModeBuilder setActive(boolean active) {
        if (active) {
            mConfigZenRule.enabled = true;
            mConfigZenRule.condition = new Condition(mRule.getConditionId(), "...",
                    Condition.STATE_TRUE);
        } else {
            mConfigZenRule.condition = null;
        }
        return this;
    }

    ZenMode build() {
        return new ZenMode(mId, mRule, mConfigZenRule);
    }
}

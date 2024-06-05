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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenPolicy;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;

/**
 * Represents either an {@link AutomaticZenRule} or the manual DND rule in a unified way.
 *
 * <p>It also adapts other rule features that we don't want to expose in the UI, such as
 * interruption filters other than {@code PRIORITY}, rules without specific icons, etc.
 */
class ZenMode {

    private static final String TAG = "ZenMode";

    /**
     * Additional value for the {@code @ZenPolicy.ChannelType} enumeration that indicates that all
     * channels can bypass DND when this policy is active.
     *
     * <p>This value shouldn't be used on "real" ZenPolicy objects sent to or returned from
     * {@link android.app.NotificationManager}; it's a way of representing rules with interruption
     * filter = {@link NotificationManager#INTERRUPTION_FILTER_ALL} in the UI.
     */
    public static final int CHANNEL_POLICY_ALL = -1;

    static final String MANUAL_DND_MODE_ID = "manual_dnd";

    @SuppressLint("WrongConstant")
    private static final ZenPolicy POLICY_INTERRUPTION_FILTER_ALL =
            new ZenPolicy.Builder()
                    .allowChannels(CHANNEL_POLICY_ALL)
                    .allowAllSounds()
                    .showAllVisualEffects()
                    .build();

    // Must match com.android.server.notification.ZenModeHelper#applyCustomPolicy.
    private static final ZenPolicy POLICY_INTERRUPTION_FILTER_ALARMS =
            new ZenPolicy.Builder()
                    .disallowAllSounds()
                    .allowAlarms(true)
                    .allowMedia(true)
                    .allowPriorityChannels(false)
                    .build();

    // Must match com.android.server.notification.ZenModeHelper#applyCustomPolicy.
    private static final ZenPolicy POLICY_INTERRUPTION_FILTER_NONE =
            new ZenPolicy.Builder()
                    .disallowAllSounds()
                    .hideAllVisualEffects()
                    .allowPriorityChannels(false)
                    .build();

    private final String mId;
    private AutomaticZenRule mRule;
    private final boolean mIsActive;
    private final boolean mIsManualDnd;

    ZenMode(String id, AutomaticZenRule rule, boolean isActive) {
        this(id, rule, isActive, false);
    }

    private ZenMode(String id, AutomaticZenRule rule, boolean isActive, boolean isManualDnd) {
        mId = id;
        mRule = rule;
        mIsActive = isActive;
        mIsManualDnd = isManualDnd;
    }

    static ZenMode manualDndMode(AutomaticZenRule dndPolicyAsRule, boolean isActive) {
        return new ZenMode(MANUAL_DND_MODE_ID, dndPolicyAsRule, isActive, true);
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public AutomaticZenRule getRule() {
        return mRule;
    }

    @NonNull
    public ListenableFuture<Drawable> getIcon(@NonNull Context context,
            @NonNull IconLoader iconLoader) {
        if (mIsManualDnd) {
            return Futures.immediateFuture(requireNonNull(
                    context.getDrawable(R.drawable.ic_do_not_disturb_on_24dp)));
        }

        return iconLoader.getIcon(context, mRule);
    }

    @NonNull
    public ZenPolicy getPolicy() {
        switch (mRule.getInterruptionFilter()) {
            case INTERRUPTION_FILTER_PRIORITY:
                return requireNonNull(mRule.getZenPolicy());

            case NotificationManager.INTERRUPTION_FILTER_ALL:
                return POLICY_INTERRUPTION_FILTER_ALL;

            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return POLICY_INTERRUPTION_FILTER_ALARMS;

            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return POLICY_INTERRUPTION_FILTER_NONE;

            case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
            default:
                Log.wtf(TAG, "Rule " + mId + " with unexpected interruptionFilter "
                        + mRule.getInterruptionFilter());
                return requireNonNull(mRule.getZenPolicy());
        }
    }

    /**
     * Updates the {@link ZenPolicy} of the associated {@link AutomaticZenRule} based on the
     * supplied policy. In some cases this involves conversions, so that the following call
     * to {@link #getPolicy} might return a different policy from the one supplied here.
     */
    @SuppressLint("WrongConstant")
    public void setPolicy(@NonNull ZenPolicy policy) {
        ZenPolicy currentPolicy = getPolicy();
        if (currentPolicy.equals(policy)) {
            return;
        }

        // A policy with CHANNEL_POLICY_ALL is only a UI representation of the
        // INTERRUPTION_FILTER_ALL filter. Thus, switching to or away to this value only updates
        // the filter, discarding the rest of the supplied policy.
        if (policy.getAllowedChannels() == CHANNEL_POLICY_ALL
                && currentPolicy.getAllowedChannels() != CHANNEL_POLICY_ALL) {
            if (mIsManualDnd) {
                throw new IllegalArgumentException("Manual DND cannot have CHANNEL_POLICY_ALL");
            }
            mRule.setInterruptionFilter(INTERRUPTION_FILTER_ALL);
            // Preserve the existing policy, e.g. if the user goes PRIORITY -> ALL -> PRIORITY that
            // shouldn't discard all other policy customizations. The existing policy will be a
            // synthetic one if the rule originally had filter NONE or ALARMS_ONLY and that's fine.
            if (mRule.getZenPolicy() == null) {
                mRule.setZenPolicy(currentPolicy);
            }
            return;
        } else if (policy.getAllowedChannels() != CHANNEL_POLICY_ALL
                && currentPolicy.getAllowedChannels() == CHANNEL_POLICY_ALL) {
            mRule.setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY);
            // Go back to whatever policy the rule had before, unless the rule never had one, in
            // which case we use the supplied policy (which we know has a valid allowedChannels).
            if (mRule.getZenPolicy() == null) {
                mRule.setZenPolicy(policy);
            }
            return;
        }

        // If policy is customized from any of the "special" ones, make the rule PRIORITY.
        if (mRule.getInterruptionFilter() != INTERRUPTION_FILTER_PRIORITY) {
            mRule.setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY);
        }
        mRule.setZenPolicy(policy);
    }

    @NonNull
    public ZenDeviceEffects getDeviceEffects() {
        return mRule.getDeviceEffects() != null
                ? mRule.getDeviceEffects()
                : new ZenDeviceEffects.Builder().build();
    }

    public boolean canBeDeleted() {
        return !mIsManualDnd;
    }

    public boolean isManualDnd() {
        return mIsManualDnd;
    }

    public boolean isActive() {
        return mIsActive;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ZenMode other
                && mId.equals(other.mId)
                && mRule.equals(other.mRule)
                && mIsActive == other.mIsActive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mRule, mIsActive);
    }

    @Override
    public String toString() {
        return mId + "(" + (mIsActive ? "active" : "inactive") + ") -> " + mRule;
    }
}

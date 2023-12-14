/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.internal.util.Preconditions;
import com.android.settings.core.BasePreferenceController;

import java.util.Collections;
import java.util.Set;

/**
 * A base preference controller for {@link ShortcutOptionPreference}
 */
public abstract class ShortcutOptionPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {
    private Set<String> mShortcutTargets = Collections.emptySet();
    private boolean mIsInSetupWizard;

    public ShortcutOptionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (getPreferenceKey().equals(preference.getKey())
                && preference instanceof ShortcutOptionPreference) {
            ((ShortcutOptionPreference) preference).setChecked(isChecked());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (isShortcutAvailable()) {
            return AVAILABLE_UNSEARCHABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Set the targets (i.e. a11y features) to be configured with the a11y shortcut option.
     * <p>
     * Note: the shortcutTargets cannot be empty, since the edit a11y shortcut option
     * is meant to configure the shortcut options for an a11y feature.
     * </>
     *
     * @param shortcutTargets the a11y features, like color correction, Talkback, etc.
     * @throws NullPointerException     if the {@code shortcutTargets} was {@code null}
     * @throws IllegalArgumentException if the {@code shortcutTargets} was empty
     */
    public void setShortcutTargets(Set<String> shortcutTargets) {
        Preconditions.checkCollectionNotEmpty(shortcutTargets, /* valueName= */ "a11y targets");

        this.mShortcutTargets = shortcutTargets;
    }

    public void setInSetupWizard(boolean isInSetupWizard) {
        this.mIsInSetupWizard = isInSetupWizard;
    }

    protected Set<String> getShortcutTargets() {
        return mShortcutTargets;
    }

    protected boolean isInSetupWizard() {
        return mIsInSetupWizard;
    }

    @Override
    public final boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        enableShortcutForTargets((Boolean) newValue);
        return false;
    }

    @ShortcutConstants.UserShortcutType
    protected int getShortcutType() {
        return ShortcutConstants.UserShortcutType.DEFAULT;
    }

    /**
     * Returns true if the shortcut is associated to the targets
     */
    protected boolean isChecked() {
        Set<String> targets = ShortcutUtils.getShortcutTargetsFromSettings(
                mContext, getShortcutType(), UserHandle.myUserId());

        return !targets.isEmpty() && targets.containsAll(getShortcutTargets());
    }

    /**
     * Enable or disable the shortcut for the given accessibility features.
     */
    protected void enableShortcutForTargets(boolean enable) {
        Set<String> shortcutTargets = getShortcutTargets();
        @ShortcutConstants.UserShortcutType int shortcutType = getShortcutType();

        if (enable) {
            for (String target : shortcutTargets) {
                ShortcutUtils.optInValueToSettings(mContext, shortcutType, target);
            }
        } else {
            for (String target : shortcutTargets) {
                ShortcutUtils.optOutValueFromSettings(mContext, shortcutType, target);
            }
        }
        ShortcutUtils.updateInvisibleToggleAccessibilityServiceEnableState(
                mContext, shortcutTargets, UserHandle.myUserId());
    }

    /**
     * Returns true when the user can associate a shortcut to the targets
     */
    protected abstract boolean isShortcutAvailable();
}

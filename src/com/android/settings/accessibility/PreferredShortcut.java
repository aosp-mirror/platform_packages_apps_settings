/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ComponentName;
import android.text.TextUtils;

import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import com.google.common.base.Objects;

/**
 * A data class for containing {@link ComponentName#flattenToString()} and
 * {@link UserShortcutType}. Represents the preferred shortcuts of the service or activity.
 */
public class PreferredShortcut {

    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    /**
     * Creates a {@link PreferredShortcut} from a encoded string described in {@link #toString()}.
     *
     * @param preferredShortcutString A string conform to the format described in {@link
     *                                #toString()}
     * @return A {@link PreferredShortcut} with the specified value
     * @throws IllegalArgumentException If preferredShortcutString does not conform to the format
     *                                  described in {@link #toString()}
     */
    public static PreferredShortcut fromString(String preferredShortcutString) {
        sStringColonSplitter.setString(preferredShortcutString);
        if (sStringColonSplitter.hasNext()) {
            final String componentName = sStringColonSplitter.next();
            final int type = Integer.parseInt(sStringColonSplitter.next());
            return new PreferredShortcut(componentName, type);
        }

        throw new IllegalArgumentException(
                "Invalid PreferredShortcut string: " + preferredShortcutString);
    }

    /** The format of {@link ComponentName#flattenToString()} */
    private String mComponentName;
    /** The format of {@link UserShortcutType} */
    private int mType;

    public PreferredShortcut(String componentName, int type) {
        mComponentName = componentName;
        mType = type;
    }

    public String getComponentName() {
        return mComponentName;
    }

    public int getType() {
        return mType;
    }

    @Override
    public String toString() {
        return mComponentName + COMPONENT_NAME_SEPARATOR + mType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PreferredShortcut that = (PreferredShortcut) o;
        return mType == that.mType && Objects.equal(mComponentName, that.mComponentName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mComponentName, mType);
    }
}

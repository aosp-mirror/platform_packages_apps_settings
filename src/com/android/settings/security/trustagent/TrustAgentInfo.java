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

package com.android.settings.security.trustagent;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

public class TrustAgentInfo implements Comparable<TrustAgentInfo> {
    private final CharSequence mLabel;
    private final ComponentName mComponentName;
    private final Drawable mIcon;

    public TrustAgentInfo(CharSequence label, ComponentName componentName, Drawable icon) {
        mLabel = label;
        mComponentName = componentName;
        mIcon = icon;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TrustAgentInfo) {
            return mComponentName.equals(((TrustAgentInfo) other).getComponentName());
        }
        return false;
    }

    @Override
    public int compareTo(TrustAgentInfo other) {
        return mComponentName.compareTo(other.getComponentName());
    }
}
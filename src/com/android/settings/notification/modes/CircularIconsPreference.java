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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import com.google.common.base.Equivalence;

public class CircularIconsPreference extends RestrictedPreference {

    private CircularIconSet<?> mIconSet = CircularIconSet.EMPTY;

    public CircularIconsPreference(Context context) {
        super(context);
        init();
    }

    public CircularIconsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularIconsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CircularIconsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_circular_icons);
    }

    <T> void setIcons(CircularIconSet<T> iconSet) {
        setIcons(iconSet, null);
    }

    <T> void setIcons(CircularIconSet<T> iconSet, @Nullable Equivalence<T> itemEquivalence) {
        if (mIconSet.hasSameItemsAs(iconSet, itemEquivalence)) {
            return;
        }

        mIconSet = iconSet;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        CircularIconsView iconContainer = checkNotNull(
                (CircularIconsView) holder.findViewById(R.id.circles_container));

        iconContainer.setVisibility(mIconSet != null && mIconSet.size() == 0 ? GONE : VISIBLE);
        iconContainer.setEnabled(isEnabled());
        iconContainer.setIcons(mIconSet);
    }
}

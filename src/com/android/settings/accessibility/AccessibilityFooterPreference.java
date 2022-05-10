/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.FooterPreference;

/** A custom preference acting as footer of a page. Disables the movement method by default. */
public final class AccessibilityFooterPreference extends FooterPreference {

    private boolean mLinkEnabled;

    public AccessibilityFooterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibilityFooterPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView title = holder.itemView.findViewById(android.R.id.title);
        if (mLinkEnabled) {
            // When a TextView has a movement method, it will set the view to focusable and
            // clickable. This makes View.onTouchEvent always return true and consumes the touch
            // event, essentially nullifying any return values of MovementMethod.onTouchEvent.
            // To still allow propagating touch events to the parent when this view doesn't have
            // links, we only set the movement method here if the text contains links.
            title.setMovementMethod(LinkMovementMethod.getInstance());

            // Groups of related title and link content by making the container focusable,
            // then make all the children inside not focusable.
            title.setFocusable(false);
        } else {
            title.setMovementMethod(/* movement= */ null);
        }
    }

    /**
     * Sets the title field supports movement method.
     */
    public void setLinkEnabled(boolean enabled) {
        if (mLinkEnabled != enabled) {
            mLinkEnabled = enabled;
            notifyChanged();
        }
    }

    /**
     * Returns true if the title field supports movement method.
     */
    public boolean isLinkEnabled() {
        return mLinkEnabled;
    }
}

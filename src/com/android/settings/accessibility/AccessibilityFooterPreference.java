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
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/**
 * A custom preference acting as footer of a page. It has a field for icon and text. It is added
 * to screen as the last preference and groups of icon and text content in accessibility-focusable
 * {@link android.view.accessibility.AccessibilityNodeInfo} for TalkBack to use.
 */
public final class AccessibilityFooterPreference extends FooterPreference {

    private CharSequence mIconContentDescription;
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
            // When a TextView has a movement method, it will set the view to clickable. This makes
            // View.onTouchEvent always return true and consumes the touch event, essentially
            // nullifying any return values of MovementMethod.onTouchEvent.
            // To still allow propagating touch events to the parent when this view doesn't have
            // links, we only set the movement method here if the text contains links.
            title.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            title.setMovementMethod(/* movement= */ null);
        }

        final LinearLayout infoFrame = holder.itemView.findViewById(R.id.icon_frame);
        if (!TextUtils.isEmpty(mIconContentDescription)) {
            // Groups related content.
            infoFrame.setContentDescription(mIconContentDescription);
            title.setFocusable(false);
        } else {
            infoFrame.setContentDescription(null);
            title.setFocusable(true);
        }
    }

    /**
     * Sets the content description of the icon.
     */
    public void setIconContentDescription(CharSequence iconContentDescription) {
        if (!TextUtils.equals(iconContentDescription, mIconContentDescription)) {
            mIconContentDescription = iconContentDescription;
            notifyChanged();
        }
    }

    /**
     * Gets the content description of the icon.
     */
    public CharSequence getIconContentDescription() {
        return mIconContentDescription;
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

    /**
     * Appends {@link AnnotationSpan} with learn more link apart from the other text.
     *
     * @param helpLinkRes The Help Uri Resource key
     */
    public void appendHelpLink(int helpLinkRes) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(getTitle()).append("\n\n").append(getLearnMoreLink(getContext(), helpLinkRes));
        setTitle(sb);
    }

    private CharSequence getLearnMoreLink(Context context, int helpLinkRes) {
        final Intent helpIntent = HelpUtils.getHelpIntent(
                context, context.getString(helpLinkRes), context.getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                context, AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, helpIntent);
        return AnnotationSpan.linkify(context.getText(R.string.footer_learn_more), linkInfo);
    }
}

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

package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A RelativeLayout which implements {@link Checkable}. With this implementation, it could be used
 * in the list item layout for {@link android.widget.AbsListView} to change UI after item click.
 * Its checked state would be propagated to the checkable child.
 *
 * <p>
 * To support accessibility, the state description is from the checkable view and is
 * changed with {@link #setChecked(boolean)}. We make the checkable child unclickable, unfocusable
 * and non-important for accessibility, so that the announcement wouldn't include
 * the checkable view.
 * <
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    private Checkable mCheckable;
    private View mCheckableChild;
    private boolean mChecked;

    public CheckableRelativeLayout(Context context) {
        super(context);
    }

    public CheckableRelativeLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        mCheckableChild = findFirstCheckableView(this);
        if (mCheckableChild != null) {
            mCheckableChild.setClickable(false);
            mCheckableChild.setFocusable(false);
            mCheckableChild.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            mCheckable = (Checkable) mCheckableChild;
            mCheckable.setChecked(isChecked());
            setStateDescriptionIfNeeded();
        }
        super.onFinishInflate();
    }

    @Nullable
    private static View findFirstCheckableView(@NonNull ViewGroup viewGroup) {
        final int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = viewGroup.getChildAt(i);
            if (child instanceof Checkable) {
                return child;
            }
            if (child instanceof ViewGroup) {
                findFirstCheckableView((ViewGroup) child);
            }
        }
        return  null;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            if (mCheckable != null) {
                mCheckable.setChecked(checked);
            }
        }
        setStateDescriptionIfNeeded();
        notifyViewAccessibilityStateChangedIfNeeded(
                AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
    }

    private void setStateDescriptionIfNeeded() {
        if (mCheckableChild == null) {
            return;
        }
        setStateDescription(mCheckableChild.getStateDescription());
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setChecked(mChecked);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setChecked(mChecked);
    }
}

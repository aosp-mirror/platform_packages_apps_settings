/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.List;

/**
 * Copied from setup wizard.
 */
public class LinkAccessibilityHelper extends ExploreByTouchHelper {

    private static final String TAG = "LinkAccessibilityHelper";

    private final TextView mView;
    private final Rect mTempRect = new Rect();

    public LinkAccessibilityHelper(TextView view) {
        super(view);
        mView = view;
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        final CharSequence text = mView.getText();
        if (text instanceof Spanned) {
            final Spanned spannedText = (Spanned) text;
            final int offset = mView.getOffsetForPosition(x, y);
            ClickableSpan[] linkSpans = spannedText.getSpans(offset, offset, ClickableSpan.class);
            if (linkSpans.length == 1) {
                ClickableSpan linkSpan = linkSpans[0];
                return spannedText.getSpanStart(linkSpan);
            }
        }
        return INVALID_ID;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        final CharSequence text = mView.getText();
        if (text instanceof Spanned) {
            final Spanned spannedText = (Spanned) text;
            ClickableSpan[] linkSpans = spannedText.getSpans(0, spannedText.length(),
                    ClickableSpan.class);
            for (ClickableSpan span : linkSpans) {
                virtualViewIds.add(spannedText.getSpanStart(span));
            }
        }
    }

    @Override
    protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
        final ClickableSpan span = getSpanForOffset(virtualViewId);
        if (span != null) {
            event.setContentDescription(getTextForSpan(span));
        } else {
            Log.e(TAG, "ClickableSpan is null for offset: " + virtualViewId);
            event.setContentDescription(mView.getText());
        }
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfo info) {
        final ClickableSpan span = getSpanForOffset(virtualViewId);
        if (span != null) {
            info.setContentDescription(getTextForSpan(span));
        } else {
            Log.e(TAG, "ClickableSpan is null for offset: " + virtualViewId);
            info.setContentDescription(mView.getText());
        }
        info.setFocusable(true);
        info.setClickable(true);
        getBoundsForSpan(span, mTempRect);
        if (!mTempRect.isEmpty()) {
            info.setBoundsInParent(getBoundsForSpan(span, mTempRect));
        } else {
            Log.e(TAG, "LinkSpan bounds is empty for: " + virtualViewId);
            mTempRect.set(0, 0, 1, 1);
            info.setBoundsInParent(mTempRect);
        }
        info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
            Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_CLICK) {
            ClickableSpan span = getSpanForOffset(virtualViewId);
            if (span != null) {
                span.onClick(mView);
                return true;
            } else {
                Log.e(TAG, "LinkSpan is null for offset: " + virtualViewId);
            }
        }
        return false;
    }

    private ClickableSpan getSpanForOffset(int offset) {
        CharSequence text = mView.getText();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            ClickableSpan[] spans = spannedText.getSpans(offset, offset, ClickableSpan.class);
            if (spans.length == 1) {
                return spans[0];
            }
        }
        return null;
    }

    private CharSequence getTextForSpan(ClickableSpan span) {
        CharSequence text = mView.getText();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            return spannedText.subSequence(spannedText.getSpanStart(span),
                    spannedText.getSpanEnd(span));
        }
        return text;
    }

    // Find the bounds of a span. If it spans multiple lines, it will only return the bounds for the
    // section on the first line.
    private Rect getBoundsForSpan(ClickableSpan span, Rect outRect) {
        CharSequence text = mView.getText();
        outRect.setEmpty();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            final int spanStart = spannedText.getSpanStart(span);
            final int spanEnd = spannedText.getSpanEnd(span);
            final Layout layout = mView.getLayout();
            final float xStart = layout.getPrimaryHorizontal(spanStart);
            final float xEnd = layout.getPrimaryHorizontal(spanEnd);
            final int lineStart = layout.getLineForOffset(spanStart);
            final int lineEnd = layout.getLineForOffset(spanEnd);
            layout.getLineBounds(lineStart, outRect);
            outRect.left = (int) xStart;
            if (lineEnd == lineStart) {
                outRect.right = (int) xEnd;
            } // otherwise just leave it at the end of the start line

            // Offset for padding
            outRect.offset(mView.getTotalPaddingLeft(), mView.getTotalPaddingTop());
        }
        return outRect;
    }
}

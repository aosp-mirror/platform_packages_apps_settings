/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.List;

/**
 * LabeledSeekBar represent a seek bar assigned with labeled, discrete values.
 * It pretends to be a group of radio button for AccessibilityServices, in order to adjust the
 * behavior of these services to keep the mental model of the visual discrete SeekBar.
 */
public class LabeledSeekBar extends SeekBar {

    private class LabeledSeekBarExploreByTouchHelper extends ExploreByTouchHelper {

        public LabeledSeekBarExploreByTouchHelper(View forView) {
            super(forView);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            return getVirtualViewIdIndexFromX(x);
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> list) {
            for (int i = 0, c = LabeledSeekBar.this.getMax(); i <= c; ++i) {
                list.add(i);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
                Bundle arguments) {
            if (virtualViewId == ExploreByTouchHelper.HOST_ID) {
                // Do nothing
                return false;
            }

            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    LabeledSeekBar.this.setProgress(virtualViewId);
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, AccessibilityNodeInfoCompat node) {
            node.setClassName(RadioButton.class.getName());
            node.setBoundsInParent(getBoundsInParentFromVirtualViewId(virtualViewId));
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            node.setContentDescription(mLabels[virtualViewId]);
            node.setClickable(true);
            node.setCheckable(true);
            node.setChecked(virtualViewId == LabeledSeekBar.this.getProgress());
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setClassName(RadioButton.class.getName());
            event.setContentDescription(mLabels[virtualViewId]);
            event.setChecked(virtualViewId == LabeledSeekBar.this.getProgress());
        }

        @Override
        protected void onPopulateNodeForHost(AccessibilityNodeInfoCompat node) {
            node.setClassName(RadioGroup.class.getName());
        }

        @Override
        protected void onPopulateEventForHost(AccessibilityEvent event) {
            event.setClassName(RadioGroup.class.getName());
        }

        private int getHalfVirtualViewWidth() {
            final int width = LabeledSeekBar.this.getWidth();
            final int barWidth = width - LabeledSeekBar.this.getPaddingStart()
                    - LabeledSeekBar.this.getPaddingEnd();
            return Math.max(0, barWidth / (LabeledSeekBar.this.getMax() * 2));
        }

        private int getVirtualViewIdIndexFromX(float x) {
            final int posBase = Math.max(0,
                    ((int) x - LabeledSeekBar.this.getPaddingStart()) / getHalfVirtualViewWidth());
            return (posBase + 1) / 2;
        }

        private Rect getBoundsInParentFromVirtualViewId(int virtualViewId) {
            int left = (virtualViewId * 2 - 1) * getHalfVirtualViewWidth()
                    + LabeledSeekBar.this.getPaddingStart();
            int right = (virtualViewId * 2 + 1) * getHalfVirtualViewWidth()
                    + LabeledSeekBar.this.getPaddingStart();

            // Edge case
            left = virtualViewId == 0 ? 0 : left;
            right = virtualViewId == LabeledSeekBar.this.getMax()
                    ? LabeledSeekBar.this.getWidth() : right;

            final Rect r = new Rect();
            r.set(left, 0, right, LabeledSeekBar.this.getHeight());
            return r;
        }
    }

    private String[] mLabels;

    private ExploreByTouchHelper mAccessHelper;

    public LabeledSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.seekBarStyle);
    }

    public LabeledSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LabeledSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendClickEventForAccessibility(progress);
            }
        });
    }

    @Override
    public synchronized void setProgress(int progress) {
        if (mAccessHelper != null) {
            mAccessHelper.invalidateRoot();
        }

        super.setProgress(progress);
    }

    public void setLabels(String[] labels) {
        mLabels = labels;
    }

    @Override
    public void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        // Tweak the listener to send accessibility event on progress changed.
        OnSeekBarChangeListener l2 = new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                l.onStopTrackingTouch(seekBar);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                l.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                l.onProgressChanged(seekBar, progress, fromUser);
                sendClickEventForAccessibility(progress);
            }
        };

        super.setOnSeekBarChangeListener(l2);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAccessHelper = new LabeledSeekBarExploreByTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mAccessHelper);
    }

    @Override
    protected void onDetachedFromWindow() {
        ViewCompat.setAccessibilityDelegate(this, null);
        mAccessHelper = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (mAccessHelper != null && mAccessHelper.dispatchHoverEvent(event)) {
            return true;
        }

        return super.dispatchHoverEvent(event);
    }

    private void sendClickEventForAccessibility(int progress) {
        if (mAccessHelper != null) {
            mAccessHelper.invalidateRoot();
            mAccessHelper.sendEventForVirtualView(progress, AccessibilityEvent.TYPE_VIEW_CLICKED);
        }
    }
}

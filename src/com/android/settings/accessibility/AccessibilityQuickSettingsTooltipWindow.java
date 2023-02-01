/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

/**
 * UI container for the accessibility quick settings tooltip.
 *
 * <p> The popup window shows the information about the operation of the quick settings. In
 * addition, the arrow is pointing to the top center of the device to display one-off menu within
 * {@code mCloseDelayTimeMillis} time.</p>
 */
public class AccessibilityQuickSettingsTooltipWindow extends PopupWindow {

    private final Context mContext;
    private Handler mHandler;
    private long mCloseDelayTimeMillis;

    public AccessibilityQuickSettingsTooltipWindow(Context context) {
        super(context);
        this.mContext = context;
    }

    private final AccessibilityDelegate mAccessibilityDelegate = new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                final AccessibilityAction clickAction = new AccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        mContext.getString(R.string.accessibility_quick_settings_tooltip_dismiss));
                info.addAction(clickAction);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    dismiss();
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        };
    /**
     * Sets up {@link #AccessibilityQuickSettingsTooltipWindow}'s layout and content.
     *
     * @param text text to be displayed
     * @param imageResId the resource ID of the image drawable
     */
    public void setup(CharSequence text, @DrawableRes int imageResId) {
        this.setup(text, imageResId, /* closeDelayTimeMillis= */ 0);
    }

    /**
     * Sets up {@link #AccessibilityQuickSettingsTooltipWindow}'s layout and content.
     *
     * <p> The system will attempt to close popup window to the target duration of the threads if
     * close delay time is positive number. </p>
     *
     * @param text text to be displayed
     * @param imageResId the resource ID of the image drawable
     * @param closeDelayTimeMillis how long the popup window be auto-closed
     */
    public void setup(CharSequence text, @DrawableRes int imageResId, long closeDelayTimeMillis) {
        this.mCloseDelayTimeMillis = closeDelayTimeMillis;

        setBackgroundDrawable(new ColorDrawable(mContext.getColor(android.R.color.transparent)));
        final LayoutInflater inflater = mContext.getSystemService(LayoutInflater.class);
        final View popupView =
                inflater.inflate(R.layout.accessibility_qs_tooltip, /* root= */ null);
        popupView.setFocusable(/* focusable= */ true);
        popupView.setAccessibilityDelegate(mAccessibilityDelegate);
        setContentView(popupView);

        final ImageView imageView = getContentView().findViewById(R.id.qs_illustration);
        imageView.setImageResource(imageResId);
        final TextView textView = getContentView().findViewById(R.id.qs_content);
        textView.setText(text);
        setWidth(getWindowWidthWith(textView));
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        setFocusable(/* focusable= */ true);
        setOutsideTouchable(/* touchable= */ true);
    }

    /**
     * Displays the content view in a popup window at the top and center position.
     *
     * @param targetView a target view to get the {@link View#getWindowToken()} token from.
     */
    public void showAtTopCenter(View targetView) {
        showAtLocation(targetView, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    /**
     * Disposes of the popup window.
     *
     * <p> Remove any pending posts of callbacks and sent messages for closing popup window. </p>
     */
    @Override
    public void dismiss() {
        super.dismiss();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(/* token= */ null);
        }
    }

    /**
     * Displays the content view in a popup window at the specified location.
     *
     * <p> The system will attempt to close popup window to the target duration of the threads if
     * close delay time is positive number. </p>
     *
     * @param parent a parent view to get the {@link android.view.View#getWindowToken()} token from
     * @param gravity the gravity which controls the placement of the popup window
     * @param x the popup's x location offset
     * @param y the popup's y location offset
     */
    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        scheduleAutoCloseAction();
    }

    private void scheduleAutoCloseAction() {
        if (mCloseDelayTimeMillis <= 0) {
            return;
        }

        if (mHandler == null) {
            mHandler = new Handler(mContext.getMainLooper());
        }
        mHandler.removeCallbacksAndMessages(/* token= */ null);
        mHandler.postDelayed(this::dismiss, mCloseDelayTimeMillis);
    }

    private int getWindowWidthWith(TextView textView) {
        final int availableWindowWidth = getAvailableWindowWidth();
        final int widthSpec =
                View.MeasureSpec.makeMeasureSpec(availableWindowWidth, View.MeasureSpec.AT_MOST);
        final int heightSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthSpec, heightSpec);
        return textView.getMeasuredWidth();
    }

    @VisibleForTesting
    int getAvailableWindowWidth() {
        final Resources res = mContext.getResources();
        final int padding = res.getDimensionPixelSize(R.dimen.accessibility_qs_tooltip_margin);
        final int screenWidth = res.getDisplayMetrics().widthPixels;
        return screenWidth - padding * 2;
    }
}

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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.systemui.biometrics.UdfpsUtils;
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.view.BottomScrollView;

import java.util.Locale;

/**
 * View for udfps enrolling.
 */
public class UdfpsEnrollEnrollingView extends GlifLayout {
    private final UdfpsUtils mUdfpsUtils;
    private final Context mContext;
    // We don't need to listen to onConfigurationChanged() for mRotation here because
    // FingerprintEnrollEnrolling is always recreated once the configuration is changed.
    private final int mRotation;
    private final boolean mIsLandscape;
    private final boolean mShouldUseReverseLandscape;
    private UdfpsEnrollView mUdfpsEnrollView;
    private View mHeaderView;
    private AccessibilityManager mAccessibilityManager;


    public UdfpsEnrollEnrollingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mRotation = mContext.getDisplay().getRotation();
        mIsLandscape = mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270;
        final boolean isLayoutRtl = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL);
        mShouldUseReverseLandscape = (mRotation == Surface.ROTATION_90 && isLayoutRtl)
                || (mRotation == Surface.ROTATION_270 && !isLayoutRtl);

        mUdfpsUtils = new UdfpsUtils();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderView = findViewById(com.google.android.setupdesign.R.id.sud_landscape_header_area);
        mUdfpsEnrollView = findViewById(R.id.udfps_animation_view);
    }

    void initView(FingerprintSensorPropertiesInternal udfpsProps,
            UdfpsEnrollHelper udfpsEnrollHelper,
            AccessibilityManager accessibilityManager) {
        mAccessibilityManager = accessibilityManager;
        initUdfpsEnrollView(udfpsProps, udfpsEnrollHelper);

        if (!mIsLandscape) {
            adjustPortraitPaddings();
        } else if (mShouldUseReverseLandscape) {
            swapHeaderAndContent();
        }
        setOnHoverListener();
    }

    void setSecondaryButtonBackground(@ColorInt int color) {
        // Set the button background only when the button is not under udfps overlay to avoid UI
        // overlap.
        if (!mIsLandscape || mShouldUseReverseLandscape) {
            return;
        }
        final Button secondaryButtonView =
                getMixin(FooterBarMixin.class).getSecondaryButtonView();
        secondaryButtonView.setBackgroundColor(color);
        if (mRotation == Surface.ROTATION_90) {
            secondaryButtonView.setGravity(Gravity.START);
        } else {
            secondaryButtonView.setGravity(Gravity.END);
        }
        mHeaderView.post(() -> {
            secondaryButtonView.setLayoutParams(
                    new LinearLayout.LayoutParams(mHeaderView.getMeasuredWidth(),
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        });
    }

    private void initUdfpsEnrollView(FingerprintSensorPropertiesInternal udfpsProps,
                                     UdfpsEnrollHelper udfpsEnrollHelper) {
        DisplayInfo displayInfo = new DisplayInfo();
        mContext.getDisplay().getDisplayInfo(displayInfo);

        final float scaleFactor = mUdfpsUtils.getScaleFactor(displayInfo);
        Rect udfpsBounds = udfpsProps.getLocation().getRect();
        udfpsBounds.scale(scaleFactor);

        final Rect overlayBounds = new Rect(
                0, /* left */
                displayInfo.getNaturalHeight() / 2, /* top */
                displayInfo.getNaturalWidth(), /* right */
                displayInfo.getNaturalHeight() /* botom */);

        UdfpsOverlayParams params = new UdfpsOverlayParams(
                udfpsBounds,
                overlayBounds,
                displayInfo.getNaturalWidth(),
                displayInfo.getNaturalHeight(),
                scaleFactor,
                displayInfo.rotation,
                udfpsProps.sensorType);

        mUdfpsEnrollView.setOverlayParams(params);
        mUdfpsEnrollView.setEnrollHelper(udfpsEnrollHelper);
    }

    private void adjustPortraitPaddings() {
        // In the portrait mode, layout_container's height is 0, so it's
        // always shown at the bottom of the screen.
        final FrameLayout portraitLayoutContainer = findViewById(R.id.layout_container);

        // In the portrait mode, the title and lottie animation view may
        // overlap when title needs three lines, so adding some paddings
        // between them, and adjusting the fp progress view here accordingly.
        final int layoutLottieAnimationPadding = (int) getResources()
                .getDimension(R.dimen.udfps_lottie_padding_top);
        portraitLayoutContainer.setPadding(0,
                layoutLottieAnimationPadding, 0, 0);
        final ImageView progressView = mUdfpsEnrollView.findViewById(
                R.id.udfps_enroll_animation_fp_progress_view);
        progressView.setPadding(0, -(layoutLottieAnimationPadding),
                0, layoutLottieAnimationPadding);
        final ImageView fingerprintView = mUdfpsEnrollView.findViewById(
                R.id.udfps_enroll_animation_fp_view);
        fingerprintView.setPadding(0, -layoutLottieAnimationPadding,
                0, layoutLottieAnimationPadding);

        // TODO(b/260970216) Instead of hiding the description text view, we should
        //  make the header view scrollable if the text is too long.
        // If description text view has overlap with udfps progress view, hide it.
        final View descView = getDescriptionTextView();
        getViewTreeObserver().addOnDrawListener(() -> {
            if (descView.getVisibility() == View.VISIBLE
                    && hasOverlap(descView, mUdfpsEnrollView)) {
                descView.setVisibility(View.GONE);
            }
        });
    }

    private void setOnHoverListener() {
        if (!mAccessibilityManager.isEnabled()) return;

        final View.OnHoverListener onHoverListener = (v, event) -> {
            // Map the touch to portrait mode if the device is in
            // landscape mode.
            final Point scaledTouch =
                    mUdfpsUtils.getTouchInNativeCoordinates(event.getPointerId(0),
                            event, mUdfpsEnrollView.getOverlayParams());

            if (mUdfpsUtils.isWithinSensorArea(event.getPointerId(0), event,
                    mUdfpsEnrollView.getOverlayParams())) {
                return false;
            }

            final String theStr = mUdfpsUtils.onTouchOutsideOfSensorArea(
                    mAccessibilityManager.isTouchExplorationEnabled(), mContext,
                    scaledTouch.x, scaledTouch.y, mUdfpsEnrollView.getOverlayParams());
            if (theStr != null) {
                v.announceForAccessibility(theStr);
            }
            return false;
        };

        findManagedViewById(mIsLandscape
                ? com.google.android.setupdesign.R.id.sud_landscape_content_area
                : com.google.android.setupdesign.R.id.sud_layout_content
        ).setOnHoverListener(onHoverListener);
    }

    private void swapHeaderAndContent() {
        // Reverse header and body
        ViewGroup parentView = (ViewGroup) mHeaderView.getParent();
        parentView.removeView(mHeaderView);
        parentView.addView(mHeaderView);

        // Hide scroll indicators
        BottomScrollView headerScrollView = mHeaderView.findViewById(
                com.google.android.setupdesign.R.id.sud_header_scroll_view);
        headerScrollView.setScrollIndicators(0);
    }

    @VisibleForTesting
    boolean hasOverlap(View view1, View view2) {
        int[] firstPosition = new int[2];
        int[] secondPosition = new int[2];

        view1.getLocationOnScreen(firstPosition);
        view2.getLocationOnScreen(secondPosition);

        // Rect constructor parameters: left, top, right, bottom
        Rect rectView1 = new Rect(firstPosition[0], firstPosition[1],
                firstPosition[0] + view1.getMeasuredWidth(),
                firstPosition[1] + view1.getMeasuredHeight());
        Rect rectView2 = new Rect(secondPosition[0], secondPosition[1],
                secondPosition[0] + view2.getMeasuredWidth(),
                secondPosition[1] + view2.getMeasuredHeight());
        return rectView1.intersect(rectView2);
    }
}

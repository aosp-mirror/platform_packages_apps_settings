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

package com.android.settings.panel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.settings.SettingsEnums;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.SliceMetadata;
import androidx.slice.widget.SliceLiveData;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.panel.PanelLoggingContract.PanelClosedKeys;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.google.android.setupdesign.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class PanelFragment extends Fragment {

    private static final String TAG = "PanelFragment";

    /**
     * Duration of the animation entering the screen, in milliseconds.
     */
    private static final int DURATION_ANIMATE_PANEL_EXPAND_MS = 250;

    /**
     * Duration of the animation exiting the screen, in milliseconds.
     */
    private static final int DURATION_ANIMATE_PANEL_COLLAPSE_MS = 200;

    /**
     * Duration of timeout waiting for Slice data to bind, in milliseconds.
     */
    private static final int DURATION_SLICE_BINDING_TIMEOUT_MS = 250;

    private View mLayoutView;
    private TextView mTitleView;
    private Button mSeeMoreButton;
    private Button mDoneButton;
    private RecyclerView mPanelSlices;

    private PanelContent mPanel;
    private MetricsFeatureProvider mMetricsProvider;
    private String mPanelClosedKey;

    private final List<LiveData<Slice>> mSliceLiveData = new ArrayList<>();

    @VisibleForTesting
    PanelSlicesLoaderCountdownLatch mPanelSlicesLoaderCountdownLatch;

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = () -> {
        return false;
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            animateIn();
            if (mPanelSlices != null) {
                mPanelSlices.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        }
    };

    private PanelSlicesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mLayoutView = inflater.inflate(R.layout.panel_layout, container, false);
        createPanelContent();
        return mLayoutView;
    }

    /**
     * Animate the old panel out from the screen, then update the panel with new content once the
     * animation is done.
     * <p>
     *     Takes the entire panel and animates out from behind the navigation bar.
     * <p>
     *     Call createPanelContent() once animation end.
     */
    void updatePanelWithAnimation() {
        final View panelContent = mLayoutView.findViewById(R.id.panel_container);
        final AnimatorSet animatorSet = buildAnimatorSet(mLayoutView,
                0.0f /* startY */, panelContent.getHeight() /* endY */,
                1.0f /* startAlpha */, 0.0f /* endAlpha */,
                DURATION_ANIMATE_PANEL_COLLAPSE_MS);

        final ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(0.0f, 1.0f);
        animatorSet.play(animator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                createPanelContent();
            }
        });
        animatorSet.start();
    }

    private void createPanelContent() {
        final FragmentActivity activity = getActivity();
        if (mLayoutView == null) {
            activity.finish();
        }

        mPanelSlices = mLayoutView.findViewById(R.id.panel_parent_layout);
        mSeeMoreButton = mLayoutView.findViewById(R.id.see_more);
        mDoneButton = mLayoutView.findViewById(R.id.done);
        mTitleView = mLayoutView.findViewById(R.id.panel_title);

        // Make the panel layout gone here, to avoid janky animation when updating from old panel.
        // We will make it visible once the panel is ready to load.
        mPanelSlices.setVisibility(View.GONE);

        final Bundle arguments = getArguments();
        final String panelType =
                arguments.getString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT);
        final String callingPackageName =
                arguments.getString(SettingsPanelActivity.KEY_CALLING_PACKAGE_NAME);
        final String mediaPackageName =
                arguments.getString(SettingsPanelActivity.KEY_MEDIA_PACKAGE_NAME);

        // TODO (b/124399577) transform interface to take a context and bundle.
        mPanel = FeatureFactory.getFactory(activity)
                .getPanelFeatureProvider()
                .getPanel(activity, panelType, mediaPackageName);

        if (mPanel == null) {
            activity.finish();
        }

        mMetricsProvider = FeatureFactory.getFactory(activity).getMetricsFeatureProvider();

        mPanelSlices.setLayoutManager(new LinearLayoutManager((activity)));

        // Add predraw listener to remove the animation and while we wait for Slices to load.
        mLayoutView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        // Start loading Slices. When finished, the Panel will animate in.
        loadAllSlices();

        mTitleView.setText(mPanel.getTitle());
        mSeeMoreButton.setOnClickListener(getSeeMoreListener());
        mDoneButton.setOnClickListener(getCloseListener());

        // If getSeeMoreIntent() is null, hide the mSeeMoreButton.
        if (mPanel.getSeeMoreIntent() == null) {
            mSeeMoreButton.setVisibility(View.GONE);
        }

        // Log panel opened.
        mMetricsProvider.action(
                0 /* attribution */,
                SettingsEnums.PAGE_VISIBLE /* opened panel - Action */,
                mPanel.getMetricsCategory(),
                callingPackageName,
                0 /* value */);
    }

    private void loadAllSlices() {
        mSliceLiveData.clear();
        final List<Uri> sliceUris = mPanel.getSlices();
        mPanelSlicesLoaderCountdownLatch = new PanelSlicesLoaderCountdownLatch(sliceUris.size());

        for (Uri uri : sliceUris) {
            final LiveData<Slice> sliceLiveData = SliceLiveData.fromUri(getActivity(), uri);

            // Add slice first to make it in order.  Will remove it later if there's an error.
            mSliceLiveData.add(sliceLiveData);

            sliceLiveData.observe(getViewLifecycleOwner(), slice -> {
                // If the Slice has already loaded, do nothing.
                if (mPanelSlicesLoaderCountdownLatch.isSliceLoaded(uri)) {
                    return;
                }

                /**
                 * Watching for the {@link Slice} to load.
                 * <p>
                 *     If the Slice comes back {@code null} or with the Error attribute, remove the
                 *     Slice data from the list, and mark the Slice as loaded.
                 * <p>
                 *     If the Slice has come back fully loaded, then mark the Slice as loaded.  No
                 *     other actions required since we already have the Slice data in the list.
                 * <p>
                 *     If the Slice does not match the above condition, we will still want to mark
                 *     it as loaded after 250ms timeout to avoid delay showing up the panel for
                 *     too long.  Since we are still having the Slice data in the list, the Slice
                 *     will show up later once it is loaded.
                 */
                final SliceMetadata metadata = SliceMetadata.from(getActivity(), slice);
                if (slice == null || metadata.isErrorSlice()) {
                    mSliceLiveData.remove(sliceLiveData);
                    mPanelSlicesLoaderCountdownLatch.markSliceLoaded(uri);
                } else if (metadata.getLoadingState() == SliceMetadata.LOADED_ALL) {
                    mPanelSlicesLoaderCountdownLatch.markSliceLoaded(uri);
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        mPanelSlicesLoaderCountdownLatch.markSliceLoaded(uri);
                        loadPanelWhenReady();
                    }, DURATION_SLICE_BINDING_TIMEOUT_MS);
                }

                loadPanelWhenReady();
            });
        }
    }

    /**
     * When all of the Slices have loaded for the first time, then we can setup the
     * {@link RecyclerView}.
     * <p>
     *     When the Recyclerview has been laid out, we can begin the animation with the
     *     {@link mOnGlobalLayoutListener}, which calls {@link #animateIn()}.
     */
    private void loadPanelWhenReady() {
        if (mPanelSlicesLoaderCountdownLatch.isPanelReadyToLoad()) {
            mAdapter = new PanelSlicesAdapter(
                    this, mSliceLiveData, mPanel.getMetricsCategory());
            mPanelSlices.setAdapter(mAdapter);
            mPanelSlices.getViewTreeObserver()
                    .addOnGlobalLayoutListener(mOnGlobalLayoutListener);
            mPanelSlices.setVisibility(View.VISIBLE);

            DividerItemDecoration itemDecoration = new DividerItemDecoration(getActivity());
            itemDecoration
                    .setDividerCondition(DividerItemDecoration.DIVIDER_CONDITION_BOTH);
            mPanelSlices.addItemDecoration(itemDecoration);
        }
    }

    /**
     * Animate a Panel onto the screen.
     * <p>
     *     Takes the entire panel and animates in from behind the navigation bar.
     * <p>
     *     Relies on the Panel being having a fixed height to begin the animation.
     */
    private void animateIn() {
        final View panelContent = mLayoutView.findViewById(R.id.panel_container);
        final AnimatorSet animatorSet = buildAnimatorSet(mLayoutView,
                panelContent.getHeight() /* startY */, 0.0f /* endY */,
                0.0f /* startAlpha */, 1.0f /* endAlpha */,
                DURATION_ANIMATE_PANEL_EXPAND_MS);
        final ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(0.0f, 1.0f);
        animatorSet.play(animator);
        animatorSet.start();
        // Remove the predraw listeners on the Panel.
        mLayoutView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
    }

    /**
     * Build an {@link AnimatorSet} to animate the Panel, {@param parentView} in or out of the
     * screen, based on the positional parameters {@param startY}, {@param endY}, the parameters
     * for alpha changes {@param startAlpha}, {@param endAlpha}, and the {@param duration} in
     * milliseconds.
     */
    @NonNull
    private static AnimatorSet buildAnimatorSet(@NonNull View parentView, float startY, float endY,
            float startAlpha, float endAlpha, int duration) {
        final View sheet = parentView.findViewById(R.id.panel_container);
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(sheet, View.TRANSLATION_Y, startY, endY),
                ObjectAnimator.ofFloat(sheet, View.ALPHA, startAlpha,endAlpha));
        return animatorSet;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (TextUtils.isEmpty(mPanelClosedKey)) {
            mPanelClosedKey = PanelClosedKeys.KEY_OTHERS;
        }

        mMetricsProvider.action(
                0 /* attribution */,
                SettingsEnums.PAGE_HIDE,
                mPanel.getMetricsCategory(),
                mPanelClosedKey,
                0 /* value */);
    }

    @VisibleForTesting
    View.OnClickListener getSeeMoreListener() {
        return (v) -> {
            mPanelClosedKey = PanelClosedKeys.KEY_SEE_MORE;
            final FragmentActivity activity = getActivity();
            activity.startActivityForResult(mPanel.getSeeMoreIntent(), 0);
            activity.finish();
        };
    }

    @VisibleForTesting
    View.OnClickListener getCloseListener() {
        return (v) -> {
            mPanelClosedKey = PanelClosedKeys.KEY_DONE;
            getActivity().finish();
        };
    }
}

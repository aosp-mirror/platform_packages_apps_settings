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

package com.android.settings.inputmethod;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;

public class TrackpadGestureDialogFragment extends BottomSheetDialogFragment {

    private Context mContext;
    private LayoutInflater mInflater;
    private View mViewArrowPrevious;
    private View mViewArrowNext;
    private ViewPager mViewPager;
    private ArrayList<View> mPageList;
    private ImageView[] mDotIndicators;
    private View[] mViewPagerItems;
    private Button mButtonStartSkip;
    private Button mButtonStartRestart;
    private Button mButtonEndNext;
    private Button mButtonEndDone;

    private static final int DOT_INDICATOR_SIZE = 12;
    private static final int DOT_INDICATOR_LEFT_PADDING = 6;
    private static final int DOT_INDICATOR_RIGHT_PADDING = 6;

    public TrackpadGestureDialogFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        final Point size = getScreenSize();
        final WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = (int) (size.x * 0.75);
        window.setAttributes(attributes);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mInflater = mContext.getSystemService(LayoutInflater.class);

        View gestureEducationView = mInflater.inflate(R.layout.trackpad_gesture_preview, null);
        addViewPager(gestureEducationView);
        dialog.setContentView(gestureEducationView);

        Window gestureDialogWindow = dialog.getWindow();
        gestureDialogWindow.setType(TYPE_SYSTEM_DIALOG);

        // Workaround for solve issue about dialog not full expanded when landscape.
        FrameLayout bottomSheet = (FrameLayout)
                dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        bottomSheet.setBackgroundResource(android.R.color.transparent);
        BottomSheetBehavior.from(bottomSheet)
                .setState(BottomSheetBehavior.STATE_EXPANDED);

        return dialog;
    }

    private ArrayList<Integer> getViewPagerResource() {
        return new ArrayList<Integer>(
                Arrays.asList(
                        R.layout.gesture_tip1_go_home,
                        R.layout.gesture_tip2_go_back,
                        R.layout.gesture_tip3_recent_apps,
                        R.layout.gesture_tip4_notifications,
                        R.layout.gesture_tip5_switch_apps));
    }

    private void addViewPager(View preview) {
        mViewPager = preview.findViewById(R.id.viewpager);
        int viewPagerResourceSize = getViewPagerResource().size();
        mViewPagerItems = new View[viewPagerResourceSize];
        for (int i = 0; i < viewPagerResourceSize; i++) {
            mViewPagerItems[i] =
                    mInflater.inflate(getViewPagerResource().get(i), null /* root */);
        }

        mPageList = new ArrayList<View>();
        for (int i = 0; i < mViewPagerItems.length; i++) {
            mPageList.add(mViewPagerItems[i]);
        }

        mViewPager.setAdapter(new GesturePagerAdapter(mPageList));

        mButtonStartRestart = (Button) preview.findViewById(R.id.button_restart);
        mButtonStartRestart.setOnClickListener(v -> {
            final int firstPos = mViewPager.getCurrentItem() - mViewPagerItems.length;
            mViewPager.setCurrentItem(firstPos, true);
        });

        mButtonEndDone = (Button) preview.findViewById(R.id.button_done);
        mButtonEndDone.setOnClickListener(v -> {
            dismiss();
        });

        mButtonStartSkip = (Button) preview.findViewById(R.id.button_skip);
        mButtonStartSkip.setOnClickListener(v -> {
            dismiss();
        });

        mButtonEndNext = (Button) preview.findViewById(R.id.button_next);
        mButtonEndNext.setOnClickListener(v -> {
            final int nextPos = mViewPager.getCurrentItem() + 1;
            mViewPager.setCurrentItem(nextPos, true);
        });

        mViewPager.addOnPageChangeListener(createPageListener());
        final ViewGroup viewGroup = (ViewGroup) preview.findViewById(R.id.viewGroup);
        mDotIndicators = new ImageView[mPageList.size()];
        for (int i = 0; i < mPageList.size(); i++) {
            final ImageView imageView = new ImageView(mContext);
            final ViewGroup.MarginLayoutParams lp =
                    new ViewGroup.MarginLayoutParams(DOT_INDICATOR_SIZE, DOT_INDICATOR_SIZE);
            lp.setMargins(DOT_INDICATOR_LEFT_PADDING, 0, DOT_INDICATOR_RIGHT_PADDING, 0);
            imageView.setLayoutParams(lp);
            mDotIndicators[i] = imageView;
            viewGroup.addView(mDotIndicators[i]);
        }
    }

    private static class GesturePagerAdapter extends PagerAdapter {
        private final ArrayList<View> mPageViewList;

        GesturePagerAdapter(ArrayList<View> pageViewList) {
            mPageViewList = pageViewList;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mPageViewList.get(position) != null) {
                container.removeView(mPageViewList.get(position));
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mPageViewList.get(position));
            return mPageViewList.get(position);
        }

        @Override
        public int getCount() {
            return mPageViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }
    }

    private ViewPager.OnPageChangeListener createPageListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                    int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffset != 0) {
                    for (int i = 0; i < mPageList.size(); i++) {
                        mViewPagerItems[i].setVisibility(View.VISIBLE);
                    }
                } else {
                    updateIndicator(position);
                }
            }

            @Override
            public void onPageSelected(int position) {}

            @Override
            public void onPageScrollStateChanged(int state) {}
        };
    }

    private void updateIndicator(int position) {
        for (int i = 0; i < mPageList.size(); i++) {
            if (position == i) {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_focused);
                mViewPagerItems[i].setVisibility(View.VISIBLE);
            } else {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_unfocused);
                mViewPagerItems[i].setVisibility(View.INVISIBLE);
            }
        }

        if (position >= 0 && position < mViewPagerItems.length - 1) {
            mButtonStartSkip.setVisibility(View.VISIBLE);
            mButtonEndNext.setVisibility(View.VISIBLE);
            mButtonStartRestart.setVisibility(View.GONE);
            mButtonEndDone.setVisibility(View.GONE);
        } else {
            mButtonStartSkip.setVisibility(View.GONE);
            mButtonEndNext.setVisibility(View.GONE);
            mButtonStartRestart.setVisibility(View.VISIBLE);
            mButtonEndDone.setVisibility(View.VISIBLE);
        }
    }

    private Point getScreenSize() {
        final Point size = new Point();
        final Activity activity = (Activity) mContext;
        final Display display = activity.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        return size;
    }
}

/*
 * Copyright (C) 2011 Google Inc.
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

package com.android.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.settings.R;

import java.util.List;

/**
 * This class provides a short tutorial that introduces the user to the features
 * available in Touch Exploration.
 */
public class AccessibilityTutorialActivity extends Activity {

    /** Intent action for launching this activity. */
    public static final String ACTION = "android.settings.ACCESSIBILITY_TUTORIAL";

    /** Instance state saving constant for the active module. */
    private static final String KEY_ACTIVE_MODULE = "active_module";

    /** The index of the module to show when first opening the tutorial. */
    private static final int DEFAULT_MODULE = 0;

    /** View animator for switching between modules. */
    private ViewAnimator mViewAnimator;

    private AccessibilityManager mAccessibilityManager;

    /** Should touch exploration be disabled when this activity is paused? */
    private boolean mDisableOnPause;

    private final AnimationListener mInAnimationListener = new AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            final int index = mViewAnimator.getDisplayedChild();
            final TutorialModule module = (TutorialModule) mViewAnimator.getChildAt(index);

            activateModule(module);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // Do nothing.
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // Do nothing.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Animation inAnimation = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left);
        inAnimation.setAnimationListener(mInAnimationListener);

        final Animation outAnimation = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left);

        mViewAnimator = new ViewAnimator(this);
        mViewAnimator.setInAnimation(inAnimation);
        mViewAnimator.setOutAnimation(outAnimation);
        mViewAnimator.addView(new TouchTutorialModule1(this, this));
        mViewAnimator.addView(new TouchTutorialModule2(this, this));

        setContentView(mViewAnimator);

        mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        if (savedInstanceState != null) {
            show(savedInstanceState.getInt(KEY_ACTIVE_MODULE, DEFAULT_MODULE));
        } else {
            show(DEFAULT_MODULE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ContentResolver cr = getContentResolver();

        if (Settings.Secure.getInt(cr, Settings.Secure.TOUCH_EXPLORATION_ENABLED, 0) == 0) {
            Settings.Secure.putInt(cr, Settings.Secure.TOUCH_EXPLORATION_ENABLED, 1);
            mDisableOnPause = true;
        } else {
            mDisableOnPause = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mDisableOnPause) {
            final ContentResolver cr = getContentResolver();
            Settings.Secure.putInt(cr, Settings.Secure.TOUCH_EXPLORATION_ENABLED, 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_ACTIVE_MODULE, mViewAnimator.getDisplayedChild());
    }

    private void activateModule(TutorialModule module) {
        module.activate();
    }

    private void deactivateModule(TutorialModule module) {
        mAccessibilityManager.interrupt();
        mViewAnimator.setOnKeyListener(null);
        module.deactivate();
    }

    private void interrupt() {
        mAccessibilityManager.interrupt();
    }

    private void next() {
        show(mViewAnimator.getDisplayedChild() + 1);
    }

    private void previous() {
        show(mViewAnimator.getDisplayedChild() - 1);
    }

    private void show(int which) {
        if ((which < 0) || (which >= mViewAnimator.getChildCount())) {
            return;
        }

        mAccessibilityManager.interrupt();

        final int displayedIndex = mViewAnimator.getDisplayedChild();
        final TutorialModule displayedView = (TutorialModule) mViewAnimator.getChildAt(
                displayedIndex);
        deactivateModule(displayedView);

        mViewAnimator.setDisplayedChild(which);
    }

    /**
     * Loads application labels and icons.
     */
    private static class AppsAdapter extends ArrayAdapter<ResolveInfo> {
        protected final int mTextViewResourceId;

        private final int mIconSize;
        private final View.OnHoverListener mDefaultHoverListener;

        private View.OnHoverListener mHoverListener;

        public AppsAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            mIconSize = context.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
            mTextViewResourceId = textViewResourceId;
            mDefaultHoverListener = new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    if (mHoverListener != null) {
                        return mHoverListener.onHover(v, event);
                    } else {
                        return false;
                    }
                }
            };

            loadAllApps();
        }

        public CharSequence getLabel(int position) {
            final PackageManager packageManager = getContext().getPackageManager();
            final ResolveInfo appInfo = getItem(position);
            return appInfo.loadLabel(packageManager);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final PackageManager packageManager = getContext().getPackageManager();
            final View view = super.getView(position, convertView, parent);
            view.setOnHoverListener(mDefaultHoverListener);
            view.setTag(position);

            final ResolveInfo appInfo = getItem(position);
            final CharSequence label = appInfo.loadLabel(packageManager);
            final Drawable icon = appInfo.loadIcon(packageManager);
            final TextView text = (TextView) view.findViewById(mTextViewResourceId);

            icon.setBounds(0, 0, mIconSize, mIconSize);

            populateView(text, label, icon);

            return view;
        }

        private void loadAllApps() {
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager pm = getContext().getPackageManager();
            final List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

            addAll(apps);
        }

        protected void populateView(TextView text, CharSequence label, Drawable icon) {
            text.setText(label);
            text.setCompoundDrawables(null, icon, null, null);
        }

        public void setOnHoverListener(View.OnHoverListener hoverListener) {
            mHoverListener = hoverListener;
        }
    }

    /**
     * Introduces using a finger to explore and interact with on-screen content.
     */
    private static class TouchTutorialModule1 extends TutorialModule implements
            View.OnHoverListener, AdapterView.OnItemClickListener {
        /**
         * Handles the case where the user overshoots the target area.
         */
        private class HoverTargetHandler extends Handler {
            private static final int MSG_ENTERED_TARGET = 1;
            private static final int DELAY_ENTERED_TARGET = 500;

            private boolean mInsideTarget = false;

            public void enteredTarget() {
                mInsideTarget = true;
                mHandler.sendEmptyMessageDelayed(MSG_ENTERED_TARGET, DELAY_ENTERED_TARGET);
            }

            public void exitedTarget() {
                mInsideTarget = false;
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ENTERED_TARGET:
                        if (mInsideTarget) {
                            addInstruction(R.string.accessibility_tutorial_lesson_1_text_4,
                                    mTargetName);
                        } else {
                            addInstruction(R.string.accessibility_tutorial_lesson_1_text_4_exited,
                                    mTargetName);
                            setFlag(FLAG_TOUCHED_TARGET, false);
                        }
                        break;
                }
            }
        }

        private static final int FLAG_TOUCH_ITEMS = 0x1;
        private static final int FLAG_TOUCHED_ITEMS = 0x2;
        private static final int FLAG_TOUCHED_TARGET = 0x4;
        private static final int FLAG_TAPPED_TARGET = 0x8;

        private static final int MORE_EXPLORED_COUNT = 1;
        private static final int DONE_EXPLORED_COUNT = 2;

        private final HoverTargetHandler mHandler;
        private final AppsAdapter mAppsAdapter;
        private final GridView mAllApps;

        private int mTouched = 0;

        private int mTargetPosition;
        private CharSequence mTargetName;

        public TouchTutorialModule1(Context context, AccessibilityTutorialActivity controller) {
            super(context, controller, R.layout.accessibility_tutorial_1,
                    R.string.accessibility_tutorial_lesson_1_title);

            mHandler = new HoverTargetHandler();

            mAppsAdapter = new AppsAdapter(context, R.layout.accessibility_tutorial_app_icon,
                    R.id.app_icon);
            mAppsAdapter.setOnHoverListener(this);

            mAllApps = (GridView) findViewById(R.id.all_apps);
            mAllApps.setAdapter(mAppsAdapter);
            mAllApps.setOnItemClickListener(this);

            findViewById(R.id.next_button).setOnHoverListener(this);

            setSkipVisible(true);
        }

        @Override
        public boolean onHover(View v, MotionEvent event) {
            switch (v.getId()) {
                case R.id.app_icon:
                    if (hasFlag(FLAG_TOUCH_ITEMS) && !hasFlag(FLAG_TOUCHED_ITEMS) && v.isEnabled()
                            && (event.getAction() == MotionEvent.ACTION_HOVER_ENTER)) {
                        mTouched++;

                        if (mTouched >= DONE_EXPLORED_COUNT) {
                            setFlag(FLAG_TOUCHED_ITEMS, true);
                            addInstruction(R.string.accessibility_tutorial_lesson_1_text_3,
                                    mTargetName);
                        } else if (mTouched == MORE_EXPLORED_COUNT) {
                            addInstruction(R.string.accessibility_tutorial_lesson_1_text_2_more);
                        }

                        v.setEnabled(false);
                    } else if (hasFlag(FLAG_TOUCHED_ITEMS)
                            && ((Integer) v.getTag() == mTargetPosition)) {
                        if (!hasFlag(FLAG_TOUCHED_TARGET)
                                && (event.getAction() == MotionEvent.ACTION_HOVER_ENTER)) {
                            mHandler.enteredTarget();
                            setFlag(FLAG_TOUCHED_TARGET, true);
                        } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                            mHandler.exitedTarget();
                        }
                    }
                    break;
            }

            return false;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (hasFlag(FLAG_TOUCHED_TARGET) && !hasFlag(FLAG_TAPPED_TARGET)
                    && (position == mTargetPosition)) {
                setFlag(FLAG_TAPPED_TARGET, true);
                final CharSequence nextText = getContext().getText(
                        R.string.accessibility_tutorial_next);
                addInstruction(R.string.accessibility_tutorial_lesson_1_text_5, nextText);
                setNextVisible(true);
            }
        }

        @Override
        public void onShown() {
            final int first = mAllApps.getFirstVisiblePosition();
            final int last = mAllApps.getLastVisiblePosition();

            mTargetPosition = 0;
            mTargetName = mAppsAdapter.getLabel(mTargetPosition);

            addInstruction(R.string.accessibility_tutorial_lesson_1_text_1);
            setFlag(FLAG_TOUCH_ITEMS, true);
        }
    }

    /**
     * Introduces using two fingers to scroll through a list.
     */
    private static class TouchTutorialModule2 extends TutorialModule implements
            AbsListView.OnScrollListener, View.OnHoverListener {
        private static final int FLAG_EXPLORE_LIST = 0x1;
        private static final int FLAG_SCROLL_LIST = 0x2;
        private static final int FLAG_COMPLETED_TUTORIAL = 0x4;

        private static final int MORE_EXPLORE_COUNT = 1;
        private static final int DONE_EXPLORE_COUNT = 2;
        private static final int MORE_SCROLL_COUNT = 2;
        private static final int DONE_SCROLL_COUNT = 4;

        private final AppsAdapter mAppsAdapter;

        private int mExploreCount = 0;
        private int mInitialVisibleItem = -1;
        private int mScrollCount = 0;

        public TouchTutorialModule2(Context context, AccessibilityTutorialActivity controller) {
            super(context, controller, R.layout.accessibility_tutorial_2,
                    R.string.accessibility_tutorial_lesson_2_title);

            mAppsAdapter = new AppsAdapter(context, android.R.layout.simple_list_item_1,
                    android.R.id.text1) {
                @Override
                protected void populateView(TextView text, CharSequence label, Drawable icon) {
                    text.setText(label);
                    text.setCompoundDrawables(icon, null, null, null);
                }
            };
            mAppsAdapter.setOnHoverListener(this);

            ((ListView) findViewById(R.id.list_view)).setAdapter(mAppsAdapter);
            ((ListView) findViewById(R.id.list_view)).setOnScrollListener(this);

            setBackVisible(true);
        }

        @Override
        public boolean onHover(View v, MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_HOVER_ENTER) {
                return false;
            }

            switch (v.getId()) {
                case android.R.id.text1:
                    if (hasFlag(FLAG_EXPLORE_LIST) && !hasFlag(FLAG_SCROLL_LIST)) {
                        mExploreCount++;

                        if (mExploreCount >= DONE_EXPLORE_COUNT) {
                            addInstruction(R.string.accessibility_tutorial_lesson_2_text_3);
                            setFlag(FLAG_SCROLL_LIST, true);
                        } else if (mExploreCount == MORE_EXPLORE_COUNT) {
                            addInstruction(R.string.accessibility_tutorial_lesson_2_text_2_more);
                        }
                    }
                    break;
            }

            return false;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if (hasFlag(FLAG_SCROLL_LIST) && !hasFlag(FLAG_COMPLETED_TUTORIAL)) {
                if (mInitialVisibleItem < 0) {
                    mInitialVisibleItem = firstVisibleItem;
                }

                final int scrollCount = Math.abs(mInitialVisibleItem - firstVisibleItem);

                if ((mScrollCount == scrollCount) || (scrollCount <= 0)) {
                    return;
                } else {
                    mScrollCount = scrollCount;
                }

                if (mScrollCount >= DONE_SCROLL_COUNT) {
                    final CharSequence finishText = getContext().getText(
                            R.string.accessibility_tutorial_finish);
                    addInstruction(R.string.accessibility_tutorial_lesson_2_text_4, finishText);
                    setFlag(FLAG_COMPLETED_TUTORIAL, true);
                    setFinishVisible(true);
                } else if (mScrollCount == MORE_SCROLL_COUNT) {
                    addInstruction(R.string.accessibility_tutorial_lesson_2_text_3_more);
                }
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // Do nothing.
        }

        @Override
        public void onShown() {
            addInstruction(R.string.accessibility_tutorial_lesson_2_text_1);
            setFlag(FLAG_EXPLORE_LIST, true);
        }
    }

    /**
     * Abstract class that represents a single module within a tutorial.
     */
    private static abstract class TutorialModule extends FrameLayout implements OnClickListener {
        private final AccessibilityTutorialActivity mController;
        private final TextView mInstructions;
        private final Button mSkip;
        private final Button mBack;
        private final Button mNext;
        private final Button mFinish;
        private final int mTitleResId;

        /** Which bit flags have been set. */
        private long mFlags;

        /** Whether this module is currently focused. */
        private boolean mIsVisible;

        /** Handler for sending accessibility events after the current UI action. */
        private InstructionHandler mHandler = new InstructionHandler();

        /**
         * Constructs a new tutorial module for the given context and controller
         * with the specified layout.
         *
         * @param context The parent context.
         * @param controller The parent tutorial controller.
         * @param layoutResId The layout to use for this module.
         */
        public TutorialModule(Context context, AccessibilityTutorialActivity controller,
                int layoutResId, int titleResId) {
            super(context);

            mController = controller;
            mTitleResId = titleResId;

            final View container = LayoutInflater.from(context).inflate(
                    R.layout.accessibility_tutorial_container, this, true);

            mInstructions = (TextView) container.findViewById(R.id.instructions);
            mSkip = (Button) container.findViewById(R.id.skip_button);
            mSkip.setOnClickListener(this);
            mBack = (Button) container.findViewById(R.id.back_button);
            mBack.setOnClickListener(this);
            mNext = (Button) container.findViewById(R.id.next_button);
            mNext.setOnClickListener(this);
            mFinish = (Button) container.findViewById(R.id.finish_button);
            mFinish.setOnClickListener(this);

            final TextView title = (TextView) container.findViewById(R.id.title);

            if (title != null) {
                title.setText(titleResId);
            }

            final ViewGroup contentHolder = (ViewGroup) container.findViewById(R.id.content);
            LayoutInflater.from(context).inflate(layoutResId, contentHolder, true);
        }

        /**
         * Called when this tutorial gains focus.
         */
        public final void activate() {
            mIsVisible = true;

            mFlags = 0;
            mInstructions.setVisibility(View.GONE);
            mController.setTitle(mTitleResId);

            onShown();
        }

        /**
         * Formats an instruction string and adds it to the speaking queue.
         *
         * @param resId The resource id of the instruction string.
         * @param formatArgs Optional formatting arguments.
         * @see String#format(String, Object...)
         */
        protected void addInstruction(final int resId, Object... formatArgs) {
            if (!mIsVisible) {
                return;
            }

            final String text = mContext.getString(resId, formatArgs);
            mHandler.addInstruction(text);
        }

        private void addInstructionSync(CharSequence text) {
            mInstructions.setVisibility(View.VISIBLE);
            mInstructions.setText(text);
            mInstructions.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }

        /**
         * Called when this tutorial loses focus.
         */
        public void deactivate() {
            mIsVisible = false;

            mController.interrupt();
        }

        /**
         * Returns {@code true} if the flag with the specified id has been set.
         *
         * @param flagId The id of the flag to check for.
         * @return {@code true} if the flag with the specified id has been set.
         */
        protected boolean hasFlag(int flagId) {
            return (mFlags & flagId) == flagId;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.skip_button:
                    mController.finish();
                    break;
                case R.id.back_button:
                    mController.previous();
                    break;
                case R.id.next_button:
                    mController.next();
                    break;
                case R.id.finish_button:
                    mController.finish();
                    break;
            }
        }

        public abstract void onShown();

        /**
         * Sets or removes the flag with the specified id.
         *
         * @param flagId The id of the flag to modify.
         * @param value {@code true} to set the flag, {@code false} to remove
         *            it.
         */
        protected void setFlag(int flagId, boolean value) {
            if (value) {
                mFlags |= flagId;
            } else {
                mFlags = ~(~mFlags | flagId);
            }
        }

        protected void setSkipVisible(boolean visible) {
            mSkip.setVisibility(visible ? VISIBLE : GONE);
        }

        protected void setBackVisible(boolean visible) {
            mBack.setVisibility(visible ? VISIBLE : GONE);
        }

        protected void setNextVisible(boolean visible) {
            mNext.setVisibility(visible ? VISIBLE : GONE);
        }

        protected void setFinishVisible(boolean visible) {
            mFinish.setVisibility(visible ? VISIBLE : GONE);
        }

        private class InstructionHandler extends Handler {
            private static final int ADD_INSTRUCTION = 1;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ADD_INSTRUCTION:
                        final String text = (String) msg.obj;
                        addInstructionSync(text);
                        break;
                }
            }

            public void addInstruction(String text) {
                obtainMessage(ADD_INSTRUCTION, text).sendToTarget();
            }
        }
    }

    /**
     * Provides a tutorial-specific class name for fired accessibility events.
     */
    public static class TutorialTextView extends TextView {
        public TutorialTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
}

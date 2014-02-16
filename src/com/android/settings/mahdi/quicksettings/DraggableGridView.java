/*
 * Copyright (c) 2011, Animoto Inc.
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi.quicksettings;

import java.util.Collections;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public class DraggableGridView extends ViewGroup implements
        View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {

    public interface OnRearrangeListener {
        public abstract void onRearrange(int oldIndex, int newIndex);
        public abstract void onDelete(int index);
    }

    protected int mColCount, mChildWidth, mChildHeight, mCellGap, mScroll = 0;
    protected float mLastDelta = 0;
    protected Handler mHandler = new Handler();
    protected int mDragged = -1, mLastX = -1, mLastY = -1, mLastTarget = -1;
    protected boolean mEnabled = true, mTouching = false, mIsDelete = false;
    public static int sAnimT = 150;
    protected ArrayList<Integer> mNewPositions = new ArrayList<Integer>();
    protected OnRearrangeListener mOnRearrangeListener;
    protected OnClickListener mSecondaryOnClickListener;
    private OnItemClickListener mOnItemClickListener;

    private Resources mSystemUiResources;

    public DraggableGridView(Context context) {
        super(context);
        init(context);
    }

    public DraggableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    protected void setListeners() {
        setOnTouchListener(this);
        super.setOnClickListener(this);
        setOnLongClickListener(this);
    }

    private void init(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }
        setListeners();
        setChildrenDrawingOrderEnabled(true);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setColumnCount(int count) {
        mColCount = count;
    }

    public void setCellGap(int gap) {
        mCellGap = gap;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mSecondaryOnClickListener = l;
    }

    protected Runnable updateTask = new Runnable() {
        public void run() {
            if (mDragged != -1) {
                if (mLastY < mCellGap * 3 && mScroll > 0)
                    mScroll -= 20;
                else if (mLastY > getBottom() - getTop() - (mCellGap * 3)
                        && mScroll < getMaxScroll())
                    mScroll += 20;
            } else if (mLastDelta != 0 && !mTouching) {
                mScroll += mLastDelta;
                mLastDelta *= .9;
                if (Math.abs(mLastDelta) < .25)
                    mLastDelta = 0;
            }
            clampScroll();
            onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            if (mLastDelta != 0) {
                mHandler.postDelayed(this, 25);
            }
        }
    };

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        mNewPositions.add(-1);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        mNewPositions.add(-1);
    };

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        mNewPositions.remove(index);
    };

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View v = (View) getChildAt(i);
            if (v.getVisibility() != GONE) {
                Point xy = getCoorFromIndex(i);
                v.layout(xy.x, xy.y, xy.x + v.getMeasuredWidth(), xy.y + v.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the cell width dynamically
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mColCount - 1) * mCellGap);
        mChildWidth = mChildHeight = (int) Math.ceil(((float) availableWidth) / mColCount);

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View v = (View) getChildAt(i);
            ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) v.getLayoutParams();
            lp.width = mChildWidth;
            lp.height = mChildHeight;
            if (v.getVisibility() != View.GONE) {
                measureChild(v,MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.AT_MOST));
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mDragged == -1)
            return i;
        else if (i == childCount - 1)
            return mDragged;
        else if (i >= mDragged)
            return i + 1;
        return i;
    }

    public int getIndexFromCoor(int x, int y) {
        int col = getColumnFromCoor(x), row = getRowFromCoor(y + mScroll);
        if (col == -1 || row == -1) // touch is between columns or rows
            return -1;
        int index = row * mColCount + col;
        if (index >= getChildCount())
            return -1;
        return index;
    }

    protected int getColumnFromCoor(int coor) {
        coor -= mCellGap;
        for (int i = 0; coor > 0; i++) {
            if (coor < mChildWidth)
                return i;
            coor -= (mChildWidth + mCellGap);
        }
        return -1;
    }

    protected int getRowFromCoor(int coor) {
        coor -= mCellGap;
        for (int i = 0; coor > 0; i++) {
            if (coor < mChildHeight)
                return i;
            coor -= (mChildHeight + mCellGap);
        }
        return -1;
    }

    protected int getTargetFromCoor(int x, int y) {
        if (getRowFromCoor(y + mScroll) == -1) // touch is between rows
            return -1;

        int leftPos = getIndexFromCoor(x - (mChildWidth / 4), y);
        int rightPos = getIndexFromCoor(x + (mChildWidth / 4), y);
        if (leftPos == -1 && rightPos == -1) // touch is in the middle of
                                             // nowhere
            return -1;
        if (leftPos == rightPos) // touch is in the middle of a visual
            return -1;

        int target = -1;
        if (rightPos > -1)
            target = rightPos;
        else if (leftPos > -1)
            target = leftPos + 1;
        if (mDragged < target)
            return target - 1;

        return target;
    }

    protected Point getCoorFromIndex(int index) {
        int col = index % mColCount;
        int row = index / mColCount;
        return new Point(mCellGap / 2 + (mChildWidth + mCellGap / 2) * col, mCellGap
                / 2 + (mChildHeight + mCellGap / 2) * row - mScroll);
    }

    public int getIndexOf(View child) {
        for (int i = 0; i < getChildCount(); i++)
            if (getChildAt(i) == child)
                return i;
        return -1;
    }

    // EVENT HANDLERS
    public void onClick(View view) {
        if (mEnabled) {
            if (mSecondaryOnClickListener != null)
                mSecondaryOnClickListener.onClick(view);
            if (mOnItemClickListener != null)
                mOnItemClickListener.onItemClick(null,
                        getChildAt(getLastIndex()), getLastIndex(),
                        getLastIndex() / mColCount);
        }
    }

    void toggleAddDelete(boolean delete) {
        int resid = R.drawable.ic_menu_add_dark;
        int stringid = R.string.add;
        if (delete) {
            resid = R.drawable.ic_menu_delete_holo_dark;
            stringid = R.string.dialog_delete_title;
        }
        final TextView name =
            ((TextView) getChildAt(getChildCount() - 1).findViewById(R.id.text));
        final ImageView iv =
            ((ImageView) getChildAt(getChildCount() - 1).findViewById(R.id.image));
        name.setText(stringid);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTileTextSize(mColCount));
        name.setPadding(0, getTileTextPadding(mColCount), 0, 0);
        iv.setImageDrawable(getResources().getDrawable(resid));
    }

    public boolean onLongClick(View view) {
        if (!mEnabled) {
            return false;
        }
        int index = getLastIndex();
        if (index != -1 && index != getChildCount() - 1) {
            toggleAddDelete(true);
            mDragged = index;
            animateDragged();
            return true;
        }
        return false;
    }

    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            mEnabled = true;
            mLastX = (int) event.getX();
            mLastY = (int) event.getY();
            mTouching = true;
            break;
        case MotionEvent.ACTION_MOVE:
            int delta = mLastY - (int) event.getY();
            if (mDragged != -1) {
                // change draw location of dragged visual
                int x = (int) event.getX(), y = (int) event.getY();
                int l = x - (3 * mChildWidth / 4), t = y - (3 * mChildHeight / 4);
                getChildAt(mDragged).layout(l, t, l + mChildWidth,
                        t + mChildHeight);

                // check for new target hover
                int target = getTargetFromCoor(x, y);
                //Check if hovering over delete target
                if (getIndexFromCoor(x, y) == getChildCount() - 1) {
                    getChildAt(mDragged).setBackgroundColor(Color.RED);
                    mIsDelete = true;
                    break;
                } else {
                    mIsDelete = false;
                    getChildAt(mDragged).setBackgroundColor(Color.parseColor("#AA222222"));
                }
                if (mLastTarget != target && target != getChildCount() - 1) {
                    if (target != -1) {
                        animateGap(target);
                        mLastTarget = target;
                    }
                }
            } else {
                mScroll += delta;
                clampScroll();
                if (Math.abs(delta) > 4)
                    mEnabled = false;
                onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            }
            mLastX = (int) event.getX();
            mLastY = (int) event.getY();
            mLastDelta = delta;
            break;
        case MotionEvent.ACTION_UP:
            if (mDragged != -1) {
                toggleAddDelete(false);
                View v = getChildAt(mDragged);
                if (mLastTarget != -1 && !mIsDelete)
                    reorderChildren(true);
                else {
                    Point xy = getCoorFromIndex(mDragged);
                    v.layout(xy.x, xy.y, xy.x + mChildWidth, xy.y + mChildHeight);
                }
                v.clearAnimation();
                if (v instanceof ImageView)
                    ((ImageView) v).setAlpha(255);
                if (mIsDelete) {
                    mLastTarget = mDragged;
                    removeViewAt(mDragged);
                    mOnRearrangeListener.onDelete(mDragged);
                    reorderChildren(false);
                }
                mLastTarget = -1;
                mDragged = -1;
            } else {
                mHandler.post(updateTask);
            }
            mTouching = false;
            mIsDelete = false;
            break;
        }
        if (mDragged != -1)
            return true;
        return false;
    }

    // EVENT HELPERS
    protected void animateDragged() {
        View v = getChildAt(mDragged);
        int x = getCoorFromIndex(mDragged).x + mChildWidth / 2, y = getCoorFromIndex(mDragged).y
                + mChildWidth / 2;
        int l = x - (3 * mChildWidth / 4), t = y - (3 * mChildHeight / 4);
        v.layout(l, t, l + mChildWidth, t + mChildHeight);
        AnimationSet animSet = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(.667f, 1, .667f, 1,
                mChildWidth * 3 / 4, mChildHeight * 3 / 4);
        scale.setDuration(sAnimT);
        AlphaAnimation alpha = new AlphaAnimation(1, .8f);
        alpha.setDuration(sAnimT);

        animSet.addAnimation(scale);
        animSet.addAnimation(alpha);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);

        v.clearAnimation();
        v.startAnimation(animSet);
    }

    protected void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (i == mDragged)
                continue;
            int newPos = i;
            if (mDragged < target && i >= mDragged + 1 && i <= target)
                newPos--;
            else if (target < mDragged && i >= target && i < mDragged)
                newPos++;

            // animate
            int oldPos = i;
            if (mNewPositions.get(i) != -1)
                oldPos = mNewPositions.get(i);
            if (oldPos == newPos)
                continue;

            Point oldXY = getCoorFromIndex(oldPos);
            Point newXY = getCoorFromIndex(newPos);
            Point oldOffset = new Point(oldXY.x - v.getLeft(), oldXY.y
                    - v.getTop());
            Point newOffset = new Point(newXY.x - v.getLeft(), newXY.y
                    - v.getTop());

            TranslateAnimation translate = new TranslateAnimation(
                    Animation.ABSOLUTE, oldOffset.x, Animation.ABSOLUTE,
                    newOffset.x, Animation.ABSOLUTE, oldOffset.y,
                    Animation.ABSOLUTE, newOffset.y);
            translate.setDuration(sAnimT);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            mNewPositions.set(i, newPos);
        }
    }

    protected void reorderChildren(boolean notify) {
        if (mOnRearrangeListener != null && notify)
            mOnRearrangeListener.onRearrange(mDragged, mLastTarget);
        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).clearAnimation();
            children.add(getChildAt(i));
        }
        removeAllViews();
        while (mDragged != mLastTarget)
            if (mLastTarget == children.size()) // dragged and dropped to the
                                               // right of the last element
            {
                children.add(children.remove(mDragged));
                mDragged = mLastTarget;
            } else if (mDragged < mLastTarget) // shift to the right
            {
                Collections.swap(children, mDragged, mDragged + 1);
                mDragged++;
            } else if (mDragged > mLastTarget) // shift to the left
            {
                Collections.swap(children, mDragged, mDragged - 1);
                mDragged--;
            }
        for (int i = 0; i < children.size(); i++) {
            mNewPositions.set(i, -1);
            addView(children.get(i));
        }
        onLayout(true, getLeft(), getTop(), getRight(), getBottom());
    }

    public void scrollToTop() {
        mScroll = 0;
    }

    public void scrollToBottom() {
        mScroll = Integer.MAX_VALUE;
        clampScroll();
    }

    protected void clampScroll() {
        int max = getMaxScroll();
        max = Math.max(max, 0);
        if (mScroll < 0) {
            if (!mTouching) {
                mScroll -= mScroll;
            } else {
                mScroll = 0;
                mLastDelta = 0;
            }
        } else if (mScroll > max) {
            if (!mTouching) {
                mScroll += (max - mScroll);
            } else {
                mScroll = max;
                mLastDelta = 0;
            }
        }
    }

    protected int getMaxScroll() {
        int rowCount = (int) Math.ceil((double) getChildCount() / mColCount), max = rowCount
                * mChildHeight + (rowCount + 1) * mCellGap - getHeight();
        return max;
    }

    public int getLastIndex() {
        return getIndexFromCoor(mLastX, mLastY);
    }

    public void setOnRearrangeListener(OnRearrangeListener l) {
        this.mOnRearrangeListener = l;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.mOnItemClickListener = l;
    }

    public int getTileTextSize(int column) {
        if (mSystemUiResources == null) {
            return 12;
        }
        // adjust the tile text size based on column count
        switch (column) {
            case 5:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_5_column_text_size", null, null)));
            case 4:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_4_column_text_size", null, null)));
            case 3:
            default:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_3_column_text_size", null, null)));
        }
    }

    public int getTileTextPadding(int column) {
        // get tile text padding based on column count
        switch (column) {
            case 5:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_5_column_text_padding", null, null)));
            case 4:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_4_column_text_padding", null, null)));
            case 3:
            default:
                return (int) (mSystemUiResources.getDimension(mSystemUiResources.getIdentifier(
                        "com.android.systemui:dimen/qs_tile_margin_below_icon", null, null)));
        }
    }

}

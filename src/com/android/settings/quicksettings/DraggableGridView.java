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
package com.android.settings.quicksettings;

import java.util.Collections;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
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

    protected int colCount, childWidth, childHeight, cellGap, scroll = 0;
    protected float lastDelta = 0;
    protected Handler handler = new Handler();
    protected int dragged = -1, lastX = -1, lastY = -1, lastTarget = -1;
    protected boolean enabled = true, touching = false, isDelete = false;
    public static int animT = 150;
    protected ArrayList<Integer> newPositions = new ArrayList<Integer>();
    protected OnRearrangeListener onRearrangeListener;
    protected OnClickListener secondaryOnClickListener;
    private OnItemClickListener onItemClickListener;

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
        setListeners();
        setChildrenDrawingOrderEnabled(true);
        setClipChildren(false);
        setClipToPadding(false);
        colCount = 3;
    }

    public void setColumnCount(int count) {
        colCount = count;
    }

    public void setCellHeight(int height) {
        childHeight = height;
    }

    public void setCellGap(int gap) {
        cellGap = gap;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        secondaryOnClickListener = l;
    }

    protected Runnable updateTask = new Runnable() {
        public void run() {
            if (dragged != -1) {
                if (lastY < cellGap * 3 && scroll > 0)
                    scroll -= 20;
                else if (lastY > getBottom() - getTop() - (cellGap * 3)
                        && scroll < getMaxScroll())
                    scroll += 20;
            } else if (lastDelta != 0 && !touching) {
                scroll += lastDelta;
                lastDelta *= .9;
                if (Math.abs(lastDelta) < .25)
                    lastDelta = 0;
            }
            clampScroll();
            onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            if (lastDelta != 0) {
                handler.postDelayed(this, 25);
            }
        }
    };

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        newPositions.add(-1);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        newPositions.add(-1);
    };

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        newPositions.remove(index);
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
                (colCount - 1) * cellGap);
        childWidth = (int) Math.ceil(((float) availableWidth) / colCount);

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View v = (View) getChildAt(i);
            ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) v.getLayoutParams();
            lp.width = childWidth;
            lp.height = childHeight;
            if (v.getVisibility() != View.GONE) {
                measureChild(v,MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.AT_MOST));
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (dragged == -1)
            return i;
        else if (i == childCount - 1)
            return dragged;
        else if (i >= dragged)
            return i + 1;
        return i;
    }

    public int getIndexFromCoor(int x, int y) {
        int col = getColumnFromCoor(x), row = getRowFromCoor(y + scroll);
        if (col == -1 || row == -1) // touch is between columns or rows
            return -1;
        int index = row * colCount + col;
        if (index >= getChildCount())
            return -1;
        return index;
    }

    protected int getColumnFromCoor(int coor) {
        coor -= cellGap;
        for (int i = 0; coor > 0; i++) {
            if (coor < childWidth)
                return i;
            coor -= (childWidth + cellGap);
        }
        return -1;
    }

    protected int getRowFromCoor(int coor) {
        coor -= cellGap;
        for (int i = 0; coor > 0; i++) {
            if (coor < childHeight)
                return i;
            coor -= (childHeight + cellGap);
        }
        return -1;
    }

    protected int getTargetFromCoor(int x, int y) {
        if (getRowFromCoor(y + scroll) == -1) // touch is between rows
            return -1;

        int leftPos = getIndexFromCoor(x - (childWidth / 4), y);
        int rightPos = getIndexFromCoor(x + (childWidth / 4), y);
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
        if (dragged < target)
            return target - 1;

        return target;
    }

    protected Point getCoorFromIndex(int index) {
        int col = index % colCount;
        int row = index / colCount;
        return new Point(cellGap / 2 + (childWidth + cellGap / 2) * col, cellGap
                / 2 + (childHeight + cellGap / 2) * row - scroll);
    }

    public int getIndexOf(View child) {
        for (int i = 0; i < getChildCount(); i++)
            if (getChildAt(i) == child)
                return i;
        return -1;
    }

    // EVENT HANDLERS
    public void onClick(View view) {
        if (enabled) {
            if (secondaryOnClickListener != null)
                secondaryOnClickListener.onClick(view);
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(null,
                        getChildAt(getLastIndex()), getLastIndex(),
                        getLastIndex() / colCount);
        }
    }

    void toggleAddDelete(boolean delete) {
        int resid = R.drawable.ic_menu_add;
        int stringid = R.string.profiles_add;
        if (delete) {
            resid = R.drawable.ic_menu_delete_holo_dark;
            stringid = R.string.dialog_delete_title;
        }
        TextView addDeleteTile = ((TextView) getChildAt(getChildCount() - 1).findViewById(R.id.tile_textview));
        addDeleteTile.setCompoundDrawablesRelativeWithIntrinsicBounds(0, resid, 0, 0);
        addDeleteTile.setText(stringid);
    }

    public boolean onLongClick(View view) {
        if (!enabled)
            return false;
        int index = getLastIndex();
        if (index != -1 && index != getChildCount() - 1) {
            toggleAddDelete(true);
            dragged = index;
            animateDragged();
            return true;
        }
        return false;
    }

    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            enabled = true;
            lastX = (int) event.getX();
            lastY = (int) event.getY();
            touching = true;
            break;
        case MotionEvent.ACTION_MOVE:
            int delta = lastY - (int) event.getY();
            if (dragged != -1) {
                // change draw location of dragged visual
                int x = (int) event.getX(), y = (int) event.getY();
                int l = x - (3 * childWidth / 4), t = y - (3 * childHeight / 4);
                getChildAt(dragged).layout(l, t, l + (childWidth * 3 / 2),
                        t + (childHeight * 3 / 2));

                // check for new target hover
                int target = getTargetFromCoor(x, y);
                //Check if hovering over delete target
                if (getIndexFromCoor(x, y) == getChildCount() - 1) {
                    getChildAt(dragged).setBackgroundColor(Color.RED);
                    isDelete = true;
                    break;
                } else {
                    isDelete = false;
                    getChildAt(dragged).setBackgroundColor(Color.parseColor("#AA222222"));
                }
                if (lastTarget != target && target != getChildCount() - 1) {
                    if (target != -1) {
                        animateGap(target);
                        lastTarget = target;
                    }
                }
            } else {
                scroll += delta;
                clampScroll();
                if (Math.abs(delta) > 4)
                    enabled = false;
                onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            }
            lastX = (int) event.getX();
            lastY = (int) event.getY();
            lastDelta = delta;
            break;
        case MotionEvent.ACTION_UP:
            if (dragged != -1) {
                toggleAddDelete(false);
                View v = getChildAt(dragged);
                if (lastTarget != -1 && !isDelete)
                    reorderChildren(true);
                else {
                    Point xy = getCoorFromIndex(dragged);
                    v.layout(xy.x, xy.y, xy.x + childWidth, xy.y + childHeight);
                }
                v.clearAnimation();
                if (v instanceof ImageView)
                    ((ImageView) v).setAlpha(255);
                if (isDelete) {
                    lastTarget = dragged;
                    removeViewAt(dragged);
                    onRearrangeListener.onDelete(dragged);
                    reorderChildren(false);
                }
                lastTarget = -1;
                dragged = -1;
            } else {
                handler.post(updateTask);
            }
            touching = false;
            isDelete = false;
            break;
        }
        if (dragged != -1)
            return true;
        return false;
    }

    // EVENT HELPERS
    protected void animateDragged() {
        View v = getChildAt(dragged);
        int x = getCoorFromIndex(dragged).x + childWidth / 2, y = getCoorFromIndex(dragged).y
                + childWidth / 2;
        int l = x - (3 * childWidth / 4), t = y - (3 * childHeight / 4);
        v.layout(l, t, l + (childWidth * 3 / 2), t + (childHeight * 3 / 2));
        AnimationSet animSet = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(.667f, 1, .667f, 1,
                childWidth * 3 / 4, childHeight * 3 / 4);
        scale.setDuration(animT);
        AlphaAnimation alpha = new AlphaAnimation(1, .5f);
        alpha.setDuration(animT);

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
            if (i == dragged)
                continue;
            int newPos = i;
            if (dragged < target && i >= dragged + 1 && i <= target)
                newPos--;
            else if (target < dragged && i >= target && i < dragged)
                newPos++;

            // animate
            int oldPos = i;
            if (newPositions.get(i) != -1)
                oldPos = newPositions.get(i);
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
            translate.setDuration(animT);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            newPositions.set(i, newPos);
        }
    }

    protected void reorderChildren(boolean notify) {
        if (onRearrangeListener != null && notify)
            onRearrangeListener.onRearrange(dragged, lastTarget);
        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).clearAnimation();
            children.add(getChildAt(i));
        }
        removeAllViews();
        while (dragged != lastTarget)
            if (lastTarget == children.size()) // dragged and dropped to the
                                               // right of the last element
            {
                children.add(children.remove(dragged));
                dragged = lastTarget;
            } else if (dragged < lastTarget) // shift to the right
            {
                Collections.swap(children, dragged, dragged + 1);
                dragged++;
            } else if (dragged > lastTarget) // shift to the left
            {
                Collections.swap(children, dragged, dragged - 1);
                dragged--;
            }
        for (int i = 0; i < children.size(); i++) {
            newPositions.set(i, -1);
            addView(children.get(i));
        }
        onLayout(true, getLeft(), getTop(), getRight(), getBottom());
    }

    public void scrollToTop() {
        scroll = 0;
    }

    public void scrollToBottom() {
        scroll = Integer.MAX_VALUE;
        clampScroll();
    }

    protected void clampScroll() {
        int max = getMaxScroll();
        max = Math.max(max, 0);
        if (scroll < 0) {
            if (!touching) {
                scroll -= scroll;
            } else {
                scroll = 0;
                lastDelta = 0;
            }
        } else if (scroll > max) {
            if (!touching) {
                scroll += (max - scroll);
            } else {
                scroll = max;
                lastDelta = 0;
            }
        }
    }

    protected int getMaxScroll() {
        int rowCount = (int) Math.ceil((double) getChildCount() / colCount), max = rowCount
                * childHeight + (rowCount + 1) * cellGap - getHeight();
        return max;
    }

    public int getLastIndex() {
        return getIndexFromCoor(lastX, lastY);
    }

    public void setOnRearrangeListener(OnRearrangeListener l) {
        this.onRearrangeListener = l;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.onItemClickListener = l;
    }
}

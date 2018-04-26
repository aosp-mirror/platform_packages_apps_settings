package com.android.settings.datausage;

import android.annotation.Nullable;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class MeasurableLinearLayout extends LinearLayout {
    private View mFixedView;
    private View mDisposableView;

    public MeasurableLinearLayout(Context context) {
        super(context, null);
    }

    public MeasurableLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public MeasurableLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    public MeasurableLinearLayout(Context context, AttributeSet attrs,
        int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mDisposableView != null && getMeasuredWidth() - mFixedView.getMeasuredWidth()
                < mDisposableView.getMeasuredWidth()) {
            mDisposableView.setVisibility(GONE);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (mDisposableView != null && mDisposableView.getVisibility() != VISIBLE) {
            mDisposableView.setVisibility(VISIBLE);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setChildren(View fixedView, View disposableView) {
        mFixedView = fixedView;
        mDisposableView = disposableView;
    }
}
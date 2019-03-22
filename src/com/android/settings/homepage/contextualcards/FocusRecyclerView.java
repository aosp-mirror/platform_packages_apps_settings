package com.android.settings.homepage.contextualcards;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FocusRecyclerView extends RecyclerView {

    private FocusListener mListener;

    public FocusRecyclerView(Context context) {
        super(context);
    }

    public FocusRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mListener != null) {
            mListener.onWindowFocusChanged(hasWindowFocus);
        }
    }

    public void setListener(FocusListener listener) {
        mListener = listener;
    }

    public interface FocusListener {
        void onWindowFocusChanged(boolean hasWindowFocus);
    }
}

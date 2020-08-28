package com.android.settings.notification.history;

import android.annotation.Nullable;
import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationHistoryRecyclerView extends RecyclerView {

    private static final String TAG = "HistoryRecyclerView";

    private OnItemSwipeDeleteListener listener;

    public NotificationHistoryRecyclerView(Context context) {
        this(context, null);
    }

    public NotificationHistoryRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationHistoryRecyclerView(Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

        setLayoutManager(new LinearLayoutManager(getContext()));
        addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new DismissTouchHelper(0, ItemTouchHelper.START | ItemTouchHelper.END));
        touchHelper.attachToRecyclerView(this);
        setNestedScrollingEnabled(false);
    }

    public void setOnItemSwipeDeleteListener(OnItemSwipeDeleteListener listener) {
        this.listener = listener;
    }

    private class DismissTouchHelper extends ItemTouchHelper.SimpleCallback {

        public DismissTouchHelper(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
            // Do nothing.
            return false;
        }

        @Override
        public void onSwiped(ViewHolder viewHolder, int direction) {
            if (listener != null) {
                listener.onItemSwipeDeleted(viewHolder.getAdapterPosition());
            }
        }
    }

    public interface OnItemSwipeDeleteListener {
        void onItemSwipeDeleted(int position);
    }
}
package com.android.settings.notification.history;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationHistoryRecyclerView extends RecyclerView {

    private static final String TAG = "HistoryRecyclerView";

    private OnItemSwipeDeleteListener listener;

    /** The amount of horizontal displacement caused by user's action, used to track the swiping. */
    private float dXLast;

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

        /** Performs haptic effect once the swiping goes past a certain location. */
        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                @NonNull ViewHolder viewHolder, float dX, float dY, int actionState,
                boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if (isCurrentlyActive) {
                View view = viewHolder.itemView;
                float swipeThreshold = getSwipeThreshold(viewHolder);
                float snapOffset = swipeThreshold * view.getWidth();
                boolean snapIntoNewLocation = dX < -snapOffset || dX > snapOffset;
                boolean snapIntoNewLocationLast = dXLast < -snapOffset || dXLast > snapOffset;
                if (snapIntoNewLocation != snapIntoNewLocationLast) {
                    view.performHapticFeedback(CLOCK_TICK);
                }
                dXLast = dX;
            } else {
                dXLast = 0;
            }
        }
    }

    public interface OnItemSwipeDeleteListener {
        void onItemSwipeDeleted(int position);
    }
}
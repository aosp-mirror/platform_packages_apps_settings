/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.localepicker;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.VisibleForTesting;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.shortcut.ShortcutsUpdateTask;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class LocaleDragAndDropAdapter
        extends RecyclerView.Adapter<LocaleDragAndDropAdapter.CustomViewHolder> {

    private static final String TAG = "LocaleDragAndDropAdapter";
    private static final String CFGKEY_SELECTED_LOCALES = "selectedLocales";
    private static final String CFGKEY_DRAG_LOCALE = "dragLocales";

    private final Context mContext;
    private final ItemTouchHelper mItemTouchHelper;

    private List<LocaleStore.LocaleInfo> mFeedItemList;
    private List<LocaleStore.LocaleInfo> mCacheItemList;
    private RecyclerView mParentView = null;
    private boolean mRemoveMode = false;
    private boolean mDragEnabled = true;
    private NumberFormat mNumberFormatter = NumberFormat.getNumberInstance();
    private LocaleStore.LocaleInfo mDragLocale;

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private final LocaleDragCell mLocaleDragCell;

        public CustomViewHolder(LocaleDragCell view) {
            super(view);
            mLocaleDragCell = view;
            mLocaleDragCell.getDragHandle().setOnTouchListener(this);
        }

        public LocaleDragCell getLocaleDragCell() {
            return mLocaleDragCell;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mDragEnabled) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_DOWN:
                        mItemTouchHelper.startDrag(this);
                }
            }
            return false;
        }
    }

    LocaleDragAndDropAdapter(LocaleListEditor parent, List<LocaleStore.LocaleInfo> feedItemList) {
        mFeedItemList = feedItemList;
        mCacheItemList = new ArrayList<>(feedItemList);
        mContext = parent.getContext();

        final float dragElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                mContext.getResources().getDisplayMetrics());

        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0 /* no swipe */) {

            @Override
            public boolean onMove(RecyclerView view, RecyclerView.ViewHolder source,
                    RecyclerView.ViewHolder target) {
                onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
                // Swipe is disabled, this is intentionally empty.
            }

            private static final int SELECTION_GAINED = 1;
            private static final int SELECTION_LOST = 0;
            private static final int SELECTION_UNCHANGED = -1;
            private int mSelectionStatus = SELECTION_UNCHANGED;

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
                // We change the elevation if selection changed
                if (mSelectionStatus != SELECTION_UNCHANGED) {
                    viewHolder.itemView.setElevation(
                            mSelectionStatus == SELECTION_GAINED ? dragElevation : 0);
                    mSelectionStatus = SELECTION_UNCHANGED;
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    mSelectionStatus = SELECTION_GAINED;
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    mSelectionStatus = SELECTION_LOST;
                }
            }
        });
    }

    public void setRecyclerView(RecyclerView rv) {
        mParentView = rv;
        mItemTouchHelper.attachToRecyclerView(rv);
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final LocaleDragCell item = (LocaleDragCell) LayoutInflater.from(mContext)
                .inflate(R.layout.locale_drag_cell, viewGroup, false);
        return new CustomViewHolder(item);
    }

    @Override
    public void onBindViewHolder(final CustomViewHolder holder, int i) {
        final LocaleStore.LocaleInfo feedItem = mFeedItemList.get(i);
        final LocaleDragCell dragCell = holder.getLocaleDragCell();
        final String label = feedItem.getFullNameNative();
        final String description = feedItem.getFullNameInUiLanguage();

        dragCell.setLabelAndDescription(label, description);
        dragCell.setLocalized(feedItem.isTranslated());
        dragCell.setCurrentDefault(feedItem.getLocale().equals(Locale.getDefault()));
        dragCell.setMiniLabel(mNumberFormatter.format(i + 1));
        dragCell.setShowCheckbox(mRemoveMode);
        dragCell.setShowMiniLabel(!mRemoveMode);
        dragCell.setShowHandle(!mRemoveMode && mDragEnabled);
        dragCell.setTag(feedItem);
        CheckBox checkbox = dragCell.getCheckbox();
        // clear listener before setChecked() in case another item already bind to
        // current ViewHolder and checked event is triggered on stale listener mistakenly.
        checkbox.setOnCheckedChangeListener(null);
        boolean isChecked = mRemoveMode ? feedItem.getChecked() : false;
        checkbox.setChecked(isChecked);
        setCheckBoxDescription(dragCell, checkbox, isChecked);

        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocaleStore.LocaleInfo feedItem =
                        (LocaleStore.LocaleInfo) dragCell.getTag();
                feedItem.setChecked(isChecked);
                setCheckBoxDescription(dragCell, checkbox, isChecked);
            }
        });
    }

    @VisibleForTesting
    protected void setCheckBoxDescription(LocaleDragCell dragCell, CheckBox checkbox,
            boolean isChecked) {
        if (!mRemoveMode) {
            return;
        }
        CharSequence checkedStatus = mContext.getText(
                isChecked ? com.android.internal.R.string.checked
                        : com.android.internal.R.string.not_checked);
        // Talkback
        dragCell.setStateDescription(checkedStatus);
        // Select to Speak
        checkbox.setContentDescription(checkedStatus);
    }

    @Override
    public int getItemCount() {
        int itemCount = (null != mFeedItemList ? mFeedItemList.size() : 0);
        if (itemCount < 2 || mRemoveMode) {
            setDragEnabled(false);
        } else {
            setDragEnabled(true);
        }
        return itemCount;
    }

    void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && toPosition >= 0) {
            final LocaleStore.LocaleInfo saved = mFeedItemList.get(fromPosition);
            mFeedItemList.remove(fromPosition);
            mFeedItemList.add(toPosition, saved);
            mDragLocale = saved;
        } else {
            // TODO: It looks like sometimes the RecycleView tries to swap item -1
            // I did not see it in a while, but if it happens, investigate and file a bug.
            Log.e(TAG, String.format(Locale.US,
                    "Negative position in onItemMove %d -> %d", fromPosition, toPosition));
        }

        if (fromPosition != toPosition) {
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .action(mContext, SettingsEnums.ACTION_REORDER_LANGUAGE);
        }

        notifyItemChanged(fromPosition); // to update the numbers
        notifyItemChanged(toPosition);
        notifyItemMoved(fromPosition, toPosition);
        // We don't call doTheUpdate() here because this method is called for each item swap.
        // So if we drag something across several positions it will be called several times.
    }

    void setRemoveMode(boolean removeMode) {
        mRemoveMode = removeMode;
        int itemCount = mFeedItemList.size();
        for (int i = 0; i < itemCount; i++) {
            mFeedItemList.get(i).setChecked(false);
            notifyItemChanged(i);
        }
    }

    boolean isRemoveMode() {
        return mRemoveMode;
    }

    void removeItem(int position) {
        int itemCount = mFeedItemList.size();
        if (itemCount <= 1) {
            return;
        }
        if (position < 0 || position >= itemCount) {
            return;
        }
        mFeedItemList.remove(position);
        notifyDataSetChanged();
    }

    void removeChecked() {
        int itemCount = mFeedItemList.size();
        LocaleStore.LocaleInfo localeInfo;
        NotificationController controller = NotificationController.getInstance(mContext);
        for (int i = itemCount - 1; i >= 0; i--) {
            localeInfo = mFeedItemList.get(i);
            if (localeInfo.getChecked()) {
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                        .action(mContext, SettingsEnums.ACTION_REMOVE_LANGUAGE);
                mFeedItemList.remove(i);
                controller.removeNotificationInfo(localeInfo.getLocale().toLanguageTag());
            }
        }
        notifyDataSetChanged();
        doTheUpdate();
    }

    int getCheckedCount() {
        int result = 0;
        for (LocaleStore.LocaleInfo li : mFeedItemList) {
            if (li.getChecked()) {
                result++;
            }
        }
        return result;
    }

    boolean isFirstLocaleChecked() {
        return mFeedItemList != null && mFeedItemList.get(0).getChecked();
    }

    void addLocale(LocaleStore.LocaleInfo li) {
        mFeedItemList.add(li);
        notifyItemInserted(mFeedItemList.size() - 1);
        doTheUpdate();
    }

    public void doTheUpdate() {
        int count = mFeedItemList.size();
        final Locale[] newList = new Locale[count];

        for (int i = 0; i < count; i++) {
            final LocaleStore.LocaleInfo li = mFeedItemList.get(i);
            newList[i] = li.getLocale();
        }

        final LocaleList ll = new LocaleList(newList);
        updateLocalesWhenAnimationStops(ll);
    }

    private LocaleList mLocalesToSetNext = null;
    private LocaleList mLocalesSetLast = null;

    public void updateLocalesWhenAnimationStops(final LocaleList localeList) {
        if (localeList.equals(mLocalesToSetNext)) {
            return;
        }

        // This will only update the Settings application to make things feel more responsive,
        // the system will be updated later, when animation stopped.
        LocaleList.setDefault(localeList);

        mLocalesToSetNext = localeList;
        final RecyclerView.ItemAnimator itemAnimator = mParentView.getItemAnimator();
        itemAnimator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
            @Override
            public void onAnimationsFinished() {
                if (mLocalesToSetNext == null || mLocalesToSetNext.equals(mLocalesSetLast)) {
                    // All animations finished, but the locale list did not change
                    return;
                }

                LocalePicker.updateLocales(mLocalesToSetNext);
                mLocalesSetLast = mLocalesToSetNext;
                new ShortcutsUpdateTask(mContext).execute();

                mLocalesToSetNext = null;

                mNumberFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
            }
        });
    }

    public void notifyListChanged(LocaleStore.LocaleInfo localeInfo) {
        if (!localeInfo.getLocale().equals(mCacheItemList.get(0).getLocale())) {
            mFeedItemList = new ArrayList<>(mCacheItemList);
            notifyDataSetChanged();
        }
    }

    public void setCacheItemList() {
        mCacheItemList = new ArrayList<>(mFeedItemList);
    }

    public List<LocaleStore.LocaleInfo> getFeedItemList() {
        return mFeedItemList;
    }
    private void setDragEnabled(boolean enabled) {
        mDragEnabled = enabled;
    }

    /**
     * Saves the list of checked locales to preserve status when the list is destroyed.
     * (for instance when the device is rotated)
     *
     * @param outInstanceState Bundle in which to place the saved state
     */
    public void saveState(Bundle outInstanceState) {
        if (outInstanceState != null) {
            final ArrayList<String> selectedLocales = new ArrayList<>();
            for (LocaleStore.LocaleInfo li : mFeedItemList) {
                if (li.getChecked()) {
                    selectedLocales.add(li.getId());
                }
            }
            outInstanceState.putStringArrayList(CFGKEY_SELECTED_LOCALES, selectedLocales);
            // Save the dragged locale before rotation
            outInstanceState.putSerializable(CFGKEY_DRAG_LOCALE, mDragLocale);
        }
    }

    /**
     * Restores the list of checked locales to preserve status when the list is recreated.
     * (for instance when the device is rotated)
     *
     * @param savedInstanceState Bundle with the data saved by {@link #saveState(Bundle)}
     * @param isDialogShowing A flag indicating whether the dialog is showing or not.
     */
    public void restoreState(Bundle savedInstanceState, boolean isDialogShowing) {
        if (savedInstanceState != null) {
            if (mRemoveMode) {
                final ArrayList<String> selectedLocales =
                        savedInstanceState.getStringArrayList(CFGKEY_SELECTED_LOCALES);
                if (selectedLocales == null || selectedLocales.isEmpty()) {
                    return;
                }
                for (LocaleStore.LocaleInfo li : mFeedItemList) {
                    li.setChecked(selectedLocales.contains(li.getId()));
                }
                notifyItemRangeChanged(0, mFeedItemList.size());
            } else if (isDialogShowing) {
                // After rotation, the dragged position will be restored to original. Restore the
                // drag locale's original position to the top.
                mDragLocale = (LocaleStore.LocaleInfo) savedInstanceState.getSerializable(
                        CFGKEY_DRAG_LOCALE);
                if (mDragLocale != null) {
                    mFeedItemList.removeIf(
                            localeInfo -> TextUtils.equals(localeInfo.getId(),
                                    mDragLocale.getId()));
                    mFeedItemList.add(0, mDragLocale);
                    notifyItemRangeChanged(0, mFeedItemList.size());
                }
            }
        }
    }
}

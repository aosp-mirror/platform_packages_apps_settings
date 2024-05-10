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

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

/**
 * Add accessibility actions to the drag-and-drop locale list
 *
 * <p>Dragging is not supported neither by TalkBack or the accessibility
 * framework at the moment. So we need custom actions to be able
 * to change the order of the locales.</p>
 *
 * <p>Also, the remove functionality is difficult to discover and use
 * with TalkBack only, so we are also adding a "remove" action.</p>
 *
 * <p>It only removes one locale at the time, but most users don't
 * really add many locales "by mistake", so there is no real need
 * to delete a lot of locales at once.</p>
 */
public class LocaleLinearLayoutManager extends LinearLayoutManager {
    private final LocaleDragAndDropAdapter mAdapter;
    private final Context mContext;
    private LocaleListEditor mLocaleListEditor;

    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveUp;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveDown;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveTop;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveBottom;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionRemove;

    public LocaleLinearLayoutManager(Context context, LocaleDragAndDropAdapter adapter) {
        super(context);
        this.mContext = context;
        this.mAdapter = adapter;

        this.mActionMoveUp = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_drag_move_up,
                mContext.getString(R.string.action_drag_label_move_up));
        this.mActionMoveDown = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_drag_move_down,
                mContext.getString(R.string.action_drag_label_move_down));
        this.mActionMoveTop = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_drag_move_top,
                mContext.getString(R.string.action_drag_label_move_top));
        this.mActionMoveBottom = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_drag_move_bottom,
                mContext.getString(R.string.action_drag_label_move_bottom));
        this.mActionRemove = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                R.id.action_drag_remove,
                mContext.getString(R.string.action_drag_label_remove));
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler,
            RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {

        super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);

        final int itemCount = this.getItemCount();
        final int position = this.getPosition(host);
        final LocaleDragCell dragCell = (LocaleDragCell) host;

        // We want the description to be something not localizable, so that any TTS engine for
        // any language can handle it. And we want the position to be part of it.
        // So we use something like "2, French (France)"
        final String description =
                (position + 1) + ", " + dragCell.getLabelView().getContentDescription();
        info.setContentDescription(description);

        if (mAdapter.isRemoveMode()) { // We don't move things around in remove mode
            return;
        }

        // The order in which we add the actions is important for the circular selection menu.
        // With the current order the "up" action will be (more or less) up, and "down" more
        // or less down ("more or less" because we have 5 actions)
        if (position > 0) { // it is not the first one
            info.addAction(mActionMoveUp);
            info.addAction(mActionMoveTop);
        }
        if (position + 1 < itemCount) { // it is not the last one
            info.addAction(mActionMoveDown);
            info.addAction(mActionMoveBottom);
        }
        if (itemCount > 1) {
            info.addAction(mActionRemove);
        }
    }

    @Override
    public boolean performAccessibilityActionForItem(RecyclerView.Recycler recycler,
            RecyclerView.State state, View host, int action, Bundle args) {

        final int itemCount = this.getItemCount();
        final int position = this.getPosition(host);
        boolean result = false;

        if (action == R.id.action_drag_move_up) {
            if (position > 0) {
                mAdapter.onItemMove(position, position - 1);
                result = true;
            }
        } else if (action == R.id.action_drag_move_down) {
            if (position + 1 < itemCount) {
                mAdapter.onItemMove(position, position + 1);
                result = true;
            }
        } else if (action == R.id.action_drag_move_top) {
            if (position != 0) {
                mAdapter.onItemMove(position, 0);
                result = true;
            }
        } else if (action == R.id.action_drag_move_bottom) {
            if (position != itemCount - 1) {
                mAdapter.onItemMove(position, itemCount - 1);
                result = true;
            }
        } else if (action == R.id.action_drag_remove) {
            if (itemCount > 1) {
                mAdapter.removeItem(position);
                result = true;
            }
        } else {
            return super.performAccessibilityActionForItem(recycler, state, host, action, args);
        }

        if (result) {
            mLocaleListEditor.showConfirmDialog(false, mAdapter.getFeedItemList().get(0));
        }
        return result;
    }

    public void setLocaleListEditor(LocaleListEditor localeListEditor) {
        mLocaleListEditor = localeListEditor;
    }
}

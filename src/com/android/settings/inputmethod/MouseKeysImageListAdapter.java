/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MouseKeysImageListAdapter extends
        RecyclerView.Adapter<MouseKeysImageListAdapter.MouseKeyImageViewHolder> {
    private static final ImmutableList<Integer> DRAWABLE_LIST = ImmutableList.of(
            R.drawable.mouse_keys_directional, R.drawable.mouse_keys_click,
            R.drawable.mouse_keys_press_hold, R.drawable.mouse_keys_release,
            R.drawable.mouse_keys_toggle_scroll, R.drawable.mouse_keys_release2);
    private static final ImmutableList<Integer> DIRECTIONAL_CHAR_KEYCODE_LIST = ImmutableList.of(
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L
    );
    private static final int LEFT_CLICK_CHAR_KEYCODE =
            KeyEvent.KEYCODE_I;
    private static final int PRESS_HOLD_CHAR_KEYCODE =
            KeyEvent.KEYCODE_M;
    private static final int RELEASE_CHAR_KEYCODE =
            KeyEvent.KEYCODE_COMMA;
    private static final ImmutableList<Integer> TOGGLE_SCROLL_CHAR_KEYCODE_LIST = ImmutableList.of(
            KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_U
    );
    private static final int RIGHT_CLICK_CHAR_KEYCODE =
            KeyEvent.KEYCODE_SLASH;
    private final List<String> mComposedSummaryList = new ArrayList<>();

    public MouseKeysImageListAdapter(@NonNull Context context,
            @Nullable InputDevice currentInputDevice) {
        composeSummaryForImages(context, currentInputDevice);
    }

    @NonNull
    @Override
    public MouseKeyImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mouse_keys_image_item, parent, false);
        return new MouseKeyImageViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull MouseKeyImageViewHolder holder, int position) {
        ((MouseKeyImageViewHolder) holder).bindView(DRAWABLE_LIST.get(position),
                mComposedSummaryList.get(position));
    }

    @Override
    public int getItemCount() {
        return DRAWABLE_LIST.size();
    }

    private void composeSummaryForImages(Context context,
            @Nullable InputDevice currentInputDevice) {
        if (currentInputDevice == null) {
            return;
        }
        mComposedSummaryList.clear();
        List<String> directionalLabelList = DIRECTIONAL_CHAR_KEYCODE_LIST.stream().map(
                (key) -> getDisplayLabel(currentInputDevice, key)).toList();
        mComposedSummaryList.add(context.getString(R.string.mouse_keys_directional_summary,
                String.join(",", directionalLabelList)));
        String leftClickLabel = getDisplayLabel(currentInputDevice, LEFT_CLICK_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_click_summary, leftClickLabel));
        String pressHoldLabel = getDisplayLabel(currentInputDevice, PRESS_HOLD_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_press_hold_summary, pressHoldLabel));
        String releaseLabel = getDisplayLabel(currentInputDevice, RELEASE_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_release_summary, releaseLabel));
        List<String> toggleScrollLabelList = TOGGLE_SCROLL_CHAR_KEYCODE_LIST.stream().map(
                (key) -> getDisplayLabel(currentInputDevice, key)).toList();
        mComposedSummaryList.add(context.getString(R.string.mouse_keys_toggle_scroll_summary,
                toggleScrollLabelList.getFirst(),
                String.join(",", toggleScrollLabelList.subList(1, toggleScrollLabelList.size()))
        ));
        String rightClickLabel = getDisplayLabel(currentInputDevice, RIGHT_CLICK_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_release2_summary, rightClickLabel));
    }

    private String getDisplayLabel(InputDevice currentInputDevice, int keycode) {
        return String.valueOf(currentInputDevice.getKeyCharacterMap().getDisplayLabel(
                currentInputDevice.getKeyCodeForKeyLocation(keycode))).toLowerCase(Locale.ROOT);
    }

    public static class MouseKeyImageViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final Context mContext;

        public MouseKeyImageViewHolder(View itemView, Context context) {
            super(itemView);
            mTextView = (TextView) itemView;
            mContext = context;
        }

        void bindView(int drawableRes, String summary) {
            mTextView.setText(summary);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(null,
                    mContext.getDrawable(drawableRes), null, null);
        }
    }
}

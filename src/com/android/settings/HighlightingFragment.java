/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

public class HighlightingFragment extends Fragment {

    private static final String TAG = "HighlightSettingsFragment";

    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 400;
    private static final String SAVE_HIGHLIGHTED_KEY = "android:view_highlighted";

    private String mViewKey;
    private boolean mViewHighlighted = false;
    private Drawable mHighlightDrawable;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mViewHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mViewHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mViewKey = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
            highlightViewIfNeeded();
        }
    }

    public void highlightViewIfNeeded() {
        if (!mViewHighlighted &&!TextUtils.isEmpty(mViewKey)) {
            highlightView(mViewKey);
        }
    }

    private Drawable getHighlightDrawable() {
        if (mHighlightDrawable == null) {
            mHighlightDrawable = getActivity().getDrawable(R.drawable.preference_highlight);
        }
        return mHighlightDrawable;
    }

    private void highlightView(String key) {
        final Drawable highlight = getHighlightDrawable();

        // Try locating the View thru its Tag / Key
        final View view = findViewForKey(getView(), key);
        if (view != null ) {
            view.setBackground(highlight);

            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final int centerX = view.getWidth() / 2;
                    final int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setPressed(true);
                    view.setPressed(false);
                }
            }, DELAY_HIGHLIGHT_DURATION_MILLIS);

            mViewHighlighted = true;
        }
    }

    private View findViewForKey(View root, String key) {
        if (checkTag(root, key)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            final int count = group.getChildCount();
            for (int n = 0; n < count; n++) {
                final View child = group.getChildAt(n);
                final View view = findViewForKey(child, key);
                if (view != null) {
                    return view;
                }
            }
        }
        return null;
    }

    private boolean checkTag(View view, String key) {
        final Object tag = view.getTag(R.id.preference_highlight_key);
        if (tag == null || !(tag instanceof String)) {
            return false;
        }
        final String viewKey = (String) tag;
        return (!TextUtils.isEmpty(viewKey) && viewKey.equals(key));
    }
}

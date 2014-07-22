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

import android.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;

/**
 * A ListFragment with a pinned header
 */
public class PinnedHeaderListFragment extends ListFragment {

    public PinnedHeaderListFragment() {
        super();
    }

    /**
     * Set the pinned header view. This can only be done when the ListView is already created.
     *
     * @param pinnedHeaderView the view to be used for the pinned header view.
     */
    public void setPinnedHeaderView(View pinnedHeaderView) {
        ((ViewGroup) getListView().getParent()).addView(pinnedHeaderView, 0);
    }

    /**
     * Clear the pinned header view. This can only be done when the ListView is already created.
     */
    public void clearPinnedHeaderView() {
        ((ViewGroup) getListView().getParent()).removeViewAt(0);
    }
}

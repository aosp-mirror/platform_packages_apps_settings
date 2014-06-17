/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

/**
 * A category with a progress spinner
 */
public class ProgressCategory extends ProgressCategoryBase {

    private int mEmptyTextRes;
    private boolean mProgress = false;
    private Preference mNoDeviceFoundPreference;
    private boolean mNoDeviceFoundAdded;

    public ProgressCategory(Context context) {
        this(context, null);
    }

    public ProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ProgressCategory(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ProgressCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_progress_category);
    }

    public void setEmptyTextRes(int emptyTextRes) {
        mEmptyTextRes = emptyTextRes;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        final View progressBar = view.findViewById(R.id.scanning_progress);

        boolean noDeviceFound = (getPreferenceCount() == 0 ||
                (getPreferenceCount() == 1 && getPreference(0) == mNoDeviceFoundPreference));
        progressBar.setVisibility(mProgress ? View.VISIBLE : View.GONE);

        if (mProgress || !noDeviceFound) {
            if (mNoDeviceFoundAdded) {
                removePreference(mNoDeviceFoundPreference);
                mNoDeviceFoundAdded = false;
            }
        } else {
            if (!mNoDeviceFoundAdded) {
                if (mNoDeviceFoundPreference == null) {
                    mNoDeviceFoundPreference = new Preference(getContext());
                    mNoDeviceFoundPreference.setLayoutResource(R.layout.preference_empty_list);
                    mNoDeviceFoundPreference.setTitle(mEmptyTextRes);
                    mNoDeviceFoundPreference.setSelectable(false);
                }
                addPreference(mNoDeviceFoundPreference);
                mNoDeviceFoundAdded = true;
            }
        }
    }

    @Override
    public void setProgress(boolean progressOn) {
        mProgress = progressOn;
        notifyChanged();
    }
}

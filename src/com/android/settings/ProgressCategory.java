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
import android.util.AttributeSet;
import android.view.View;

public class ProgressCategory extends ProgressCategoryBase {

    private boolean mProgress = false;

    public ProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_progress_category);
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        final View textView = view.findViewById(R.id.scanning_text);
        final View progressBar = view.findViewById(R.id.scanning_progress);

        final int visibility = mProgress ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    @Override
    public void setProgress(boolean progressOn) {
        mProgress = progressOn;
        notifyChanged();
    }
}


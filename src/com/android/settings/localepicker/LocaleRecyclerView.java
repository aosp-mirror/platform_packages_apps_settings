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
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

class LocaleRecyclerView extends RecyclerView {
    public LocaleRecyclerView(Context context) {
        super(context);
    }

    public LocaleRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocaleRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            LocaleDragAndDropAdapter adapter = (LocaleDragAndDropAdapter) this.getAdapter();
            if (adapter != null) {
                adapter.doTheUpdate();
            }
        }
        return super.onTouchEvent(e);
    }
}

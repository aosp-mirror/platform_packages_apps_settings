/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.widget;

import android.annotation.Nullable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public abstract class EmptyTextSettings extends SettingsPreferenceFragment {

    private TextView mEmpty;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmpty = new TextView(getContext());
        mEmpty.setGravity(Gravity.CENTER);
        final int textPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.empty_text_padding);
        mEmpty.setPadding(textPadding, 0 /* top */, textPadding, 0 /* bottom */);
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, value, true);
        mEmpty.setTextAppearance(value.resourceId);
        ((ViewGroup) view.findViewById(android.R.id.list_container)).addView(mEmpty,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setEmptyView(mEmpty);
    }

    protected void setEmptyText(int text) {
        mEmpty.setText(text);
    }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

/**
 * ViewHolder for Settings represented as SwitchPreferences.
 */
public class InlineSwitchViewHolder extends SearchViewHolder {

    public final Switch switchView;

    private final Context mContext;

    public InlineSwitchViewHolder(View view, Context context) {
        super(view);
        mContext = context;
        switchView = view.findViewById(R.id.switchView);
    }

    @Override
    public int getClickActionMetricName() {
        return MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_INLINE_RESULT;
    }

    @Override
    public void onBind(SearchFragment fragment, final SearchResult result) {
        super.onBind(fragment, result);
        if (mContext == null) {
            return;
        }
        final InlineSwitchPayload payload = (InlineSwitchPayload) result.payload;
        switchView.setChecked(payload.getValue(mContext) == InlineSwitchPayload.TRUE);
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final Pair<Integer, Object> value = Pair.create(
                    MetricsEvent.FIELD_SETTINGS_SEARCH_INLINE_RESULT_VALUE, isChecked
                            ? 1L : 0L);
            fragment.onSearchResultClicked(this, result, value);
            int newValue = isChecked ? InlineSwitchPayload.TRUE : InlineSwitchPayload.FALSE;
            payload.setValue(mContext, newValue);
        });
    }
}

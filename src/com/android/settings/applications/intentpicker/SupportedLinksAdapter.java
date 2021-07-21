/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.intentpicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import com.android.settings.R;

import java.util.List;

/** This adapter is for supported links dialog. */
public class SupportedLinksAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<SupportedLinkWrapper> mWrapperList;

    public SupportedLinksAdapter(Context context, List<SupportedLinkWrapper> list) {
        mContext = context;
        mWrapperList = list;
    }

    @Override
    public int getCount() {
        return mWrapperList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < mWrapperList.size()) {
            return mWrapperList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.supported_links_dialog_item, /* root= */ null);
        }
        final CheckedTextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(mWrapperList.get(position).getDisplayTitle(mContext));
        textView.setEnabled(mWrapperList.get(position).isEnabled());
        textView.setChecked(mWrapperList.get(position).isChecked());
        textView.setOnClickListener(l -> {
            textView.toggle();
            mWrapperList.get(position).setChecked(textView.isChecked());
        });
        return convertView;
    }
}

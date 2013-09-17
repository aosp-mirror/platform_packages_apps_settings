/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

/**
 * Extends ListPreference to allow us to show the icons for the available SMS applications. We do
 * this because the names of SMS applications are very similar and the user may not be able to
 * determine what app they are selecting without an icon.
 */
public class SmsListPreference extends ListPreference {
    private Drawable[] mEntryDrawables;

    public class SmsArrayAdapter extends ArrayAdapter<CharSequence> {
        private Drawable[] mImageDrawables = null;
        private int mSelectedIndex = 0;

        public SmsArrayAdapter(Context context, int textViewResourceId,
                CharSequence[] objects, Drawable[] imageDrawables, int selectedIndex) {
            super(context, textViewResourceId, objects);
            mSelectedIndex = selectedIndex;
            mImageDrawables = imageDrawables;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
            View view = inflater.inflate(R.layout.sms_preference_item, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView)view.findViewById(R.id.sms_text);
            checkedTextView.setText(getItem(position));
            if (position == mSelectedIndex) {
                checkedTextView.setChecked(true);
            }
            ImageView imageView = (ImageView)view.findViewById(R.id.sms_image);
            imageView.setImageDrawable(mImageDrawables[position]);
            return view;
        }
    }

    public SmsListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEntryDrawables(Drawable[] entries) {
        mEntryDrawables = entries;
    }

    public Drawable[] getEntryDrawables() {
        return mEntryDrawables;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        int selectedIndex = findIndexOfValue(getValue());
        ListAdapter adapter = new SmsArrayAdapter(getContext(),
            R.layout.sms_preference_item, getEntries(), mEntryDrawables, selectedIndex);
        builder.setAdapter(adapter, this);
        super.onPrepareDialogBuilder(builder);
    }
}
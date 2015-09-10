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

package com.android.settings.applications;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference;

public class AppDomainsPreference extends ListDialogPreference {
    private int mNumEntries;

    public AppDomainsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.app_domains_dialog);
        setListItemLayoutResource(R.layout.app_domains_item);
    }

    @Override
    public void setTitles(CharSequence[] titles) {
        mNumEntries = (titles != null) ? titles.length : 0;
        super.setTitles(titles);
    }

    @Override
    public CharSequence getSummary() {
        final Context context = getContext();
        if (mNumEntries == 0) {
            return context.getString(R.string.domain_urls_summary_none);
        }

        // The superclass summary is the text of the first entry in the list
        final CharSequence summary = super.getSummary();
        final int whichVersion = (mNumEntries == 1)
                ? R.string.domain_urls_summary_one
                : R.string.domain_urls_summary_some;
        return context.getString(whichVersion, summary);
    }

    @Override
    protected void onBindListItem(View view, int index) {
        final CharSequence title = getTitleAt(index);
        if (title != null) {
            final TextView domainName = (TextView) view.findViewById(R.id.domain_name);
            domainName.setText(title);
        }
    }
}

/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ProgressBar;

import com.android.settings.R;

public class StorageItemPreference extends Preference {
    public final int userHandle;

    private ProgressBar progressBar;
    private static final int PROGRESS_MAX = 100;
    private int progress = -1;

    public StorageItemPreference(Context context, int titleRes) {
        this(context, context.getText(titleRes), UserHandle.USER_NULL);
        setLayoutResource(R.layout.storage_item);
    }

    public StorageItemPreference(Context context, CharSequence title, int userHandle) {
        super(context);

        setTitle(title);
        setSummary(R.string.memory_calculating_size);

        this.userHandle = userHandle;
    }

    public void setLoading() {
        setSummary(R.string.memory_calculating_size);
    }

    public void setStorageSize(long size, long total) {
        setSummary(Formatter.formatFileSize(getContext(), size));
        progress = (int)(size * PROGRESS_MAX / total);
        updateProgressBar();
    }

    protected void updateProgressBar() {
        if (progressBar == null)
            return;

        if (progress == -1) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(PROGRESS_MAX);
        progressBar.setProgress(progress);
    }

    @Override
    protected void onBindView(View view) {
        progressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        updateProgressBar();
        super.onBindView(view);
    }
}

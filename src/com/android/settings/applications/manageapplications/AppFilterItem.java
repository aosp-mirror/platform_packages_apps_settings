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
 */

package com.android.settings.applications.manageapplications;

import androidx.annotation.StringRes;

import com.android.settingslib.applications.ApplicationsState;

import java.util.Objects;

/**
 * Data model for a displayable app filter.
 */
public class AppFilterItem implements Comparable<AppFilterItem> {

    @StringRes
    private final int mTitle;
    @AppFilterRegistry.FilterType
    private final int mFilterType;
    private final ApplicationsState.AppFilter mFilter;

    public AppFilterItem(ApplicationsState.AppFilter filter,
            @AppFilterRegistry.FilterType int filterType,
            @StringRes int title) {
        mTitle = title;
        mFilterType = filterType;
        mFilter = filter;
    }

    public int getTitle() {
        return mTitle;
    }

    public ApplicationsState.AppFilter getFilter() {
        return mFilter;
    }

    public int getFilterType() {
        return mFilterType;
    }

    @Override
    public int compareTo(AppFilterItem appFilter) {
        if (appFilter == null) {
            return 1;
        }
        if (this == appFilter) {
            return 0;
        }
        return mFilterType - appFilter.mFilterType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AppFilterItem)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        final AppFilterItem other = (AppFilterItem) o;
        return mTitle == other.mTitle
                && mFilterType == other.mFilterType
                && mFilter == other.mFilter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFilter, mTitle, mFilterType);
    }
}

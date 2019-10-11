/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.ui.search;

import android.text.TextUtils;

import java.util.Objects;


/**
 * Data class for {@link SettingsSearchResultRegressionTest}
 */
public class SearchData {
    public final String title;
    public final String key;

    public String getTitle() {
        return title;
    }

    public String getKey() {
        return key;
    }

    public static final String DELIM = ";";

    public static SearchData from(String searchDataString) {
        String[] split = searchDataString.trim().split(DELIM, -1);

        if (split.length != 2) {
            throw new IllegalArgumentException("Arg is invalid: " + searchDataString);
        }

        return new SearchData.Builder()
                .setTitle(split[0])
                .setKey(split[1])
                .build();
    }

    @Override
    public String toString() {
        return title + DELIM + key;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SearchData)) {
            return false;
        }

        SearchData other = (SearchData) obj;
        return TextUtils.equals(this.title, other.title)
                && TextUtils.equals(this.key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, key);
    }

    private SearchData(
            SearchData.Builder builder) {
        this.title = builder.title;
        this.key = builder.key;
    }

    public static class Builder {
        protected String title = "";
        protected String key = "";

        public SearchData build() {
            return new SearchData(this);
        }

        public SearchData.Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public SearchData.Builder setKey(String key) {
            this.key = key;
            return this;
        }
    }
}

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
 * limitations under the License
 */

package com.android.settings.slices;

import android.net.Uri;
import android.text.TextUtils;

/**
 * TODO (b/67996923) Add SlicesIndexingManager
 * Data class representing a slice stored by {@link SlicesIndexingManager}.
 * Note that {@link #key} is treated as a primary key for this class and determines equality.
 */
public class SliceData {

    private final String key;

    private final String title;

    private final String summary;

    private final String screenTitle;

    private final int iconResource;

    private final String fragmentClassName;

    private final Uri uri;

    private final String preferenceController;

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getScreenTitle() {
        return screenTitle;
    }

    public int getIconResource() {
        return iconResource;
    }

    public String getFragmentClassName() {
        return fragmentClassName;
    }

    public Uri getUri() {
        return uri;
    }

    public String getPreferenceController() {
        return preferenceController;
    }

    private SliceData(Builder builder) {
        key = builder.mKey;
        title = builder.mTitle;
        summary = builder.mSummary;
        screenTitle = builder.mScreenTitle;
        iconResource = builder.mIconResource;
        fragmentClassName = builder.mFragmentClassName;
        uri = builder.mUri;
        preferenceController = builder.mPrefControllerClassName;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SliceData)) {
            return false;
        }
        SliceData newObject = (SliceData) obj;
        return TextUtils.equals(key, newObject.key);
    }

    static class Builder {
        private String mKey;

        private String mTitle;

        private String mSummary;

        private String mScreenTitle;

        private int mIconResource;

        private String mFragmentClassName;

        private Uri mUri;

        private String mPrefControllerClassName;

        public Builder setKey(String key) {
            mKey = key;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setSummary(String summary) {
            mSummary = summary;
            return this;
        }

        public Builder setScreenTitle(String screenTitle) {
            mScreenTitle = screenTitle;
            return this;
        }

        public Builder setIcon(int iconResource) {
            mIconResource = iconResource;
            return this;
        }

        public Builder setPreferenceControllerClassName(String controllerClassName) {
            mPrefControllerClassName = controllerClassName;
            return this;
        }

        public Builder setFragmentName(String fragmentClassName) {
            mFragmentClassName = fragmentClassName;
            return this;
        }

        public Builder setUri(Uri uri) {
            mUri = uri;
            return this;
        }

        public SliceData build() {
            if (TextUtils.isEmpty(mKey)) {
                throw new IllegalStateException("Key cannot be empty");
            }

            if (TextUtils.isEmpty(mTitle)) {
                throw new IllegalStateException("Title cannot be empty");
            }

            if (TextUtils.isEmpty(mFragmentClassName)) {
                throw new IllegalStateException("Fragment Name cannot be empty");
            }

            if (TextUtils.isEmpty(mPrefControllerClassName)) {
                throw new IllegalStateException("Preference Controller cannot be empty");
            }

            if (mUri == null) {
                throw new IllegalStateException("Uri cannot be null");
            }

            return new SliceData(this);
        }

        public String getKey() {
            return mKey;
        }
    }

}
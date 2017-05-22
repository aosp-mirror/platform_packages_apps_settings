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

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

/**
 * {@link ResultPayload} for saved query.
 */
public class SavedQueryPayload extends ResultPayload {

    public final String query;

    public SavedQueryPayload(String query) {
        super(null /* Intent */);
        this.query = query;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    SavedQueryPayload(Parcel in) {
        super(null /* Intent */);
        query = in.readString();
    }

    @Override
    public int getType() {
        return PayloadType.SAVED_QUERY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
    }

    public static final Creator<SavedQueryPayload> CREATOR = new Creator<SavedQueryPayload>() {
        @Override
        public SavedQueryPayload createFromParcel(Parcel in) {
            return new SavedQueryPayload(in);
        }

        @Override
        public SavedQueryPayload[] newArray(int size) {
            return new SavedQueryPayload[size];
        }
    };
}

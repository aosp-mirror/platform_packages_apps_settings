/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

import android.content.Intent;
import android.os.Parcel;

/**
 * Encapsulates the standard intent based results as seen in first party apps and Settings results.
 */
public class IntentPayload extends ResultPayload {
    public final Intent intent;

    private IntentPayload(Parcel in) {
        intent = in.readParcelable(IntentPayload.class.getClassLoader());
    }

    public IntentPayload(Intent newIntent) {
        intent = newIntent;
    }

    @ResultPayload.PayloadType public int getType() {
        return PayloadType.INTENT;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(intent, flags);
    }

    public static final Creator<IntentPayload> CREATOR = new Creator<IntentPayload>() {
        @Override
        public IntentPayload createFromParcel(Parcel in) {
            return new IntentPayload(in);
        }

        @Override
        public IntentPayload[] newArray(int size) {
            return new IntentPayload[size];
        }
    };

}
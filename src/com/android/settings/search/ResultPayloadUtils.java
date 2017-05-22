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
import android.os.Parcelable;

/**
 * Utility class to Marshall and Unmarshall the payloads stored in the SQLite Database
 */
public class ResultPayloadUtils {

    private static final String TAG = "PayloadUtil";

    public static byte[] marshall(ResultPayload payload) {
        Parcel parcel = Parcel.obtain();
        payload.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        T result;
        Parcel parcel = unmarshall(bytes);
        result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }

    private static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return parcel;
    }
}

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

package com.android.settings.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.text.ParseException;

/**
 * Data model for a support phone number.
 */
public final class SupportPhone implements Parcelable {

    public final String language;
    public final String number;
    public final boolean isTollFree;

    public SupportPhone(String config) throws ParseException {
        // Config follows this format: language:[tollfree|tolled]:number
        final String[] tokens = config.split(":");
        if (tokens.length != 3) {
            throw new ParseException("Phone config is invalid " + config, 0);
        }
        language = tokens[0];
        isTollFree = TextUtils.equals(tokens[1], "tollfree");
        number = tokens[2];
    }

    protected SupportPhone(Parcel in) {
        language = in.readString();
        number = in.readString();
        isTollFree = in.readInt() != 0;
    }

    public Intent getDialIntent() {
        return new Intent(Intent.ACTION_DIAL)
                .setData(new Uri.Builder()
                        .scheme("tel")
                        .appendPath(number)
                        .build());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(language);
        dest.writeString(number);
        dest.writeInt(isTollFree ? 1 : 0);
    }

    public static final Creator<SupportPhone> CREATOR = new Creator<SupportPhone>() {
        @Override
        public SupportPhone createFromParcel(Parcel in) {
            return new SupportPhone(in);
        }

        @Override
        public SupportPhone[] newArray(int size) {
            return new SupportPhone[size];
        }
    };
}

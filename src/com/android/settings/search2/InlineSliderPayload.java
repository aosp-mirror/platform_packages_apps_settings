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

import android.net.Uri;
import android.os.Parcel;

/**
 * Payload for Inline Settings results represented by a Slider.
 */
public class InlineSliderPayload extends ResultPayload {
    public final Uri uri;

    private InlineSliderPayload(Parcel in) {
        uri = in.readParcelable(InlineSliderPayload.class.getClassLoader());
    }

    public InlineSliderPayload(Uri newUri) {
        uri = newUri;
    }

    @Override
    public int getType() {
        return PayloadType.INLINE_SLIDER;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
    }

    public static final Creator<InlineSliderPayload> CREATOR = new Creator<InlineSliderPayload>() {
        @Override
        public InlineSliderPayload createFromParcel(Parcel in) {
            return new InlineSliderPayload(in);
        }

        @Override
        public InlineSliderPayload[] newArray(int size) {
            return new InlineSliderPayload[size];
        }
    };
}
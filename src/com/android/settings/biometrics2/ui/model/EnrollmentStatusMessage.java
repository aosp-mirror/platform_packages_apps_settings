/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Enrolling status message (help or error)
 */
public final class EnrollmentStatusMessage {

    private final int mMsgId;
    @NonNull private final CharSequence mStr;

    public EnrollmentStatusMessage(int msgId, @Nullable CharSequence str) {
        mMsgId = msgId;
        mStr = str != null ? str : "";
    }

    public int getMsgId() {
        return mMsgId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + "{id:" + mMsgId + ", str:" + mStr + "}";
    }

    /**
     * Gets status string
     */
    @NonNull
    public CharSequence getStr() {
        return mStr;
    }
}

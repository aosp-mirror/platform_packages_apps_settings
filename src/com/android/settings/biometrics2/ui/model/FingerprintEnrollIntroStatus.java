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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Fingerprint onboarding introduction page data, it contains following information which needs
 * to be passed from view model to view.
 * 1. mEnrollableStatus: User is allowed to enroll a new fingerprint or not.
 * 2. mHasScrollToBottom: User has scrolled to the bottom of this page or not.
 */
public final class FingerprintEnrollIntroStatus {

    /**
     * Unconfirmed case, it means that this value is invalid, and view shall bypass this value.
     */
    public static final int FINGERPRINT_ENROLLABLE_UNKNOWN = -1;

    /**
     * User is allowed to enrolled a new fingerprint.
     */
    public static final int FINGERPRINT_ENROLLABLE_OK = 0;

    /**
     * User is not allowed to enrolled a new fingerprint because the number of enrolled fingerprint
     * has reached maximum.
     */
    public static final int FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX = 1;

    @IntDef(prefix = {"FINGERPRINT_ENROLLABLE_"}, value = {
            FINGERPRINT_ENROLLABLE_UNKNOWN,
            FINGERPRINT_ENROLLABLE_OK,
            FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollableStatus {
    }

    private final boolean mHasScrollToBottom;

    @FingerprintEnrollableStatus
    private final int mEnrollableStatus;

    public FingerprintEnrollIntroStatus(boolean hasScrollToBottom, int enrollableStatus) {
        mEnrollableStatus = enrollableStatus;
        mHasScrollToBottom = hasScrollToBottom;
    }

    /**
     * Get enrollable status. It means that user is allowed to enroll a new fingerprint or not.
     */
    @FingerprintEnrollableStatus
    public int getEnrollableStatus() {
        return mEnrollableStatus;
    }

    /**
     * Get info for this onboarding introduction page has scrolled to bottom or not
     */
    public boolean hasScrollToBottom() {
        return mHasScrollToBottom;
    }
}

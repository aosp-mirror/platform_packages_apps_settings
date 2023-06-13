/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.biometrics2.ui.model


enum class FingerprintEnrollable {
    // Unconfirmed case, this value is invalid, and view shall bypass this value
    FINGERPRINT_ENROLLABLE_UNKNOWN,
    // User is allowed to enrolled a new fingerprint
    FINGERPRINT_ENROLLABLE_OK,
    // User is not allowed to enroll because the number has reached maximum
    FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
}

/**
 * Fingerprint onboarding introduction page data, it contains following information which needs
 * to be passed from view model to view.
 * 1. mEnrollableStatus: User is allowed to enroll a new fingerprint or not.
 * 2. mHasScrollToBottom: User has scrolled to the bottom of this page or not.
 */
class FingerprintEnrollIntroStatus(
    private val mHasScrollToBottom: Boolean,
    /** Enrollable status. It means that user is allowed to enroll a new fingerprint or not. */
    val enrollableStatus: FingerprintEnrollable
) {
    /** Get info for this onboarding introduction page has scrolled to bottom or not */
    fun hasScrollToBottom(): Boolean {
        return mHasScrollToBottom
    }

    override fun toString(): String {
        return ("${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
                + "{scrollToBottom:$mHasScrollToBottom"
                + ", enrollableStatus:$enrollableStatus}")
    }
}

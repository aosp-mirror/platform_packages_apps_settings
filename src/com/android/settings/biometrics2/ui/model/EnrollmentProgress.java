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

/**
 * Biometric Enrollment progress
 */
public final class EnrollmentProgress {

    public static final int INITIAL_STEPS = -1;
    public static final int INITIAL_REMAINING = 0;

    private final int mSteps;
    private final int mRemaining;

    public EnrollmentProgress(int steps, int remaining) {
        mSteps = steps;
        mRemaining = remaining;
    }

    public int getSteps() {
        return mSteps;
    }

    public int getRemaining() {
        return mRemaining;
    }

    public boolean isInitialStep() {
        return mSteps == INITIAL_STEPS;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + "{steps:" + mSteps + ", remaining:" + mRemaining + "}";
    }
}

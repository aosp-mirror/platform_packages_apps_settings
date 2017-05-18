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
 */

package android.app.admin;

import android.annotation.NonNull;

/**
 * Stub implementation of framework's PasswordMetrics for Robolectric tests. Otherwise Robolectric
 * is throwing ClassNotFoundError.
 *
 * TODO: Remove this class when Robolectric supports O
 */
public class PasswordMetrics {

    // Maximum allowed number of repeated or ordered characters in a sequence before we'll
    // consider it a complex PIN/password.
    public static final int MAX_ALLOWED_SEQUENCE = 3;

    public int length = 0;
    public int letters = 0;
    public int upperCase = 0;
    public int lowerCase = 0;
    public int numeric = 0;
    public int symbols = 0;
    public int nonLetter = 0;

    public static int maxLengthSequence(@NonNull String string) {
        // Stub implementation
        return 1;
    }

    public static PasswordMetrics computeForPassword(@NonNull String password) {
        return new PasswordMetrics();
    }
}
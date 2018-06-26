/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics;

import com.android.settings.SubSettings;

/**
 * Abstract base class for biometric settings, such as Fingerprint, Face, Iris
 */
public abstract class BiometricSettings extends SubSettings {
    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

    /**
     * Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
     * will be useful if the user accidentally entered this flow.
     */
    protected static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

    /**
     * Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
     * device was left idle. This is used to clear the credential token to require the user to
     * re-enter their pin/pattern/password before continuing.
     */
    protected static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingNameTextValidatorTest {
    private AudioSharingNameTextValidator mValidator;

    @Before
    public void setUp() {
        mValidator = new AudioSharingNameTextValidator();
    }

    @Test
    public void testValidNames() {
        assertThat(mValidator.isTextValid("ValidName")).isTrue();
        assertThat(mValidator.isTextValid("12345678")).isTrue();
        assertThat(mValidator.isTextValid("Name_With_Underscores")).isTrue();
        assertThat(mValidator.isTextValid("ÄÖÜß")).isTrue();
        assertThat(mValidator.isTextValid("ThisNameIsExactly32Characters!")).isTrue();
    }

    @Test
    public void testInvalidNames() {
        assertThat(mValidator.isTextValid(null)).isFalse();
        assertThat(mValidator.isTextValid("")).isFalse();
        assertThat(mValidator.isTextValid("abc")).isFalse();
        assertThat(mValidator.isTextValid("ThisNameIsWayTooLongForAnAudioSharingName")).isFalse();
        assertThat(mValidator.isTextValid("Invalid\uDC00")).isFalse();
    }
}

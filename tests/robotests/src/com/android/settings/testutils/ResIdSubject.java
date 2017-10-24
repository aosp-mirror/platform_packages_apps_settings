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

package com.android.settings.testutils;

import static com.google.common.truth.Truth.assertAbout;

import static org.robolectric.RuntimeEnvironment.application;

import android.support.annotation.Nullable;

import com.google.common.truth.ComparableSubject;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.SubjectFactory;

/**
 * Custom subject for use with {@link com.google.common.truth.Truth}, to provide a more readable
 * error message, so that instead of "Not true that 2130706432 equals to 17170444", it will say
 * "Not true that color/my_color equals to android:color/black".
 *
 * <p>Usage:
 * <pre>{@code
 *     ResIdSubject.assertResId(activity.getThemeResId()).isEqualTo(android.R.style.Theme_Material)
 * }</pre>
 */
public class ResIdSubject extends ComparableSubject<ResIdSubject, Integer> {

    public static final SubjectFactory<ResIdSubject, Integer> FACTORY =
            new SubjectFactory<ResIdSubject, Integer>() {
                @Override
                public ResIdSubject getSubject(
                        FailureStrategy failureStrategy, Integer integer) {
                    return new ResIdSubject(failureStrategy, integer);
                }
            };

    public static ResIdSubject assertResId(int resId) {
        return assertAbout(ResIdSubject.FACTORY).that(resId);
    }

    public ResIdSubject(
            FailureStrategy failureStrategy,
            @Nullable Integer integer) {
        super(failureStrategy, integer);
    }

    public void isEqualTo(int other) {
        Integer subject = getSubject();
        if (subject == null || subject != other) {
            fail("equals to", resIdToString(other));
        }
    }

    @Override
    protected String getDisplaySubject() {
        String resourceName = "<" + resIdToString(getSubject()) + ">";
        String customName = internalCustomName();
        if (customName != null) {
            return customName + " " + resourceName;
        } else {
            return resourceName;
        }
    }

    private static String resIdToString(int resId) {
        return application.getResources().getResourceName(resId);
    }
}

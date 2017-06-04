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

package com.android.settings.dashboard;

import android.content.res.Resources;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/***
 * Matches on the first view with id if there are multiple views using the same Id.
 */
public class FirstIdViewMatcher {

    public static Matcher<View> withFirstId(final int id) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            private boolean mMatched;

            public void describeTo(Description description) {
                String idDescription = Integer.toString(id);
                if (resources != null) {
                    try {
                        idDescription = resources.getResourceName(id);
                    } catch (Resources.NotFoundException e) {
                        // No big deal, will just use the int value.
                        idDescription = String.format("%s (resource name not found)", id);
                    }
                }
                description.appendText("with first id: " + idDescription);
            }

            public boolean matchesSafely(View view) {
                this.resources = view.getResources();
                if (mMatched) {
                    return false;
                } else {
                    mMatched = id == view.getId();
                    return mMatched;
                }
            }
        };
    }
}

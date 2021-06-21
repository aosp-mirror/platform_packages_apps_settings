/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SliceDataTest {

    private static final String KEY = "KEY";
    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final String SCREEN_TITLE = "screen title";
    private static final String KEYWORDS = "a, b, c";
    private static final String FRAGMENT_NAME = "fragment name";
    private static final int ICON = 1234; // I declare a thumb war
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String PREF_CONTROLLER = "com.android.settings.slices.tester";
    private static final int SLICE_TYPE = SliceData.SliceType.SWITCH;
    private static final String UNAVAILABLE_SLICE_SUBTITLE = "subtitleOfUnavailableSlice";

    @Test
    public void testBuilder_buildsMatchingObject() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setKeywords(KEYWORDS)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .setSliceType(SLICE_TYPE)
                .setUnavailableSliceSubtitle(UNAVAILABLE_SLICE_SUBTITLE)
                .setIsPublicSlice(true);

        SliceData data = builder.build();

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
        assertThat(data.getSummary()).isEqualTo(SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SCREEN_TITLE);
        assertThat(data.getKeywords()).isEqualTo(KEYWORDS);
        assertThat(data.getIconResource()).isEqualTo(ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(URI);
        assertThat(data.getPreferenceController()).isEqualTo(PREF_CONTROLLER);
        assertThat(data.getSliceType()).isEqualTo(SLICE_TYPE);
        assertThat(data.getUnavailableSliceSubtitle()).isEqualTo(UNAVAILABLE_SLICE_SUBTITLE);
        assertThat(data.isPublicSlice()).isEqualTo(true);
    }

    @Test(expected = SliceData.InvalidSliceDataException.class)
    public void testBuilder_noKey_throwsIllegalStateException() {
        new SliceData.Builder()
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .build();
    }

    @Test(expected = SliceData.InvalidSliceDataException.class)
    public void testBuilder_noTitle_throwsIllegalStateException() {
        new SliceData.Builder()
                .setKey(KEY)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .build();
    }

    @Test(expected = SliceData.InvalidSliceDataException.class)
    public void testBuilder_noFragment_throwsIllegalStateException() {
        new SliceData.Builder()
                .setKey(KEY)
                .setFragmentName(FRAGMENT_NAME)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .build();
    }

    @Test(expected = SliceData.InvalidSliceDataException.class)
    public void testBuilder_noPrefController_throwsIllegalStateException() {
        new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setUri(URI)
                .setFragmentName(FRAGMENT_NAME)
                .build();
    }

    @Test
    public void testBuilder_noSubtitle_buildsMatchingObject() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData data = builder.build();

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
        assertThat(data.getSummary()).isNull();
        assertThat(data.getScreenTitle()).isEqualTo(SCREEN_TITLE);
        assertThat(data.getIconResource()).isEqualTo(ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(URI);
        assertThat(data.getPreferenceController()).isEqualTo(PREF_CONTROLLER);
    }

    @Test
    public void testBuilder_noScreenTitle_buildsMatchingObject() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData data = builder.build();

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
        assertThat(data.getSummary()).isEqualTo(SUMMARY);
        assertThat(data.getScreenTitle()).isNull();
        assertThat(data.getIconResource()).isEqualTo(ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(URI);
        assertThat(data.getPreferenceController()).isEqualTo(PREF_CONTROLLER);
    }

    @Test
    public void testBuilder_noIcon_buildsMatchingObject() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData data = builder.build();

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
        assertThat(data.getSummary()).isEqualTo(SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SCREEN_TITLE);
        assertThat(data.getIconResource()).isEqualTo(0);
        assertThat(data.getFragmentClassName()).isEqualTo(FRAGMENT_NAME);
        assertThat(data.getUri()).isEqualTo(URI);
        assertThat(data.getPreferenceController()).isEqualTo(PREF_CONTROLLER);
    }

    @Test
    public void testBuilder_noUri_buildsMatchingObject() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(null)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData data = builder.build();

        assertThat(data.getKey()).isEqualTo(KEY);
        assertThat(data.getTitle()).isEqualTo(TITLE);
        assertThat(data.getSummary()).isEqualTo(SUMMARY);
        assertThat(data.getScreenTitle()).isEqualTo(SCREEN_TITLE);
        assertThat(data.getIconResource()).isEqualTo(ICON);
        assertThat(data.getFragmentClassName()).isEqualTo(FRAGMENT_NAME);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(PREF_CONTROLLER);
    }

    @Test
    public void testEquality_identicalObjects() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData dataOne = builder.build();
        SliceData dataTwo = builder.build();

        assertThat(dataOne.hashCode()).isEqualTo(dataTwo.hashCode());
        assertThat(dataOne).isEqualTo(dataTwo);
    }

    @Test
    public void testEquality_matchingKey_EqualObjects() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData dataOne = builder.build();

        builder.setTitle(TITLE + " diff")
                .setSummary(SUMMARY + " diff")
                .setScreenTitle(SCREEN_TITLE + " diff")
                .setIcon(ICON + 1)
                .setFragmentName(FRAGMENT_NAME + " diff")
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER + " diff");

        SliceData dataTwo = builder.build();

        assertThat(dataOne.hashCode()).isEqualTo(dataTwo.hashCode());
        assertThat(dataOne).isEqualTo(dataTwo);
    }

    @Test
    public void testEquality_differentKey_differentObjects() {
        SliceData.Builder builder = new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        SliceData dataOne = builder.build();

        builder.setKey("not key");
        SliceData dataTwo = builder.build();

        assertThat(dataOne.hashCode()).isNotEqualTo(dataTwo.hashCode());
        assertThat(dataOne).isNotEqualTo(dataTwo);
    }
}

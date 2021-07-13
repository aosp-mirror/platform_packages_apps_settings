/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.network.helper;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

@RunWith(AndroidJUnit4.class)
public class SelectableSubscriptionsTest {

    @Before
    public void setUp() {
    }

    @Test
    public void atomicToList_nullInput_getNoneNullEmptyList() {
        List<Integer> result = SelectableSubscriptions.atomicToList(null);

        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void atomicToList_zeroLengthInput_getEmptyList() {
        List<Integer> result = SelectableSubscriptions.atomicToList(new AtomicIntegerArray(0));

        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void atomicToList_subIdInArray_getList() {
        AtomicIntegerArray array = new AtomicIntegerArray(3);
        array.set(0, 3);
        array.set(1, 7);
        array.set(2, 4);

        List<Integer> result = SelectableSubscriptions.atomicToList(array);

        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0)).isEqualTo(3);
        assertThat(result.get(1)).isEqualTo(7);
        assertThat(result.get(2)).isEqualTo(4);
    }
}

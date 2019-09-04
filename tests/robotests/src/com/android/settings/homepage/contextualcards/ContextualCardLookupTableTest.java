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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.homepage.contextualcards.ContextualCardLookupTable.ControllerRendererMapping;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardLookupTableTest {

    private static final int UNSUPPORTED_CARD_TYPE = -99999;
    private static final int UNSUPPORTED_VIEW_TYPE = -99999;

    private List<ControllerRendererMapping> mOriginalLookupTable;

    @Before
    public void setUp() {
        mOriginalLookupTable = new ArrayList<>();
        ContextualCardLookupTable.LOOKUP_TABLE.stream()
                .forEach(mapping -> mOriginalLookupTable.add(mapping));
    }

    @After
    public void reset() {
        ContextualCardLookupTable.LOOKUP_TABLE.clear();
        ContextualCardLookupTable.LOOKUP_TABLE.addAll(mOriginalLookupTable);
    }

    @Test
    public void getCardControllerClass_hasSupportedCardType_shouldGetCorrespondingController() {
        for (ControllerRendererMapping mapping : ContextualCardLookupTable.LOOKUP_TABLE) {
            assertThat(ContextualCardLookupTable.getCardControllerClass(mapping.mCardType))
                    .isEqualTo(mapping.mControllerClass);
        }
    }

    @Test
    public void getCardControllerClass_hasUnsupportedCardType_shouldAlwaysGetNull() {
        assertThat(ContextualCardLookupTable.getCardControllerClass(UNSUPPORTED_CARD_TYPE))
                .isNull();
    }

    @Test
    public void
    getCardRendererClassByViewType_hasSupportedViewType_shouldGetCorrespondingRenderer() {
        for (ControllerRendererMapping mapping : ContextualCardLookupTable.LOOKUP_TABLE) {
            assertThat(ContextualCardLookupTable.getCardRendererClassByViewType(mapping.mViewType))
                    .isEqualTo(mapping.mRendererClass);
        }
    }

    @Test
    public void getCardRendererClassByViewType_hasUnsupportedViewType_shouldAlwaysGetNull() {
        assertThat(ContextualCardLookupTable.getCardRendererClassByViewType(
                UNSUPPORTED_VIEW_TYPE)).isNull();
    }

    @Test(expected = IllegalStateException.class)
    public void
    getCardRendererClassByViewType_hasDuplicateViewType_shouldThrowsIllegalStateException() {
        final ControllerRendererMapping mapping1 =
                new ControllerRendererMapping(
                        1111 /* cardType */, UNSUPPORTED_VIEW_TYPE /* viewType */,
                        ContextualCardController.class, ContextualCardRenderer.class
                );
        final ControllerRendererMapping mapping2 =
                new ControllerRendererMapping(
                        2222 /* cardType */, UNSUPPORTED_VIEW_TYPE /* viewType */,
                        ContextualCardController.class, ContextualCardRenderer.class
                );
        ContextualCardLookupTable.LOOKUP_TABLE.add(mapping1);
        ContextualCardLookupTable.LOOKUP_TABLE.add(mapping2);

        ContextualCardLookupTable.getCardRendererClassByViewType(UNSUPPORTED_VIEW_TYPE);
    }
}

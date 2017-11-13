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

import static com.android.settings.dashboard.DashboardData.STABLE_ID_CONDITION_CONTAINER;
import static com.android.settings.dashboard.DashboardData.STABLE_ID_SUGGESTION_CONDITION_FOOTER;
import static com.android.settings.dashboard.DashboardData.STABLE_ID_SUGGESTION_CONTAINER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.widget.RemoteViews;

import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.AirplaneModeCondition;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardDataTest {
    private static final String TEST_SUGGESTION_TITLE = "Use fingerprint";
    private static final String TEST_CATEGORY_TILE_TITLE = "Display";

    private DashboardData mDashboardDataWithOneConditions;
    private DashboardData mDashboardDataWithTwoConditions;
    private DashboardData mDashboardDataWithNoItems;
    @Mock
    private Tile mTestCategoryTile;
    @Mock
    private Tile mTestSuggestion;
    @Mock
    private DashboardCategory mDashboardCategory;
    @Mock
    private Condition mTestCondition;
    @Mock
    private Condition mSecondCondition; // condition used to test insert in DiffUtil

    @Before
    public void SetUp() {
        MockitoAnnotations.initMocks(this);

        // Build suggestions
        final List<Tile> suggestions = new ArrayList<>();
        mTestSuggestion.title = TEST_SUGGESTION_TITLE;
        suggestions.add(mTestSuggestion);

        // Build oneItemConditions
        final List<Condition> oneItemConditions = new ArrayList<>();
        when(mTestCondition.shouldShow()).thenReturn(true);
        oneItemConditions.add(mTestCondition);

        // Build twoItemConditions
        final List<Condition> twoItemsConditions = new ArrayList<>();
        when(mSecondCondition.shouldShow()).thenReturn(true);
        twoItemsConditions.add(mTestCondition);
        twoItemsConditions.add(mSecondCondition);

        // Build category
        mTestCategoryTile.title = TEST_CATEGORY_TILE_TITLE;
        mDashboardCategory.title = "test";
        mDashboardCategory.tiles = new ArrayList<>();
        mDashboardCategory.tiles.add(mTestCategoryTile);

        // Build DashboardData
        mDashboardDataWithOneConditions = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setSuggestionConditionMode(DashboardData.HEADER_MODE_FULLY_EXPANDED)
                .build();

        mDashboardDataWithTwoConditions = new DashboardData.Builder()
                .setConditions(twoItemsConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setSuggestionConditionMode(DashboardData.HEADER_MODE_FULLY_EXPANDED)
                .build();

        mDashboardDataWithNoItems = new DashboardData.Builder()
                .setConditions(null)
                .setCategory(null)
                .setSuggestions(null)
                .build();
    }

    @Test
    public void testBuildItemsData_shouldSetstableId() {
        final List<DashboardData.Item> items = mDashboardDataWithOneConditions.getItemList();

        // Header, suggestion, condition, footer, 1 tile
        assertThat(items).hasSize(4);

        assertThat(items.get(0).id).isEqualTo(STABLE_ID_SUGGESTION_CONTAINER);
        assertThat(items.get(1).id).isEqualTo(STABLE_ID_CONDITION_CONTAINER);
        assertThat(items.get(2).id).isEqualTo(STABLE_ID_SUGGESTION_CONDITION_FOOTER);
        assertThat(items.get(3).id).isEqualTo(Objects.hash(mTestCategoryTile.title));
    }

    @Test
    public void testBuildItemsData_containsAllData() {
        final Object[] expectedObjects = {
                mDashboardDataWithOneConditions.getSuggestions(),
                mDashboardDataWithOneConditions.getConditions(),
                null, mTestCategoryTile};
        final int expectedSize = expectedObjects.length;

        assertThat(mDashboardDataWithOneConditions.getItemList()).hasSize(expectedSize);

        for (int i = 0; i < expectedSize; i++) {
            final Object item = mDashboardDataWithOneConditions.getItemEntityByPosition(i);
            if (item instanceof List) {
                assertThat(item).isEqualTo(expectedObjects[i]);
            } else if (item instanceof DashboardData.SuggestionConditionHeaderData) {
                DashboardData.SuggestionConditionHeaderData i1 =
                        (DashboardData.SuggestionConditionHeaderData) item;
                DashboardData.SuggestionConditionHeaderData i2 =
                        (DashboardData.SuggestionConditionHeaderData) expectedObjects[i];
                assertThat(i1.title).isEqualTo(i2.title);
                assertThat(i1.conditionCount).isEqualTo(i2.conditionCount);
                assertThat(i1.hiddenSuggestionCount).isEqualTo(i2.hiddenSuggestionCount);
            } else {
                assertThat(item).isSameAs(expectedObjects[i]);
            }
        }
    }

    @Test
    public void testGetPositionByEntity_selfInstance_returnPositionFound() {
        final int position = mDashboardDataWithOneConditions
                .getPositionByEntity(mDashboardDataWithOneConditions.getConditions());
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByEntity_notExisted_returnNotFound() {
        final Condition condition = mock(AirplaneModeCondition.class);
        final int position = mDashboardDataWithOneConditions.getPositionByEntity(condition);
        assertThat(position).isEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_selfInstance_returnPositionFound() {
        final int position = mDashboardDataWithOneConditions
                .getPositionByTile(mTestCategoryTile);
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_equalTitle_returnPositionFound() {
        final Tile tile = mock(Tile.class);
        tile.title = TEST_CATEGORY_TILE_TITLE;
        final int position = mDashboardDataWithOneConditions.getPositionByTile(tile);
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_notExisted_returnNotFound() {
        final Tile tile = mock(Tile.class);
        tile.title = "";
        final int position = mDashboardDataWithOneConditions.getPositionByTile(tile);
        assertThat(position).isEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testDiffUtil_DataEqual_noResultData() {
        List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testDiffUtil(mDashboardDataWithOneConditions,
                mDashboardDataWithOneConditions, testResultData);
    }

    @Test
    public void testDiffUtil_InsertOneCondition_ResultDataOneChanged() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        // Item in position 2 is the condition container containing the list of conditions, which
        // gets 1 more item
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_CHANGE, 1, 1));

        testDiffUtil(mDashboardDataWithOneConditions,
                mDashboardDataWithTwoConditions, testResultData);
    }

    @Test
    public void testDiffUtil_RemoveOneSuggestion_causeItemRemoveAndChange() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_REMOVE, 0, 1));
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_CHANGE, 1, 1));
        // Build DashboardData
        final List<Condition> oneItemConditions = new ArrayList<>();
        when(mTestCondition.shouldShow()).thenReturn(true);
        oneItemConditions.add(mTestCondition);
        final List<Tile> suggestions = new ArrayList<>();
        mTestSuggestion.title = TEST_SUGGESTION_TITLE;
        suggestions.add(mTestSuggestion);

        final DashboardData oldData = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setSuggestionConditionMode(DashboardData.HEADER_MODE_DEFAULT)
                .build();
        final DashboardData newData = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setSuggestions(null)
                .setCategory(mDashboardCategory)
                .setSuggestionConditionMode(DashboardData.HEADER_MODE_DEFAULT)
                .build();

        testDiffUtil(oldData, newData, testResultData);
    }

    @Test
    public void testDiffUtil_DeleteAllData_ResultDataOneDeleted() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_REMOVE, 0, 4));

        testDiffUtil(mDashboardDataWithOneConditions, mDashboardDataWithNoItems, testResultData);
    }

    @Test
    public void testDiffUtil_typeSuggestedContainer_ResultDataNothingChanged() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_CHANGE, 0, 1));
        Tile tile = new Tile();
        tile.remoteViews = mock(RemoteViews.class);

        DashboardData prevData = new DashboardData.Builder()
                .setConditions(null)
                .setCategory(null)
                .setSuggestions(Arrays.asList(tile))
                .build();
        DashboardData currentData = new DashboardData.Builder()
                .setConditions(null)
                .setCategory(null)
                .setSuggestions(Arrays.asList(tile))
                .build();
        testDiffUtil(prevData, currentData, testResultData);
    }

    /**
     * Test when using the
     * {@link com.android.settings.dashboard.DashboardData.ItemsDataDiffCallback}
     * to transfer List from {@paramref baseDashboardData} to {@paramref diffDashboardData}, whether
     * the transform data result is equals to {@paramref testResultData}
     * <p>
     * The steps are described below:
     * 1. Calculate a {@link android.support.v7.util.DiffUtil.DiffResult} from
     * {@paramref baseDashboardData} to {@paramref diffDashboardData}
     * <p>
     * 2. Dispatch the {@link android.support.v7.util.DiffUtil.DiffResult} calculated from step 1
     * into {@link ListUpdateResult}
     * <p>
     * 3. Get result data(a.k.a. baseResultData) from {@link ListUpdateResult} and compare it to
     * {@paramref testResultData}
     * <p>
     * Because baseResultData and {@paramref testResultData} don't have sequence. When do the
     * comparison, we will sort them first and then compare the inside data from them one by one.
     */
    private void testDiffUtil(DashboardData baseDashboardData, DashboardData diffDashboardData,
            List<ListUpdateResult.ResultData> testResultData) {
        final DiffUtil.DiffResult diffUtilResult = DiffUtil.calculateDiff(
                new DashboardData.ItemsDataDiffCallback(
                        baseDashboardData.getItemList(), diffDashboardData.getItemList()));

        // Dispatch to listUpdateResult, then listUpdateResult will have result data
        final ListUpdateResult listUpdateResult = new ListUpdateResult();
        diffUtilResult.dispatchUpdatesTo(listUpdateResult);

        final List<ListUpdateResult.ResultData> baseResultData = listUpdateResult.getResultData();
        assertThat(testResultData.size()).isEqualTo(baseResultData.size());

        // Sort them so we can compare them one by one using a for loop
        Collections.sort(baseResultData);
        Collections.sort(testResultData);
        final int size = baseResultData.size();
        for (int i = 0; i < size; i++) {
            // Refer to equals method in ResultData
            assertThat(baseResultData.get(i)).isEqualTo(testResultData.get(i));
        }
    }

    /**
     * This class contains the result about how the changes made to convert one
     * list to another list. It implements ListUpdateCallback to record the result data.
     */
    private static class ListUpdateResult implements ListUpdateCallback {
        final private List<ResultData> mResultData;

        public ListUpdateResult() {
            mResultData = new ArrayList<>();
        }

        public List<ResultData> getResultData() {
            return mResultData;
        }

        @Override
        public void onInserted(int position, int count) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_INSERT, position, count));
        }

        @Override
        public void onRemoved(int position, int count) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_REMOVE, position, count));
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mResultData.add(
                    new ResultData(ResultData.TYPE_OPERATION_MOVE, fromPosition, toPosition));
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_CHANGE, position, count));
        }

        /**
         * This class contains general type and field to record the operation data generated
         * in {@link ListUpdateCallback}. Please refer to {@link ListUpdateCallback} for more info.
         * <p>
         * The following are examples about the data stored in this class:
         * <p>
         * "The data starts from position(arg1) with count number(arg2) is changed(operation)"
         * or "The data is moved(operation) from position1(arg1) to position2(arg2)"
         */
        private static class ResultData implements Comparable<ResultData> {
            public static final int TYPE_OPERATION_INSERT = 0;
            public static final int TYPE_OPERATION_REMOVE = 1;
            public static final int TYPE_OPERATION_MOVE = 2;
            public static final int TYPE_OPERATION_CHANGE = 3;

            public final int operation;
            public final int arg1;
            public final int arg2;

            public ResultData(int operation, int arg1, int arg2) {
                this.operation = operation;
                this.arg1 = arg1;
                this.arg2 = arg2;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                if (!(obj instanceof ResultData)) {
                    return false;
                }

                ResultData targetData = (ResultData) obj;

                return operation == targetData.operation && arg1 == targetData.arg1
                        && arg2 == targetData.arg2;
            }

            @Override
            public int compareTo(@NonNull ResultData resultData) {
                if (this.operation != resultData.operation) {
                    return operation - resultData.operation;
                }

                if (arg1 != resultData.arg1) {
                    return arg1 - resultData.arg1;
                }

                return arg2 - resultData.arg2;
            }

            @Override
            public String toString() {
                return "op:" + operation + ",arg1:" + arg1 + ",arg2:" + arg2;
            }
        }
    }
}

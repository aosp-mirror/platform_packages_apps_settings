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
import static com.android.settings.dashboard.DashboardData.STABLE_ID_CONDITION_FOOTER;
import static com.android.settings.dashboard.DashboardData.STABLE_ID_SUGGESTION_CONDITION_DIVIDER;
import static com.android.settings.dashboard.DashboardData.STABLE_ID_SUGGESTION_CONTAINER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.service.settings.suggestions.Suggestion;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
public class DashboardDataTest {

    private static final String TEST_SUGGESTION_TITLE = "Use fingerprint";
    private static final String TEST_CATEGORY_TILE_TITLE = "Display";

    private DashboardData mDashboardDataWithOneConditions;
    private DashboardData mDashboardDataWithTwoConditions;
    private DashboardData mDashboardDataWithNoItems;
    private DashboardCategory mDashboardCategory;
    @Mock
    private Tile mTestCategoryTile;
    @Mock
    private Condition mTestCondition;
    @Mock
    private Condition mSecondCondition; // condition used to test insert in DiffUtil
    private Suggestion mTestSuggestion;

    @Before
    public void SetUp() {
        MockitoAnnotations.initMocks(this);

        mDashboardCategory = new DashboardCategory();

        // Build suggestions
        final List<Suggestion> suggestions = new ArrayList<>();
        mTestSuggestion = new Suggestion.Builder("pkg")
                .setTitle(TEST_SUGGESTION_TITLE)
                .setPendingIntent(mock(PendingIntent.class))
                .build();
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

        mDashboardCategory.addTile(mTestCategoryTile);

        // Build DashboardData
        mDashboardDataWithOneConditions = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setConditionExpanded(true)
                .build();

        mDashboardDataWithTwoConditions = new DashboardData.Builder()
                .setConditions(twoItemsConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setConditionExpanded(true)
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

        // suggestion, separator, condition, footer, 1 tile
        assertThat(items).hasSize(5);

        assertThat(items.get(0).id).isEqualTo(STABLE_ID_SUGGESTION_CONTAINER);
        assertThat(items.get(1).id).isEqualTo(STABLE_ID_SUGGESTION_CONDITION_DIVIDER);
        assertThat(items.get(2).id).isEqualTo(STABLE_ID_CONDITION_CONTAINER);
        assertThat(items.get(3).id).isEqualTo(STABLE_ID_CONDITION_FOOTER);
        assertThat(items.get(4).id).isEqualTo(Objects.hash(mTestCategoryTile.title));
    }

    @Test
    public void testBuildItemsData_containsAllData() {
        final Object[] expectedObjects = {
                mDashboardDataWithOneConditions.getSuggestions(),
                null /* divider */,
                mDashboardDataWithOneConditions.getConditions(),
                null /* footer */, mTestCategoryTile};
        final int expectedSize = expectedObjects.length;

        assertThat(mDashboardDataWithOneConditions.getItemList()).hasSize(expectedSize);

        for (int i = 0; i < expectedSize; i++) {
            final Object item = mDashboardDataWithOneConditions.getItemEntityByPosition(i);
            if (item instanceof List) {
                assertThat(item).isEqualTo(expectedObjects[i]);
            } else if (item instanceof DashboardData.ConditionHeaderData) {
                DashboardData.ConditionHeaderData i1 = (DashboardData.ConditionHeaderData) item;
                DashboardData.ConditionHeaderData i2 =
                        (DashboardData.ConditionHeaderData) expectedObjects[i];
                assertThat(i1.title).isEqualTo(i2.title);
                assertThat(i1.conditionCount).isEqualTo(i2.conditionCount);
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
        final int position = mDashboardDataWithOneConditions.getPositionByTile(mTestCategoryTile);
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
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        // Item in position 3 is the condition container containing the list of conditions, which
        // gets 1 more item
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_CHANGE, 2, 1));

        testDiffUtil(mDashboardDataWithOneConditions,
                mDashboardDataWithTwoConditions, testResultData);
    }

    @Test
    public void testDiffUtil_RemoveOneSuggestion_causeItemRemoveAndChange() {
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        // removed suggestion and the divider
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_REMOVE, 0, 2));
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_CHANGE, 2, 1));
        // Build DashboardData
        final List<Condition> oneItemConditions = new ArrayList<>();
        when(mTestCondition.shouldShow()).thenReturn(true);
        oneItemConditions.add(mTestCondition);
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(mTestSuggestion);

        final DashboardData oldData = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setCategory(mDashboardCategory)
                .setSuggestions(suggestions)
                .setConditionExpanded(false)
                .build();
        final DashboardData newData = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setSuggestions(null)
                .setCategory(mDashboardCategory)
                .setConditionExpanded(false)
                .build();

        testDiffUtil(oldData, newData, testResultData);
    }

    @Test
    public void testDiffUtil_DeleteAllData_ResultDataOneDeleted() {
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_REMOVE, 0, 5));

        testDiffUtil(mDashboardDataWithOneConditions, mDashboardDataWithNoItems, testResultData);
    }

    @Test
    public void testDiffUtil_typeSuggestedContainer_ResultDataNothingChanged() {
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();

        DashboardData prevData = new DashboardData.Builder()
                .setConditions(null)
                .setCategory(null)
                .setSuggestions(Collections.singletonList(mTestSuggestion))
                .build();
        DashboardData currentData = new DashboardData.Builder()
                .setConditions(null)
                .setCategory(null)
                .setSuggestions(Collections.singletonList(mTestSuggestion))
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
     * 1. Calculate a {@link androidx.recyclerview.widget.DiffUtil.DiffResult} from
     * {@paramref baseDashboardData} to {@paramref diffDashboardData}
     * <p>
     * 2. Dispatch the {@link androidx.recyclerview.widget.DiffUtil.DiffResult} calculated from step 1
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

        private List<ResultData> getResultData() {
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

            private static final int TYPE_OPERATION_INSERT = 0;
            private static final int TYPE_OPERATION_REMOVE = 1;
            private static final int TYPE_OPERATION_MOVE = 2;
            private static final int TYPE_OPERATION_CHANGE = 3;

            private final int operation;
            private final int arg1;
            private final int arg2;

            private ResultData(int operation, int arg1, int arg2) {
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

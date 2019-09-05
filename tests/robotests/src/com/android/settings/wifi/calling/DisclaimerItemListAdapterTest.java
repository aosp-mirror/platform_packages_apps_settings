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

package com.android.settings.wifi.calling;

import static com.android.settings.wifi.calling.DisclaimerItemListAdapter
        .DisclaimerItemViewHolder.ID_DISCLAIMER_ITEM_TITLE;
import static com.android.settings.wifi.calling.DisclaimerItemListAdapter
        .DisclaimerItemViewHolder.ID_DISCLAIMER_ITEM_DESCRIPTION;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DisclaimerItemListAdapterTest {

    private static final int ITEM_POSITION = 0;
    private static final int DISCLAIMER_TITLE_STRING_ID = 0;
    private static final int DISCLAIMER_MESSAGE_STRING_ID = 1;

    @Mock
    private LayoutInflater mLayoutInflater;
    @Mock
    private TextView mTestView;
    @Mock
    private TextView mDescView;
    @Mock
    private View mView;
    @Mock
    private ViewGroup mViewGroup;
    @Mock
    private Context mContext;

    private MockDisclaimerItem mDisclaimerItem;
    private DisclaimerItemListAdapter mDisclaimerItemListAdapter;
    private List<DisclaimerItem> mDisclaimerItemList = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mDisclaimerItem = spy(new MockDisclaimerItem(mContext, 0 /* subId */));
        mDisclaimerItemList.add(mDisclaimerItem);

        when(mLayoutInflater.inflate(anyInt(), anyObject(), anyBoolean())).thenReturn(mView);
        when(mViewGroup.getContext()).thenReturn(mContext);
        when(mViewGroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(
                mLayoutInflater);
        when(mView.findViewById(ID_DISCLAIMER_ITEM_TITLE)).thenReturn(mTestView);
        when(mView.findViewById(ID_DISCLAIMER_ITEM_DESCRIPTION)).thenReturn(mDescView);
    }

    @Test
    public void onBindViewHolder_haveItem_shouldSetText() {
        final DisclaimerItemListAdapter.DisclaimerItemViewHolder viewHolder =
                new DisclaimerItemListAdapter.DisclaimerItemViewHolder(mView);

        mDisclaimerItemListAdapter = new DisclaimerItemListAdapter(mDisclaimerItemList);
        mDisclaimerItemListAdapter.onCreateViewHolder(mViewGroup, 0 /* viewType */);
        mDisclaimerItemListAdapter.onBindViewHolder(viewHolder, ITEM_POSITION);

        // Check the text is set when the DisclaimerItem exists.
        verify(viewHolder.titleView).setText(DISCLAIMER_TITLE_STRING_ID);
        verify(viewHolder.descriptionView).setText(DISCLAIMER_MESSAGE_STRING_ID);
    }

    private class MockDisclaimerItem extends DisclaimerItem {
        MockDisclaimerItem(Context context, int subId) {
            super(context, subId);
        }

        @Override
        protected int getTitleId() {
            return DISCLAIMER_TITLE_STRING_ID;
        }

        @Override
        protected int getMessageId() {
            return DISCLAIMER_MESSAGE_STRING_ID;
        }

        @Override
        protected String getName() {
            return "MockDisclaimerItem";
        }

        @Override
        protected String getPrefKey() {
            return "mock_pref_key";
        }
    }
}

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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDisclaimerItemFactory;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDisclaimerItemFactory.class, ShadowFragment.class})
public class WifiCallingDisclaimerFragmentTest {

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private DisclaimerItem mDisclaimerItem;
    @Mock
    private LayoutInflater mLayoutInflater;
    @Mock
    private View mView;
    @Mock
    private ViewGroup mViewGroup;
    @Mock
    private Button mAgreeButton;
    @Mock
    private Button mDisagreeButton;
    @Mock
    private RecyclerView mRecyclerView;

    @Captor
    ArgumentCaptor<OnClickListener> mOnClickListenerCaptor;
    @Captor
    ArgumentCaptor<OnScrollListener> mOnScrollListenerCaptor;

    private WifiCallingDisclaimerFragment mFragment;
    private List<DisclaimerItem> mDisclaimerItemList = new ArrayList<>();
    private List<DisclaimerItem> mEmptyDisclaimerItemList = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = spy(new WifiCallingDisclaimerFragment());

        doReturn(mActivity).when(mFragment).getActivity();

        when(mLayoutInflater.inflate(anyInt(), any(), anyBoolean())).thenReturn(mView);
        when(mView.findViewById(R.id.agree_button)).thenReturn(mAgreeButton);
        when(mView.findViewById(R.id.disagree_button)).thenReturn(mDisagreeButton);
        when(mView.findViewById(R.id.disclaimer_item_list)).thenReturn(mRecyclerView);
        when(mView.getId()).thenReturn(R.id.agree_button);

        mOnScrollListenerCaptor = ArgumentCaptor.forClass(OnScrollListener.class);
        doNothing().when(mRecyclerView).addOnScrollListener(mOnScrollListenerCaptor.capture());
        mOnClickListenerCaptor = ArgumentCaptor.forClass(OnClickListener.class);
        doNothing().when(mAgreeButton).setOnClickListener(mOnClickListenerCaptor.capture());

        mDisclaimerItemList.add(mDisclaimerItem);
    }

    @Test
    public void onCreate_notHaveItem_shouldFinishFragment() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mEmptyDisclaimerItemList);

        mFragment.onCreate(null /* savedInstanceState */);

        // Check the fragment is finished when the DisclaimerItemList is empty.
        verify(mFragment).finish(Activity.RESULT_OK);
    }

    @Test
    public void onCreate_haveItem_shouldNotFinishFragment() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        mFragment.onCreate(null /* savedInstanceState */);

        // Check the fragment is not finished when the DisclaimerItemList is not empty.
        verify(mFragment, never()).finish(Activity.RESULT_OK);
    }

    @Test
    public void onScrolled_canNotScroll_shouldEnableAgreeButton() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        when(mRecyclerView.canScrollVertically(1)).thenReturn(false);

        mFragment.onCreate(null /* savedInstanceState */);
        mFragment.onCreateView(mLayoutInflater, mViewGroup, null /* savedInstanceState */);

        mOnScrollListenerCaptor.getValue().onScrolled(mRecyclerView, 0, 0);

        // Check the agreeButton is enabled when the view is scrolled to the bottom end.
        verify(mAgreeButton).setEnabled(true);
        // Check the OnScrollListener is removed when the view is scrolled to the bottom end.
        verify(mRecyclerView).removeOnScrollListener(any());
    }

    @Test
    public void onScrolled_canScroll_shouldNotEnableAgreeButton() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        when(mRecyclerView.canScrollVertically(1)).thenReturn(true);

        mFragment.onCreate(null /* savedInstanceState */);
        mFragment.onCreateView(mLayoutInflater, mViewGroup, null /* savedInstanceState */);

        mOnScrollListenerCaptor.getValue().onScrolled(mRecyclerView, 0, 0);

        // Check the agreeButton is not enabled when the view is not scrolled to the bottom end.
        verify(mAgreeButton, never()).setEnabled(anyBoolean());
        // Check the OnScrollListener is not removed when the view is not scrolled to
        // the bottom end.
        verify(mRecyclerView, never()).removeOnScrollListener(any());
    }

    @Test
    public void onClick_agreeButton_shouldFinishFragment() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        mFragment.onCreate(null /* savedInstanceState */);
        mFragment.onCreateView(mLayoutInflater, mViewGroup, null /* savedInstanceState */);

        mOnClickListenerCaptor.getValue().onClick(mAgreeButton);

        // Check the onAgreed callback is called when "CONTINUE" button is clicked.
        verify(mDisclaimerItem).onAgreed();
        // Check the WFC disclaimer fragment is finished when "CONTINUE" button is clicked.
        verify(mFragment).finish(Activity.RESULT_OK);
    }
}

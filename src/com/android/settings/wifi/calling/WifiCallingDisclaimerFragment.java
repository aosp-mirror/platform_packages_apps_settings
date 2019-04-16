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

import android.app.Activity;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying disclaimers for WFC.
 */
public class WifiCallingDisclaimerFragment extends InstrumentedFragment
        implements View.OnClickListener {
    private static final String TAG = "WifiCallingDisclaimerFragment";

    private static final String STATE_IS_SCROLL_TO_BOTTOM = "state_is_scroll_to_bottom";

    private List<DisclaimerItem> mDisclaimerItemList = new ArrayList<DisclaimerItem>();
    private Button mAgreeButton;
    private Button mDisagreeButton;
    private boolean mScrollToBottom;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIFI_CALLING;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        final int subId = (args != null) ? args.getInt(WifiCallingSettingsForSub.EXTRA_SUB_ID)
                : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

        mDisclaimerItemList = DisclaimerItemFactory.create(getActivity(), subId);
        if (mDisclaimerItemList.isEmpty()) {
            finish(Activity.RESULT_OK);
            return;
        }

        if (savedInstanceState != null) {
            mScrollToBottom = savedInstanceState.getBoolean(
                    STATE_IS_SCROLL_TO_BOTTOM, mScrollToBottom);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.wfc_disclaimer_fragment, container, false);

        mAgreeButton = view.findViewById(R.id.agree_button);
        mAgreeButton.setOnClickListener(this);
        mDisagreeButton = view.findViewById(R.id.disagree_button);
        mDisagreeButton.setOnClickListener(this);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(
                R.id.disclaimer_item_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new DisclaimerItemListAdapter(mDisclaimerItemList));

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1 /* scrolling down */)) {
                    mScrollToBottom = true;
                    updateButtonState();
                    recyclerView.removeOnScrollListener(this);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_SCROLL_TO_BOTTOM, mScrollToBottom);
    }

    private void updateButtonState() {
        mAgreeButton.setEnabled(mScrollToBottom);
    }

    @Override
    public void onClick(View v) {
        if (v == mAgreeButton) {
            for (DisclaimerItem item : mDisclaimerItemList) {
                item.onAgreed();
            }
            finish(Activity.RESULT_OK);
        } else if (v == mDisagreeButton) {
            finish(Activity.RESULT_CANCELED);
        }
    }

    @VisibleForTesting
    void finish(int result) {
        Activity activity = getActivity();
        activity.setResult(result, null);
        activity.finish();
    }
}
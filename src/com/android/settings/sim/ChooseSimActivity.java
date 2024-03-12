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

package com.android.settings.sim;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SwitchToEuiccSubscriptionSidecar;
import com.android.settings.network.SwitchToRemovableSlotSidecar;
import com.android.settings.network.UiccSlotUtil;

import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.Dividable;
import com.google.android.setupdesign.items.IItem;
import com.google.android.setupdesign.items.Item;
import com.google.android.setupdesign.items.ItemGroup;
import com.google.android.setupdesign.items.RecyclerItemAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Activity to show a list of profiles for user to choose. */
public class ChooseSimActivity extends Activity
        implements RecyclerItemAdapter.OnItemSelectedListener, SidecarFragment.Listener {
    // Whether there is a pSIM profile in the selection list.
    public static final String KEY_HAS_PSIM = "has_psim";
    // After the user selects eSIM profile, whether continue to show Mobile Network Settings screen
    // to select other preferences.
    // Note: KEY_NO_PSIM_CONTINUE_TO_SETTINGS and mNoPsimContinueToSettings are not used for now
    // for UI changes. We may use them in the future.
    public static final String KEY_NO_PSIM_CONTINUE_TO_SETTINGS = "no_psim_continue_to_settings";

    private static final String TAG = "ChooseSimActivity";
    private static final int INDEX_PSIM = -1;
    private static final String STATE_SELECTED_INDEX = "selected_index";
    private static final String STATE_IS_SWITCHING = "is_switching";

    private boolean mHasPsim;
    private boolean mNoPsimContinueToSettings;
    private ArrayList<SubscriptionInfo> mEmbeddedSubscriptions = new ArrayList<>();
    private SubscriptionInfo mRemovableSubscription = null;

    private ItemGroup mItemGroup;
    private SwitchToEuiccSubscriptionSidecar mSwitchToEuiccSubscriptionSidecar;
    private SwitchToRemovableSlotSidecar mSwitchToRemovableSlotSidecar;

    // Variables have states.
    private int mSelectedItemIndex;
    private boolean mIsSwitching;

    /** Returns an intent of {@code ChooseSimActivity} */
    public static Intent getIntent(Context context) {
        return new Intent(context, ChooseSimActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.choose_sim_activity);

        Intent intent = getIntent();
        mHasPsim = intent.getBooleanExtra(KEY_HAS_PSIM, false);
        mNoPsimContinueToSettings = intent.getBooleanExtra(KEY_NO_PSIM_CONTINUE_TO_SETTINGS, false);

        updateSubscriptions();

        if (mEmbeddedSubscriptions.size() == 0) {
            Log.e(TAG, "Unable to find available eSIM subscriptions.");
            finish();
            return;
        }

        if (savedInstanceState != null) {
            mSelectedItemIndex = savedInstanceState.getInt(STATE_SELECTED_INDEX);
            mIsSwitching = savedInstanceState.getBoolean(STATE_IS_SWITCHING);
        }

        GlifLayout layout = findViewById(R.id.glif_layout);
        int subscriptionCount = mEmbeddedSubscriptions.size();
        if (mHasPsim) { // Choose a number to use
            subscriptionCount++;
        }
        layout.setHeaderText(getString(R.string.choose_sim_title));
        MessageFormat msgFormat = new MessageFormat(
            getString(R.string.choose_sim_text),
            Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", subscriptionCount);
        layout.setDescriptionText(msgFormat.format(arguments));

        displaySubscriptions();

        mSwitchToRemovableSlotSidecar = SwitchToRemovableSlotSidecar.get(getFragmentManager());
        mSwitchToEuiccSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchToRemovableSlotSidecar.addListener(this);
        mSwitchToEuiccSubscriptionSidecar.addListener(this);
    }

    @Override
    public void onPause() {
        mSwitchToEuiccSubscriptionSidecar.removeListener(this);
        mSwitchToRemovableSlotSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_INDEX, mSelectedItemIndex);
        outState.putBoolean(STATE_IS_SWITCHING, mIsSwitching);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemSelected(IItem item) {
        if (mIsSwitching) {
            // If we already selected an item, do not try to switch to another one.
            return;
        }
        mIsSwitching = true;
        Item subItem = (Item) item;
        subItem.setSummary(getString(R.string.choose_sim_activating));
        mSelectedItemIndex = subItem.getId();
        if (mSelectedItemIndex == INDEX_PSIM) {
            Log.i(TAG, "Ready to switch to pSIM slot.");
            mSwitchToRemovableSlotSidecar.run(UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID, null);
        } else {
            Log.i(TAG, "Ready to switch to eSIM subscription with index: " + mSelectedItemIndex);
            mSwitchToEuiccSubscriptionSidecar.run(
                    mEmbeddedSubscriptions.get(mSelectedItemIndex).getSubscriptionId(),
                    UiccSlotUtil.INVALID_PORT_ID, null);
        }
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchToRemovableSlotSidecar) {
            switch (mSwitchToRemovableSlotSidecar.getState()) {
                case SidecarFragment.State.SUCCESS:
                    mSwitchToRemovableSlotSidecar.reset();
                    Log.i(TAG, "Switch slot successfully.");
                    SubscriptionManager subMgr = getSystemService(SubscriptionManager.class);
                    if (subMgr.canDisablePhysicalSubscription()) {
                        SubscriptionInfo removableSub =
                                SubscriptionUtil.getFirstRemovableSubscription(this);
                        if (removableSub != null) {
                            subMgr.setUiccApplicationsEnabled(
                                    removableSub.getSubscriptionId(), true);
                        }
                    }
                    finish();
                    break;
                case SidecarFragment.State.ERROR:
                    mSwitchToRemovableSlotSidecar.reset();
                    Log.e(TAG, "Failed to switch slot in ChooseSubscriptionsActivity.");
                    handleEnableRemovableSimError();
                    // We don't call finish() and just stay on this page.
                    break;
            }
        } else if (fragment == mSwitchToEuiccSubscriptionSidecar) {
            switch (mSwitchToEuiccSubscriptionSidecar.getState()) {
                case SidecarFragment.State.SUCCESS:
                    mSwitchToEuiccSubscriptionSidecar.reset();
                    if (mNoPsimContinueToSettings) {
                        // Currently, there shouldn't be a case that mNoPsimContinueToSettings is
                        // true. If this can be true in the future, we should finish() this page
                        // and direct to Settings page here.
                        Log.e(
                                TAG,
                                "mNoPsimContinueToSettings is true which is not supported for"
                                        + " now.");
                    } else {
                        Log.i(TAG, "User finished selecting eSIM profile.");
                        finish();
                    }
                    break;
                case SidecarFragment.State.ERROR:
                    mSwitchToEuiccSubscriptionSidecar.reset();
                    Log.e(TAG, "Failed to switch subscription in ChooseSubscriptionsActivity.");
                    Item item = (Item) mItemGroup.getItemAt(mSelectedItemIndex);
                    item.setEnabled(false);
                    item.setSummary(getString(R.string.choose_sim_could_not_activate));
                    mIsSwitching = false;
                    // We don't call finish() and just stay on this page.
                    break;
            }
        }
    }

    private void displaySubscriptions() {
        View rootView = findViewById(android.R.id.content);
        GlifRecyclerLayout layout = rootView.findViewById(R.id.glif_layout);
        RecyclerItemAdapter adapter = (RecyclerItemAdapter) layout.getAdapter();
        adapter.setOnItemSelectedListener(this);
        mItemGroup = (ItemGroup) adapter.getRootItemHierarchy();

        // Display pSIM profile.
        if (mHasPsim) {
            Item item = new DisableableItem();
            // Title
            CharSequence title = null;
            if (mRemovableSubscription != null) {
                title =
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(
                                mRemovableSubscription.getSubscriptionId(), this);
            }
            item.setTitle(TextUtils.isEmpty(title) ? getString(R.string.sim_card_label) : title);

            if (mIsSwitching && mSelectedItemIndex == INDEX_PSIM) {
                item.setSummary(getString(R.string.choose_sim_activating));
            } else {
                // Phone number
                String phoneNumber =
                        SubscriptionUtil.getFormattedPhoneNumber(this, mRemovableSubscription);
                item.setSummary(TextUtils.isEmpty(phoneNumber) ? "" : phoneNumber);
            }

            // pSIM profile has index -1.
            item.setId(INDEX_PSIM);
            mItemGroup.addChild(item);
        }

        // Display all eSIM profiles.
        int index = 0;
        for (SubscriptionInfo sub : mEmbeddedSubscriptions) {
            Item item = new DisableableItem();
            CharSequence title =
                    SubscriptionUtil.getUniqueSubscriptionDisplayName(
                            sub.getSubscriptionId(), this);
            item.setTitle(TextUtils.isEmpty(title) ? sub.getDisplayName() : title);
            if (mIsSwitching && mSelectedItemIndex == index) {
                item.setSummary(getString(R.string.choose_sim_activating));
            } else {
                String phoneNumber = SubscriptionUtil.getFormattedPhoneNumber(this, sub);
                item.setSummary(TextUtils.isEmpty(phoneNumber) ? "" : phoneNumber);
            }
            item.setId(index++);
            mItemGroup.addChild(item);
        }
    }

    private void updateSubscriptions() {
        List<SubscriptionInfo> subscriptions =
                SubscriptionUtil.getSelectableSubscriptionInfoList(this);
        if (subscriptions != null) {
            for (SubscriptionInfo sub : subscriptions) {
                if (sub == null) {
                    continue;
                }
                if (sub.isEmbedded()) {
                    mEmbeddedSubscriptions.add(sub);
                } else {
                    mRemovableSubscription = sub;
                }
            }
        }
    }

    private void handleEnableRemovableSimError() {
        // mSelectedItemIndex will be -1 if pSIM is selected. Since pSIM is always be
        // listed at index 0, we change the itemIndex to 0 if pSIM is selected.
        int itemIndex = mSelectedItemIndex == INDEX_PSIM ? 0 : mSelectedItemIndex;
        Item item = (Item) mItemGroup.getItemAt(itemIndex);
        item.setEnabled(false);
        item.setSummary(getString(R.string.choose_sim_could_not_activate));
        mIsSwitching = false;
    }

    class DisableableItem extends Item implements Dividable {
        @Override
        public boolean isDividerAllowedAbove() {
            return true;
        }

        @Override
        public boolean isDividerAllowedBelow() {
            return true;
        }

        @Override
        public void onBindView(View view) {
            super.onBindView(view);
            TextView title = view.findViewById(com.google.android.setupdesign.R.id.sud_items_title);
            TextView summary =
                    view.findViewById(com.google.android.setupdesign.R.id.sud_items_summary);
            title.setEnabled(isEnabled());
            summary.setEnabled(isEnabled());
        }
    }
}

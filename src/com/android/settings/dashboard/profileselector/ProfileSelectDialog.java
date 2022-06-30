/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.dashboard.profileselector;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.android.internal.widget.DialogTitle;
import com.android.internal.widget.LinearLayoutManager;
import com.android.internal.widget.RecyclerView;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/**
 * A {@link DialogFragment} that can select one of the different profiles.
 */
public class ProfileSelectDialog extends DialogFragment implements UserAdapter.OnClickListener {

    private static final String TAG = "ProfileSelectDialog";
    private static final String ARG_SELECTED_TILE = "selectedTile";
    private static final String ARG_SOURCE_METRIC_CATEGORY = "sourceMetricCategory";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private int mSourceMetricCategory;
    private Tile mSelectedTile;
    private OnShowListener mOnShowListener;
    private OnCancelListener mOnCancelListener;
    private OnDismissListener mOnDismissListener;

    /**
     * Display the profile select dialog, adding the fragment to the given FragmentManager.
     *
     * @param manager              The FragmentManager this fragment will be added to.
     * @param tile                 The tile for this fragment.
     * @param sourceMetricCategory The source metric category.
     * @param onShowListener       The listener listens to the dialog showing event.
     * @param onDismissListener    The listener listens to the dialog dismissing event.
     * @param onCancelListener     The listener listens to the dialog cancelling event.
     */
    public static void show(FragmentManager manager, Tile tile, int sourceMetricCategory,
            OnShowListener onShowListener, OnDismissListener onDismissListener,
            OnCancelListener onCancelListener) {
        final ProfileSelectDialog dialog = new ProfileSelectDialog();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_SELECTED_TILE, tile);
        args.putInt(ARG_SOURCE_METRIC_CATEGORY, sourceMetricCategory);
        dialog.setArguments(args);
        dialog.mOnShowListener = onShowListener;
        dialog.mOnDismissListener = onDismissListener;
        dialog.mOnCancelListener = onCancelListener;
        dialog.show(manager, "select_profile");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = requireArguments();
        mSelectedTile = arguments.getParcelable(ARG_SELECTED_TILE, Tile.class);
        mSourceMetricCategory = arguments.getInt(ARG_SOURCE_METRIC_CATEGORY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(getContext(), mSelectedTile.userHandle, this);
    }

    /**
     * Creates the profile select dialog.
     */
    public static Dialog createDialog(Context context, List<UserHandle> userProfiles,
            UserAdapter.OnClickListener onClickListener) {
        LayoutInflater layoutInflater = context.getSystemService(LayoutInflater.class);

        DialogTitle titleView =
                (DialogTitle) layoutInflater.inflate(R.layout.user_select_title, null);
        titleView.setText(com.android.settingslib.R.string.choose_profile);

        View contentView = layoutInflater.inflate(R.layout.user_select, null);

        RecyclerView listView = contentView.findViewById(R.id.list);
        listView.setAdapter(
                UserAdapter.createUserRecycleViewAdapter(context, userProfiles, onClickListener));
        listView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        return new AlertDialog.Builder(context)
                .setCustomTitle(titleView)
                .setView(contentView)
                .create();
    }

    @Override
    public void onClick(int position) {
        final UserHandle user = mSelectedTile.userHandle.get(position);
        // Show menu on top level items.
        final Intent intent = new Intent(mSelectedTile.getIntent());
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider()
                .logStartedIntentWithProfile(intent, mSourceMetricCategory,
                        position == 1 /* isWorkProfile */);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivityAsUser(intent, user);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        // The fragment shows the dialog within onStart()
        if (mOnShowListener != null) {
            mOnShowListener.onShow(getDialog());
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mOnCancelListener != null) {
            mOnCancelListener.onCancel(dialog);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss(dialog);
        }
    }

    public static void updateUserHandlesIfNeeded(Context context, Tile tile) {
        final List<UserHandle> userHandles = tile.userHandle;
        if (tile.userHandle == null || tile.userHandle.size() <= 1) {
            return;
        }
        final UserManager userManager = UserManager.get(context);
        for (int i = userHandles.size() - 1; i >= 0; i--) {
            if (userManager.getUserInfo(userHandles.get(i).getIdentifier()) == null) {
                if (DEBUG) {
                    Log.d(TAG, "Delete the user: " + userHandles.get(i).getIdentifier());
                }
                userHandles.remove(i);
            }
        }
    }
}

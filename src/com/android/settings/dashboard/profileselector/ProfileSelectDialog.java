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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

public class ProfileSelectDialog extends DialogFragment implements OnClickListener {

    private static final String TAG = "ProfileSelectDialog";
    private static final String ARG_SELECTED_TILE = "selectedTile";
    private static final String ARG_SOURCE_METRIC_CATEGORY = "sourceMetricCategory";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private int mSourceMetricCategory;
    private Tile mSelectedTile;

    /**
     * Display the profile select dialog, adding the fragment to the given FragmentManager.
     * @param manager The FragmentManager this fragment will be added to.
     * @param tile The tile for this fragment.
     * @param sourceMetricCategory The source metric category.
     */
    public static void show(FragmentManager manager, Tile tile, int sourceMetricCategory) {
        final ProfileSelectDialog dialog = new ProfileSelectDialog();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_SELECTED_TILE, tile);
        args.putInt(ARG_SOURCE_METRIC_CATEGORY, sourceMetricCategory);
        dialog.setArguments(args);
        dialog.show(manager, "select_profile");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedTile = getArguments().getParcelable(ARG_SELECTED_TILE);
        mSourceMetricCategory = getArguments().getInt(ARG_SOURCE_METRIC_CATEGORY);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final UserAdapter adapter = UserAdapter.createUserAdapter(UserManager.get(context), context,
                mSelectedTile.userHandle);
        builder.setTitle(com.android.settingslib.R.string.choose_profile)
                .setAdapter(adapter, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final UserHandle user = mSelectedTile.userHandle.get(which);
        // Show menu on top level items.
        final Intent intent = mSelectedTile.getIntent();
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider()
                .logStartedIntentWithProfile(intent, mSourceMetricCategory,
                        which == 1 /* isWorkProfile */);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivityAsUser(intent, user);
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

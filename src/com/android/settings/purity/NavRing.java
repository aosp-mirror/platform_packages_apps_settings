/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.purity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import static com.android.internal.util.cm.NavigationRingConstants.*;
import com.android.internal.util.cm.NavigationRingHelpers;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;

public class NavRing extends Fragment implements
        ShortcutPickHelper.OnPickListener, GlowPadView.OnTriggerListener {
    private GlowPadView mGlowPadView;
    private ShortcutPickHelper mPicker;
    private String[] mTargetActivities;
    private ViewGroup mContainer;

    private int mTargetIndex = 0;
    private int mStartPosOffset;
    private int mEndPosOffset;

    private boolean mIsLandscape;
    private boolean mIsScreenLarge;

    private ActionHolder mActions;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;

    private class ActionHolder {
        private ArrayList<CharSequence> mAvailableEntries = new ArrayList<CharSequence>();
        private ArrayList<String> mAvailableValues = new ArrayList<String>();

        public void addAction(String action, int entryResId) {
            mAvailableEntries.add(getString(entryResId));
            mAvailableValues.add(action);
        }
        public int getActionIndex(String action) {
            int count = mAvailableValues.size();
            for (int i = 0; i < count; i++) {
                if (TextUtils.equals(mAvailableValues.get(i), action)) {
                    return i;
                }
            }
            return -1;
        }
        public String getAction(int index) {
            if (index > mAvailableValues.size()) {
                return null;
            }
            return mAvailableValues.get(index);
        }
        public CharSequence[] getEntries() {
            return mAvailableEntries.toArray(new CharSequence[mAvailableEntries.size()]);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;
        setHasOptionsMenu(true);
        createActionList();

        mIsScreenLarge = !Utils.isPhone(getActivity());
        mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        mPicker = new ShortcutPickHelper(getActivity(), this);

        return inflater.inflate(R.layout.navigation_ring_targets, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGlowPadView = ((GlowPadView) getActivity().findViewById(R.id.navring_target));
        mGlowPadView.setOnTriggerListener(this);
        updateDrawables();
    }

    private void createActionList() {
        mActions = new ActionHolder();
        mActions.addAction(ACTION_NONE, R.string.navring_action_none);

        if (NavigationRingHelpers.isAssistantAvailable(getActivity())) {
            mActions.addAction(ACTION_ASSIST, R.string.navring_action_google_now);
        }
        if (NavigationRingHelpers.isTorchAvailable(getActivity())) {
            mActions.addAction(ACTION_TORCH, R.string.navring_action_torch);
        }

        mActions.addAction(ACTION_SCREENSHOT, R.string.navring_action_take_screenshot);
        mActions.addAction(ACTION_IME, R.string.navring_action_open_ime_switcher);
        mActions.addAction(ACTION_SILENT, R.string.navring_action_ring_silent);

        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            mActions.addAction(ACTION_VIBRATE, R.string.navring_action_ring_vibrate);
            mActions.addAction(ACTION_RING_SILENT_VIBRATE, R.string.navring_action_ring_vibrate_silent);
        }

        mActions.addAction(ACTION_KILL, R.string.navring_action_kill_app);
        mActions.addAction(ACTION_POWER, R.string.navring_action_screen_off);

        mActions.addAction(ACTION_APP, R.string.select_application);
    }

    private void setDrawables() {
        final ArrayList<TargetDrawable> targets = new ArrayList<TargetDrawable>();
        final Context context = getActivity();

        if (!mIsLandscape || mIsScreenLarge) {
            mStartPosOffset =  1;
            mEndPosOffset = 4;
        } else {
            mStartPosOffset = 3;
            mEndPosOffset =  2;
        }

         // Add Initial Place Holder Targets
        for (int i = 0; i < mStartPosOffset; i++) {
            targets.add(NavigationRingHelpers.getTargetDrawable(context, null));
        }
        // Add User Targets
        for (int i = 0; i < mTargetActivities.length; i++) {
            final TargetDrawable drawable =
                    NavigationRingHelpers.getTargetDrawable(context, mTargetActivities[i]);
            // we also want empty targets to be selectable here
            drawable.setEnabled(true);
            targets.add(drawable);
        }

        // Add End Place Holder Targets
        for (int i = 0; i < mEndPosOffset; i++) {
            targets.add(NavigationRingHelpers.getTargetDrawable(context, null));
        }

        mGlowPadView.setTargetResources(targets);
        NavigationRingHelpers.swapSearchIconIfNeeded(context, mGlowPadView);
    }

    @Override
    public void onResume() {
        super.onResume();

        // If running on a phone, remove padding around container
        if (!mIsScreenLarge) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
            .setIcon(R.drawable.ic_settings_backup)
            .setAlphabeticShortcut('r')
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;
            default:
                return false;
        }
    }

    private void resetAll() {
        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.lockscreen_target_reset_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.navring_target_reset_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        NavigationRingHelpers.resetActionsToDefaults(getActivity());
                        updateDrawables();
                        Toast.makeText(getActivity(),
                                R.string.navring_target_reset,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        d.show();
    }

    private void saveAll() {
        final ContentResolver cr = getActivity().getContentResolver();
        for (int i = 0; i < mTargetActivities.length; i++) {
            Settings.System.putString(cr,
                    Settings.System.NAVIGATION_RING_TARGETS[i], mTargetActivities[i]);
        }
        setDrawables();
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        mTargetActivities[mTargetIndex] = uri;
        saveAll();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPicker.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateDrawables() {
        mTargetActivities = NavigationRingHelpers.getTargetActions(getActivity());
        setDrawables();
    }

    private void onTargetChange(String uri) {
        if (uri.equals(ACTION_APP)) {
            final String label = getResources().getString(R.string.lockscreen_target_empty);
            final ShortcutIconResource iconResource =
                    ShortcutIconResource.fromContext(getActivity(), android.R.drawable.ic_delete);
            mPicker.pickShortcut(
                    new String[] { label },
                    new ShortcutIconResource[] { iconResource },
                    getId());
        } else {
            mTargetActivities[mTargetIndex] = uri;
            saveAll();
        }
    }

    @Override
    public void onTrigger(View v, final int target) {
        mTargetIndex = target - mStartPosOffset;

        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                onTargetChange(mActions.getAction(item));
                dialog.dismiss();
            }
        };

        final int selection = mActions.getActionIndex(mTargetActivities[mTargetIndex]);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.navring_choose_action_title)
                .setSingleChoiceItems(mActions.getEntries(), selection, l)
                .create();

        dialog.show();
    }

    @Override
    public void onGrabbed(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    @Override
    public void onFinishFinalAnimation() {
    }
}

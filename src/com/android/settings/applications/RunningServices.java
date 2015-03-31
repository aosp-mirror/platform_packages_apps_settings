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
package com.android.settings.applications;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.Utils;

public class RunningServices extends Fragment {

    private static final int SHOW_RUNNING_SERVICES = 1;
    private static final int SHOW_BACKGROUND_PROCESSES = 2;

    private RunningProcessesView mRunningProcessesView;
    private Menu mOptionsMenu;
    private View mLoadingContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.manage_applications_running, null);
        mRunningProcessesView = (RunningProcessesView) rootView.findViewById(
                R.id.running_processes);
        mRunningProcessesView.doCreate();
        mLoadingContainer = rootView.findViewById(R.id.loading_container);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;
        menu.add(0, SHOW_RUNNING_SERVICES, 1, R.string.show_running_services)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, SHOW_BACKGROUND_PROCESSES, 2, R.string.show_background_processes)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        updateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
        Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, haveData, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRunningProcessesView.doPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SHOW_RUNNING_SERVICES:
                mRunningProcessesView.mAdapter.setShowBackground(false);
                break;
            case SHOW_BACKGROUND_PROCESSES:
                mRunningProcessesView.mAdapter.setShowBackground(true);
                break;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        boolean showingBackground = mRunningProcessesView.mAdapter.getShowBackground();
        mOptionsMenu.findItem(SHOW_RUNNING_SERVICES).setVisible(showingBackground);
        mOptionsMenu.findItem(SHOW_BACKGROUND_PROCESSES).setVisible(!showingBackground);
    }

    private final Runnable mRunningProcessesAvail = new Runnable() {
        @Override
        public void run() {
            Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, true, true);
        }
    };

}

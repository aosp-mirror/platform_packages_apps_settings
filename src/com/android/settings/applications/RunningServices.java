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

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.LoadingViewController;

public class RunningServices extends SettingsPreferenceFragment {

    private static final int SHOW_RUNNING_SERVICES = 1;
    private static final int SHOW_BACKGROUND_PROCESSES = 2;

    private RunningProcessesView mRunningProcessesView;
    private Menu mOptionsMenu;
    private View mLoadingContainer;
    private LoadingViewController mLoadingViewController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle(R.string.runningservices_settings_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.manage_applications_running, null);
        mRunningProcessesView = rootView.findViewById(R.id.running_processes);
        mRunningProcessesView.doCreate();
        mLoadingContainer = rootView.findViewById(R.id.loading_container);
        mLoadingViewController = new LoadingViewController(
                mLoadingContainer, mRunningProcessesView);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;
        menu.add(0, SHOW_RUNNING_SERVICES, 1, R.string.show_running_services)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, SHOW_BACKGROUND_PROCESSES, 2, R.string.show_background_processes)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        updateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
        mLoadingViewController.handleLoadingContainer(haveData /* done */, false /* animate */);
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

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RUNNING_SERVICES;
    }

    private final Runnable mRunningProcessesAvail = new Runnable() {
        @Override
        public void run() {
            mLoadingViewController.showContent(true /* animate */);
        }
    };

}

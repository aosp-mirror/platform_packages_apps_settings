/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.network;

import android.annotation.Nullable;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;

import java.util.List;

/**
 * Wrapper around {@link NetworkScoreManager} to facilitate unit testing.
 *
 * TODO: delete this class once robolectric supports Android O
 */
public class NetworkScoreManagerWrapper {
    private final NetworkScoreManager mNetworkScoreManager;

    public NetworkScoreManagerWrapper(NetworkScoreManager networkScoreManager) {
        mNetworkScoreManager = networkScoreManager;
    }

    /**
     * Returns the list of available scorer apps. The list will be empty if there are
     * no valid scorers.
     */
    public List<NetworkScorerAppData> getAllValidScorers() {
        return mNetworkScoreManager.getAllValidScorers();
    }

    /**
     * Obtain the package name of the current active network scorer.
     *
     * <p>At any time, only one scorer application will receive {@link #ACTION_SCORE_NETWORKS}
     * broadcasts and be allowed to call {@link #updateScores}. Applications may use this method to
     * determine the current scorer and offer the user the ability to select a different scorer via
     * the {@link #ACTION_CHANGE_ACTIVE} intent.
     * @return the full package name of the current active scorer, or null if there is no active
     *         scorer.
     */
    @Nullable
    public String getActiveScorerPackage() {
        return mNetworkScoreManager.getActiveScorerPackage();
    }

    /**
     * Returns metadata about the active scorer or <code>null</code> if there is no active scorer.
     */
    @Nullable
    public NetworkScorerAppData getActiveScorer() {
        return mNetworkScoreManager.getActiveScorer();
    }

    /**
     * Set the active scorer to a new package and clear existing scores.
     *
     * <p>Should never be called directly without obtaining user consent. This can be done by using
     * the {@link #ACTION_CHANGE_ACTIVE} broadcast, or using a custom configuration activity.
     *
     * @return true if the operation succeeded, or false if the new package is not a valid scorer.
     * @throws SecurityException if the caller is not a system process or does not hold the
     *         {@link android.Manifest.permission#REQUEST_NETWORK_SCORES} permission
     */
    public boolean setActiveScorer(String packageName) throws SecurityException {
        return mNetworkScoreManager.setActiveScorer(packageName);
    }
}
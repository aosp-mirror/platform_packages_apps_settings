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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.settings.R;
import com.android.settings.Settings.NetworkProviderSettingsActivity;
import com.android.settings.fuelgauge.batterysaver.BatterySaverButtonPreferenceControllerComponentTest;
import com.android.settings.network.NetworkProviderSettings;
import com.android.settings.testutils.CommonUtils;
import com.android.settings.testutils.Constants;
import com.android.settings.testutils.UiUtils;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@MediumTest
/*
This test is just for demonstration purpose. For component tests, this approach is not recommended.
The reason why it is written this way is because the current Settings app wifi codes have tight
coupling with UI, so it's not easy to drive from API without binding the test deeply with the code.
 */
public class WifiSettings2ActivityTest {
    private static final String TAG =
            BatterySaverButtonPreferenceControllerComponentTest.class.getSimpleName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Test @Ignore
    public void test_connect_to_wifi() throws Exception {
        //For some reason the ActivityScenario gets null activity here
        mInstrumentation.getTargetContext().startActivity(
                new Intent(Settings.ACTION_WIFI_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        UiUtils.waitForActivitiesInStage(Constants.ACTIVITY_LAUNCH_WAIT_TIMEOUT, Stage.RESUMED);

        final NetworkProviderSettings[] settings = new NetworkProviderSettings[1];
        mInstrumentation.runOnMainSync(() -> {
            NetworkProviderSettingsActivity activity = (NetworkProviderSettingsActivity)
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                            Stage.RESUMED).iterator().next();
            settings[0] =
                    (NetworkProviderSettings) activity.getSupportFragmentManager().getFragments()
                            .get(0);
        });

        //For some reason this view does not appear immediately after the fragment is resumed.
        View root = settings[0].getView();
        UiUtils.waitUntilCondition(Constants.VIEW_APPEAR_WAIT_MEDIUM_TIMEOUT,
                () -> root.findViewById(R.id.settings_button) != null);
        View view = root.findViewById(R.id.settings_button);
        view.callOnClick();

        UiUtils.waitForActivitiesInStage(Constants.ACTIVITY_LAUNCH_WAIT_TIMEOUT, Stage.RESUMED);
        Button[] button = new Button[1];
        mInstrumentation.runOnMainSync(() -> {
            FragmentActivity activity =
                    (FragmentActivity) ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED).iterator().next();
            List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
            Log.d(TAG, "fragment class is " + fragments.get(0).getClass());
            button[0] = fragments.get(0).getView().findViewById(R.id.button3);
        });

        //HttpURLConnection needs to run outside of main thread, so running it in the test thread
        final URL url = new URL("https://www.google.net/");

        //Make sure the connectivity is available before disconnecting from wifi
        assertThat(CommonUtils.connectToURL(url)).isTrue();

        //Disconnect from wifi
        button[0].callOnClick();

        //Make sure the Internet connectivity is gone
        assertThat(CommonUtils.connectToURL(url)).isFalse();

        //Connect to wifi
        button[0].callOnClick();
        ConnectivityManager manager =
                (ConnectivityManager) mInstrumentation.getTargetContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        //For some reason I can't find a way to tell the time that the internet connectivity is
        //actually available with the new, non-deprecated ways, so I still need to use this.
        UiUtils.waitUntilCondition(Constants.WIFI_CONNECT_WAIT_TIMEOUT,
                () -> manager.getActiveNetworkInfo().isConnected());

        //Make sure the connectivity is back again
        assertThat(CommonUtils.connectToURL(url)).isTrue();
    }
}

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

package com.android.settings.testutils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class CommonUtils {
    private static final String TAG = CommonUtils.class.getSimpleName();

    public static void takeScreenshot(Activity activity) {
        long now = System.currentTimeMillis();

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath =
                    Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";
            Log.d(TAG, "screenshot path is " + mPath);

            // create bitmap screen capture
            View v1 = activity.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    public static boolean connectToURL(URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.connect();
            if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while (null != (line = reader.readLine())) {
                    response.append(line);
                }
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return false;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }

        return false;
    }

    /**
     * Return a resource identifier for the given resource name in Settings app.
     *
     * @param name The name of the desired resource.
     * @return int The associated resource identifier.  Returns 0 if no such resource was found.  (0
     * is not a valid resource ID.)
     */
    public static int getResId(String name) {
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getResources()
                .getIdentifier(name, "id", Constants.SETTINGS_PACKAGE_NAME);
    }
}

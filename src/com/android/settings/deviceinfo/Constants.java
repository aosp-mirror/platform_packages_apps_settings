/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import com.android.settings.R;

import android.os.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Some of the constants used in this package
 */
class Constants {
    static final int MEDIA_INDEX = 0;
    static final int DOWNLOADS_INDEX = 1;
    static final int PIC_VIDEO_INDEX = 2;
    static final int MUSIC_INDEX = 3;
    static final int MEDIA_APPS_DATA_INDEX = 4;
    static final int MEDIA_MISC_INDEX = 5;
    static final int NUM_MEDIA_DIRS_TRACKED = MEDIA_MISC_INDEX + 1;

    static class MediaDirectory {
        final String[] mDirPaths;
        final String mKey;
        final String mPreferenceName;
        final int mColor; // Required when mPreferenceName is not null
        MediaDirectory(String pref, String debugInfo, int color, String... paths) {
            mPreferenceName = pref;
            mKey = debugInfo;
            mColor = color;
            mDirPaths = paths;
        }
    }
    static final ArrayList<MediaDirectory> mMediaDirs = new ArrayList<MediaDirectory>();
    static final List<String> ExclusionTargetsForMiscFiles = new ArrayList<String>();
    static {
        mMediaDirs.add(MEDIA_INDEX,
                new MediaDirectory(null,
                        "/sdcard",
                        0,
                        Environment.getExternalStorageDirectory().getAbsolutePath()));
        mMediaDirs.add(DOWNLOADS_INDEX,
                new MediaDirectory("memory_internal_downloads",
                        "/sdcard/download",
                        R.color.memory_downloads,
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        mMediaDirs.add(PIC_VIDEO_INDEX,
                new MediaDirectory("memory_internal_dcim",
                        "/sdcard/pic_video",
                        R.color.memory_video,
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MOVIES).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath()));
        mMediaDirs.add(MUSIC_INDEX,
                new MediaDirectory("memory_internal_music",
                        "/sdcard/audio",
                        R.color.memory_audio,
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_ALARMS).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_RINGTONES).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PODCASTS).getAbsolutePath()));
        mMediaDirs.add(MEDIA_APPS_DATA_INDEX,
                new MediaDirectory(null,
                        "/sdcard/Android",
                        0,
                        Environment.getExternalStorageAndroidDataDir().getAbsolutePath()));
        mMediaDirs.add(MEDIA_MISC_INDEX,
                new MediaDirectory("memory_internal_media_misc",
                        "misc on /sdcard",
                        R.color.memory_misc,
                        new String[] {})); // No associated directory to add to exclusion list
        // prepare a lit of strings representing dirpaths that should be skipped while looking
        // for 'other' files
        for (int j = 0; j < Constants.NUM_MEDIA_DIRS_TRACKED; j++) {
            String[] dirs = Constants.mMediaDirs.get(j).mDirPaths;
            int len = dirs.length;
            for (int k = 0; k < len; k++) {
                ExclusionTargetsForMiscFiles.add(dirs[k]);
            }
        }
        // also add /sdcard/Android
        ExclusionTargetsForMiscFiles.add(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android");
    }
}

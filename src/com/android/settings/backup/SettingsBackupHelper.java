/**
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.os.ParcelFileDescriptor;

import com.android.settings.shortcut.CreateShortcutPreferenceController;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Backup agent for Settings APK
 */
public class SettingsBackupHelper extends BackupAgentHelper {

    @Override
    public void onCreate() {
        super.onCreate();
        addHelper("no-op", new NoOpHelper());
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        CreateShortcutPreferenceController.updateRestoredShortcuts(this);
    }

    /**
     * Backup helper which does not do anything. Having at least one helper ensures that the
     * transport is not empty and onRestoreFinished is called eventually.
     */
    private static class NoOpHelper implements BackupHelper {

        private final int VERSION_CODE = 1;

        @Override
        public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                ParcelFileDescriptor newState) {

            try (FileOutputStream out = new FileOutputStream(newState.getFileDescriptor())) {
                if (getVersionCode(oldState) != VERSION_CODE) {
                    data.writeEntityHeader("dummy", 1);
                    data.writeEntityData(new byte[1], 1);
                }

                // Write new version code
                out.write(VERSION_CODE);
                out.flush();
            } catch (IOException e) { }
        }

        @Override
        public void restoreEntity(BackupDataInputStream data) { }

        @Override
        public void writeNewStateDescription(ParcelFileDescriptor newState) { }

        private int getVersionCode(ParcelFileDescriptor state) {
            if (state == null) {
                return 0;
            }
            try (FileInputStream in = new FileInputStream(state.getFileDescriptor())) {
                return in.read();
            } catch (IOException e) {
                return 0;
            }
        }
    }
}

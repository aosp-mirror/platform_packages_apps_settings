/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import com.android.settings.deletionhelper.FetchDownloadsLoader.DownloadsResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class FetchDownloadsLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testEmptyDirectory() throws Exception {
        DownloadsResult result =
                FetchDownloadsLoader.collectFiles(temporaryFolder.getRoot());
        assertNotNull(result);
        assertEquals(0, result.totalSize);
        assertEquals(0, result.files.size());
    }

    @Test
    public void testFilesInDirectory() throws Exception {
        temporaryFolder.newFile();
        temporaryFolder.newFile();

        DownloadsResult result =
                FetchDownloadsLoader.collectFiles(temporaryFolder.getRoot());
        assertNotNull(result);
        assertEquals(0, result.totalSize);
        assertEquals(2, result.files.size());
    }

    @Test
    public void testNestedDirectories() throws Exception {
        File tempDir = temporaryFolder.newFolder();

        File testFile = File.createTempFile("test", null, tempDir);
        testFile.deleteOnExit();
        DownloadsResult result =
                FetchDownloadsLoader.collectFiles(temporaryFolder.getRoot());
        assertNotNull(result);
        assertEquals(0, result.totalSize);
        assertEquals(1, result.files.size());
    }

    @Test
    public void testSumFileSizes() throws Exception {
        File first = temporaryFolder.newFile();
        FileWriter fileWriter = new FileWriter(first);
        fileWriter.write("test");
        fileWriter.close();

        File second = temporaryFolder.newFile();
        fileWriter = new FileWriter(second);
        fileWriter.write("test2");
        fileWriter.close();

        DownloadsResult result =
                FetchDownloadsLoader.collectFiles(temporaryFolder.getRoot());
        assertNotNull(result);
        assertEquals(9, result.totalSize);
        assertEquals(2, result.files.size());
    }
}

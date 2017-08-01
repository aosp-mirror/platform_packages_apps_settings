/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.graph;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseIntArray;

import com.android.settings.TestConfig;
import com.android.settingslib.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsageGraphTest {
    private UsageGraph mGraph;

    @Before
    public void setUp() {
        // Set up a graph view of width 1000, height 200, and corner radius 5.
        Context context = spy(RuntimeEnvironment.application);
        Resources resources = spy(context.getResources());
        doReturn(resources).when(context).getResources();
        doReturn(5).when(resources).getDimensionPixelSize(R.dimen.usage_graph_line_corner_radius);
        doReturn(1).when(resources).getDimensionPixelSize(R.dimen.usage_graph_line_width);
        doReturn(1).when(resources).getDimensionPixelSize(R.dimen.usage_graph_dot_size);
        doReturn(1).when(resources).getDimensionPixelSize(R.dimen.usage_graph_dot_interval);
        doReturn(1).when(resources).getDimensionPixelSize(R.dimen.usage_graph_divider_size);
        mGraph = spy(new UsageGraph(context, null));
        doReturn(1000).when(mGraph).getWidth();
        doReturn(200).when(mGraph).getHeight();

        // Set the conceptual size of the graph to 500ms x 100%.
        mGraph.setMax(500, 100);
    }

    @Test
    public void testCalculateLocalPaths_singlePath() {
        SparseIntArray paths = new SparseIntArray();
        paths.append(0, 100);
        paths.append(500, 50);
        paths.append(501, -1);

        SparseIntArray localPaths = new SparseIntArray();
        mGraph.calculateLocalPaths(paths, localPaths);

        assertThat(localPaths.size()).isEqualTo(3);
        assertThat(localPaths.keyAt(0)).isEqualTo(0);
        assertThat(localPaths.valueAt(0)).isEqualTo(0);
        assertThat(localPaths.keyAt(1)).isEqualTo(1000);
        assertThat(localPaths.valueAt(1)).isEqualTo(100);
        assertThat(localPaths.keyAt(2)).isEqualTo(1001);
        assertThat(localPaths.valueAt(2)).isEqualTo(-1);
    }

    @Test
    public void testCalculateLocalPaths_multiplePaths() {
        SparseIntArray paths = new SparseIntArray();
        paths.append(0, 100);
        paths.append(200, 75);
        paths.append(201, -1);

        paths.append(300, 50);
        paths.append(500, 25);
        paths.append(501, -1);

        SparseIntArray localPaths = new SparseIntArray();
        mGraph.calculateLocalPaths(paths, localPaths);

        assertThat(localPaths.size()).isEqualTo(6);

        assertThat(localPaths.keyAt(0)).isEqualTo(0);
        assertThat(localPaths.valueAt(0)).isEqualTo(0);
        assertThat(localPaths.keyAt(1)).isEqualTo(400);
        assertThat(localPaths.valueAt(1)).isEqualTo(50);
        assertThat(localPaths.keyAt(2)).isEqualTo(401);
        assertThat(localPaths.valueAt(2)).isEqualTo(-1);

        assertThat(localPaths.keyAt(3)).isEqualTo(600);
        assertThat(localPaths.valueAt(3)).isEqualTo(100);
        assertThat(localPaths.keyAt(4)).isEqualTo(1000);
        assertThat(localPaths.valueAt(4)).isEqualTo(150);
        assertThat(localPaths.keyAt(5)).isEqualTo(1001);
        assertThat(localPaths.valueAt(5)).isEqualTo(-1);
    }

    @Test
    public void testCalculateLocalPaths_similarPointMiddle() {
        SparseIntArray paths = new SparseIntArray();
        paths.append(0, 100);
        paths.append(1, 99); // This point should be omitted.
        paths.append(500, 50);
        paths.append(501, -1);

        SparseIntArray localPaths = new SparseIntArray();
        mGraph.calculateLocalPaths(paths, localPaths);

        assertThat(localPaths.size()).isEqualTo(3);
        assertThat(localPaths.keyAt(0)).isEqualTo(0);
        assertThat(localPaths.valueAt(0)).isEqualTo(0);
        assertThat(localPaths.keyAt(1)).isEqualTo(1000);
        assertThat(localPaths.valueAt(1)).isEqualTo(100);
        assertThat(localPaths.keyAt(2)).isEqualTo(1001);
        assertThat(localPaths.valueAt(2)).isEqualTo(-1);
    }

    @Test
    public void testCalculateLocalPaths_similarPointEnd() {
        SparseIntArray paths = new SparseIntArray();
        paths.append(0, 100);
        paths.append(499, 51);
        paths.append(500, 50); // This point should be kept: it's the last one.
        paths.append(501, -1);

        SparseIntArray localPaths = new SparseIntArray();
        mGraph.calculateLocalPaths(paths, localPaths);

        assertThat(localPaths.size()).isEqualTo(4);
        assertThat(localPaths.keyAt(0)).isEqualTo(0);
        assertThat(localPaths.valueAt(0)).isEqualTo(0);
        assertThat(localPaths.keyAt(1)).isEqualTo(998);
        assertThat(localPaths.valueAt(1)).isEqualTo(98);
        assertThat(localPaths.keyAt(2)).isEqualTo(1000);
        assertThat(localPaths.valueAt(2)).isEqualTo(100);
        assertThat(localPaths.keyAt(3)).isEqualTo(1001);
        assertThat(localPaths.valueAt(3)).isEqualTo(-1);
    }
}

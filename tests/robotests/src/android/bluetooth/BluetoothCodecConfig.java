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

package android.bluetooth;

/**
 * A placeholder class to prevent ClassNotFound exceptions caused by lack of visibility.
 */
public class BluetoothCodecConfig {

    public static final int SAMPLE_RATE_NONE = 0;
    public static final int SAMPLE_RATE_48000 = 0x1 << 1;
    public static final int SOURCE_CODEC_TYPE_INVALID = 1000 * 1000;
    public static final int CODEC_PRIORITY_DEFAULT = 0;
    public static final int BITS_PER_SAMPLE_NONE = 0;
    public static final int CHANNEL_MODE_NONE = 0;

    public int getSampleRate() {
        return 0;
    }
}

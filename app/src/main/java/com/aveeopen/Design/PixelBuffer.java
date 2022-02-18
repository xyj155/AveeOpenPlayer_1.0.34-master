/*
 * Copyright 2020 Avee Player. All rights reserved.
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

package com.aveeopen.Design;

import java.util.Arrays;

public class PixelBuffer {
    private final byte[] data;
    private final int width;
    private final int height;
    private final int pixelStride;
    private final int rowStride;
    private final long timestamp;

    public PixelBuffer(byte[] data, int width, int height, int pixelStride, int rowStride, long timestamp) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.pixelStride = pixelStride;
        this.rowStride = rowStride;
        this.timestamp = timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixelStride() {
        return pixelStride;
    }

    public int getRowStride() {
        return rowStride;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PixelBuffer{" +
                "data=" + Arrays.toString(data) +
                ", width=" + width +
                ", height=" + height +
                ", pixelStride=" + pixelStride +
                ", rowStride=" + rowStride +
                ", timestamp=" + timestamp +
                '}';
    }
}
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

package com.aveeopen.comp.VisualUI;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaMuxerManager {

    private static final String TAG = MediaMuxerManager.class.getSimpleName();

    private static final String MP4_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4";

    private static MediaMuxerManager instance;

    private MediaMuxer mediaMuxer;

    private volatile boolean isStart = false;

    private int trackCount = 0;

    public static MediaMuxerManager getInstance() {
        if (instance == null) {
            synchronized (MediaMuxerManager.class) {
                if (instance == null) {
                    instance = new MediaMuxerManager();
                }
            }
        }
        return instance;
    }

    public MediaMuxerManager() {

    }

    public void init() {
        try {
            Log.d(TAG, "init");
            mediaMuxer = new MediaMuxer(MP4_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG, "init " + e);
        }

    }

    public int addTrack(MediaFormat mediaFormat) {
        synchronized (instance) {
            Log.d(TAG, "addTrack");
            trackCount++;
            return mediaMuxer.addTrack(mediaFormat);
        }
    }

    public void writeSampleData(int traceIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        synchronized (instance) {
            if (!isStart) {
                return;
            }
            Log.d(TAG, "writeSampleData");
            mediaMuxer.writeSampleData(traceIndex, byteBuffer, bufferInfo);
        }
    }

    public void start() {
        synchronized (instance) {
            //已经开始或者音视频都没有全部添加返回
            if (isStart || trackCount != 2) {
                return;
            }
            Log.d(TAG, "start");
            mediaMuxer.start();
            isStart = true;
        }
    }

    public void close() {
        isStart = false;
        mediaMuxer.stop();
        mediaMuxer.release();
        instance = null;
    }

    public boolean isReady() {
        return trackCount == 2;
    }


}
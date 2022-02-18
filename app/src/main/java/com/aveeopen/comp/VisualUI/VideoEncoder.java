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
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class VideoEncoder {

    private MediaCodec mCodec;
    private Surface mSurface;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();


    public void setLister(EncodeLister lister) {
        mLister = lister;
    }

    private MediaMuxer mMuxer;
    EncodeLister mLister;

    void prepare(int mWidth, int mHeight) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(Config.VIDEO_MIME, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, Config.VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, Config.FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Config.VIDEO_I_FRAME_INTERVAL);

        try {
            mCodec = MediaCodec.createEncoderByType(Config.VIDEO_MIME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mCodec.createInputSurface();
        if (mLister != null) {
            mLister.onSurfaceCreated(mSurface);
        }
        mCodec.start();
        mMuxer = new MediaMuxer("/sdcard/1/testsss.mp4",
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }


    private int mTrackIndex = -1;

    void start(ByteBuffer byteBuffer) {
        int status = mCodec.dequeueOutputBuffer(mBufferInfo, 10000);
        if (status >= 0) {
            // encoded sample
            ByteBuffer data = mCodec.getOutputBuffer(status);
            if (data != null) {
                final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                // pass to whoever listens to
                if (endOfStream == 0 && mLister != null) {
                    mLister.onSampleEncoded(mBufferInfo, data);
                }
                // releasing buffer is important
                mCodec.releaseOutputBuffer(status, false);
                MediaFormat newFormat = mCodec.getOutputFormat();
                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.writeSampleData(mTrackIndex, byteBuffer, mBufferInfo);
                mMuxer.start();
            }
        }

    }

    void stop() {
        if (mCodec != null) {
            mCodec.stop();
            mCodec.release();
        }
        mSurface.release();
    }

    public interface EncodeLister {
        void onSurfaceCreated(Surface surface);

        void onSampleEncoded(MediaCodec.BufferInfo info, ByteBuffer data);
    }

}
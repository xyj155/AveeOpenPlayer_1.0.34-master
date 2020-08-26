/*
 * Copyright 2019 Avee Player. All rights reserved.
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Bundle;
//import android.test.UiThreadTest;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.aveeopen.AveeBufferCallBack;
import com.aveeopen.Common.Events.WeakEvent;
import com.aveeopen.Common.Events.WeakEvent1;
import com.aveeopen.Common.Events.WeakEvent3;
import com.aveeopen.Common.Events.WeakEvent4;
import com.aveeopen.Common.Events.WeakEventR;
import com.aveeopen.Common.Tuple2;
import com.aveeopen.Common.UtilsUI;
import com.aveeopen.ContextData;
import com.aveeopen.comp.LibraryQueueUI.MyView;
import com.aveeopen.comp.Visualizer.Elements.Element;
import com.aveeopen.comp.Visualizer.VisualizerViewCore;
import com.aveeopen.R;
import com.aveeopen.comp.glis.EglCore;
import com.coder.ffmpeg.utils.FFmpegUtils;
import com.google.android.exoplayer.AspectRatioFrameLayout;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.security.Policy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import xyz.mylib.creator.handler.CreatorExecuteResponseHander;
import xyz.mylib.creator.task.AvcExecuteAsyncTask;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class Fragment2 extends Fragment {

    public static WeakEvent1<VisualizerViewCore /*surface*/> onSurfaceCreated = new WeakEvent1<>();
    public static WeakEventR<Boolean> onRequestShowVideoContentState = new WeakEventR<>();
    public static WeakEvent onToggleVideoScalingMode = new WeakEvent();
    public static WeakEventR<Integer> onRequestVideoScalingMode = new WeakEventR<>();
    public static WeakEventR<Float> onRequestVideoWidthHeightRatio = new WeakEventR<>();
    public static WeakEvent onToggleVisualPreferShowContent = new WeakEvent();
    public static WeakEvent1<SurfaceHolder /*holder*/> onVideoSurfaceHolderCreated = new WeakEvent1<>();
    public static WeakEvent onVideoSurfaceHolderDestroyed = new WeakEvent();
    public static WeakEventR<Boolean> onRequestUIComponentNeedChangedValue = new WeakEventR<>();
    public static WeakEvent onVideoElementInteracted = new WeakEvent();
    public static WeakEvent1<Boolean /*need*/> onUIComponentNeedChanged = new WeakEvent1<>();//unused?
    public static WeakEvent onCustomizeAction = new WeakEvent();
    public static WeakEvent4<ContextData /*contextData*/, Integer /*rootIdentifier*/, Element.CustomizationList /*customizationList*/, Integer /*elementIndex*/> onPickElementAction = new WeakEvent4<>();
    public static WeakEvent3<ContextData /*contextData*/, Integer /*rootIdentifier*/, Element.CustomizationList /*customizationList*/> onResetAction = new WeakEvent3<>();

    private View rootView;
    private AspectRatioFrameLayout videoFrame;
    private VisualizerViewCore surfaceViewVisualizer;
    private SurfaceView surfaceViewVideo;
    private int surfaceViewTag = 0;
    private int surfaceViewVideoTag = 0;
    private View layoutButtons;
    private ImageButton btn1;
    private ImageButton btn3;
    private float widthHeightRatio = 0.0f;

    public Fragment2() {
    }

    public static Fragment2 newInstance() {
        Fragment2 fragment = new Fragment2();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private MediaCodec mVideoEncodec;
    private int width = 1080, height = 1920;
    private static final String TAG = "Fragment2";
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLSurface windowSurface;
    private MediaCodec.BufferInfo mVideoBuffInfo;
    private MediaMuxer mediaMuxer;
    private long pts = 0;
    private MediaFormat videoFormat;
    private Surface inputSurface;
    private boolean mMuxerStarted = false;
    private int frameRate = 30;

    private void prepareEncoder() {
        try {
            mediaMuxer = new MediaMuxer("/sdcard/1/temp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoBuffInfo = new MediaCodec.BufferInfo();
            mVideoEncodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);//30帧
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);//RGBA
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mVideoEncodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = mVideoEncodec.createInputSurface();
        } catch (Exception e) {
            Log.i(TAG, "prepareEncoder: " + e.getMessage());
        }


    }

    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        // 停止录制
        if (endOfStream) {
            mVideoEncodec.signalEndOfInputStream();
        }
        //拿到输出缓冲区,用于取到编码后的数据
        ByteBuffer[] encoderOutputBuffers = mVideoEncodec.getOutputBuffers();
        while (true) {
            //拿到输出缓冲区的索引
            int encoderStatus = mVideoEncodec.dequeueOutputBuffer(mVideoBuffInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {

                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //拿到输出缓冲区,用于取到编码后的数据
                encoderOutputBuffers = mVideoEncodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                //
                MediaFormat newFormat = mVideoEncodec.getOutputFormat();
                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mediaMuxer.addTrack(newFormat);
                //
                mediaMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
            } else {
                //获取解码后的数据
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                //
                if ((mVideoBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mVideoBuffInfo.size = 0;
                }
                //
                if (mVideoBuffInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mVideoBuffInfo.offset);
                    encodedData.limit(mVideoBuffInfo.offset + mVideoBuffInfo.size);
                    // 编码
                    mediaMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBuffInfo);
                }
                //释放资源
                mVideoEncodec.releaseOutputBuffer(encoderStatus, false);

                if ((mVideoBuffInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {

                    }
                    break;      // out of while
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_2, container, false);
        setStatusBarDimensions(rootView.findViewById(R.id.viewStatusBarBg));

        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                final int w = right - left;
                final int h = bottom - top;

                rootView.post(new Runnable() {
                    @Override
                    public void run() {
                        updateVideoSize(w, h);
                    }
                });

            }
        });

        layoutButtons = rootView.findViewById(R.id.layoutButtons);

        ImageButton btn0 = (ImageButton) layoutButtons.findViewById(R.id.btn0);
        btn0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createThemeChooserMenu(v);
            }
        });

        btn1 = (ImageButton) layoutButtons.findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCustomizeAction.invoke();
            }
        });

        ImageButton btn4 = (ImageButton) layoutButtons.findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleVisualPreferShowContent.invoke();
            }
        });

        btn3 = (ImageButton) layoutButtons.findViewById(R.id.btn3);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleVideoScalingMode.invoke();
            }
        });
        //

        MyView surfaceViewBackground = (MyView) rootView.findViewById(R.id.surfaceViewBackground);
        surfaceViewBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoElementInteracted.invoke();
            }
        });

        videoFrame = (AspectRatioFrameLayout) rootView.findViewById(R.id.videoFrame);
        surfaceViewVisualizer = (VisualizerViewCore) rootView.findViewById(R.id.surfaceViewVisualizer);
        surfaceViewVisualizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoElementInteracted.invoke();
            }
        });


        surfaceViewVideo = (SurfaceView) rootView.findViewById(R.id.surfaceViewVideo);
        surfaceViewVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoElementInteracted.invoke();
            }
        });

        onSurfaceCreated.invoke(surfaceViewVisualizer);

        if (surfaceViewVideo != null) {
            final SurfaceHolder surfaceHolder = surfaceViewVideo.getHolder();
            onVideoSurfaceHolderCreated.invoke(surfaceHolder);
        }

        int _videoScalingMode = onRequestVideoScalingMode.invoke(0);
        updateVideoScaleMode(_videoScalingMode);

        float _widthHeightRatio = onRequestVideoWidthHeightRatio.invoke(1.0f);
        setVideoSize(_widthHeightRatio);

        {
            boolean need = onRequestUIComponentNeedChangedValue.invoke(true);
            boolean showVideoContent = onRequestShowVideoContentState.invoke(false);
            updateSurfaceVisibility(need, showVideoContent);
        }


        prepareEncoder();

        mTrackIndex = mediaMuxer.addTrack(mVideoEncodec.getOutputFormat());
        mVideoEncodec.start();
        CodecInputSurface mInputSurface = new CodecInputSurface(inputSurface);
        mInputSurface.makeCurrent();
        surfaceViewVisualizer.setAveeBufferCallBack(new AveeBufferCallBack() {
            @Override
            public void bufferCallBack(ByteBuffer byteBuffer, int textureId) {
                drainEncoder(false);
                mInputSurface.setPresentationTime(computePresentationTimeNsec(textureId));
                // Submit it to the encoder
                mInputSurface.swapBuffers();
//                int outputBufIndex = mVideoEncodec.dequeueOutputBuffer(mVideoBuffInfo, 1000);
//                if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    mTrackIndex = mediaMuxer.addTrack(mVideoEncodec.getOutputFormat());
//                    mediaMuxer.start();
//                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    ByteBuffer[] inputBuffers = mVideoEncodec.getInputBuffers();
//                    int inputBufIndex = mVideoEncodec.dequeueInputBuffer(1000);
//                    ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
//                    inputBuffer.clear();
//                    inputBuffer.put(byteBuffer);
//                    mVideoEncodec.queueInputBuffer(inputBufIndex, 0, byteBuffer.array().length, System.nanoTime() / 1000, 0);
//                } else {
//                    while (outputBufIndex >= 0) {
//                        ByteBuffer outPutBuffer = mVideoEncodec.getInputBuffers()[outputBufIndex];
//                        if (outPutBuffer != null) {
//                            outPutBuffer.position(mVideoBuffInfo.offset);
//                            outPutBuffer.limit(mVideoBuffInfo.offset + mVideoBuffInfo.size);
//                            if (pts == 0) {
//                                pts = mVideoBuffInfo.presentationTimeUs;
//                            }
//                            mVideoBuffInfo.presentationTimeUs = mVideoBuffInfo.presentationTimeUs - pts;
//                            mediaMuxer.writeSampleData(mTrackIndex, outPutBuffer, mVideoBuffInfo);
//                            mVideoEncodec.releaseOutputBuffer(outputBufIndex, false);
//                            outputBufIndex = mVideoEncodec.dequeueOutputBuffer(mVideoBuffInfo, 0);
//                        }
//
//                    }
//                }


            }

            @Override
            public void bufferCallBack(int textureId) {

            }
        });
        return rootView;
    }

    private  long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / frameRate;
    }

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;

        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    private long prevOutputPTSUs = 0;
    private int mTrackIndex = 0;
    private int videoTrack = -1;

    @Override
    public void onDestroyView() {
        onVideoSurfaceHolderDestroyed.invoke();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    boolean isViewCreated() {
        return rootView != null;
    }

    boolean isSurfaceVisible() {
        return surfaceViewVideo != null && (surfaceViewVideo.getVisibility() == View.VISIBLE || surfaceViewVideoTag == 1) ||
                surfaceViewVisualizer != null && (surfaceViewVisualizer.getVisibility() == View.VISIBLE || surfaceViewTag == 1);

    }

    public void updateSurfaceVisibility(boolean visible, boolean showVideoContent) {
        //we do little delay show, so when we swipe to other fragment we don't get immediate "hang"
        if (visible) {
            if (showVideoContent) {

                if (surfaceViewVisualizer != null) {
                    surfaceViewTag = 0;
                    surfaceViewVisualizer.setVisibility(View.GONE);
                }

                if (surfaceViewVideo != null) {
                    surfaceViewVideoTag = 1;
                    surfaceViewVideo.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (surfaceViewVideoTag == 1) {
                                surfaceViewVideo.setVisibility(View.VISIBLE);
                                final SurfaceHolder surfaceHolder = surfaceViewVideo.getHolder();
                                onVideoSurfaceHolderCreated.invoke(surfaceHolder);

                            }
                        }
                    }, 250);
                }

            } else {
                if (surfaceViewVideo != null) {
                    surfaceViewVideoTag = 0;
                    surfaceViewVideo.setVisibility(View.GONE);
                }
                if (surfaceViewVisualizer != null) {
                    surfaceViewTag = 1;
                    surfaceViewVisualizer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (surfaceViewTag == 1) {
                                surfaceViewVisualizer.setVisibility(View.VISIBLE);
                            }
                        }
                    }, 250);
                }
            }
        } else {
            if (surfaceViewVideo != null) {
                surfaceViewVideoTag = 0;
                surfaceViewVideo.setVisibility(View.GONE);
            }
            if (surfaceViewVisualizer != null) {
                surfaceViewTag = 0;
                surfaceViewVisualizer.setVisibility(View.GONE);
            }
        }
    }

    //@UiThreadTest
    public void setShowVideoContentState(boolean state) {
        updateSurfaceVisibility(isSurfaceVisible(), state);
    }

    public void updateVideoScaleMode(int mode) {
        if (mode == 1) {
            btn3.setImageResource(R.drawable.ic_vis_fit3);
        } else if (mode == 2) {
            btn3.setImageResource(R.drawable.ic_vis_fit_crop3);
        } else if (mode == 3) {
            btn3.setImageResource(R.drawable.ic_vis_stretch3);
        }
    }

    public void setVideoSize(float widthHeightRatio) {
        if (!isViewCreated()) return;
        this.widthHeightRatio = widthHeightRatio;
        updateVideoSize(rootView.getWidth(), rootView.getHeight());
    }

    public void setVideoSizeTh(final float widthHeightRatio) {
        if (!isViewCreated()) return;
        rootView.post(new Runnable() {
            @Override
            public void run() {
                Fragment2.this.widthHeightRatio = widthHeightRatio;
                updateVideoSize(rootView.getWidth(), rootView.getHeight());
            }
        });
    }

    void updateVideoSize(float w, float h) {
        if (widthHeightRatio == 0.0f) {
            float fullScreenRatio;
            if (w > 0.0f && h > 0.0f) {
                fullScreenRatio = w / h;
                if (videoFrame != null) videoFrame.setAspectRatio(fullScreenRatio);
            }
        } else {
            if (videoFrame != null) videoFrame.setAspectRatio(widthHeightRatio);
        }
    }

    public void animateShow(boolean show) {

        if (layoutButtons == null) return;

        int mShortAnimTime;

        mShortAnimTime = layoutButtons.getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        if (show) {
            layoutButtons.animate()
                    .translationX(0).alpha(1.0f)
                    .setDuration(mShortAnimTime);
        } else {
            layoutButtons.animate()
                    .translationX(layoutButtons.getWidth()).alpha(0.0f)
                    .setDuration(mShortAnimTime);
        }
    }

    private void createThemeChooserMenu(View v) {
        ContextData contextData = new ContextData(getActivity());
        FragmentManager fragmentManager = contextData.getFragmentManager();

        if (fragmentManager != null) {
            VisualizerStyleDialog.createAndShowDialog(fragmentManager);
        }
    }

    public void showCustomizationMenu(Tuple2<Integer, Element.CustomizationList> currentCustomization) {
        View v = btn1;
        if (v == null) return;

        if (currentCustomization == null) return;
        if (currentCustomization.obj2 == null) return;

        final int rootIdentifier = currentCustomization.obj1;
        final Element.CustomizationList customizationDataList = currentCustomization.obj2;

        PopupMenu popup = new PopupMenu(v.getContext(), v);


        MenuItem menuItem = popup.getMenu().add(Menu.NONE,
                0,
                0,
                this.getString(R.string.reset_visualize));

        for (int i = 0; i < customizationDataList.dataCount(); i++) {
            Element.CustomizationData customizationData = customizationDataList.getData(i);

            String name = customizationData.getCustomizationName();
            if (name == null || name.isEmpty())
                continue;//skip empty, eg element have no customization

            MenuItem menuItem2 = popup.getMenu().add(Menu.NONE,
                    i + 1,
                    i + 1,
                    name
            );
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int id = item.getItemId();

                if (id == 0) {
                    onResetAction.invoke(new ContextData(getActivity()), rootIdentifier, customizationDataList);
                } else {
                    id = id - 1;
                    if (id >= 0 && id < customizationDataList.dataCount()) {
                        onPickElementAction.invoke(new ContextData(getActivity()), rootIdentifier, customizationDataList, id);
                    }
                }

                return true;
            }
        });

        popup.show();
    }

    private static void setStatusBarDimensions(View view) {
        if (view == null) return;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = UtilsUI.getStatusBarHeight(view.getContext());
    }
}
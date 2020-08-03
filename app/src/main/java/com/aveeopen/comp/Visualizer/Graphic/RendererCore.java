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

package com.aveeopen.comp.Visualizer.Graphic;


import android.content.Context;
import android.content.res.Resources;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Range;

import com.aveeopen.Common.tlog;
import com.aveeopen.PlayerCore;
import com.aveeopen.comp.Visualizer.InternalVisualizationDataProvider;
import com.aveeopen.comp.Visualizer.VisualizerViewCore;
import com.aveeopen.comp.Visualizer.Elements.Element;
import com.aveeopen.comp.Visualizer.Elements.IFrameDataProvider;
import com.aveeopen.comp.Visualizer.Elements.RootElement;
import com.research.GLRecorder.GLRecorder;
import com.research.GLRecorder.gles.ScreenUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RendererCore implements GLSurfaceView.Renderer {

    private final RenderState renderState;
    private RootElement newRootElement = null;
    private RootElement rootElement = null;
    private int phone_height;
    private int phone_width;
    private int max_width;
    private int max_height;
    private Context context;

    public RendererCore(Context context, Resources resources, InternalVisualizationDataProvider internalDataProvider) {
        renderState = new RenderState(internalDataProvider);
        renderState.onResources(resources);
        this.context = context;
        RootElement newSkinThemePreset = VisualizerViewCore.onRequestSelectedSkinThemePreset.invoke(null);
        setThemeElements(newSkinThemePreset);
    }

    private EGLConfig mConfig;

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        renderState.onSurfaceCreated();
        if (rootElement != null)
            rootElement.reCreateGLResources(renderState);
        mConfig = config;
    }
    private void getSize()
    {
        int flag = 0;

//            todo capabilities code
        for(MediaCodecInfo codecInfo : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()){
            if(!codecInfo.isEncoder())
                continue;
            String[] types = codecInfo.getSupportedTypes();
            for(int j=0;j<types.length;j++){
                if("video/avc".equalsIgnoreCase(types[j])){
                    MediaCodecInfo.CodecCapabilities codecCaps = codecInfo.getCapabilitiesForType("video/avc");
                    MediaCodecInfo.VideoCapabilities vidCaps = codecCaps.getVideoCapabilities();
                    Range<Integer> framerates = vidCaps.getSupportedFrameRates();
                    Range<Integer> widths = vidCaps.getSupportedWidths();
                    Range<Integer> heights = vidCaps.getSupportedHeights();
                    Log.d("H.264Encoder", "Found encoder with\n" + widths.toString()
                            + " x " + heights.toString() + " @ " + framerates.toString() + " fps aligned to " + vidCaps.getWidthAlignment());
//                        Log.e(TAG, "maxWidth: " + vidCaps.getSupportedWidths().getUpper());
//                        Log.e(TAG, "maxHeight: " + vidCaps.getSupportedHeights().getUpper());

                    if(flag == 0)
                    {
                        this.max_width = vidCaps.getSupportedWidths().getUpper();
                        this.max_height = vidCaps.getSupportedHeights().getUpper();
                    }
                    flag++;
                }
            }
        }


    }
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        getSize();//get maxwidth and maxheight

        renderState.onSurfaceChanged(PlayerCore.s().getAppContext(), width, height);
        GLES20.glViewport(0, 0, renderState.getScreenWidth(), renderState.getScreenHeight());
        phone_width = width;
        phone_height = height;

        float width_ratio = (float) max_width / (float) width;
        float height_ratio = (float) max_height / (float) height;
        GLRecorder.init(ScreenUtil.getScreenWidth(context), ScreenUtil.getScreenHeight(context), width_ratio, height_ratio, mConfig);
        GLRecorder.setRecordOutputFile("/sdcard/1/Record.mp4");
    }

    private void chooseViewport()//According to mRecordingEnabled to choose viewport
    {
        if (GLRecorder.mRecordingEnabled) {
            GLES20.glViewport(0, 0, max_width, max_height);
        } else {
            GLES20.glViewport(0, 0, phone_width, phone_height);
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLRecorder.beginDraw();
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            return;
        }
        chooseViewport();
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        renderState.onFrameStart();

        if (rootElement != null) {
            IFrameDataProvider iframeDataProvider = rootElement.getFrameDataProvider();
            if (iframeDataProvider != null)
                renderState.res.meter.setFrameDataRmsValue(iframeDataProvider.getRms());

            rootElement.onEarlyUpdate(renderState, null);
        }

        if (rootElement != newRootElement) {
            rootElement = newRootElement;
            if (rootElement != null)
                rootElement.reCreateGLResources(renderState);
        }

        if (rootElement != null)
            rootElement.onRender(renderState, null);

        renderState.onFrameEnd();
        renderState.bindFrameBuffer(null);
        GLRecorder.endDraw();
    }

    public int getFps() {
        return renderState.getFps();
    }

    public int getFrameTimeMs() {
        return renderState.getFrameTime();
    }

    public void setThemeElements(RootElement root) {
        newRootElement = root;
    }

    public void setThemeCustomizationData(int rootIdentifier, Element.CustomizationList customization) {
        if (rootElement != null) {
            if (rootElement.getIdentifier() == rootIdentifier)
                rootElement.setCustomization(customization);
            else
                tlog.w("rootElement identifier not match");
        }
    }

    public int readThemeCustomizationData(Element.CustomizationList customization) {
        if (rootElement != null) {
            if (rootElement.getCustomization(customization, 0))
                return rootElement.getIdentifier();
        }
        return -1;
    }

}
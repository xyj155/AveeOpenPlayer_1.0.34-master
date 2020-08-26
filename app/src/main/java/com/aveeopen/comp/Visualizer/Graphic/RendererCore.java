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


import android.content.res.Resources;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.aveeopen.AveeBufferCallBack;
import com.aveeopen.Common.tlog;
import com.aveeopen.PlayerCore;
import com.aveeopen.comp.Visualizer.InternalVisualizationDataProvider;
import com.aveeopen.comp.Visualizer.VisualizerViewCore;
import com.aveeopen.comp.Visualizer.Elements.Element;
import com.aveeopen.comp.Visualizer.Elements.IFrameDataProvider;
import com.aveeopen.comp.Visualizer.Elements.RootElement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RendererCore implements GLSurfaceView.Renderer {

    private final RenderState renderState;
    private RootElement newRootElement = null;
    private RootElement rootElement = null;
    private int fboTextureid;
    private int cameraTextureid;
    private int width=1080,height=1920;

    public RendererCore(Resources resources, InternalVisualizationDataProvider internalDataProvider) {
        renderState = new RenderState(internalDataProvider);
        renderState.onResources(resources);

        RootElement newSkinThemePreset = VisualizerViewCore.onRequestSelectedSkinThemePreset.invoke(null);
        setThemeElements(newSkinThemePreset);
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        renderState.onSurfaceCreated();
        if (rootElement != null)
            rootElement.reCreateGLResources(renderState);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        renderState.onSurfaceChanged(PlayerCore.s().getAppContext(), width, height);
        GLES20.glViewport(0, 0, renderState.getScreenWidth(), renderState.getScreenHeight());
    }

    private AveeBufferCallBack aveeBufferCallBack;

    public void setAveeBufferCallBack(AveeBufferCallBack aveeBufferCallBack) {
        this.aveeBufferCallBack = aveeBufferCallBack;
    }

    private void glBufferGen(GL10 gl, int width, int height) {
        int screenshotSize = width * height;
        ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
        bb.order(ByteOrder.nativeOrder());
        gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
        int pixelsBuffer[] = new int[screenshotSize];
        bb.asIntBuffer().get(pixelsBuffer);
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//        bitmap.setPixels(pixelsBuffer, screenshotSize - width, -width, 0, 0, width, height);
        short sBuffer[] = new short[screenshotSize];
        ShortBuffer sb = ShortBuffer.wrap(sBuffer);
//        bitmap.copyPixelsToBuffer(sb);

        for (int i = 0; i < screenshotSize; ++i) {
            short v = sBuffer[i];
            sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
        }

        sb.rewind();
//        bitmap.copyPixelsFromBuffer(sb);

    }

    private static final String TAG = "RendererCore";
    private int fboId;
    @Override
    public void onDrawFrame(GL10 unused) {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            return;
        }

        //fbo
        int[] fbos = new int[1];
        GLES20.glGenBuffers(1, fbos, 0);
        fboId = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);

        int []textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        fboTextureid = textureIds[0];
        int screenshotSize = width * height;
        ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
        bb.order(ByteOrder.nativeOrder());
        unused.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
        int pixelsBuffer[] = new int[screenshotSize];
        bb.asIntBuffer().get(pixelsBuffer);
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//        bitmap.setPixels(pixelsBuffer, screenshotSize - width, -width, 0, 0, width, height);
        short sBuffer[] = new short[screenshotSize];
        ShortBuffer sb = ShortBuffer.wrap(sBuffer);
//        bitmap.copyPixelsToBuffer(sb);

        for (int i = 0; i < screenshotSize; ++i) {
            short v = sBuffer[i];
            sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
        }

        sb.rewind();

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        int []textureidseos = new int[1];
        GLES20.glGenTextures(1, textureidseos, 0);
        cameraTextureid = textureidseos[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureid);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        Log.i(TAG, "onDrawFrame: cameraTextureid==========="+cameraTextureid);
        Log.i(TAG, "onDrawFrame: fboTextureid========="+fboTextureid);
        renderState.onFrameStart();


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureid);
        if (aveeBufferCallBack != null) {
            aveeBufferCallBack.bufferCallBack(bb,fboTextureid);
        }
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

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
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
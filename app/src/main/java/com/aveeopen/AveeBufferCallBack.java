package com.aveeopen;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public interface AveeBufferCallBack {
    void bufferCallBack(ByteBuffer byteBuffer,int textureId);
    void bufferCallBack(int textureId);
}

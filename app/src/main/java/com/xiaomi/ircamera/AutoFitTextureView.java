package com.xiaomi.ircamera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("尺寸不能为负");
        }
        ratioWidth = width;
        ratioHeight = height;
        post(() -> requestLayout());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height);
            return;
        }

        // 根据摄像头比例调整尺寸，保持画面完整显示
        float ratio = (float) ratioWidth / ratioHeight;
        
        // 如果宽度受限，调整高度
        if (width < height * ratio) {
            int newHeight = Math.round(width / ratio);
            setMeasuredDimension(width, newHeight);
        } else {
            // 如果高度受限，调整宽度
            int newWidth = Math.round(height * ratio);
            setMeasuredDimension(newWidth, height);
        }
    }
}

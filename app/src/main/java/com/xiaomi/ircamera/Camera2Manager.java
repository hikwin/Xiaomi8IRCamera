package com.xiaomi.ircamera;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Camera2Manager {
    private static final String TAG = "Camera2Manager";

    private Context context;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ImageReader imageReader;
    private MediaRecorder mediaRecorder;
    private TextureView textureView;
    private String currentCameraId;
    private boolean isRecording = false;
    private String videoFilePath;
    private Size previewSize;

    public interface CameraCallback {
        void onOpened();
        void onClosed();
        void onError(String error);
        void onPhotoSaved(String path);
        void onVideoStarted(String path);
        void onVideoStopped();
    }

    private CameraCallback callback;

    public Camera2Manager(Context context, TextureView textureView, CameraCallback callback) {
        this.context = context;
        this.textureView = textureView;
        this.callback = callback;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void openCamera(String cameraId) {
        currentCameraId = cameraId;
        
        // 固定使用640x480尺寸
        previewSize = new Size(640, 480);
        Log.d(TAG, "使用固定预览尺寸: 640x480");

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("没有相机权限");
            return;
        }

        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "打开相机失败", e);
            callback.onError("打开相机失败: " + e.getMessage());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "摄像头已打开: " + currentCameraId);
            cameraDevice = camera;
            // 等待SurfaceTexture准备好
            waitForSurfaceTexture();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "摄像头已断开");
            closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "相机错误: " + error);
            String errorMsg = getCameraErrorMessage(error);
            callback.onError("相机错误: " + errorMsg);
            closeCamera();
        }
    };

    private String getCameraErrorMessage(int error) {
        switch (error) {
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                return "摄像头正在被使用";
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                return "打开的摄像头数量已达上限";
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                return "摄像头已被禁用";
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                return "摄像头设备发生致命错误";
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                return "摄像头服务发生致命错误";
            default:
                return "未知错误(" + error + ")";
        }
    }

    private void waitForSurfaceTexture() {
        if (textureView.isAvailable()) {
            createPreviewSession();
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "SurfaceTexture已可用");
                    createPreviewSession();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
            });
            
            // 如果SurfaceTexture已经存在但isAvailable返回false
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                Log.d(TAG, "SurfaceTexture已存在，直接创建预览");
                createPreviewSession();
            }
        }
    }

    private void createPreviewSession() {
        if (cameraDevice == null) {
            Log.e(TAG, "cameraDevice为空，无法创建预览会话");
            callback.onError("相机设备未就绪");
            return;
        }

        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            Log.e(TAG, "SurfaceTexture为空，等待TextureView准备就绪");
            // 尝试重新获取
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "SurfaceTexture在监听器中可用");
                    createPreviewSession();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
            });
            return;
        }

        try {
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(surface);
            surfaces.add(imageReader.getSurface());

            cameraDevice.createCaptureSession(surfaces,
                    sessionStateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "创建预览会话失败", e);
            callback.onError("创建预览失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "创建预览会话时发生未知异常", e);
            callback.onError("创建预览失败: " + e.getMessage());
        }
    }

    private final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            try {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                Log.d(TAG, "预览会话已配置成功");
                callback.onOpened();
            } catch (CameraAccessException e) {
                Log.e(TAG, "设置预览请求失败", e);
                callback.onError("设置预览失败: " + e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "配置预览会话失败");
            callback.onError("配置预览失败");
        }
    };

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            // 拍照时会触发，但实际保存逻辑在capturePhoto方法中
        }
    };

    public void capturePhoto() {
        if (captureSession == null || cameraDevice == null) {
            callback.onError("相机未就绪");
            return;
        }

        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            String photoPath = getPhotoFilePath();
            ImageReader photoReader = ImageReader.newInstance(
                    previewSize.getWidth(),
                    previewSize.getHeight(),
                    ImageFormat.JPEG, 1);

            final boolean[] hasCaptured = {false};

            photoReader.setOnImageAvailableListener(reader -> {
                if (hasCaptured[0]) return;
                hasCaptured[0] = true;

                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);

                        savePhotoToPublicStorage(bytes, photoPath);
                        Log.d(TAG, "照片已保存: " + photoPath);
                        callback.onPhotoSaved(photoPath);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "处理照片失败", e);
                    callback.onError("处理照片失败: " + e.getMessage());
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    photoReader.close();
                    createPreviewSession();
                }
            }, backgroundHandler);

            captureBuilder.addTarget(photoReader.getSurface());

            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture == null) {
                callback.onError("SurfaceTexture为空，无法拍照");
                photoReader.close();
                return;
            }
            Surface previewSurface = new Surface(surfaceTexture);
            captureBuilder.addTarget(previewSurface);
            
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, photoReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.capture(captureBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "拍照失败", e);
                                callback.onError("拍照失败: " + e.getMessage());
                                photoReader.close();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            callback.onError("拍照配置失败");
                            photoReader.close();
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "创建拍照请求失败", e);
            callback.onError("拍照失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "拍照时发生未知异常", e);
            callback.onError("拍照失败: " + e.getMessage());
        }
    }

    private void savePhotoToPublicStorage(byte[] bytes, String fileName) {
        try {
            // 旋转图片-90度（逆时针90度，抵消顺时针90度）
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            
            // 转换为JPEG字节数组
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
            byte[] rotatedBytes = stream.toByteArray();
            
            originalBitmap.recycle();
            rotatedBitmap.recycle();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/XiaomiIRCamera");
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            out.write(rotatedBytes);
                        }
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "XiaomiIRCamera");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(rotatedBytes);
                }
                android.media.MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "保存照片到公共存储失败", e);
        }
    }

    public void startRecording() {
        if (isRecording || cameraDevice == null) return;

        try {
            videoFilePath = getVideoFileName();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            
            Size videoSize = getSupportedVideoSize();
            mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoEncodingBitRate(10000000);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, videoFilePath);
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/XiaomiIRCamera");
                Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    mediaRecorder.setOutputFile(context.getContentResolver().openFileDescriptor(uri, "rw").getFileDescriptor());
                } else {
                    callback.onError("无法创建视频文件");
                    return;
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "XiaomiIRCamera");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, videoFilePath);
                mediaRecorder.setOutputFile(file.getAbsolutePath());
            }
            
            // 设置视频旋转角度，必须在prepare之前调用
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = wm.getDefaultDisplay().getRotation();
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(currentCameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            
            int orientation = 0;
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                orientation = 270;
            } else {
                orientation = 90;
            }
            mediaRecorder.setOrientationHint(orientation);
            Log.d(TAG, "设置视频旋转角度: " + orientation + " (前置: " + (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) + ")");

            mediaRecorder.prepare();
            
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture == null) {
                callback.onError("SurfaceTexture为空，无法录像");
                return;
            }
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recorderSurface = mediaRecorder.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                builder.addTarget(previewSurface);
                                builder.addTarget(recorderSurface);
                                session.setRepeatingRequest(builder.build(), null, backgroundHandler);
                                mediaRecorder.start();
                                isRecording = true;
                                callback.onVideoStarted(videoFilePath);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "录像失败", e);
                                callback.onError("录像失败: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            callback.onError("录像配置失败");
                        }
                    }, backgroundHandler);

        } catch (Exception e) {
            Log.e(TAG, "准备录像失败", e);
            callback.onError("录像失败: " + e.getMessage());
        }
    }

    private Size getSupportedVideoSize() {
        // 固定使用640x480，与预览尺寸一致
        return new Size(640, 480);
    }

    public void stopRecording() {
        if (!isRecording) return;
        
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
            callback.onVideoStopped();
            createPreviewSession(); // 恢复预览
        } catch (Exception e) {
            Log.e(TAG, "停止录像失败", e);
        }
    }

    public void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
        callback.onClosed();
    }

    private String getPhotoFilePath() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "IMG_" + timeStamp + ".jpg";
    }

    private String getVideoFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "VID_" + timeStamp + ".mp4";
    }

    private void startBackgroundThread() {
        if (backgroundThread != null) {
            stopBackgroundThread();
        }
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "停止后台线程失败", e);
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}

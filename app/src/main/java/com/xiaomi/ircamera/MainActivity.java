package com.xiaomi.ircamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 100;

    private TextureView textureView;
    private FrameLayout previewContainer;
    private SeekBar heightAdjustBar;
    private ImageButton btnHelp;
    private Spinner cameraSpinner;
    private Button btnCapture;
    private Button btnRecord;
    private Button btnDetectCameras;

    private Camera2Manager camera2Manager;
    private CameraDetector cameraDetector;
    private List<CameraDetector.CameraInfo> detectedCameras = new ArrayList<>();
    private boolean isDetecting = false;
    private String videoFilePath = "";
    private String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        
        if (hasAllPermissions()) {
            if (textureView.isAvailable()) {
                detectCameras();
            }
        } else {
            requestPermissions();
        }
    }

    private boolean hasAllPermissions() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        List<String> needPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needPermissions.add(permission);
            }
        }

        if (!needPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, needPermissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Log.d(TAG, "权限已授予，自动刷新摄像头");
                detectCameras();
            } else {
                Toast.makeText(this, "需要权限才能运行", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initViews() {
        textureView = findViewById(R.id.textureView);
        previewContainer = findViewById(R.id.previewContainer);
        heightAdjustBar = findViewById(R.id.heightAdjustBar);
        btnHelp = findViewById(R.id.btnHelp);
        cameraSpinner = findViewById(R.id.cameraSpinner);
        btnCapture = findViewById(R.id.btnCapture);
        btnRecord = findViewById(R.id.btnRecord);
        btnDetectCameras = findViewById(R.id.btnDetectCameras);

        btnHelp.setOnClickListener(v -> showHelpDialog());

        initHeightAdjustBar();

        btnCapture.setOnClickListener(v -> capturePhoto());
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnDetectCameras.setOnClickListener(v -> detectCameras());
        btnCapture.setEnabled(false);
        btnRecord.setEnabled(false);

        cameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isDetecting && position < detectedCameras.size()) {
                    openCamera(detectedCameras.get(position).cameraId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "SurfaceTexture可用: " + width + "x" + height);
                if (hasAllPermissions()) {
                    if (detectedCameras.isEmpty() && !isDetecting) {
                        detectCameras();
                    } else if (!detectedCameras.isEmpty() && camera2Manager == null) {
                        openCamera(detectedCameras.get(0).cameraId);
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(TAG, "SurfaceTexture销毁");
                if (camera2Manager != null) {
                    camera2Manager.closeCamera();
                    camera2Manager = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
        });
    }

    private void initHeightAdjustBar() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int savedProgress = prefs.getInt("height_adjust_progress", 800);
        
        heightAdjustBar.setMax(1600);
        heightAdjustBar.setProgress(savedProgress);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;
        int controlPanelHeight = 200;
        int defaultHeight = screenHeight - controlPanelHeight;
        
        updatePreviewHeight(savedProgress, defaultHeight);
        
        heightAdjustBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updatePreviewHeight(progress, defaultHeight);
                    prefs.edit().putInt("height_adjust_progress", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updatePreviewHeight(int progress, int defaultHeight) {
        int offset = progress - 800;
        int height = defaultHeight + offset;
        if (height < 100) height = 100;
        
        ViewGroup.LayoutParams params = previewContainer.getLayoutParams();
        params.height = height;
        previewContainer.setLayoutParams(params);
        
        Log.d(TAG, "调整预览高度: " + height + " (offset: " + offset + "px)");
    }

    private void showHelpDialog() {
        String helpText = "小米8红外相机 v1.1\n\n" +
                "【功能介绍】\n" +
                "本应用专为小米8手机设计，可调用前置红外摄像头，实现红外拍摄与夜视仪功能。\n\n" +
                "【操作说明】\n" +
                "• 刷新：探测并列出所有可用摄像头\n" +
                "• 下拉列表：切换不同摄像头（含红外摄像头）\n" +
                "• 拍照：拍摄红外照片并保存到相册\n" +
                "• 录像：录制红外视频并保存到相册\n" +
                "• 右侧滑动条：调整预览画面高度\n" +
                "  -800px ~ +800px范围，默认0\n" +
                "  负值减小高度，正值增加高度\n\n" +
                "【保存位置】\n" +
                "照片和视频自动保存到：\n" +
                "内部存储/DCIM/XiaomiIRCamera/\n\n" +
                "【注意事项】\n" +
                "• 红外摄像头画面可能为黑白\n" +
                "• 建议在暗光环境下使用夜视功能\n" +
                "• 首次使用请授予相机和存储权限\n\n" +
                "作者：酷安 @CWAYER";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("帮助");
        
        TextView textView = new TextView(this);
        textView.setText(helpText);
        textView.setTextSize(14);
        textView.setPadding(48, 32, 48, 32);
        textView.setLineSpacing(1.2f, 1.2f);
        builder.setView(textView);
        
        builder.setPositiveButton("知道了", null);
        builder.show();
    }

    private void detectCameras() {
        if (isDetecting) {
            Log.d(TAG, "正在探测中，忽略重复请求");
            return;
        }

        if (!hasAllPermissions()) {
            Log.d(TAG, "权限未授予，请求权限");
            requestPermissions();
            return;
        }

        isDetecting = true;
        btnDetectCameras.setEnabled(false);
        btnCapture.setEnabled(false);
        btnRecord.setEnabled(false);

        if (camera2Manager != null) {
            camera2Manager.closeCamera();
            camera2Manager = null;
        }

        try {
            cameraDetector = new CameraDetector(
                    (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE));

            cameraDetector.detectAllCameras(new CameraDetector.CameraDetectCallback() {
                @Override
                public void onCameraDetected(List<CameraDetector.CameraInfo> cameras) {
                    runOnUiThread(() -> {
                        try {
                            isDetecting = false;
                            btnDetectCameras.setEnabled(true);
                            
                            detectedCameras = cameras;
                            String preferredId = cameraDetector.getPreferredCameraId(cameras);
                            updateCameraSpinner();

                            if (!cameras.isEmpty() && preferredId != null) {
                                final String openId = preferredId;
                                int preferredIndex = -1;
                                for (int i = 0; i < cameras.size(); i++) {
                                    if (cameras.get(i).cameraId.equals(preferredId)) {
                                        preferredIndex = i;
                                        break;
                                    }
                                }
                                if (preferredIndex > 0) {
                                    cameraSpinner.setSelection(preferredIndex);
                                }
                                textureView.postDelayed(() -> {
                                    if (textureView.isAvailable()) {
                                        openCamera(openId);
                                    }
                                }, 300);
                            } else {
                                Toast.makeText(MainActivity.this, "未检测到可用摄像头", Toast.LENGTH_LONG).show();
                                btnCapture.setEnabled(false);
                                btnRecord.setEnabled(false);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理摄像头探测结果时出错", e);
                            Toast.makeText(MainActivity.this, "处理结果出错", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        isDetecting = false;
                        btnDetectCameras.setEnabled(true);
                        Toast.makeText(MainActivity.this, "探测失败: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "启动摄像头探测时出错", e);
            isDetecting = false;
            btnDetectCameras.setEnabled(true);
            Toast.makeText(this, "探测启动失败", Toast.LENGTH_LONG).show();
        }
    }

    private void updateCameraSpinner() {
        try {
            List<String> items = new ArrayList<>();
            for (CameraDetector.CameraInfo cam : detectedCameras) {
                String item = "ID:" + cam.cameraId + " " + cam.getLensFacingName() + " " + cam.getColorFilterName();
                if (cam.isIR) item += " [红外]";
                items.add(item);
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cameraSpinner.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "更新Spinner失败", e);
        }
    }

    private void openCamera(String cameraId) {
        if (isDetecting) {
            Log.d(TAG, "正在探测中，延迟打开摄像头");
            return;
        }

        Log.d(TAG, "打开摄像头: " + cameraId);
        
        if (camera2Manager != null) {
            camera2Manager.closeCamera();
        }

        camera2Manager = new Camera2Manager(this, textureView, new Camera2Manager.CameraCallback() {
            @Override
            public void onOpened() {
                runOnUiThread(() -> {
                    btnCapture.setEnabled(true);
                    btnRecord.setEnabled(true);
                });
            }

            @Override
            public void onClosed() {
                camera2Manager = null;
                runOnUiThread(() -> {
                    btnCapture.setEnabled(false);
                    btnRecord.setEnabled(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "错误: " + error, Toast.LENGTH_LONG).show();
                    btnCapture.setEnabled(false);
                    btnRecord.setEnabled(false);
                });
            }

            @Override
            public void onPhotoSaved(String path) {
                runOnUiThread(() -> {
                    String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/XiaomiIRCamera/" + path;
                    Toast.makeText(MainActivity.this, "照片已保存:\n" + fullPath, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "照片已保存: " + fullPath);
                });
            }

            @Override
            public void onVideoStarted(String path) {
                runOnUiThread(() -> {
                    videoFilePath = path;
                    btnRecord.setText("停止录像");
                    btnRecord.setBackgroundResource(R.drawable.btn_record_stop_bg);
                    Toast.makeText(MainActivity.this, "录像开始", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onVideoStopped() {
                runOnUiThread(() -> {
                    btnRecord.setText("录像");
                    btnRecord.setBackgroundResource(R.drawable.btn_record_bg);
                    String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/XiaomiIRCamera/" + videoFilePath;
                    Toast.makeText(MainActivity.this, "录像已保存:\n" + fullPath, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "录像已保存: " + fullPath);
                });
            }
        });

        camera2Manager.openCamera(cameraId);
    }

    private void capturePhoto() {
        if (camera2Manager != null) {
            camera2Manager.capturePhoto();
        } else {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRecording() {
        if (camera2Manager == null) {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        if (camera2Manager.isRecording()) {
            camera2Manager.stopRecording();
        } else {
            camera2Manager.startRecording();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera2Manager != null) {
            if (camera2Manager.isRecording()) {
                camera2Manager.stopRecording();
            }
            camera2Manager.closeCamera();
            camera2Manager = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera2Manager != null) {
            camera2Manager.closeCamera();
            camera2Manager = null;
        }
    }
}

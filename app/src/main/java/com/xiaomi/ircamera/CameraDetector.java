package com.xiaomi.ircamera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class CameraDetector {
    private static final String TAG = "CameraDetector";
    private CameraManager cameraManager;

    public interface CameraDetectCallback {
        void onCameraDetected(List<CameraInfo> cameras);
        void onError(String error);
    }

    public static class CameraInfo {
        public String cameraId;
        public Integer lensFacing;
        public Integer colorFilterArrangement;
        public boolean isIR;
        public String characteristics;
        public boolean isFrontGBRG;

        public String getLensFacingName() {
            if (lensFacing == null) return "未知";
            switch (lensFacing) {
                case CameraCharacteristics.LENS_FACING_FRONT:
                    return "前置";
                case CameraCharacteristics.LENS_FACING_BACK:
                    return "后置";
                case CameraCharacteristics.LENS_FACING_EXTERNAL:
                    return "外部";
                default:
                    return "未知(" + lensFacing + ")";
            }
        }

        public String getColorFilterName() {
            if (colorFilterArrangement == null) return "未知";
            switch (colorFilterArrangement) {
                case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB:
                    return "RGGB";
                case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG:
                    return "GRBG";
                case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG:
                    return "GBRG";
                case CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR:
                    return "BGGR";
                case 4: // MONOCHROME
                    return "单色/红外";
                default:
                    return "其他(" + colorFilterArrangement + ")";
            }
        }

        public boolean isFrontGBRG() {
            return lensFacing != null && 
                   lensFacing == CameraCharacteristics.LENS_FACING_FRONT &&
                   colorFilterArrangement != null &&
                   colorFilterArrangement == CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG;
        }
    }

    public CameraDetector(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public void detectAllCameras(CameraDetectCallback callback) {
        try {
            List<CameraInfo> cameras = new ArrayList<>();
            
            String[] publicIds = cameraManager.getCameraIdList();
            Log.d(TAG, "公开摄像头ID: " + java.util.Arrays.toString(publicIds));
            
            for (String id : publicIds) {
                try {
                    CameraInfo info = getCameraInfo(id);
                    if (info != null) {
                        cameras.add(info);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "获取公开摄像头 " + id + " 信息失败", e);
                }
            }

            for (int i = 2; i <= 10; i++) {
                String id = String.valueOf(i);
                try {
                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                    CameraInfo info = new CameraInfo();
                    info.cameraId = id;
                    info.lensFacing = chars.get(CameraCharacteristics.LENS_FACING);
                    info.colorFilterArrangement = chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
                    info.isIR = (info.colorFilterArrangement != null && info.colorFilterArrangement == 4);
                    info.characteristics = "隐藏";
                    info.isFrontGBRG = info.isFrontGBRG();
                    
                    Log.d(TAG, "发现隐藏摄像头 ID:" + id + 
                          ", 朝向:" + info.getLensFacingName() + 
                          ", 滤镜:" + info.getColorFilterName() + 
                          ", 红外:" + info.isIR);
                    
                    cameras.add(info);
                } catch (CameraAccessException e) {
                    Log.d(TAG, "摄像头 ID:" + id + " 无法访问: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "摄像头 ID:" + id + " 探测异常", e);
                }
            }

            callback.onCameraDetected(cameras);
        } catch (Exception e) {
            Log.e(TAG, "摄像头探测过程发生严重错误", e);
            callback.onError("探测失败: " + e.getMessage());
        }
    }

    public String getPreferredCameraId(List<CameraInfo> cameras) {
        if (cameras == null || cameras.isEmpty()) return null;

        for (CameraInfo cam : cameras) {
            if (cam.isFrontGBRG()) {
                Log.d(TAG, "优先选择前置GBRG摄像头: ID=" + cam.cameraId);
                return cam.cameraId;
            }
        }

        for (CameraInfo cam : cameras) {
            if (cam.lensFacing != null && cam.lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                Log.d(TAG, "选择前置摄像头: ID=" + cam.cameraId);
                return cam.cameraId;
            }
        }

        Log.d(TAG, "选择默认摄像头: ID=" + cameras.get(0).cameraId);
        return cameras.get(0).cameraId;
    }

    private CameraInfo getCameraInfo(String cameraId) {
        try {
            CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraId);
            CameraInfo info = new CameraInfo();
            info.cameraId = cameraId;
            info.lensFacing = chars.get(CameraCharacteristics.LENS_FACING);
            info.colorFilterArrangement = chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
            info.isIR = (info.colorFilterArrangement != null && info.colorFilterArrangement == 4);
            info.characteristics = "公开";
            info.isFrontGBRG = info.isFrontGBRG();
            return info;
        } catch (Exception e) {
            Log.e(TAG, "获取摄像头 " + cameraId + " 信息失败", e);
            return null;
        }
    }
}

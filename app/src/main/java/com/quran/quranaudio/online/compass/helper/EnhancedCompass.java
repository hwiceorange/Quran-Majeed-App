package com.quran.quranaudio.online.compass.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * 增强型罗盘类
 * 功能：
 * 1. 融合磁力计和加速计数据
 * 2. 使用旋转向量传感器获取更稳定的方向数据
 * 3. 检测磁场异常和设备倾斜
 * 4. 提供罗盘校准建议
 */
public class EnhancedCompass implements SensorEventListener {
    private static final String TAG = "EnhancedCompass";
    
    // 传感器相关
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor rotationVectorSensor;
    
    // 传感器数据
    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    
    // 滤波器参数
    private static final float ALPHA = 0.8f; // 低通滤波器参数
    private float[] filteredAccel = new float[3];
    private float[] filteredMagnetic = new float[3];
    
    // 磁场检测参数
    private static final float MAGNETIC_FIELD_NORMAL_MIN = 25f; // 正常磁场强度最小值 (μT)
    private static final float MAGNETIC_FIELD_NORMAL_MAX = 65f; // 正常磁场强度最大值 (μT)
    private static final float TILT_THRESHOLD = 25f; // 倾斜角度阈值 (度)
    
    // 状态变量
    private boolean isAccelerometerReady = false;
    private boolean isMagnetometerReady = false;
    private boolean isRotationVectorReady = false;
    private float currentMagneticStrength = 0f;
    private float currentTiltAngle = 0f;
    private float currentAzimuth = 0f;
    
    // 回调接口
    private EnhancedCompassListener listener;
    
    public interface EnhancedCompassListener {
        void onAzimuthChanged(float azimuth);
        void onMagneticFieldChanged(float strength, MagneticFieldStatus status);
        void onTiltChanged(float tiltAngle, boolean isDeviceLevel);
        void onCalibrationNeeded(String reason);
        void onAccuracyChanged(String sensorName, int accuracy);
    }
    
    public enum MagneticFieldStatus {
        NORMAL,     // 正常
        WEAK,       // 弱磁场
        STRONG,     // 强磁场  
        DISTURBED   // 磁场干扰
    }
    
    public EnhancedCompass(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        // 初始化传感器
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        
        Log.d(TAG, "传感器初始化:");
        Log.d(TAG, "加速计: " + (accelerometer != null ? "可用" : "不可用"));
        Log.d(TAG, "磁力计: " + (magnetometer != null ? "可用" : "不可用"));
        Log.d(TAG, "旋转向量: " + (rotationVectorSensor != null ? "可用" : "不可用"));
    }
    
    public void start() {
        Log.d(TAG, "启动增强型罗盘");
        
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
        
        // 优先使用旋转向量传感器（如果可用）
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "使用旋转向量传感器获取更稳定的方向数据");
        }
    }
    
    public void stop() {
        Log.d(TAG, "停止增强型罗盘");
        sensorManager.unregisterListener(this);
        isAccelerometerReady = false;
        isMagnetometerReady = false;
        isRotationVectorReady = false;
    }
    
    public void setListener(EnhancedCompassListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                handleAccelerometerData(event.values);
                break;
                
            case Sensor.TYPE_MAGNETIC_FIELD:
                handleMagnetometerData(event.values);
                break;
                
            case Sensor.TYPE_ROTATION_VECTOR:
                handleRotationVectorData(event.values);
                break;
        }
    }
    
    private void handleAccelerometerData(float[] values) {
        // 应用低通滤波器减少噪音
        filteredAccel[0] = ALPHA * filteredAccel[0] + (1 - ALPHA) * values[0];
        filteredAccel[1] = ALPHA * filteredAccel[1] + (1 - ALPHA) * values[1];
        filteredAccel[2] = ALPHA * filteredAccel[2] + (1 - ALPHA) * values[2];
        
        System.arraycopy(filteredAccel, 0, accelerometerData, 0, 3);
        isAccelerometerReady = true;
        
        // 计算设备倾斜角度
        calculateTiltAngle();
        
        // 如果磁力计数据也准备好了，计算方位角
        if (isMagnetometerReady && !isRotationVectorReady) {
            calculateAzimuthFromAccelMagnetic();
        }
    }
    
    private void handleMagnetometerData(float[] values) {
        // 应用低通滤波器
        filteredMagnetic[0] = ALPHA * filteredMagnetic[0] + (1 - ALPHA) * values[0];
        filteredMagnetic[1] = ALPHA * filteredMagnetic[1] + (1 - ALPHA) * values[1];
        filteredMagnetic[2] = ALPHA * filteredMagnetic[2] + (1 - ALPHA) * values[2];
        
        System.arraycopy(filteredMagnetic, 0, magnetometerData, 0, 3);
        isMagnetometerReady = true;
        
        // 计算磁场强度
        calculateMagneticFieldStrength();
        
        // 如果加速计数据也准备好了，计算方位角
        if (isAccelerometerReady && !isRotationVectorReady) {
            calculateAzimuthFromAccelMagnetic();
        }
    }
    
    private void handleRotationVectorData(float[] values) {
        // 旋转向量传感器提供最稳定的方向数据
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
        
        float[] orientationAngles = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        
        // 获取方位角并转换为度数
        float azimuth = (float) Math.toDegrees(orientationAngles[0]);
        azimuth = (azimuth + 360) % 360; // 确保结果在0-360度之间
        
        currentAzimuth = azimuth;
        isRotationVectorReady = true;
        
        if (listener != null) {
            listener.onAzimuthChanged(azimuth);
        }
        
        Log.v(TAG, "旋转向量方位角: " + azimuth + "°");
    }
    
    private void calculateAzimuthFromAccelMagnetic() {
        // 使用加速计和磁力计数据计算方位角
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            Log.w(TAG, "无法获取旋转矩阵，可能存在磁场干扰");
            if (listener != null) {
                listener.onCalibrationNeeded("旋转矩阵计算失败，可能存在磁场干扰");
            }
            return;
        }
        
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        
        // 获取方位角并转换为度数
        float azimuth = (float) Math.toDegrees(orientationAngles[0]);
        azimuth = (azimuth + 360) % 360;
        
        currentAzimuth = azimuth;
        
        if (listener != null) {
            listener.onAzimuthChanged(azimuth);
        }
        
        Log.v(TAG, "加速计+磁力计方位角: " + azimuth + "°");
    }
    
    private void calculateTiltAngle() {
        // 计算设备相对于水平面的倾斜角度
        float x = accelerometerData[0];
        float y = accelerometerData[1];
        float z = accelerometerData[2];
        
        // 计算倾斜角度
        double tiltRadians = Math.acos(z / Math.sqrt(x * x + y * y + z * z));
        float tiltDegrees = (float) Math.toDegrees(tiltRadians);
        
        currentTiltAngle = tiltDegrees;
        boolean isDeviceLevel = tiltDegrees < TILT_THRESHOLD;
        
        if (listener != null) {
            listener.onTiltChanged(tiltDegrees, isDeviceLevel);
        }
        
        if (!isDeviceLevel) {
            Log.d(TAG, "设备倾斜角度: " + tiltDegrees + "° (超过阈值 " + TILT_THRESHOLD + "°)");
        }
    }
    
    private void calculateMagneticFieldStrength() {
        // 计算磁场强度 (μT)
        float x = magnetometerData[0];
        float y = magnetometerData[1];
        float z = magnetometerData[2];
        
        currentMagneticStrength = (float) Math.sqrt(x * x + y * y + z * z);
        
        // 判断磁场状态
        MagneticFieldStatus status;
        if (currentMagneticStrength < MAGNETIC_FIELD_NORMAL_MIN) {
            status = MagneticFieldStatus.WEAK;
        } else if (currentMagneticStrength > MAGNETIC_FIELD_NORMAL_MAX) {
            status = MagneticFieldStatus.STRONG;
        } else {
            // 检查磁场稳定性
            if (isMagneticFieldStable()) {
                status = MagneticFieldStatus.NORMAL;
            } else {
                status = MagneticFieldStatus.DISTURBED;
            }
        }
        
        if (listener != null) {
            listener.onMagneticFieldChanged(currentMagneticStrength, status);
        }
        
        // 如果磁场异常，建议校准
        if (status != MagneticFieldStatus.NORMAL) {
            String reason = getCalibrationReason(status);
            if (listener != null) {
                listener.onCalibrationNeeded(reason);
            }
        }
        
        Log.v(TAG, "磁场强度: " + currentMagneticStrength + " μT, 状态: " + status);
    }
    
    private boolean isMagneticFieldStable() {
        // 这里可以实现更复杂的稳定性检测算法
        // 例如计算磁场向量的方差或检测突然变化
        return true; // 简化实现
    }
    
    private String getCalibrationReason(MagneticFieldStatus status) {
        switch (status) {
            case WEAK:
                return "磁场信号较弱，建议远离金属物体";
            case STRONG:
                return "磁场信号过强，建议远离磁性设备";
            case DISTURBED:
                return "检测到磁场干扰，建议校准罗盘";
            default:
                return "建议校准罗盘以获得更准确的方向";
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorName = getSensorName(sensor.getType());
        String accuracyLevel = getAccuracyLevel(accuracy);
        
        Log.d(TAG, sensorName + " 精度变化: " + accuracyLevel);
        
        if (listener != null) {
            listener.onAccuracyChanged(sensorName, accuracy);
        }
        
        // 如果精度太低，建议校准
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if (listener != null) {
                listener.onCalibrationNeeded(sensorName + "精度不可靠，请校准设备");
            }
        }
    }
    
    private String getSensorName(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "加速计";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "磁力计";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "旋转向量";
            default:
                return "未知传感器";
        }
    }
    
    private String getAccuracyLevel(int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "高精度";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "中等精度";
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "低精度";
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return "不可靠";
            default:
                return "未知";
        }
    }
    
    // Getter方法
    public float getCurrentAzimuth() {
        return currentAzimuth;
    }
    
    public float getCurrentMagneticStrength() {
        return currentMagneticStrength;
    }
    
    public float getCurrentTiltAngle() {
        return currentTiltAngle;
    }
    
    public boolean isRotationVectorAvailable() {
        return rotationVectorSensor != null;
    }
}

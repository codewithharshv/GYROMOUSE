package com.example.myapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorHelper implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private SensorCallback callback;

    private boolean isTracking = false;
    private float sensitivity = 1.0f;

    // Calibration offsets
    private float gyroOffsetX = 0;
    private float gyroOffsetY = 0;
    private float gyroOffsetZ = 0;

    public interface SensorCallback {
        void onMotionDetected(float dx, float dy);

        void onTiltDetected(float pitch, float roll);
    }

    public SensorHelper(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void setCallback(SensorCallback callback) {
        this.callback = callback;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void start() {
        if (isTracking)
            return;
        if (sensorManager != null) {
            if (gyroscope != null)
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            isTracking = true;
        }
    }

    public void stop() {
        if (!isTracking)
            return;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            isTracking = false;
        }
    }

    public void calibrate() {
        // No-op for now
    }

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3]; // Not using magnetometer yet, but gravity is enough for pitch/roll

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float rX = event.values[0];
            float rZ = event.values[2];

            // Apply sensitivity
            float dx = -rZ * sensitivity * 20;
            float dy = -rX * sensitivity * 20;

            if (Math.abs(dx) < 0.1)
                dx = 0;
            if (Math.abs(dy) < 0.1)
                dy = 0;

            if (callback != null && (dx != 0 || dy != 0)) {
                callback.onMotionDetected(dx, dy);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
            calculateOrientation();
        }
    }

    private void calculateOrientation() {
        if (callback == null)
            return;

        // Simple calculation from gravity vector
        // Pitch: Rotation around X-axis
        // Roll: Rotation around Y-axis

        // Assuming Landscape orientation for the Game
        // In landscape, X is along the short side (vertical), Y is along the long side
        // (horizontal)

        // However, Accelerometer values are relative to DEVICE coordinates (Portrait
        // default).
        // X: Horizontal (Right +)
        // Y: Vertical (Up +)
        // Z: Perpendicular to screen (Front +)

        float x = gravity[0];
        float y = gravity[1];
        float z = gravity[2];

        // Formulate pitch and roll in radians
        double pitch = Math.atan2(y, Math.sqrt(x * x + z * z));
        double roll = Math.atan2(-x, z);

        // Convert to degrees if needed, or normalized -1 to 1?
        // User wants Left/Right tilt -> Steering.
        // Tilting device left/right (like steering wheel) rotation around Y axis of
        // screen?
        // In default portrait: Rotating around Z axis? No.
        // Imagine holding phone in landscape.
        // Steering left: Left side down, right side up. This is rotation around Y-axis
        // of the phone (Portrait Y).
        // Which corresponds to X-axis of Accelerometer? No.

        // Let's use Rotation Matrix for proper orientation
        // Ideally we need Magnetometer for full Rotation Matrix, but for tilt
        // (pitch/roll) gravity is enough.
        // But getRotationMatrix needs both.
        // Let's stick to raw gravity vector analysis which is faster and lag-free.

        // Normalized values
        // Steering:
        // When held in landscape, X axis of sensor points UP (or down).
        // Y axis of sensor points RIGHT (or left).

        // Wait, standard Android axes:
        // X: right
        // Y: up
        // Z: forward

        // Landscape (90 deg CCW):
        // Screen X is Sensor Y.
        // Screen Y is Sensor -X.

        // Steering (tilting left/right relative to landscape view):
        // This effectively changes the Y-component of gravity (Sensor Y).
        // If flat: X=0, Y=0, Z=9.8
        // Steer Left (left side down): Sensor Y becomes positive?
        // Steer Right (right side down): Sensor Y becomes negative?

        // Accel / Brake (tilting forward/back):
        // This changes Sensor X.
        // Forward (top edge down): Sensor X becomes positive?

        // Let's pass normalized values based on gravity
        // Max tilt ~ 45 degrees -> ~7 m/s^2 component?
        // Gravity is 9.8. sin(45) * 9.8 = 6.9.

        float normSteer = y / 9.8f;
        float normDrive = x / 9.8f;

        // Clamp
        if (normSteer > 1.0f)
            normSteer = 1.0f;
        if (normSteer < -1.0f)
            normSteer = -1.0f;
        if (normDrive > 1.0f)
            normDrive = 1.0f;
        if (normDrive < -1.0f)
            normDrive = -1.0f;

        // Apply sensitivity here or in fragment?
        // Let's apply in fragment to distinct from sensitivity of joystick
        // Passing raw normalized values

        callback.onTiltDetected(normDrive, normSteer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }
}

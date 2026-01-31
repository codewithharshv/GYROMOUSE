package com.example.myapp;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

public class MouseFragment extends Fragment implements SensorHelper.SensorCallback {

    private SensorHelper sensorHelper;
    private NetworkManager networkManager;
    private View touchPad;
    private View touchpadArea;
    private View scrollStrip;
    private AppCompatButton toggleButton;

    private boolean isTouchpadMode = false;
    private GestureDetector gestureDetector;

    // Touchpad tracking
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    private long lastTapTime = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 300;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mouse, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorHelper = new SensorHelper(requireContext());
        sensorHelper.setCallback(this);
        networkManager = NetworkManager.getInstance();

        // Initialize views
        touchPad = view.findViewById(R.id.touch_pad);
        touchpadArea = view.findViewById(R.id.touchpad_area);
        scrollStrip = view.findViewById(R.id.scroll_strip);
        toggleButton = view.findViewById(R.id.btn_touchpad_toggle);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());

        // Left/Right Click buttons
        view.findViewById(R.id.btn_left).setOnClickListener(v -> networkManager.sendClick("L"));
        view.findViewById(R.id.btn_right).setOnClickListener(v -> networkManager.sendClick("R"));

        // Touchpad toggle button
        toggleButton.setOnClickListener(v -> toggleTouchpadMode());

        // Setup gesture detector for tap detection
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                handleTap(e);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                networkManager.sendClick("D"); // Double click
                return true;
            }
        });

        // Setup touchpad area touch listener
        setupTouchpadArea();

        // Setup scroll strip touch listener
        setupScrollStrip();

        // Setup original touch pad for gyro mode (scroll functionality)
        setupOriginalTouchPad();
    }

    private void toggleTouchpadMode() {
        isTouchpadMode = !isTouchpadMode;

        if (isTouchpadMode) {
            // Switch to Touchpad Mode
            touchPad.setVisibility(View.GONE);
            touchpadArea.setVisibility(View.VISIBLE);
            scrollStrip.setVisibility(View.VISIBLE);
            toggleButton.setBackgroundTintList(getResources().getColorStateList(R.color.btn_green, null));

            // Disable gyroscope
            if (sensorHelper != null) {
                sensorHelper.stop();
            }
        } else {
            // Switch to Gyro Mode
            touchPad.setVisibility(View.VISIBLE);
            touchpadArea.setVisibility(View.GONE);
            scrollStrip.setVisibility(View.GONE);
            toggleButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x40FFFFFF));

            // Enable gyroscope
            if (sensorHelper != null) {
                sensorHelper.start();
            }
        }
    }

    private void setupTouchpadArea() {
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Pass to gesture detector for tap detection
                gestureDetector.onTouchEvent(event);

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            // Single finger movement - move cursor
                            float dx = event.getX() - lastTouchX;
                            float dy = event.getY() - lastTouchY;

                            // Apply sensitivity multiplier
                            float sensitivity = 1.5f;
                            dx *= sensitivity;
                            dy *= sensitivity;

                            networkManager.sendMotion(dx, dy);

                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                        }
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        // Two finger tap detected
                        if (event.getPointerCount() == 2) {
                            networkManager.sendClick("R"); // Right click
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void handleTap(MotionEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.getPointerCount() == 1) {
            // Single finger tap = left click
            networkManager.sendClick("L");
        }
    }

    private void setupScrollStrip() {
        scrollStrip.setOnTouchListener(new View.OnTouchListener() {
            float lastScrollY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastScrollY = event.getY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dy = event.getY() - lastScrollY;
                        if (Math.abs(dy) > 10) {
                            int scrollAmount = (int) (dy / 5);
                            networkManager.sendScroll(scrollAmount);
                            lastScrollY = event.getY();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupOriginalTouchPad() {
        touchPad.setOnTouchListener(new View.OnTouchListener() {
            float lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = event.getY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dy = event.getY() - lastY;
                        if (Math.abs(dy) > 10) {
                            int scrollAmount = (int) (dy / 5);
                            networkManager.sendScroll(scrollAmount);
                            lastY = event.getY();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only start sensor if not in touchpad mode
        if (!isTouchpadMode && sensorHelper != null) {
            sensorHelper.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorHelper != null) {
            sensorHelper.stop();
        }
    }

    @Override
    public void onMotionDetected(float dx, float dy) {
        // Only send gyro motion if not in touchpad mode
        if (!isTouchpadMode && networkManager != null) {
            networkManager.sendMotion(dx, dy);
        }
    }

    @Override
    public void onTiltDetected(float pitch, float roll) {
        // Not used in MouseFragment
    }
}

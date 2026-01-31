package com.example.myapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MouseFragment extends Fragment implements SensorHelper.SensorCallback {

    private SensorHelper sensorHelper;
    private NetworkManager networkManager;
    private View touchPad;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        return inflater.inflate(R.layout.fragment_mouse, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorHelper = new SensorHelper(requireContext());
        sensorHelper.setCallback(this);
        networkManager = NetworkManager.getInstance();

        view.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());

        // Left/Right Click
        view.findViewById(R.id.btn_left).setOnClickListener(v -> networkManager.sendClick("L"));
        view.findViewById(R.id.btn_right).setOnClickListener(v -> networkManager.sendClick("R"));

        // Touchpad for scrolling? Or absolute positioning?
        // Let's implement scroll on the touchpad area for now, or just leave it as
        // visual anchor.
        touchPad = view.findViewById(R.id.touch_pad);
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
                        if (Math.abs(dy) > 10) { // Threshold
                            int scrollAmount = (int) (dy / 5); // Scale down
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
        if (sensorHelper != null)
            sensorHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorHelper != null)
            sensorHelper.stop();
    }

    @Override
    public void onMotionDetected(float dx, float dy) {
        // Send to network
        if (networkManager != null) {
            networkManager.sendMotion(dx, dy);
        }
    }

    @Override
    public void onTiltDetected(float pitch, float roll) {
        // Not used in MouseFragment
    }
}

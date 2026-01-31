package com.example.myapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.content.Context;
import android.widget.TextView;
import android.widget.LinearLayout;

public class GameFragment extends Fragment implements JoystickView.JoystickListener, SensorHelper.SensorCallback {

    private NetworkManager networkManager;
    private SensorHelper sensorHelper;
    private boolean isTiltEnabled = false;
    private float tiltSensitivity = 1.0f;
    private float leftStickSensitivity = 1.0f;
    private float rightStickSensitivity = 1.0f;

    private JoystickView leftJoystick;
    private JoystickView rightJoystick;

    // Joystick Key State
    private boolean isLeftStickUp = false;
    private boolean isLeftStickDown = false;
    private boolean isLeftStickLeft = false;
    private boolean isLeftStickRight = false;

    // Customization Mode
    private boolean isCustomizing = false;
    private View selectedView = null;
    private float dX, dY;
    private View customizationToolbar;
    private TextView tvSelectedControl;
    private SeekBar seekSize, seekOpacity;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sensorHelper != null) {
            sensorHelper.stop();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        networkManager = NetworkManager.getInstance();
        sensorHelper = new SensorHelper(requireContext());
        sensorHelper.setCallback(this);

        leftJoystick = view.findViewById(R.id.joystick_left);
        rightJoystick = view.findViewById(R.id.joystick_right);

        // Enable Customization for Joysticks
        setupJoystickCustomization(leftJoystick, "Left Stick");
        setupJoystickCustomization(rightJoystick, "Right Stick");

        // Enable Customization for DPAD Container and Action Container
        setupContainerCustomization(view.findViewById(R.id.dpad_container), "D-Pad");
        setupContainerCustomization(view.findViewById(R.id.action_buttons_container), "Actions"); // Correct ID?

        leftJoystick.setListener(this);
        rightJoystick.setListener(this);

        // Setup Buttons with Press/Release logic
        setupButton(view.findViewById(R.id.btn_up), "UP", "dpad");
        setupButton(view.findViewById(R.id.btn_down), "DOWN", "dpad");
        setupButton(view.findViewById(R.id.btn_left), "LEFT", "dpad");
        setupButton(view.findViewById(R.id.btn_right), "RIGHT", "dpad");

        setupButton(view.findViewById(R.id.btn_triangle), "TRIANGLE", "action");
        setupButton(view.findViewById(R.id.btn_square), "SQUARE", "action");
        setupButton(view.findViewById(R.id.btn_circle), "CIRCLE", "action");
        setupButton(view.findViewById(R.id.btn_x), "CROSS", "action");

        setupButton(view.findViewById(R.id.btn_l_shoulder), "L1", "shoulder");
        setupButton(view.findViewById(R.id.btn_r_shoulder), "R1", "shoulder");

        setupButton(view.findViewById(R.id.btn_start), "START", "system");
        setupButton(view.findViewById(R.id.btn_select), "SELECT", "system");

        // Settings Overlay
        View settingsOverlay = view.findViewById(R.id.settings_overlay);
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            settingsOverlay.setVisibility(View.VISIBLE);
        });
        view.findViewById(R.id.btn_close_settings).setOnClickListener(v -> {
            settingsOverlay.setVisibility(View.GONE);
        });

        // Settings Controls
        CheckBox cbTilt = view.findViewById(R.id.cb_tilt_control);
        cbTilt.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isTiltEnabled = isChecked;
            if (isChecked) {
                sensorHelper.start();
            } else {
                sensorHelper.stop();
            }
        });

        setupSensitivitySeekBar(view.findViewById(R.id.seek_sensitivity_left), value -> {
            leftStickSensitivity = value;
            leftJoystick.setSensitivity(value);
        });
        setupSensitivitySeekBar(view.findViewById(R.id.seek_sensitivity_right), value -> {
            rightStickSensitivity = value;
            rightJoystick.setSensitivity(value);
        });
        setupSensitivitySeekBar(view.findViewById(R.id.seek_sensitivity_tilt), value -> {
            tiltSensitivity = value;
        });

        prefs = requireContext().getSharedPreferences("layout_prefs", Context.MODE_PRIVATE);

        // Customization UI
        customizationToolbar = view.findViewById(R.id.customization_toolbar);
        tvSelectedControl = view.findViewById(R.id.tv_selected_control);
        seekSize = view.findViewById(R.id.seek_size);
        seekOpacity = view.findViewById(R.id.seek_opacity);

        view.findViewById(R.id.btn_customize_layout).setOnClickListener(v -> {
            settingsOverlay.setVisibility(View.GONE);
            enterCustomizationMode();
        });

        view.findViewById(R.id.btn_save_layout).setOnClickListener(v -> {
            saveLayout();
            exitCustomizationMode();
        });

        view.findViewById(R.id.btn_reset_layout).setOnClickListener(v -> {
            // Basic reset logic could be clearing prefs or manually resetting props
            // For now just exit or maybe clear selection
            // To implement true reset, we'd need default LayoutParams.
            // Let's just reset current selection for now if any
            if (selectedView != null) {
                selectedView.setScaleX(1.0f);
                selectedView.setScaleY(1.0f);
                selectedView.setAlpha(1.0f);
                // Reset position is harder without storing initial deps
            }
        });

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (selectedView != null) {
                    float scale = progress / 100.0f;
                    selectedView.setScaleX(scale);
                    selectedView.setScaleY(scale);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (selectedView != null) {
                    float alpha = progress / 100.0f;
                    selectedView.setAlpha(alpha);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        restoreLayout();
    }

    private void enterCustomizationMode() {
        isCustomizing = true;
        customizationToolbar.setVisibility(View.VISIBLE);
        // Disable sensor if needed?
    }

    private void exitCustomizationMode() {
        isCustomizing = false;
        selectedView = null;
        customizationToolbar.setVisibility(View.GONE);
    }

    private void saveLayout() {
        SharedPreferences.Editor editor = prefs.edit();
        saveViewProps(editor, leftJoystick, "joy_left");
        saveViewProps(editor, rightJoystick, "joy_right");
        saveViewProps(editor, getView().findViewById(R.id.dpad_container), "dpad");
        saveViewProps(editor, getView().findViewById(R.id.action_buttons_container), "actions");
        saveViewProps(editor, getView().findViewById(R.id.btn_l_shoulder), "l1");
        saveViewProps(editor, getView().findViewById(R.id.btn_r_shoulder), "r1");
        // Add others as needed
        editor.apply();
    }

    private void saveViewProps(SharedPreferences.Editor editor, View v, String key) {
        if (v == null)
            return;
        editor.putFloat(key + "_x", v.getX());
        editor.putFloat(key + "_y", v.getY());
        editor.putFloat(key + "_scale", v.getScaleX());
        editor.putFloat(key + "_alpha", v.getAlpha());
    }

    private void restoreLayout() {
        restoreViewProps(leftJoystick, "joy_left");
        restoreViewProps(rightJoystick, "joy_right");
        restoreViewProps(getView().findViewById(R.id.dpad_container), "dpad");
        restoreViewProps(getView().findViewById(R.id.action_buttons_container), "actions");
        restoreViewProps(getView().findViewById(R.id.btn_l_shoulder), "l1");
        restoreViewProps(getView().findViewById(R.id.btn_r_shoulder), "r1");
    }

    private void restoreViewProps(View v, String key) {
        if (v == null)
            return;
        if (prefs.contains(key + "_x"))
            v.setX(prefs.getFloat(key + "_x", 0));
        if (prefs.contains(key + "_y"))
            v.setY(prefs.getFloat(key + "_y", 0));
        if (prefs.contains(key + "_scale")) {
            float s = prefs.getFloat(key + "_scale", 1.0f);
            v.setScaleX(s);
            v.setScaleY(s);
        }
        if (prefs.contains(key + "_alpha"))
            v.setAlpha(prefs.getFloat(key + "_alpha", 1.0f));
    }

    private void makeEditable(View v, String name) {
        // This logic needs to decide if we are customizing or using the button
        // Currently setOnTouchListener is used for functionality.
        // We need to wrap it.
    }

    private void setupSensitivitySeekBar(SeekBar seekBar, SensitivityCallback callback) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Map 0-200 to 0.1-2.0
                float val = progress / 100.0f;
                if (val < 0.1f)
                    val = 0.1f;
                callback.onChange(val);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    interface SensitivityCallback {
        void onChange(float val);
    }

    private void setupJoystickCustomization(View v, String name) {
        v.setOnTouchListener((view, event) -> {
            if (isCustomizing) {
                return handleCustomizationTouch(view, event, name);
            }
            return false; // Propagate to JoystickView.onTouchEvent
        });
    }

    private void setupContainerCustomization(View v, String name) {
        v.setOnTouchListener((view, event) -> {
            if (isCustomizing) {
                return handleCustomizationTouch(view, event, name);
            }
            return false; // Children handle events?
            // If container and children have buttons, button needs to be customizing aware?
            // Actually, for dpad_container, the buttons are children.
            // If we drag container, we drag hole thing?
            // Or we just customize buttons individually?
            // User asked customize "buttons position". Usually grouping is better.
            // Let's allow moving the container.
            // Intercept touch on container might work if we claim it?
            // This might struggle with children buttons consuming click.
        });

        // To be safe, customization check should happen on buttons first.
        // If customizing, buttons consume move?
        // Actually, for simplicity, user can drag individual buttons if they want.
        // Or we create a mode where we put a overlay on top to capture touches.

        // Let's stick to individual button Customization plus the containers if they
        // have ID.
    }

    private void setupButton(View btn, String keyName, String group) {
        btn.setOnTouchListener((v, event) -> {
            if (isCustomizing) {
                return handleCustomizationTouch(v, event, keyName);
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendButtonEvent(keyName, group, true);
                v.setPressed(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                sendButtonEvent(keyName, group, false);
                v.setPressed(false);
            }
            return true;
        });
    }

    private boolean handleCustomizationTouch(View v, MotionEvent event, String name) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedView = v;
                dX = v.getX() - event.getRawX();
                dY = v.getY() - event.getRawY();
                tvSelectedControl.setText("Editing: " + name);

                // Update sliders
                seekSize.setProgress((int) (v.getScaleX() * 100));
                seekOpacity.setProgress((int) (v.getAlpha() * 100));
                break;

            case MotionEvent.ACTION_MOVE:
                v.animate()
                        .x(event.getRawX() + dX)
                        .y(event.getRawY() + dY)
                        .setDuration(0)
                        .start();
                break;
        }
        return true;
    }

    private void sendButtonEvent(String key, String group, boolean isPressed) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "button");
            json.put("group", group); // dpad, action, shoulder, system
            json.put("key", key);
            json.put("action", isPressed ? "PRESS" : "RELEASE");
            networkManager.sendJson(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoystickMoved(float x, float y, int id) {
        // Checking customizing state for Joysticks?
        // JoystickView has its own logic. We might need to "hijack" it.
        // Or simpler: put JoystickView in a container and move the container?
        // For now, let's assume we can move JoystickView itself if we attach a separate
        // listener
        // But JoystickView consumes touch.
        // We might need to Modify JoystickView to support "Edit Mode" or check a flag.

        // Simpler approach: Check isCustomizing here? No, onJoystickMoved is result.
        // We need to intercept touch on the view.

        // NOTE: View.OnTouchListener on JoystickView will override onTouchEvent if it
        // returns true.
        // So we can set a listener on JoystickView too!
        String source = (id == R.id.joystick_left) ? "left_stick" : "right_stick";
        try {
            JSONObject json = new JSONObject();
            json.put("type", "analog");
            json.put("source", source);
            json.put("x", x);
            json.put("y", y);
            networkManager.sendJson(json.toString());

            // Dual Mapping for Left Joystick (WASD + Arrows)
            if (id == R.id.joystick_left) {
                // Thresholds for triggering keys
                boolean currentUp = y < -0.5f; // Up is negative Y
                boolean currentDown = y > 0.5f;
                boolean currentLeft = x < -0.5f;
                boolean currentRight = x > 0.5f;

                if (currentUp != isLeftStickUp) {
                    isLeftStickUp = currentUp;
                    sendButtonEvent("W", "keyboard", isLeftStickUp);
                    sendButtonEvent("UP", "keyboard", isLeftStickUp);
                }
                if (currentDown != isLeftStickDown) {
                    isLeftStickDown = currentDown;
                    sendButtonEvent("S", "keyboard", isLeftStickDown);
                    sendButtonEvent("DOWN", "keyboard", isLeftStickDown);
                }
                if (currentLeft != isLeftStickLeft) {
                    isLeftStickLeft = currentLeft;
                    sendButtonEvent("A", "keyboard", isLeftStickLeft);
                    sendButtonEvent("LEFT", "keyboard", isLeftStickLeft);
                }
                if (currentRight != isLeftStickRight) {
                    isLeftStickRight = currentRight;
                    sendButtonEvent("D", "keyboard", isLeftStickRight);
                    sendButtonEvent("RIGHT", "keyboard", isLeftStickRight);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoystickReleased(int id) {
        onJoystickMoved(0, 0, id);
    }

    @Override
    public void onMotionDetected(float dx, float dy) {
        // Gyro data - maybe used for camera if not using right stick?
        // User requirements say "Tilt... Left/Right -> Steering". This is from
        // Accelerometer usually (Gravity).
        // onMotionDetected comes from Gyro in SensorHelper logic.
        // We can ignore this if we only want Tilt for steering, OR map it to Mouse
        // Look.
    }

    @Override
    public void onTiltDetected(float pitch, float roll) {
        if (!isTiltEnabled)
            return;

        // roll is steering (Left/Right)
        // pitch is drive (Forward/Back)

        try {
            JSONObject json = new JSONObject();
            json.put("type", "analog");
            json.put("source", "tilt");
            json.put("x", roll * tiltSensitivity);
            json.put("y", pitch * tiltSensitivity); // Invert if needed by receiver
            networkManager.sendJson(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

package com.example.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    private Paint outerPaint;
    private Paint innerPaint;
    private float outerRadius;
    private float innerRadius;
    private float centerX;
    private float centerY;
    private float joystickX;
    private float joystickY;
    private boolean isPressed = false;
    private JoystickListener listener;
    private float sensitivity = 1.0f;

    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent, int id);

        void onJoystickReleased(int id);
    }

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        outerPaint = new Paint();
        outerPaint.setColor(Color.parseColor("#44FFFFFF")); // Semi-transparent white
        outerPaint.setStyle(Paint.Style.FILL);
        outerPaint.setAntiAlias(true);

        innerPaint = new Paint();
        innerPaint.setColor(Color.parseColor("#AAFFFFFF")); // More solid white
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2.0f;
        centerY = h / 2.0f;
        // Adjust radii based on view size
        outerRadius = Math.min(w, h) / 2.0f;
        innerRadius = outerRadius * 0.4f;

        resetJoystick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw outer circle
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint);
        // Draw inner joystick knob
        canvas.drawCircle(joystickX, joystickY, innerRadius, innerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isPressed = true;
                updateJoystickPosition(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                resetJoystick();
                if (listener != null) {
                    listener.onJoystickReleased(getId());
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateJoystickPosition(float x, float y) {
        // Calculate displacement from center
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float maxDistance = outerRadius - innerRadius;

        // Clamp to circle
        if (distance > maxDistance) {
            float ratio = maxDistance / distance;
            dx *= ratio;
            dy *= ratio;
            joystickX = centerX + dx;
            joystickY = centerY + dy;
        } else {
            joystickX = x;
            joystickY = y;
        }

        // Normalize output (-1.0 to 1.0)
        if (listener != null) {
            float normX = dx / maxDistance * sensitivity;
            float normY = dy / maxDistance * sensitivity; // Invert Y if needed? Usually Up is -1 in screen coords but
                                                          // +1 in games.
            // In screen coords: Up (y < centerY) -> dy is negative.
            // Let's keep raw normalized values: Up = -1, Down = 1, Left = -1, Right = 1.
            // Receiver can invert if needed.

            // Clamp sensitivity logic: simply multiplying might go > 1.0, so clamp again.
            if (normX > 1.0f)
                normX = 1.0f;
            if (normX < -1.0f)
                normX = -1.0f;
            if (normY > 1.0f)
                normY = 1.0f;
            if (normY < -1.0f)
                normY = -1.0f;

            listener.onJoystickMoved(normX, normY, getId());
        }

        invalidate();
    }

    private void resetJoystick() {
        joystickX = centerX;
        joystickY = centerY;
        invalidate();
    }

    public void setListener(JoystickListener listener) {
        this.listener = listener;
    }

    public void setSensitivity(float s) {
        this.sensitivity = s;
    }

    public void setColors(int outer, int inner) {
        outerPaint.setColor(outer);
        innerPaint.setColor(inner);
        invalidate();
    }
}

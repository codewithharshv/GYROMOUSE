import socket
import json
import pyautogui
import pynput
import threading
import time

# Configuration
UDP_IP = "0.0.0.0"
UDP_PORT = 5005
SENSITIVITY_MOUSE = 2
SENSITIVITY_SCROLL = 5

# Global State for Continuous Motion
joystick_velocity_x = 0.0
joystick_velocity_y = 0.0
running = True

# Setup key controller
keyboard = pynput.keyboard.Controller()
mouse = pynput.mouse.Controller()

# Key Mappings
BUTTON_MAP = {
    "UP": pynput.keyboard.Key.up,
    "DOWN": pynput.keyboard.Key.down,
    "LEFT": pynput.keyboard.Key.left,
    "RIGHT": pynput.keyboard.Key.right,
    "CROSS": pynput.keyboard.Key.enter,       # Select / Confirm
    "CIRCLE": pynput.keyboard.Key.esc,        # Back / Cancel
    "TRIANGLE": 'm',                          # Map / Menu
    "SQUARE": 'r',                            # Reload / Interact
    "START": pynput.keyboard.Key.esc,         # Pause
    "SELECT": pynput.keyboard.Key.tab,        # Map
    "L1": 'q',
    "R1": 'e'
}

print(f"Listening on {UDP_PORT}...")

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

def handle_button(data):
    key_name = data.get("key")
    action = data.get("action")
    
    key = BUTTON_MAP.get(key_name)
    if not key:
        print(f"Unknown key: {key_name}")
        return

    if action == "PRESS":
        keyboard.press(key)
    elif action == "RELEASE":
        keyboard.release(key)

def handle_analog(data):
    source = data.get("source")
    x = data.get("x", 0)
    y = data.get("y", 0)
    
    if source == "left_stick":
        # WASD Emulation? or continuous movement?
        # Simple thresholding for WASD
        if y < -0.5: keyboard.press('w')
        else: keyboard.release('w')
        
        if y > 0.5: keyboard.press('s')
        else: keyboard.release('s')
        
        if x < -0.5: keyboard.press('a')
        else: keyboard.release('a')
        
        if x > 0.5: keyboard.press('d')
        else: keyboard.release('d')
        
    elif source == "right_stick":
        # Update velocity for continuous loop
        global joystick_velocity_x, joystick_velocity_y
        if abs(x) > 0.1: joystick_velocity_x = x
        else: joystick_velocity_x = 0.0
        
        if abs(y) > 0.1: joystick_velocity_y = y
        else: joystick_velocity_y = 0.0
    
    elif source == "tilt":
        # Steering (Roll -> x)
        # Drive (Pitch -> y)
        if abs(x) > 0.2: # Deadzone
             # Steering logic (maybe 'a' and 'd')
             if x < 0: 
                 keyboard.press('a')
                 keyboard.release('d')
             else:
                 keyboard.press('d')
                 keyboard.release('a')
        else:
            keyboard.release('a')
            keyboard.release('d')

def handle_mouse_motion(data):
    dx = data.get("dx", 0)
    dy = data.get("dy", 0)
    mouse.move(dx * SENSITIVITY_MOUSE, dy * SENSITIVITY_MOUSE)

def handle_mouse_click(data):
    action = data.get("action")
    if action == "L":
        mouse.click(pynput.mouse.Button.left)
    elif action == "R":
        mouse.click(pynput.mouse.Button.right)

def handle_mouse_scroll(data):
    amount = data.get("amount", 0)
    mouse.scroll(0, amount)

def handle_keyboard(data):
    key_name = data.get("key")
    # Mapping for simple keys or look up in BUTTON_MAP if appropriate
    # For now, let's assume it handles same keys as BUTTON_MAP or single chars
    key = BUTTON_MAP.get(key_name)
    if not key:
        # Try finding single char (e.g. for typing debug)
        if len(key_name) == 1:
            key = key_name
        else:
            print(f"Unknown keyboard key: {key_name}")
            return
            
    keyboard.press(key)
    keyboard.release(key)



def motion_loop():
    global joystick_velocity_x, joystick_velocity_y, running
    while running:
        if joystick_velocity_x != 0.0 or joystick_velocity_y != 0.0:
            dx = joystick_velocity_x * SENSITIVITY_MOUSE
            dy = joystick_velocity_y * SENSITIVITY_MOUSE
            # pynput can be problematic in some games, try pyautogui
            try:
                pyautogui.moveRel(int(dx), int(dy), _pause=False)
            except Exception:
                mouse.move(dx, dy)
        time.sleep(0.016) # ~60Hz

# Start motion thread
t = threading.Thread(target=motion_loop)
t.start()

while True:
    try:
        data, addr = sock.recvfrom(1024)
        message = data.decode()
        
        # Check if JSON
        if message.startswith("{"):
            payload = json.loads(message)
            msg_type = payload.get("type")
            
            if msg_type == "button":
                handle_button(payload)
            elif msg_type == "analog":
                handle_analog(payload)
            elif msg_type == "mouse_motion":
                handle_mouse_motion(payload)
            elif msg_type == "mouse_click":
                handle_mouse_click(payload)
            elif msg_type == "mouse_scroll":
                handle_mouse_scroll(payload)
            elif msg_type == "keyboard":
                handle_keyboard(payload)
            elif msg_type == "handshake":
                print(f"Handshake requested from {addr}")
                sock.sendto(b"ACK", addr)
                
        else:
            print(f"Received raw: {message}")
            
    except Exception as e:
        print(f"Error: {e}")

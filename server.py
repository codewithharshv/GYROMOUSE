import socket
import threading
from pynput.mouse import Button, Controller as MouseController
from pynput.keyboard import Key, Controller as KeyboardController

# --- Configuration ---
HOST = '0.0.0.0'  # Listen on all interfaces
PORT = 5005       # Must match App's default port

# --- Controllers ---
mouse = MouseController()
keyboard = KeyboardController()

def handle_udp():
    """Handles high-frequency motion data via UDP"""
    udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_sock.bind((HOST, PORT))
    print(f"UDP Listeninig on {HOST}:{PORT}")

    while True:
        try:
            data, addr = udp_sock.recvfrom(1024)
            message = data.decode('utf-8')
            
            if message.startswith("M:"): # M:dx,dy
                parts = message[2:].split(',')
                if len(parts) == 2:
                    dx = float(parts[0])
                    dy = float(parts[1])
                    mouse.move(dx, dy)
            elif message.startswith("S:"): # S:amount
                amount = int(message[2:])
                mouse.scroll(0, amount)
            elif message.startswith("C:"): # C:L or C:R
                action = message[2:]
                if action == 'L':
                    mouse.click(Button.left)
                elif action == 'R':
                    mouse.click(Button.right)
            elif message.startswith("K:"): # K:Key
                 key_name = message[2:]
                 print(f"Key: {key_name}")
                 # Simple mapping
                 if key_name == "UP": keyboard.press(Key.up); keyboard.release(Key.up)
                 elif key_name == "DOWN": keyboard.press(Key.down); keyboard.release(Key.down)
                 elif key_name == "LEFT": keyboard.press(Key.left); keyboard.release(Key.left)
                 elif key_name == "RIGHT": keyboard.press(Key.right); keyboard.release(Key.right)
                 elif key_name == "A": keyboard.press('a'); keyboard.release('a') # Example
                 elif key_name == "B": keyboard.press('b'); keyboard.release('b')
                 
        except Exception as e:
            print(f"UDP Error: {e}")

def handle_tcp():
    """Handles connection checks via TCP"""
    tcp_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tcp_sock.bind((HOST, PORT))
    tcp_sock.listen(5)
    print(f"TCP Listening on {HOST}:{PORT}")
    
    while True:
        conn, addr = tcp_sock.accept()
        print(f"Connection check from {addr}")
        # Just close it, the app only checks if it can connect
        conn.close()

if __name__ == "__main__":
    t_udp = threading.Thread(target=handle_udp)
    t_tcp = threading.Thread(target=handle_tcp)
    
    t_udp.start()
    t_tcp.start()
    
    print("GyroMouse Server Running...")
    print("Press Ctrl+C to stop (or close window)")

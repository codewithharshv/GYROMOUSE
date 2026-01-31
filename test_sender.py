import socket
import json
import time

UDP_IP = "127.0.0.1"
UDP_PORT = 5005

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

def send_stick(x, y):
    msg = {
        "type": "analog",
        "source": "right_stick",
        "x": x,
        "y": y
    }
    data = json.dumps(msg).encode('utf-8')
    sock.sendto(data, (UDP_IP, UDP_PORT))
    print(f"Sent: {msg}")

# Simulate holding right
print("Simulating joystick hold right...")
send_stick(1.0, 0.0)
time.sleep(2)
# Release
print("Releasing...")
send_stick(0.0, 0.0)

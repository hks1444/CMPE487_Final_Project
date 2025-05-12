import requests
import time
import random

SERVER_URL = "http://172.20.10.7:8080/load"
REQUEST_INTERVALS = [0.01, 0.1, 0.5, 1.0]

def generate_load():
    current_interval_index = 0
    while True:
        interval = REQUEST_INTERVALS[current_interval_index]
        current_interval_index = (current_interval_index + 1) % len(REQUEST_INTERVALS)
        print(f"Using interval {interval}s for the next 5 seconds")
        end_time = time.time() + 5  # keep this interval for 5 seconds

        while time.time() < end_time:
            try:
                resp = requests.get(SERVER_URL)
                print(f"Sent request. Response: {resp.status_code}")
            except Exception as e:
                print(f"Failed to send request: {e}")
            time.sleep(interval)

if __name__ == "__main__":
    generate_load()

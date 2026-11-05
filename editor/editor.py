import csv
import math
import os
import threading
import time

import requests
from flask import Flask, render_template, jsonify, request, Response
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

INPUT_CSV = 'sta.csv'
MAPPED_CSV = 'sta-mapped.csv'
ZOOM = 7

TARGET_TIMES_URL = 'https://www.msil.go.jp/data/tiles/smoni/targetTimes.json'
TILE_IMAGE_URL = 'https://www.msil.go.jp/data/tiles/smoni/tileimage/{basetime}/{validtime}/{z}/{x}/{y}.png'
USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) SnetViewer-Editor/1.0'

# Refresh the timestamp at most once per minute, matching the app's update cadence.
TARGET_TIME_CACHE_TTL = 60.0

_target_time_lock = threading.Lock()
_cached_basetime = None
_cached_validtime = None
_cached_at = 0.0


def fetch_latest_target_time(force_refresh=False):
    """Return the latest (basetime, validtime) from targetTimes.json.

    The result is cached briefly so loading all editor tiles does not query the
    timestamp endpoint once per tile. If a refresh fails, the last known
    timestamp is reused.
    """
    global _cached_basetime, _cached_validtime, _cached_at

    now = time.monotonic()
    with _target_time_lock:
        if (not force_refresh and _cached_basetime is not None
                and now - _cached_at < TARGET_TIME_CACHE_TTL):
            return _cached_basetime, _cached_validtime

        try:
            resp = requests.get(
                TARGET_TIMES_URL,
                headers={'User-Agent': USER_AGENT},
                timeout=12,
            )
            resp.raise_for_status()
            entries = resp.json()

            if not isinstance(entries, list) or not entries:
                raise ValueError('targetTimes.json did not contain any entries')

            latest = max(
                (entry for entry in entries
                 if isinstance(entry, dict) and entry.get('basetime')),
                key=lambda entry: str(entry.get('basetime')),
                default=None,
            )
            if latest is None:
                raise ValueError('targetTimes.json did not contain a valid basetime')

            basetime = str(latest['basetime']).strip()
            validtime = str(latest.get('validtime') or basetime).strip()
            if not basetime or not validtime:
                raise ValueError('targetTimes.json contained an empty timestamp')

            _cached_basetime = basetime
            _cached_validtime = validtime
            _cached_at = now
            return basetime, validtime
        except Exception as exc:
            if _cached_basetime is not None:
                print(f'Warning: Could not refresh targetTimes.json, using cached timestamp: {exc}')
                return _cached_basetime, _cached_validtime
            raise

def latlng_to_pixels(lat, lng, zoom):
    n = 2.0 ** zoom
    x = n * ((lng + 180.0) / 360.0)
    lat_rad = math.radians(lat)
    y = n * (1.0 - (math.log(math.tan(lat_rad) + (1.0 / math.cos(lat_rad))) / math.pi)) / 2.0

    tilex = int(x)
    tiley = int(y)
    pixelx = int((x - tilex) * 256)
    pixely = int((y - tiley) * 256)

    return tilex, tiley, pixelx, pixely

def init_mapped_csv():
    if not os.path.exists(MAPPED_CSV) and os.path.exists(INPUT_CSV):
        with open(INPUT_CSV, 'r', encoding='utf-8') as infile, \
             open(MAPPED_CSV, 'w', encoding='utf-8', newline='') as outfile:

            reader = csv.DictReader(infile, delimiter=';')
            fieldnames = reader.fieldnames + ['tilex', 'tiley', 'pixelx', 'pixely']
            writer = csv.DictWriter(outfile, fieldnames=fieldnames, delimiter=';')
            writer.writeheader()

            for row in reader:
                lat = float(row['lat'])
                lng = float(row['lng'])
                tx, ty, px, py = latlng_to_pixels(lat, lng, ZOOM)

                row['tilex'] = tx
                row['tiley'] = ty
                row['pixelx'] = px
                row['pixely'] = py
                writer.writerow(row)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/proxy/tiles/<int:z>/<int:x>/<int:y>.png')
def proxy_tile(z, x, y):
    try:
        basetime, validtime = fetch_latest_target_time()
        external_url = TILE_IMAGE_URL.format(
            basetime=basetime,
            validtime=validtime,
            z=z,
            x=x,
            y=y,
        )
        resp = requests.get(
            external_url,
            headers={'User-Agent': USER_AGENT},
            timeout=15,
        )
        resp.raise_for_status()

        # Create a Flask response with the image data
        proxy_resp = Response(resp.content, mimetype=resp.headers['Content-Type'])

        # Explicitly add the CORS header so Canvas can read it
        proxy_resp.headers['Access-Control-Allow-Origin'] = '*'

        return proxy_resp

    except requests.exceptions.RequestException as e:
        print(f"Error fetching tile {z}/{x}/{y}: {e}")
        # Return a 404 or a transparent 1x1 image on error
        return "Tile not found", 404
    except Exception as e:
        print(f"Error resolving target time for tile {z}/{x}/{y}: {e}")
        return "Target time unavailable", 503

@app.route('/api/data', methods=['GET'])
def get_data():
    data = []
    with open(MAPPED_CSV, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f, delimiter=';')
        for row in reader:
            data.append(row)
    return jsonify(data)

@app.route('/api/update', methods=['POST'])
def update_data():
    updates = request.json
    rows = []

    # Read and update
    with open(MAPPED_CSV, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f, delimiter=';')
        fieldnames = reader.fieldnames
        for row in reader:
            if row['name'] == updates['name']:
                row['tilex'] = updates['tilex']
                row['tiley'] = updates['tiley']
                row['pixelx'] = updates['pixelx']
                row['pixely'] = updates['pixely']
            rows.append(row)

    # Write back
    with open(MAPPED_CSV, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter=';')
        writer.writeheader()
        writer.writerows(rows)

    return jsonify({"status": "success"})

if __name__ == '__main__':
    init_mapped_csv()
    try:
        latest_basetime, latest_validtime = fetch_latest_target_time(force_refresh=True)
        print(f"Using latest target time: basetime={latest_basetime}, validtime={latest_validtime}")
    except Exception as e:
        print(f"Warning: Could not fetch the latest target time at startup: {e}")
    app.run(debug=True, port=5234)

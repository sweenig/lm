#!/usr/bin/env python3

import time
import sys

# Setup Prometheus
from prometheus_client import Gauge
from prometheus_client import start_http_server

gtemp = Gauge('temp','Temperature as measured by BME280',['scale'])
gtemp.labels('celsius')
gtemp.labels('fahrenheit')

gpressure = Gauge('pressure','Pressure as measured by BME280')

ghumidity = Gauge('humidity','Humidity as measured by BME280')

glight = Gauge('light','Light as measured by LTR559')

gnoise = Gauge('noise','Noise as measured by MEMS microphone',['range'])
gnoise.labels('low')
gnoise.labels('mid')
gnoise.labels('high')
gnoise.labels('amp')

# Setup hardware sensors
try:
    # Transitional fix for breaking change in LTR559
    from ltr559 import LTR559
    ltr559 = LTR559()
except ImportError:
    import ltr559

from bme280 import BME280
from subprocess import PIPE, Popen

bme280 = BME280() # BME280 temperature/pressure/humidity sensor

from enviroplus.noise import Noise
noise = Noise()

def get_cpu_temperature():
    # Get the temperature of the CPU for compensation
    process = Popen(['vcgencmd', 'measure_temp'], stdout=PIPE, universal_newlines=True)
    output, _error = process.communicate()
    return float(output[output.index('=') + 1:output.rindex("'")])


def main():
    # Tuning factor for compensation. Decrease this number to adjust the
    # temperature down, and increase to adjust up
    factor = 2.25
    cpu_temps = [get_cpu_temperature()] * 5
    delay = 50

    # The main loop
    try:
        while True:
            cpu_temp = get_cpu_temperature() # Smooth out with some averaging to decrease jitter
            cpu_temps = cpu_temps[1:] + [cpu_temp]
            avg_cpu_temp = sum(cpu_temps) / float(len(cpu_temps))
            raw_temp = bme280.get_temperature()
            tempC = raw_temp - ((avg_cpu_temp - raw_temp) / factor)
            tempF = ( tempC * 9 / 5 ) + 32
            print(f"temperature: {tempC} C")
            gtemp.labels('celsius').set(tempC)
            print(f"temperature: {tempF} F")
            gtemp.labels('fahrenheit').set(tempF)

            pressure = bme280.get_pressure()
            print(f"pressure: {pressure} hPa")
            gpressure.set(pressure)

            humidity = bme280.get_humidity()
            print(f"humidity: {humidity} %")
            ghumidity.set(humidity)

            proximity = ltr559.get_proximity()
            if proximity < 10: lux = ltr559.get_lux()
            else: lux = 1
            print(f"light: {lux} Lux")
            glight.set(lux)

            low, mid, high, amp = noise.get_noise_profile()
            low *= 128
            mid *= 128
            high *= 128
            amp *= 64
            print(f"Noise profile: {[low,mid,high]} @ {amp} dB")
            gnoise.labels('low').set(low)
            gnoise.labels('mid').set(mid)
            gnoise.labels('high').set(high)
            gnoise.labels('amp').set(amp)

            time.sleep(delay)

    # Exit cleanly
    except KeyboardInterrupt:
        sys.exit(0)


if __name__ == "__main__":
    metrics_port = 8000
    start_http_server(metrics_port)
    main()
# OpenMetrics Auto DataSource Creator
## Problem
OpenMetrics are a very easy way for developers to add metrics to their applications. The result is an HTTP hosted page on the hosting resource that contains a plain text rendering of the metrics and their values. LogicMonitor has a module called "OpenMetrics_Template" which you can use as a starting point to build the DS required to monitor the metrics available. However, this isn't very easy to do.
## Solution
I've written this Python script that reads the OpenMetrics page, parses out the metrics and creates the DataSource definition in an XML file. It creates all the datapoints for you with their appropriate types (counter vs. gauge) and customizes it for your application.
## Usage
### Serving the Metrics via OpenMetrics
If you have an Enviro hat or Enviro+ hat for the RaspberryPi, feel free to replicate the example [here](example/serve_metrics.py). It simply gathers the sensor readings from the Enviro/Enviro+ hat and serves them up using the Prometheus (old name for OpenMetrics) client.
The result is a page that looks like this:
```
# HELP python_gc_objects_collected_total Objects collected during gc
# TYPE python_gc_objects_collected_total counter
python_gc_objects_collected_total{generation="0"} 287.0
python_gc_objects_collected_total{generation="1"} 50.0
python_gc_objects_collected_total{generation="2"} 0.0
# HELP python_gc_objects_uncollectable_total Uncollectable object found during GC
# TYPE python_gc_objects_uncollectable_total counter
python_gc_objects_uncollectable_total{generation="0"} 0.0
python_gc_objects_uncollectable_total{generation="1"} 0.0
python_gc_objects_uncollectable_total{generation="2"} 0.0
# HELP python_gc_collections_total Number of times this generation was collected
# TYPE python_gc_collections_total counter
python_gc_collections_total{generation="0"} 75.0
python_gc_collections_total{generation="1"} 6.0
python_gc_collections_total{generation="2"} 0.0
# HELP python_info Python platform information
# TYPE python_info gauge
python_info{implementation="CPython",major="3",minor="7",patchlevel="3",version="3.7.3"} 1.0
# HELP process_virtual_memory_bytes Virtual memory size in bytes.
# TYPE process_virtual_memory_bytes gauge
process_virtual_memory_bytes 7.2982528e+07
# HELP process_resident_memory_bytes Resident memory size in bytes.
# TYPE process_resident_memory_bytes gauge
process_resident_memory_bytes 2.7041792e+07
# HELP process_start_time_seconds Start time of the process since unix epoch in seconds.
# TYPE process_start_time_seconds gauge
process_start_time_seconds 1.69817691508e+09
# HELP process_cpu_seconds_total Total user and system CPU time spent in seconds.
# TYPE process_cpu_seconds_total counter
process_cpu_seconds_total 128.4
# HELP process_open_fds Number of open file descriptors.
# TYPE process_open_fds gauge
process_open_fds 8.0
# HELP process_max_fds Maximum number of open file descriptors.
# TYPE process_max_fds gauge
process_max_fds 1024.0
# HELP temp Temperature as measured by BME280
# TYPE temp gauge
temp{scale="celsius"} 24.842082007484926
temp{scale="fahrenheit"} 76.71574761347287
# HELP pressure Pressure as measured by BME280
# TYPE pressure gauge
pressure 980.9724990710467
# HELP humidity Humidity as measured by BME280
# TYPE humidity gauge
humidity 37.180572459362416
# HELP light Light as measured by LTR559
# TYPE light gauge
light 534.5991
# HELP noise Noise as measured by MEMS microphone
# TYPE noise gauge
noise{range="low"} 63.18473473654132
noise{range="mid"} 34.42067041500264
noise{range="high"} 14.835566572556312
noise{range="amp"} 18.740161954016713
```
As you can see, the text follows a very specific format, listing out the description of the metric (on the `# HELP` line), the metric type (on the `# TYPE` line), and the metric's value on the other lines. To over-simplify it, metrics can have sub-metrics or alternate representations of the metric depending on the unit. Notice that the `temp` datapoint has a celsius and a fahrenheit representation. They're both the same metric, just different units. The `noise` datapoint also has sub-metrics. In this case, `noise` acts more like a group with the `range`s representing the different metrics under noise. 
### Generating the DS file
To generate the DS file, you must have access to the OpenMetrics page. The Python script requires two arguments:
1. ds_name: This is the name of the DataSource you want to create. For the example, this value would be EnviroPi or something similar. You would want to name it after the application hosting the OpenMetrics page.
2. test_host: This is the (resolvable) name or IP address of the resource on which the OpenMetrics page is hosted. This does not have to be the resource(s) you will end up monitoring, but it should match the metrics on them. 

There are two optional arguments:
1. test_port: This is the port number on which the test_host is hosting the OpenMetrics page. If you omit it, the script assumes port 8000.
2. test_path: This is the path on which the test_host is hosting the OpenMetrics page. If you omit it, the script assumes `/metrics`

#### Executing the script
1. Navigate to the directory where this repo is cloned.
2. Execute `python build_ds.py EnviroPi enviropi.local` where `EnviroPi` is the desired name of the DS and `enviropi.local` is the name of the device the OpenMetrics page is running on.
3. If you need to specify the port and path: `python build_ds.py EnviroPi enviropi.local 8000 "/metrics"`

The output should look something like this:
```
Querying enviropi.local for OpenMetrics data...OK
  Found metric python_gc_objects_collected_total. Gathering metadata...OK
  Found metric python_gc_objects_uncollectable_total. Gathering metadata...OK
  Found metric python_gc_collections_total. Gathering metadata...OK
  Found metric python_info. Gathering metadata...OK
  Found metric process_virtual_memory_bytes. Gathering metadata...OK
  Found metric process_resident_memory_bytes. Gathering metadata...OK
  Found metric process_start_time_seconds. Gathering metadata...OK
  Found metric process_cpu_seconds_total. Gathering metadata...OK
  Found metric process_open_fds. Gathering metadata...OK
  Found metric process_max_fds. Gathering metadata...OK
  Found metric temp. Gathering metadata...OK
  Found metric pressure. Gathering metadata...OK
  Found metric humidity. Gathering metadata...OK
  Found metric light. Gathering metadata...OK
  Found metric noise. Gathering metadata...OK
Outputting to EnviroPi_DS.xml...OK
```
Once you've completed successfully, you can see in the output the file name of the XML file generated.
### Importing the File
Go to `LogicMonitor >> Settings >> DataSources >> Add >> Import from File...` and browse to the file that was created. You should be able to add it and nothing will apply.
Notice the AppliesTo of the newly imported DataSource. Simply add that property to any resource(s) that this DataSource should apply to. The value of the property doesn't matter, but you could put `true` or `enabled` or `1`. You could technically also put `false`, the AppliesTo only looks for that property to exist, it doesn't look at the value.
### What this Script Does NOT Do
This script does not build graphs for you. This is completely dependent on the data that is coming in and you'd want to build these graphs yourself customizing them to the story you're trying to tell with the data.
## Conclusion
I hope this helps, feel free to use it, but please include attribution to this GitHub repo.
> Written with [StackEdit](https://stackedit.io/).
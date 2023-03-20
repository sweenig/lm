# lmwrapper

lmwrapper is a Python library for simplifying interaction with the LogicMonitor API. It uses the LogicMonitor Python SDK, but takes it a step further by facilitating credentials management. All credentials are stored in a JSON file called creds.json that looks like this:

```json
{
  "lmstuartweenig": {
    "API_ACCESS_ID": "adelarthrosomatous",
    "API_ACCESS_KEY": "zygomaticoauricularis",
    "COMPANY_NAME": "lmstuartweenig"
  },
  "traininglab": {
    "API_ACCESS_ID": "adelarthrosomatous",
    "API_ACCESS_KEY": "zygomaticoauricularis",
    "COMPANY_NAME": "traininglab"
  }
}
```

If only one set of credentials is needed, the JSON file would look like this:
```json
{
  "lmstuartweenig": {
    "API_ACCESS_ID": "adelarthrosomatous",
    "API_ACCESS_KEY": "zygomaticoauricularis",
    "COMPANY_NAME": "lmstuartweenig"
  }
}

```

## Installation

Just put the file [lm.py](../blob/main/lm.py) into the same directory as the creds.json file. You'll either need to have the [logicmonitor_sdk](https://www.logicmonitor.com/support/rest-api-developers-guide/logicmonitor-sdks) installed or downloaded into the current directory.

## Setup
You can use this tool either in a script or in the interpreter. Either way, once you've got the credentials file setup, getting started only takes one line:

```python
>>> from lm import *
```

## SDK Usage
If you are using this utility in a script, the first entry in your credentials file will be used.

In interactive mode, if multiple sets of credentials are found in the credentials file, you will be prompted for which set you want to use. Simply return the number (index) of the set you want to use, then you can call any of the [published SDK methods](https://www.logicmonitor.com/swagger-ui-master/api-v3/lm-sdkv3-docs.html) against the `lm` object. The example below uses the [REPL](https://en.wikipedia.org/wiki/Read%E2%80%93eval%E2%80%93print_loop) style of getting the output. You could just as easily pprint it or store the results in a variable.

```python
>>> from lm import *
The following credentials were found in creds.json.
0. lmstuartweenig
1. traininglab
Which one would you like to use? 0
>>> lm.get_device_list()
{'items': [{'auto_properties': [{'name': 'predef.externalResourceID',
                                 'value': '06:57:9f:e2:63:b8'},
                                {'name': 'auto.eri.override', 'value': '1'},
                                {'name': 'auto.product.name', 'value': 'null'},
                                {'name': 'auto.idledays', 'value': '98'},
                                {'name': 'auto.ip.v4.routing_enabled',
                                 'value': 'false'},
                                {'name': 'auto.enterprise_number',
                                 'value': '8072'},
                                {'name': 'auto.endpoint.uptime',
                                 'value': '482 days, 22:17:23.40'},
                                {'name': 'auto.memory.total',
                                 'value': '8373010432'},
---OUTPUT TRUNCATED---

```

## Sending log data to LM Logs
I've added my functions for sending logs to LM Logs. It uses the same credentials from the credentials file. LM Logs log entries can be associated with a specific device in your portal. You need to pass the system.deviceId into the `log_msg` function in order for this to work. If you don't know the id at runtime, you can find the name by using the `getDeviceByDisplayName()` function:

```python
from lm import *
id = getDeviceByDisplayName("docker-plex")
print(id)
```

Once you have the device id (optional), you can call the `log_msg()` function. By default, this doesn't immediately send the log to LM Logs. Instead, it adds the log message to a queue so they can be send in bulk after some/all of your logs have been gathered. By default the `max_log_queue_size` determines the queue size threshold to automatically send logs. Otherwise, you can call the `log_msg()` function with `flush_queue=True` to immediately send the logs. 

```python
from lm import *
log_msg("This is the message", getDeviceByDisplayName("docker-plex"), flush_queue=True)
```

The `log_msg()` function will echo to the screen immediately and if the `show_response` parameter is set to `True`, you will also get a message confirming that the logs were sent to LM Logs. Note that the show_response parameter only has an effect if the queue is being flushed automatically or manually, and it only prints the information to stdout.

```python
from lm import *
log_msg("This is the first message", getDeviceByDisplayName("docker-plex"))
log_msg("This is the second message", getDeviceByDisplayName("docker-plex"))
log_msg("This is the last message", getDeviceByDisplayName("docker-plex"), flush_queue=True, show_response=True)
```

A severity option is available in the `log_msg()` function as well. Simply include a string indicating whatever severity you want. The message posted to LM Logs will have this severity prepended. You can remove this by modifying the `log_queue.append()` call. 

You'll also notice that you can change the values of the `debug` and `info` variables. If set to False, log messages with these severities will only print to screen and not be sent to LM Logs. This can be handy for sending all info to LM Logs while developing, but during production run, you can turn them off without having to remove all of the `log_msg()` calls.

## LM API v3 Calls

I've also included my `LM_API()` function, which I use to make all v3 API calls since the SDK didn't support v3. Even now that it does support v3, I still use this because sleeping dogs are laying. The return value is a dictionary, converted from the raw JSON response.

I use it for GET, POST, PUT, DELETE, and PATCH, although it does not have native pagination. Maybe later.

LM_API("GET","/alert/alerts")
```
{'total': 3, 'items': [{'resourceId': 705, 'anomaly': False, 'instanceName': 'SNMP_Network_Interfaces-Port 6 [ID:6]', 'monitorObjectId': 13, 'endEpoch': 0, 'rule': '', 'threshold': '> 1', 'type': 'dataSourceAlert', 'startEpoch': 1668631459, 'enableAnomalyAlertGeneration': '', 'internalId': 'LMD19453', 'ackComment': '', 'monitorObjectName':...
---OUTPUT TRUNCATED---
```

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Attributions
Special thanks go to the following for spawning the idea, method, and benefits:

- Michael Leo at Harvard University
- Stefan Wuensch at Harvard University

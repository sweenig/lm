# (C) 2021 - 2023 Author: Stuart Weenig

import logicmonitor_sdk, json, requests, hashlib, base64, hmac, time
from datetime import datetime
from os import path
import __main__ as main
from functools import wraps

# simple helper function not really used here
def response_to_json(response):
    return(json.dumps(response.to_dict()))

credsfile = "C:/Users/sweenig/creds.json"

error_message = f"""File not found: {credsfile}
Create a config file called creds.json in this directory that looks like this:
{{
  "API_ACCESS_ID": "adelarthrosomatous",
  "API_ACCESS_KEY": "zygomaticoauricularis",
  "COMPANY_NAME": "yourportalname"
}}"""

def paginate(lmmethod, **kwargs): # paginates calls for any method in the SDK.
    data = []
    end_found = False
    offset = 0
    size = 1000
    while not end_found:
        current = lmmethod(size=size, offset=offset, **kwargs).items
        data += current
        offset += len(current)
        end_found = len(current) != size
    return data

class AutoPagingLMApi:
    """Proxy LMApi that adds automatic pagination with per-call overrides."""
    def __init__(self, api, default_page_size=1000, default_paginate=True):
        self._api = api
        self._default_page_size = default_page_size
        self._default_paginate = default_paginate

    def __getattr__(self, name):
        attr = getattr(self._api, name)
        if not callable(attr):
            return attr

        @wraps(attr)
        def wrapped(*args, paginate=None, page_size=None, **kwargs):
            should_paginate = self._default_paginate if paginate is None else paginate
            if not should_paginate:
                return attr(*args, **kwargs)

            request_kwargs = dict(kwargs)
            size = page_size or request_kwargs.pop("size", None) or self._default_page_size
            offset = request_kwargs.pop("offset", 0)
            data = []
            while True:
                response = attr(*args, size=size, offset=offset, **request_kwargs)
                items = getattr(response, "items", None)
                if items is None:
                    return response
                data.extend(items)
                if len(items) < size:
                    break
                offset += len(items)
            return data
        return wrapped

    def paginate(self, lmmethod, **kwargs):
        return paginate(lmmethod, **kwargs)

# General SDK access
if path.exists(credsfile):
    with open(credsfile) as f: creds = json.load(f)
    if len(creds) == 0:
        print(f"No credentials found in file {credsfile}.")
    elif len(creds) == 1 or hasattr(main,'__file__'):
        print(f"Using creds {list(creds.values())[0]['API_ACCESS_ID']}@{list(creds.values())[0]['COMPANY_NAME']}")
        creds = list(creds.values())[0]
    elif len(creds) > 1:
        print(f"The following credentials were found in {credsfile}.")
        for i, key in enumerate(creds): print(f"{i}. {key}")
        try:
            credidx = int(input("Which one would you like to use? "))
        except KeyboardInterrupt:
            print("\n")
            quit()
        creds = list(creds.values())[credidx]
    configuration = logicmonitor_sdk.Configuration()
    configuration.company           = creds['COMPANY_NAME']
    configuration.access_id         = creds['API_ACCESS_ID']
    configuration.access_key        = creds['API_ACCESS_KEY']
    base_lm = logicmonitor_sdk.LMApi(logicmonitor_sdk.ApiClient(configuration))
    lm = AutoPagingLMApi(base_lm)
    # build the creds for use in other functions
    lm_creds = {"AccessId": creds['API_ACCESS_ID'],"AccessKey": creds['API_ACCESS_KEY'],"Company": creds['COMPANY_NAME']}
else:
    print(error_message)
    lm = None

# get device id by device name
def getDeviceByDisplayName(displayName):
    items = lm.get_device_list(filter=f"displayName:\"{displayName}\"").items
    if len(items) > 0:
        return items[0].id
    else:
        return None

# Logging to LM Logs
max_log_queue_size = 200
info = True
debug = True
log_queue = []

# actually sends the logs
def send_logs(messages, show_response=False):
    resource_path = '/log/ingest'
    headers =  {'Content-Type': 'application/json','X-Version': '3'}
    url = f"https://{lm_creds['Company']}.logicmonitor.com/rest{resource_path}"
    body = json.dumps(messages)
    timestamp = int(time.time()*1000)
    req_var =  "POST" + str(timestamp) + body + resource_path
    signature = base64.b64encode(bytes(hmac.new(
                        bytes(lm_creds['AccessKey'], 'latin-1'),
                        bytes(req_var, 'latin-1'),
                        digestmod=hashlib.sha256
                    ).hexdigest(), 'latin-1')).decode('latin-1')
    auth = f"LMv1 {lm_creds['AccessId']}:{signature}:{timestamp}"
    headers['Authorization'] =  auth
    try:
        response = requests.post(url, verify=True, headers=headers, data=body)
        if response.status_code != 202:
            print(f"Failed to send log. Error: {response.status_code} {response.text}\nLog message: {messages}")
        else:
            if show_response: print(f"SUCCESS submitting {len(messages)} log messages:",response.text)
    except Exception as e: print("Unable to connect. Error: ", e)

# queues logs into a list so we're submitting in bulk instead of one at a time
def log_msg(msg, deviceId, severity="INFO", flush_queue=False, show_response=False):
    if not lm:
        print(error_message)
        return
    global log_queue
    deviceId = str(deviceId)
    if (severity == "DEBUG" and debug) or (severity == "INFO" and info) or severity not in ("DEBUG", "INFO"):
        print(f"{datetime.now().strftime('[%Y-%m-%d %H:%M:%S]')} {severity}: {msg}")
        log_queue.append({"msg":f"{severity}: {msg}", "timestamp":int(time.time()*1000), "_lm.resourceId":{"system.deviceId": deviceId}})
        if len(log_queue) >= max_log_queue_size or flush_queue:
            send_logs(log_queue, show_response)
            log_queue = []

def LM_API(httpVerb, resourcePath, data="", queryParams={}):
    if not lm:
        print(error_message)
        return
    queryParams_str = "&".join([f"{k}={v}" for k,v in queryParams.items()])
    url = 'https://'+ lm_creds['Company'] +'.logicmonitor.com/santaba/rest' + resourcePath + "?" + queryParams_str
    epoch = str(int(time.time() * 1000))
    requestVars = httpVerb + epoch + data + resourcePath
    digest = hmac.new(
            lm_creds['AccessKey'].encode('utf-8'),
            msg=requestVars.encode('utf-8'),
            digestmod=hashlib.sha256
    ).hexdigest()
    signature = base64.b64encode(digest.encode('utf-8')).decode('utf-8')
    auth = 'LMv1 ' + lm_creds['AccessId'] + ':' + str(signature) + ':' + epoch
    headers = {
        'Content-Type':'application/json',
        'Authorization':auth,
        'X-Version': "3"
    }
    if httpVerb == "GET":
        response = requests.get(url, data=data, headers=headers)
    elif httpVerb == "POST":
        response = requests.post(url, data=data, headers=headers)
    elif httpVerb == "PUT":
        response = requests.put(url, data=data, headers=headers)
    elif httpVerb == "DELETE":
        response = requests.delete(url, data=data, headers=headers)
    elif httpVerb == "PATCH":
        response = requests.patch(url, data=data, headers=headers)
    # print('Response Status:',response.status_code)
    # print('Response Body:',response.content)
    return json.loads(response.text)

def LM_APIp(queryParams = {}, **kwargs):
    data = []
    end_found = False
    offset = 0
    size = 1000
    while not end_found:
        current = LM_API(**kwargs, queryParams={"size":size, "offset": offset, **queryParams})['items']
        data += current
        offset += len(current)
        end_found = len(current) != size
    return data

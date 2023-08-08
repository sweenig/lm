from lm import lm
import datetime, json
from pprint import pprint,pformat
s = "2023/08/08 9:00:00" # %Y/%m/%d %H:%M:%S format in tz of system running the script. 8am central (my laptop) is 9am eastern (when i want to start)
interval = 1440
desiredVersion = 33006 # five digit format, first two digits are major version, last 3 are minor. For 33.006 use 33006
try:
    collectors = {x.id:x.build for x in lm.get_collector_list(size=1000).items}
except Exception as e:
    print(f"There was an error obtaining the collector list:\n{e}")

startTime = int(datetime.datetime.strptime(s,"%Y/%m/%d %H:%M:%S").timestamp())
for id, version in list(collectors.items()):
    if int(version) < desiredVersion:
        payload = {'onetimeUpgradeInfo': {'majorVersion': int(str(desiredVersion)[:2]),'minorVersion': int(str(desiredVersion)[2:]),'startEpoch': startTime,}}
        # payload = {'onetimeUpgradeInfo': None} # uncomment to delete all* schedules
        try:
            response = lm.patch_collector_by_id(id,payload,op_type="replace")
            if "errorCode" in response.to_dict():
                pprint(response.to_dict())
            else:
                print(f"SUCCESS: Scheduled collector {id} for upgrade from {version} to {desiredVersion} at {datetime.datetime.fromtimestamp(startTime).strftime('%Y/%m/%d %H:%M:%S')}")
                startTime += (60 * interval)
        except Exception as e:
            print(f"ERROR: There was an error scheduling the upgrade for {id}: {json.loads(e.body)['errorMessage']}")
    else:
        print(f"SKIP: Collector {id} is already at or higher than the desired version: {version}")
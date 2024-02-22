#! /usr/bin/env python
import logicmonitor_sdk, argparse, time
from datetime import datetime

parser = argparse.ArgumentParser()
for arg,help in {
    'id':'LM API Access ID',
    'key': 'LM API Access Key',
    'company': 'LM API Company',
    'dashboardId': 'ID of the dashboard on which the widget resides',
    'widgetId': 'ID of the widget to be updated'
}.items():
    parser.add_argument(arg, help=help)
args = parser.parse_args()

info = True
debug = False

lm_creds = {
    "AccessId": args.id,
    "AccessKey": args.key,
    "Company": args.company
}

configuration = logicmonitor_sdk.Configuration()
configuration.access_id = args.id
configuration.access_key = args.key
configuration.company = args.company
lm = logicmonitor_sdk.LMApi(logicmonitor_sdk.ApiClient(configuration))

widget_id = args.widgetId

print("Fetching device list...")

devices = []
end_found = False
offset = 0
size = 1000
while not end_found:
    current = lm.get_device_list(size=size,offset=offset).items
    devices += current
    offset += len(current)
    end_found = len(current) != size

devices = [{**y.to_dict(),**{x.name:x.value for x in y.system_properties}} for y in devices]

print("Generating output...")

content = """<script>
function sortTable(n) {
  var table, rows, switching, i, x, y, shouldSwitch, dir, switchcount = 0;
  table = document.getElementById("device_table");
  switching = true;
  dir = "asc";
  while (switching) {
    switching = false;
    rows = table.rows;
    for (i = 1; i < (rows.length - 1); i++) {
      shouldSwitch = false;
      x = rows[i].getElementsByTagName("TD")[n];
      y = rows[i + 1].getElementsByTagName("TD")[n];
      if (dir == "asc") {
        if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {shouldSwitch= true; break;}
      } else {
        if (x.innerHTML.toLowerCase() < y.innerHTML.toLowerCase()) {shouldSwitch = true; break;}
      }
    }
    if (shouldSwitch) {
      rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
      switching = true;
      switchcount ++;
    } else {
      if (switchcount == 0 && dir == "asc") {
        dir = "desc";
        switching = true;
      }
    }
  }
  header_cells = document.getElementsByClassName("device_table_header");
  for (var i = 0; i < header_cells.length; i++) {header_cells[i].children[0].innerHTML = "";}
  if (dir == "asc"){
    header_cells[n].children[0].innerHTML = "&#9650";
  } else {
    header_cells[n].children[0].innerHTML = "&#9660";
  }
}
</script>
<style>
    table{border-collapse:collapse}
    td,tr,th{
        border:1px solid black;
        padding:5px;
    }
    th {
        cursor:pointer;
        color:blue;
        text-decoration:underline;
    }
</style>"""

content += """
<table id="device_table"><tr>
    <th class="device_table_header" onclick="sortTable(0)">Device Name<span id="sort"></span></th>
    <th class="device_table_header" onclick="sortTable(1)">IP/FQDN<span id="sort"></span></th>
    <th class="device_table_header" onclick="sortTable(2)">ID<span id="sort"></span></th>
</tr>
"""

for device in devices:
    content += f"""<tr><td>{device['display_name']}</td><td>{device['name']}</td><td>{device['id']}</td></tr>\n"""

content += """</table>\n"""
currtime = datetime.now().strftime("%B %d, %Y at %I:%M %p")
content += f"""<p>Last updated {currtime}<p>"""

print(content)

print("Updating widget content...")

body = {
    "content":content,
    "dashboard_id":args.dashboardId,
    "name":"My HTML Widget"
}

try:
    patch_operation = lm.patch_widget_by_id(widget_id,body)
except Exception as e:
    print(e)

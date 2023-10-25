# (C) 2023 Stuart Weenig
import requests, argparse
from jinja2 import Environment, FileSystemLoader

parser = argparse.ArgumentParser()
for arg,help in {
    "ds_name": "The name of the DS",
    "test_host": "IP or hostname of the host serving the OpenMetrics page"
}.items(): parser.add_argument(arg, help=help)
for arg,help,default in [
    ("test_port", "TCP port that the test_host is listening on", 8000),
    ("test_path", "Path to the metrics page", "/metrics")
]: parser.add_argument(arg, nargs='?', help=help, default=default)
args = parser.parse_args()

# from: https://realpython.com/primer-on-jinja-templating/
environment = Environment(loader=FileSystemLoader('templates/'))
template = environment.get_template("openmetrics.xml")

metrics = []
metric_name = "dummyplaceholder"
print(f"Querying {args.test_host} for OpenMetrics data...", end="")
data = requests.get(f"http://{args.test_host}:{args.test_port}{args.test_path}")
if data.ok:
    print(data.reason)
    for section in data.text.split("# HELP"):
        for line in section.splitlines():
            if line.startswith(" "):
                description = ' '.join(line.split(" ")[2:])
                metric_name = line.split(" ")[1]
                print(f"  Found metric {metric_name}. Gathering metadata...", end="")
            if line.startswith("# TYPE"):
                metric_type = line.split(" ")[-1]
            if line.startswith(metric_name):
                metric_variation_name = line.split(" ")[0].replace("{","_").replace("=","_").replace("\"","").replace("}","").replace(",","_").replace(".","_")
                metrics.append({
                    "name": metric_variation_name,
                    "desc": description,
                    "type": metric_type
                })
        if len(section.splitlines()) > 0: print(f"OK")
    content = template.render(metrics=metrics, ds_name=args.ds_name)
    print(f"Outputting to {args.ds_name}_DS.xml...", end="")
    try:
        with open(f"{args.ds_name}_DS.xml", mode="w") as f: f.write(content)
        print("OK")
    except Exception as e: print(f"ERROR.\n{e}")
else: print(f"ERRROR.\nThere was an error {data.status_code} communicating with {data.url}:\n{data.reason}\n{data.text}")
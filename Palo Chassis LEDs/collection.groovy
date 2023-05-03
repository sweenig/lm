def host = hostProps.get("system.hostname")
def port = hostProps.get("paloalto.port")?: 443
def apikey = hostProps.get("paloalto.apikey.pass")?.trim()
if (apikey == null) {println("No paloalto.apikey.pass set");return 1}

status_map = [
    "Off": 0,
    "Green": 1,
    "Yellow": 2,
    "Red": 3,
    "AndAnother": 4
]
def response
def command = URLEncoder.encode("<show><system><state><filter>chassis.leds</filter></state></system></show>", "UTF-8")
def url = "https://${host}:$port/api/?type=op&key=${apikey}&cmd=${command}"
def getRequestConn = url.toURL().openConnection()
if (getRequestConn.responseCode == 200) {
    body = getRequestConn.content.text
    response = new XmlSlurper().parseText(body)
    data = response.toString()
    data.tokenize("{,}").each{
        kv = it.tokenize(":")
        if ((it.trim() != "chassis.leds:") && (kv.size() > 1)){
            println("${kv[0].replaceAll('\'','').trim()}.status:${status_map[kv[1].trim()]}")
        }
    }
    return 0
} else {return 2}
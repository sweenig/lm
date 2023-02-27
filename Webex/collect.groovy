/*******************************************************************************
 * © 2023 Aqueduct Technologies Inc. All rights reserved.
 ******************************************************************************/
import groovy.json.JsonSlurper
serviceId = hostProps.get("webex.serviceId")
def STATUS_MAP = ["operational": 1, "degraded_performance": 2, "partial_outage": 3, "major_outage": 4]
url = "https://service-status.webex.com/customer/dashServices/${serviceId}?_=${new Date().getTime()}"
def status
def getRequestConn = url.toURL().openConnection()
if (getRequestConn.responseCode == 200) {
    status = new JsonSlurper().parseText(getRequestConn.content.text)
} else {println "Failed to GET ${url}\nStatus code: ${getRequestConn.responseCode}";return 1}

status.components.each{group ->
    group.components.each{component ->
        println("${component.componentId}.status: ${STATUS_MAP[component.status]}")
    }
}

return 0
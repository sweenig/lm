// (C) 2023 Aqueduct Technologies Inc.
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.santaba.agent.groovyapi.http.HTTP
import com.santaba.agent.util.Settings
import com.santaba.agent.util.script.ScriptCache

debug = false

creds = [
    "id": hostProps.get("lmlogs.lmauditlogs.id"),
    "key": hostProps.get("lmlogs.lmauditlogs.key")
]
ScriptCache collectorCache = ScriptCache.getCache()
// Map proxyInfo = getProxyInfo()

groups = lm_api("GET", "/setting/admin/groups", creds, ["size": 1000, "offset": 0, "fields": "id,name"])
groupsMap = groups.data.items.collectEntries{[it.id,it.name]}
if (debug) {println(groupsMap)}
users = lm_api("GET", "/setting/admins", creds, ["size": 1000, "offset": 0, "fields": "username,adminGroupIds"])
usersMap = users.data.items.collectEntries{[it.username,groupsMap[it.adminGroupIds[0]]]}
if (debug) {println(usersMap)}

oldestTimestamp =
    collectorCache.get("audit_log_previous_newest_timestamp")!=null ?
    collectorCache.get("audit_log_previous_newest_timestamp") as Long : 0
newestTimestamp = ((new Date()).getTime() / 1000).toLong()

if (debug) {println("Collector cache data: ${oldestTimestamp}")}

queryParams = [
    'size':1000,
    'offset':0,
    'filter':"happenedOn>\"${oldestTimestamp}\",happenedOn<:\"${newestTimestamp}\",username:\"(!(System*|AQ_NOC*))\""
]
response = lm_api("GET", "/setting/accesslogs", creds, queryParams)
if (response.code.toInteger() == 200){
    if (debug) {println("Fetched ${response.data.items.size()} log entries.")}
    events = []
    response.data.items.each{
        event_dict = [:]
        event_dict["_lm.logsource_name"] = "LMAuditLog"
        event_dict["timestamp"] = it.happenedOn
        event_dict["message"] = it.description
        event_dict["sourceIP"] = it.ip
        event_dict["username"] = it.username // to do: lookup this against existing user list to return customer and validity of user
        event_dict["Tenant"] = usersMap[it.username] ?: it.username
        event_dict["entryId"] = it.id
        event_dict["_lm.resourceId"] = ["system.deviceId": hostProps.get('system.deviceId')]
        events.add(event_dict)
    }
    payload = JsonOutput.toJson(events)
    if (debug){println(payload)}
    push_logs = lm_log('/log/ingest',payload)
    
    collectorCache.set("audit_log_previous_newest_timestamp", newestTimestamp as String, 12 * 60 * 60 * 1000)
    if (push_logs.toString() == "202"){
        return events.size()
    } else {
        println("There was an error ${push_logs} sending the logs to LM")
        return -2
    }
} else {
    println("There was an error ${response.code} fetching the logs: ${response}")
    return -1
}

def lm_api(httpVerb, resourcePath, credentials, queryParams = [:], payload = "", proxyInfo = null) {
    account = credentials['account'] ?: Settings.getSetting(Settings.AGENT_COMPANY) //Grab portal account name from the Collector's config file
    if (debug) {println("${httpVerb}ing ${resourcePath} from ${account} using parameters: ${queryParams}")}
    accessId = credentials['id']
    accessKey = credentials['key']
    url = "https://" + account + ".logicmonitor.com" + "/santaba/rest"
    headers = calcSignature(httpVerb,resourcePath,accessKey,accessId,payload)
    httpClient = HTTP.open(account + ".logicmonitor.com", 443)
    if (proxyInfo?.enabled) {
        if(proxyInfo.user && proxyInfo.pass){
            httpClient.setHTTPProxy(proxyInfo.host, proxyInfo.port.toInteger(), proxyInfo.user, proxyInfo.pass)
        }
        else{
            httpClient.setHTTPProxy(proxyInfo.host, proxyInfo.port.toInteger())
        }
    }
    returnMap = [:]
    switch (httpVerb){
        case "GET":
            full_url = url + resourcePath + "?" + queryParams.collect{k,v -> "${k}=${URLEncoder.encode(v.toString(), "UTF-8")}"}.join("&")
            if (debug) {println("Full url: ${full_url}")}
            returnMap['response'] = httpClient.get(full_url, headers)
            returnMap['data'] = new JsonSlurper().parseText(httpClient.getResponseBody())
            break
        case "POST":
            returnMap['response'] = httpClient.post(url + resourcePath, payload, headers)
            break
        default:
            println("httpVerb ${httpVerb} is not supported")
            break
    }
    returnMap['code'] = httpClient.getStatusCode()
    httpClient.close()
    return returnMap
}

def lm_log(resourcePath, payload, proxyInfo = null) {
    //Grab portal account name from the Collector's config file
    account = hostProps.get("lmaccount")?:Settings.getSetting(Settings.AGENT_COMPANY)
    accessId = hostProps.get('lmlogs.winevent.lmaccess.id')
    accessKey = hostProps.get('lmlogs.winevent.lmaccess.key')
    url = "https://" + account + ".logicmonitor.com" + "/rest"
    headers = calcSignature('POST',resourcePath,accessKey,accessId,payload)
    httpClient = HTTP.open(account + ".logicmonitor.com", 443)

    if (proxyInfo?.enabled) {
        if(proxyInfo.user && proxyInfo.pass){
            httpClient.setHTTPProxy(proxyInfo.host, proxyInfo.port.toInteger(), proxyInfo.user, proxyInfo.pass)
        }
        else{
            httpClient.setHTTPProxy(proxyInfo.host, proxyInfo.port.toInteger())
        }
    }
    response = httpClient.post(url + resourcePath, payload, headers)
    responseCode = httpClient.getStatusCode()
    httpClient.close()
    return responseCode
}

def calcSignature(verb,resourcePath,accessKey,accessId,data = '') {
    epoch = System.currentTimeMillis()
    requestVars = verb + epoch + data + resourcePath
    hmac = Mac.getInstance("HmacSHA256")
    secret = new SecretKeySpec(accessKey.getBytes(), "HmacSHA256")
    hmac.init(secret)
    hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()))
    signature = hmac_signed.bytes.encodeBase64()
    headers = ["X-Version":"3","Content-Type":"application/json"]
    headers.put("Authorization" , "LMv1 " + accessId + ":" + signature + ":" + epoch)
    return headers
}
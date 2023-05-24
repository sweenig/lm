// (C) 2023 Aqueduct Technologies LLC
import groovy.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.HttpEntity
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils

deviceId = hostProps.get("system.deviceID")
debug = true

// get the details of the datasource in question and extract the id
// setup your request parameters
queryParams = ["fields": "id","filter": "dataSourceName:%22VMware_ESXi_HostCPUCores%22"] // here is where you set the ds name filter
httpVerb = "GET"
resourcePath = "/device/devices/${deviceId}/devicedatasources"
if (debug) {println("${httpVerb}ing ${resourcePath}")}
// make the request
response = LM_API(httpVerb, resourcePath, "lmaccess", [:], queryParams)
// validate the response and extract the desired value
if ((response.code >= 200) && (response.code < 300) && (response.body.items.size() > 0)){
    hdsId = response.body.items[0].id
} else {println("There was a ${response.code} error: ${response.body}");return 1}

// get the details of all the instances of the DS (of that hdsId) on that device
// and extract the ID of the one we want to patch
queryParams = [:]
httpVerb = "GET"
resourcePath = "/device/devices/${deviceId}/devicedatasources/${hdsId}/instances"
if (debug) {println("${httpVerb}ing ${resourcePath}")}
response = LM_API(httpVerb, resourcePath, "lmaccess")
if ((response.code >= 200) && (response.code < 300) && (response.body.items.size() > 0)){
    // get instance of interest
    instanceId = response.body.items.findAll{it.displayName == "6"}.collect{it.id}[0] //here's where you set the criteria for the instance you want to patch
} else {println("There was a ${response.code} error: ${response.body}");return 2}

// patch the instance with new data
queryParams = ["opType":"replace"]
httpVerb = "PATCH"
data = ["customProperties":[["name":"foo","value":"bar"]]] //here is where you set the data you want to patch in
resourcePath = "/device/devices/${deviceId}/devicedatasources/${hdsId}/instances/${instanceId}"
if (debug) {println("${httpVerb}ing ${resourcePath}")}
response = LM_API(httpVerb, resourcePath, "lmaccess", data, queryParams)
if ((response.code >= 200) && (response.code < 300)){
    println(response.body.customProperties)
} else {println("There was a ${response.code} error: ${response.body}");return 3}

return 0

def LM_API(httpVerb, endpointPath, creds="lmaccess", data = [:], queryParams=[:]){
    def api_id = hostProps.get("${creds}.id")
    def api_key = hostProps.get("${creds}.key")
    def api_company = hostProps.get("${creds}.company")
    def queryParamsString = queryParams.collect{k,v->"${k}=${v}"}.join("&")
    def url = "https://${api_company}.logicmonitor.com/santaba/rest${endpointPath}?${queryParamsString}"
    epoch_time = System.currentTimeMillis()
    def json_data = JsonOutput.toJson(data)
    if ((httpVerb == "GET") || (httpVerb == "DELETE")){requestVars = httpVerb + epoch_time + endpointPath}
    else {requestVars = httpVerb + epoch_time + json_data + endpointPath}
    hmac = Mac.getInstance("HmacSHA256")
    secret = new SecretKeySpec(api_key.getBytes(), "HmacSHA256")
    hmac.init(secret)
    hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()))
    signature = hmac_signed.bytes.encodeBase64()
    CloseableHttpClient httpclient = HttpClients.createDefault()
    if (httpVerb == "GET"){http_request = new HttpGet(url)}
    else if (httpVerb == "PATCH"){
        http_request = new HttpPatch(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else if (httpVerb == "PUT"){
        http_request = new HttpPut(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else if (httpVerb == "POST"){
        http_request = new HttpPost(url)
        http_request.setEntity(new StringEntity(json_data, ContentType.APPLICATION_JSON))}
    else {http_request = null}
    if (http_request){
        http_request.setHeader("Authorization" , "LMv1 " + api_id + ":" + signature + ":" + epoch_time)
        http_request.setHeader("Accept", "application/json")
        http_request.setHeader("Content-type", "application/json")
        http_request.setHeader("X-Version", "3")
        response = httpclient.execute(http_request)
        body = EntityUtils.toString(response.getEntity())
        code = response.getStatusLine().getStatusCode()
        return ["body":new JsonSlurper().parseText(body), "code":code]}
    else {return ["body":"", "code": -1]}
}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import com.santaba.agent.groovyapi.http.*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

debug = false

accessId = hostProps.get("alerting_status.id")
accessKey = hostProps.get("alerting_status.key")

def lm_get_request(_resourcePath, _queryParams, _accessId = accessId, _accessKey = accessKey) {
    account = ''
    File file = new File('../conf/agent.conf')
    file.text.eachLine{if (it =~ /company=/){ account = it.split('=')[1].trim() }}
    url = "https://" + account + ".logicmonitor.com" + "/santaba/rest"
    httpClient = HTTP.open(account + ".logicmonitor.com", 443)
    headers=calcSignature('GET',_resourcePath,accessKey,accessId) // calcSignature defined below
    headers.put("X-Version", "3")
    fullURL = url + _resourcePath + _queryParams
    if (debug){println("URL: " + fullURL)}
    response = httpClient.get(fullURL, headers)
    if (debug){println(response)}
    responseBody = httpClient.getResponseBody()
    try {
        allResponse = new JsonSlurper().parseText(responseBody)
    } catch (Exception e) {
        allResponse = responseBody
    }
    return allResponse
}

def calcSignature(_verb,_resourcePath,_accessKey,_accessId,_data = '') {
    epoch = System.currentTimeMillis()
    requestVars = _verb + epoch + _data + _resourcePath
    hmac = Mac.getInstance("HmacSHA256")
    secret = new SecretKeySpec(_accessKey.getBytes(), "HmacSHA256")
    hmac.init(secret)
    hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()))
    signature = hmac_signed.bytes.encodeBase64()
    headers = [:]
    headers.put("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch)
    return headers
}

lm_get_request("/device/devices/${hostProps.get('system.deviceId')}",'?fields=disableAlerting').each{println(it)}
return 0
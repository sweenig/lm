import com.santaba.agent.groovyapi.http.HTTP
import groovy.json.JsonSlurper
wildvalue = instanceProps.get("WILDVALUE")
headers = ["x-api-key": hostProps.get("ambee_api.key")]
risk_map = ["Low":0, "Moderate":1, "High":2, "Very High":3]
try{
    httpClient = HTTP.open("https://api.ambeedata.com", 443)
    url = "https://api.ambeedata.com/forecast/pollen/by-lat-lng?lat=${instanceProps.get("auto.lat")}&lng=${instanceProps.get("auto.lon")}&_=${new Date().getTime()}"
    getResponse = httpClient.get(url, headers)
    data = new JsonSlurper().parseText(httpClient.getResponseBody()).data
    [0:"",24:"1d_",(-1):"2d_"].each{idx,label->
        data[idx].Count.each{k,v-> println("${wildvalue}.${label}count_${k}: ${v}")}
        data[idx].Risk.each{k,v-> println("${wildvalue}.${label}risk_${k}: ${risk_map[v]}")}
        data[idx].Species.each{type,values->
            if (type=="Others"){println("${wildvalue}.${label}species_others: ${values}")}
            else {values.each{k,v-> println("${wildvalue}.${label}${type}_${k.replaceAll(' ','').replaceAll('/','')}: ${v}")}}
        }
    }
    return 0
} catch (Exception e) {println(e);println("Error details and troubleshooting steps at http://tinyurl.com/5x2bcwjy");return 1}
finally {httpClient.close()}
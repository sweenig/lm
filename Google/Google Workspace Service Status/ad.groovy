import groovy.json.JsonSlurper

try{
    jsondata = new URL("https://www.google.com/appsstatus/dashboard/products.json").getText()
    data = new JsonSlurper().parseText(jsondata)
    data.products.each{println("${it.id}##${it.title}")}
    return 0
} catch (Exception e){return 1}
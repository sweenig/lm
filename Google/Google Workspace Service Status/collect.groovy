import groovy.json.JsonSlurper

debug = false

try{
    if (debug){
        jsondata = new URL("https://www.google.com/appsstatus/dashboard/products.json").getText()
        data = new JsonSlurper().parseText(jsondata)
        println(data)
        products = data.products.collectEntries{[(it.id):it.title]}
    }
    services = [:]
    jsondata = new URL("https://www.google.com/appsstatus/dashboard/incidents.json").getText()
    data = new JsonSlurper().parseText(jsondata)
    if (debug){println(data)}
    data.each{
        it.affected_products.each{product->
            if (debug) {println("${products[product.id]} (${product.id}): ${it.most_recent_update.status}")}
            if (services.containsKey(product.id)){
                if (services[product.id].containsKey(it.most_recent_update.status)){
                    services[product.id][it.most_recent_update.status] += 1
                } else {services[product.id][it.most_recent_update.status] = 1}
            } else {services[product.id] = [(it.most_recent_update.status): 1]}
        }
    }
    services.each{id,statuses->
        statuses.each{k,v->
            println("${id}.${k}: ${v}")
        }
    }
    return 0
} catch (Exception e){println("Error: ${e}");return 1}
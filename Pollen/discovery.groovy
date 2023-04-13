hostProps.get("pollen_locations").tokenize("|").each{location->
    (name, lat, lon) = location.tokenize(",")
    wildvalue = name.replaceAll("=","").replaceAll(":","").replaceAll("#","").replaceAll(" ","")
    println("${name}##${name}######lat=${lat}&lon=${lon}")
}
return 0
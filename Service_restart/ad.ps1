$hostname = '##HOSTNAME##'
$prop = '##Service_restart.services##'
$splitprops = $prop.split(",| ",[System.StringSplitOptions]::RemoveEmptyEntries)
$processedServices = @{} # Hashtable to store processed services
Foreach ($i in $splitprops){
  $services = get-service -name $i -ComputerName $hostname
  if ($services){
    Foreach ($service in $services) {
      if (-not $processedServices.ContainsKey($service.Name)) { # Check if service name is not in hashtable
        "$($service.Name)##$($service.DisplayName)"
        $processedServices[$service.Name] = $true # Add service name to hashtable
      }
    }
  } else {
    Write-Host "Service '$i' not found."
  }
}
exit 0

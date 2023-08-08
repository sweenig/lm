$hostname = '##HOSTNAME##'
$prop = '##Service_restart.services##'
$splitprops = $prop.split(",| ",[System.StringSplitOptions]::RemoveEmptyEntries)
Foreach ($i in $splitprops){
  $service = get-service -name $i -ComputerName $hostname
  if ($service){
    "$($service.Name)##$($service.DisplayName)"
  } else {
    Write-Host "Service '$i' not found."
  }
}
exit 0
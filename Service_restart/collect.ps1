$hostname = "##HOSTNAME##"
$username = '##wmi.user##'
$password = '##wmi.pass##'
$credential = $null
if($username -and $password){
  $securePassword = $password | ConvertTo-SecureString -AsPlainText -Force
  $credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)
}
$service = "##WILDVALUE##"
$wait = 15 # seconds to wait

# Define a function to get service status
function Get-ServiceStatus($service, $hostname, $credential) {
  $service_status = (Get-Service -Name $service -ComputerName $hostname -ErrorAction SilentlyContinue).Status
  if (-not $service_status -and $credential) {
    $service_status = (Get-WmiObject -Class Win32_Service -Filter "Name='$service'" -ComputerName $hostname -Credential $credential).State
  }
  return $service_status
}

# Try to get the service status
$service_status = Get-ServiceStatus -service $service -hostname $hostname -credential $credential

Write-Host "$service is $service_status on $hostname"

if ($service_status -eq "Running") {
  Write-Host "result_code: 1 (running)"
} else {
  Write-Host "not running so i will start then wait $wait seconds and test"
  Start-Service -InputObject $(Get-Service -Computer $hostname -Name $service -ErrorAction SilentlyContinue)
  Start-Sleep $wait

  # Check the service status again
  $service_status = Get-ServiceStatus -service $service -hostname $hostname -credential $credential

  if ($service_status -eq "Running") {
    Write-Host "result_code: 2 (first restart worked)"
  } else {
    Write-Host "first restart failed so i will start second time"
    Start-Service -InputObject $(Get-Service -Computer $hostname -Name $service -ErrorAction SilentlyContinue)
    Start-Sleep $wait

    # Check the service status again
    $service_status = Get-ServiceStatus -service $service -hostname $hostname -credential $credential

    if ($service_status -eq "Running") {
      Write-Host "result_code: 3 (second restart worked)"
    } else {
      Write-Host "result_code: 4 (second restart failed so probably trigger alert)"
    }
  }
}

$hostname = '##HOSTNAME##'
$username = '##wmi.user##'
$password = '##wmi.pass##'
if($username -and $password){
  $securePassword = $password | ConvertTo-SecureString -AsPlainText -Force
  $credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)
}
$prop = '##Service_restart.services##'
$splitprops = $prop.split(",| ",[System.StringSplitOptions]::RemoveEmptyEntries)

# Get all services once through WMI
$allServices = Get-WmiObject -Class Win32_Service -ComputerName $hostname -Credential $credential

Foreach ($i in $splitprops){
    Try {
        $services = $allServices | Where-Object { $_.Name -eq $i }
        if (-not $services) {
            # Get all services through alternative command
            $services = Get-Service -name $i -ComputerName $hostname
        }
        Foreach ($service in $services) {
            "$($service.Name)##$($service.DisplayName)##Windows Service: $($service.Name)"
        }
    } Catch {
        Write-Host "Error occurred while processing service '$i': $_"
    }
}
exit 0

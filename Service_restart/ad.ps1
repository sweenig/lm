$hostname = '##HOSTNAME##'
$username = '##wmi.user##'
$password = '##wmi.pass##'
if($username -and $password){
  $securePassword = $password | ConvertTo-SecureString -AsPlainText -Force
  $credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)
}
$prop = '##Service_restart.services##'

$splitprops = $prop.split(",| ",[System.StringSplitOptions]::RemoveEmptyEntries)
$processedServices = @{} # Hashtable to store processed services

# Get all services once
$allServices = Get-WmiObject -Class Win32_Service -ComputerName $hostname -Credential $credential

Foreach ($i in $splitprops){
    Try {
        $services = $allServices | Where-Object { $_.Name -eq $i }
        if (-not $services) {
            $services = Get-Service -name $i -ComputerName $hostname
        }
        Foreach ($service in $services) {
            if (-not $processedServices.ContainsKey($service.Name)) { # Check if service name is not in hashtable
                "$($service.Name)##$($service.DisplayName)##Windows Service-$($service.Name)"
                $processedServices[$service.Name] = $true # Add service name to hashtable
            }
        }
    } Catch {
        Write-Host "Error occurred while processing service '$i': $_"
    }
}
exit 0

# Alerting Status PropertySource
This property source simply looks for the status of the alerting toggle in LM and reports it back as a property.
## Implementation
1. Create an API token (RO) and put "Alerting Status PropertySource" in the description of the token.
2. Add two properties to your root group (assuming you want to run it on all devices):
	* alerting_status.id - give this the id value from the token created in step 1
	* alerting_status.key - give this the key value from the token created in step 1
3. Import [the DS](Alerting_Status.json) or create a new PropertySource. If you create the PropertySource from scratch, make the AppliesTo like this `alerting_status.id && alerting_status.key`. Use [the code](alerting_status.groovy) as the script.
4. Test it and send it.

It should create a property called auto.disablealerting on the device with the value `true` or `false`.
> **_NOTE:_** The disable alerting boolean uses reverse logic and might not be intuitive. A value of `true` means alerting _is_ disabled on the device. A value of `false` means it's _not_ disabled.


> Written with [StackEdit](https://stackedit.io/).
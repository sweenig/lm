# Accessing the LogicMonitor API with Groovy

## Why is this needed?
Sometimes you will find that you need to access the LM API from within a Groovy script. Sometimes that Groovy script is running as part of a DataSource and you want to update the properties of the instances every time collection runs. This can be a really handy way to capture non-numeric data during every collection. Either way, you can pop this function (and the imports) into your Groovy script, and you have an easy way to do calls to the API.

## Prerequisites
As with any LM API call, you will have to have credentials available to the Groovy script. Most of the time people will have applied `lmaccess.id`, `lmaccess.key`, and `lmaccess.company` at the root of the account. However, I don't like this practice because it reuses passwords for every task. This can lead to overuse and in extreme cases the credentials can be disabled. It is better to use individual credentials for every task. That's one way this script is different: it allows you to specify the name of the credentials to use.
Create a set of API credentials and store them as properties. Set the prefix for these properties to be something unique but shared for these three properties. For example:
```
vmware_patching.id
vmware_patching.key
vmware_patching.company
```
In this case, `vmware_patching` becomes our credential name. Remember this for later, it will be important.

## Adding the function into your script
The next thing to do is to add the code into your Groovy script. You'll need to add two parts. To follow standard practice, you'll put the imports at the top of your script (they actually have to be imported before the function) and you'll put the function at the bottom of your script. 
Go take a look at [LM_API.groovy](LM_API.groovy). There are imports and the function definition. As long as both of those bits are in your code, you should be good.

## Calling the function
Take a look at [this example](example.groovy). The purpose of this script is to add a property to an instance named `6` on the current device under the `VMware_ESXi_HostCPUCores` DataSource. To do this, we must first find the deviceDataSource ID and then find the instance ID, then do a PATCH to update the customProperties of the instance.
1. [line 12] Luckily, we have the `:deviceId` simply by using `hostProps.get("system.deviceID")`.
2. [line 17] This is where we set the name of the DataSource we want to look for on our device. Make sure to encode the double quote (`"`) marks or LM will complain about an invalid filter.
3. [line 22] Next, we have to do a GET on `/device/devices/:deviceId/devicedatasources`. This will tell us the ID of the DataSource as it exists on that device. We store that in a variable called `hdsId`. Notice the call to the LM_API function. The third argument is "lmaccess". This is your credential name. When the function is called, it fetches properties from the device that have that prefix. In this case it pulls `lmaccess.id`, `lmaccess.key`, and `lmaccess.company`. If your credential name were `vmware_patching` as above, you'd call the function like this: 

`LM_API(httpVerb, resourcePath, "vmware_patching", [:], queryParams)`

4. [line 34] Then, we have to do a GET on `/device/devices/:deviceId/devicedatasources/:hdsId/instances` to get the IDs of all the instances under that DataSource on that device. 
5. [lines 35-37] Once we have that list, we'll filter down to just the ID of the instance we want to modify and store it in a variable called instanceId. Line 37 has the actual filter of `6` as the displayName of the instance.
6. [lines 41-44] Now, we setup the stuff we want to PATCH into LM. We need to pass in [the proper opType](https://www.logicmonitor.com/support/rest-api-developers-guide/v1/devices/update-a-device#:~:text=Define%20custom%20properties%20for%20this%20device.%20Each%20property%20needs%20to%20have%20a%20name%20and%20a%20value.%20To%20add%20or%20update%20just%20one%20or%20a%20few%20device%20properties%20in%20the%20customProperties%20object%2C%20but%20not%20all%20of%20them%2C%20you%E2%80%99ll%20need) since we're going to be messing with properties. In this case, we use `replace`, which updates/creates the properties we specify but leaves other properties alone. Our data consists of `customProperties`, which is actually a list of dictionaries/maps. 
7. [line 46] Finally, we make the PATCH call to LM. The response of a PATCH to LM will return the entire object's definition, so you can verify everything went well just by looking at the output. In this case, we just print all the custom properties (the one we patched and any that were untouched). 

## Response checking
In our [example](example.groovy), we did some simple error checking on each response to make sure the API call was successful. The basic check looks for a response code between 200 and 299, which indicates a successful operation. In the case of the first two GET requests, it also checks that the response has a non-zero size. You can make a successful call to the API and the API could return 0 records. This might be because your filter is bad or because the DataSource doesn't exist on that device.
Either way, when there is a failure, I return a non-zero integer. Some developers always return 1 no matter what the issue is. However, this is ignoring the power of the return code. By returning a different value for each error, it becomes trivial to know which block of code failed just by looking at the output of a poll now operation.
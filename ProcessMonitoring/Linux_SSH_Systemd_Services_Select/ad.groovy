/*******************************************************************************
 *  © 2007-2022 - LogicMonitor, Inc. All rights reserved.
 ******************************************************************************/

import com.jcraft.jsch.JSch
import com.santaba.agent.util.Settings

host = hostProps.get("system.hostname")
services_regex = hostProps.get("Linux_SSH_Systemd_Services_Select.includeRegEx") ?: sshd
services_exclude_regex = hostProps.get("Linux_SSH_Systemd_Services_Select.excludeRegEx") ?: ''
user = hostProps.get("ssh.user")
pass = hostProps.get("ssh.pass")
port = hostProps.get("ssh.port")?.toInteger() ?: 22
cert = hostProps.get("ssh.cert") ?: '~/.ssh/id_rsa'
timeout = 15000 // timeout in milliseconds

def azureHost = hostProps.get("system.azure.privateIpAddress")
if (azureHost && hostProps.get("auto.network.resolves") == "false") host = azureHost

// Expected pattern of output lines with data
def line_pattern = ~/^\/?\s*(\S+)\s+(\w*loaded|not-found|masked\w*)\s+(\w*active|inactive|failed\w*)\s+(\S+)\s+(.*)$/

// Run command to show any unit that systemd loaded or attempted to load, regardless of its current state on the system.
def command = 'systemctl list-units --all --type=service --plain --state=loaded | egrep service'
def command_output = getCommandOutput(command)

command_output.eachLine { line ->
    def matcher = line_pattern.matcher(line) ?: [:]
    // Process lines that contain a match
    if (matcher.size() > 0) {
        def service = matcher[0][1]
        def svc_desc = matcher[0][5]
        // Match and exclude services from the regex properties
        if (service ==~ /${services_regex}/ && !(service ==~ /${services_exclude_regex}/)) {
            println "${service}##${service}##${svc_desc}";
        }
    }
}       

// Helper function for SSH connection and command passing
def getCommandOutput(String input_command) {
    try {
        // instantiate JSCH object.
        jsch = new JSch()

        // do we have an user and no pass ?
        if (user && !pass) {
            // Yes, so lets try connecting via cert.
            jsch.addIdentity(cert)
        }

        // create session.
        session = jsch.getSession(user, host, port)

        // given we are running non-interactively, we will automatically accept new host keys.
        session.setConfig("StrictHostKeyChecking", "no");
        String authMethod = Settings.getSetting(Settings.SSH_PREFEREDAUTHENTICATION, Settings.DEFAULT_SSH_PREFEREDAUTHENTICATION);
        session.setConfig("PreferredAuthentications", authMethod);

        // set session timeout, in milliseconds.
        session.setTimeout(timeout)

        // is host configured with a user & password?
        if (pass) {
            // set password.
            session.setPassword(pass);
        }

        // connect
        session.connect()

        // execute command.
        channel = session.openChannel("exec")
        channel.setCommand(input_command)

        // collect command output.
        def commandOutput = channel.getInputStream()
        channel.connect()

        def output = commandOutput.text;

        // disconnect
        channel.disconnect()

        return output
    }
    // ensure we disconnect the session.
    finally {
        session.disconnect()
    }
}

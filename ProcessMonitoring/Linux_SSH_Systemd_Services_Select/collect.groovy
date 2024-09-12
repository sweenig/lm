import com.jcraft.jsch.JSch
import com.santaba.agent.util.Settings

host = hostProps.get("system.hostname")
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
def command = 'systemctl list-units --all --type=service --plain'
def command_output = getCommandOutput(command)

// Establish load, active, and sub values for unloaded services not listed
def load   = 4
def active = 4
def sub    = 4

command_output.eachLine { line ->
    def matcher = line_pattern.matcher(line) ?: [:]
    // Process lines that contain a match
    if (matcher.size() > 0) {
        def wildvalue = matcher[0][1]
        load          = matcher[0][2]
        active        = matcher[0][3]
        sub           = matcher[0][4]

        // Modify load responses to integer values
        if (load.contains("loaded")) {
            load = 0
        }
        else if (load.contains("masked")) {
            load = 1
        }
        else if (load.contains("not-found")) {
            load = 2
        }

        // Modify active responses to integer values
        // Check for inactive first as inactive contains the word active
        if (active.contains("inactive")) {
            active = 1
        }
        else if (active.contains("active")) {
            active = 0
        }
        else if (active.contains("failed")) {
            active = 2
        }

        // Modify sub responses to integer values
        if (sub.contains("running")) {
            sub = 0
        }
        else if (sub.contains("exited")) {
            sub = 1
        }
        else if (sub.contains("failed")) {
            sub = 2
        }
        else if (sub.contains("dead")) {
            sub = 3
        }

        println "${wildvalue}.load=${load}"
        println "${wildvalue}.active=${active}"
        println "${wildvalue}.sub=${sub}"
    }
}
return 0


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

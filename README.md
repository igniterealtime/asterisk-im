# Asterisk-IM

The Asterisk-IM project integrates the Asterisk PBX and Openfire XMPP (Jabber) server to create a unified communication platform for telephony and instant messaging.

Asterisk-IM is easily deployed as a plugin for Openfire and is fully supported in the Spark IM client.

Read more about Asterisk-IM's architecture or find out more about client compatability.


## Requirements

| | |
| --- | --- |
| Openfire | 5.0.0 or later |
| Java | 17 or later (as required by Openfire 5) |
| Asterisk | 16 or later, with the Asterisk Manager Interface (AMI) enabled |
| Maven | 3.6 or later (to build) |

The plugin talks to Asterisk over AMI only. It has been verified against Asterisk 16, 18, 20
and 22; the underlying Asterisk-Java 3.42.2 library supports Asterisk 10 through 23.


## How to build

All dependencies resolve from Maven Central and the Ignite Realtime repository, so no manual
`install-file` steps are needed:

```
git clone https://github.com/igniterealtime/asterisk-im.git
cd asterisk-im/
mvn clean install
```

Alternatively, `./build.sh` runs the same build after checking the prerequisites, and offers to
install anything missing — a JDK 17+ through the platform package manager, and Maven either
system-wide or unpacked into `~/.local/share/maven`, which needs no root. `./build.sh deps`
performs that check on its own, and `ASSUME_YES=1` / `NO_INSTALL=1` make it non-interactive.

The deployable plugin is `./server/target/asterisk-openfire-plugin-assembly.jar`. Rename it to
`asterisk.jar` and drop it into Openfire's `plugins` directory (or upload it through the admin
console at *Plugins* &rarr; *Upload Plugin*). `./build.sh` writes that copy for you as
`./server/target/asterisk.jar`.

The build also produces `./client/target/asterisk-im-client-<version>.jar`, the Smack extension
that lets an XMPP client consume the plugin's phone events.


## Asterisk configuration

The plugin needs an AMI account. In `manager.conf`:

```
[general]
enabled = yes
port = 5038
bindaddr = 0.0.0.0

[openfire]
secret = <a secret>
read = system,call,log,verbose,agent,user,config,dtmf,reporting,cdr,dialplan,originate,command
write = system,call,log,verbose,agent,user,config,dtmf,reporting,cdr,dialplan,originate,command
```

The `command` permission is required: the admin console discovers SIP and IAX2 devices by
issuing `sip show peers` / `iax2 show peers` over AMI. PJSIP endpoints are discovered with the
`PJSIPShowEndpoints` action instead, which needs no special permission beyond `system`.

### Channel technologies

Devices are mapped to users as `<technology>/<device>`, matching the channel names Asterisk
reports — for example `PJSIP/2001`, `SIP/1001` or `IAX2/3001`.

`chan_sip` was deprecated in Asterisk 17 and removed in Asterisk 21, so on newer servers PJSIP
is normally the only SIP technology available. The plugin probes each technology before
querying it and silently skips the ones a given server does not provide, so a single build
works across the whole supported range.

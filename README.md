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


## Upgrading from 2.0.0

### Openfire 5 and Java 17 are required

Openfire refuses to load a plugin whose `minServerVersion` is newer than the server, so upgrade
Openfire to 5.0.0 or later, on Java 17 or later, before installing this release. A 4.x server
keeps running the 2.0.0 plugin; it will not load this one.

### PJSIP phone mappings have to be re-selected

2.0.0 offered PJSIP devices by their AOR, so a mapping was stored as the bare name `2001`.
Asterisk names PJSIP channels `PJSIP/<endpoint>-<uniqueid>`, so a bare name never matched a live
channel: those mappings could not be dialled and produced no call events. This release reports
the same device as `PJSIP/2001`, which does match.

Existing PJSIP mappings are not migrated, because the endpoint a given AOR belongs to is not
known from the stored value alone. Re-select the device for each affected user under
*Asterisk-IM* &rarr; *Phone Mappings*. The affected rows are the ones with no technology prefix:

```sql
SELECT * FROM phoneDevice WHERE device NOT LIKE '%/%';
```

SIP and IAX2 mappings were already stored as `SIP/1001` and `IAX2/3001` and are unaffected.

### Mappings that name a chan_sip device

`chan_sip` was removed in Asterisk 21. If the Asterisk upgrade crosses that boundary, every
`SIP/<device>` mapping has to be re-created as `PJSIP/<endpoint>` — the plugin can only offer
the devices the server still reports.

### The database schema is unchanged

The schema stays at version 2, so there is no upgrade script to run and no table to alter. The
install and upgrade scripts now name Openfire's `ofVersion` table rather than `jiveVersion`,
which reads as a change but is not one: Openfire's `SchemaManager` has rewritten `jiveVersion`
to `ofVersion` in plugin scripts since Openfire 3.7, so an existing installation already records
its version in `ofVersion`. Confirm with:

```sql
SELECT name, version FROM ofVersion WHERE name = 'asterisk-im';
```

That should report version 2. If it reports nothing while the `phoneServer`, `phoneDevice` and
`phoneUser` tables exist, an earlier install left the schema half-recorded and Openfire will try
to create those tables again on the next start; record the version by hand instead:

```sql
INSERT INTO ofVersion (name, version) VALUES ('asterisk-im', 2);
```

### Plugin logging follows Openfire's configuration again

The plugin no longer bundles its own SLF4J and Log4j jars, which had detached its output from
the server's logging setup. Its log records now land in Openfire's own logs and honour the
levels set under *Server* &rarr; *Logs*; anything that used to be configured against the
bundled jars no longer applies.

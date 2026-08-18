# Asterisk-IM

The Asterisk-IM project integrates the Asterisk PBX and Openfire XMPP (Jabber) server to create a unified communication platform for telephony and instant messaging.

Asterisk-IM is easily deployed as a plugin for Openfire and is fully supported in the Spark IM client. 

Read more about Asterisk-IM's architecture or find out more about client compatability.


## How to build on a clean CentOS7 machine

```
sudo yum -y install java-1.8.0-openjdk-devel java-1.8.0-openjdk-headless java-1.8.0-openjdk git maven unzip
wget https://www.igniterealtime.org/downloadServlet?filename=openfire/openfire-4.0.4-1.i386.rpm -O openfire-4.0.4-1.i386.rpm
rpm2cpio openfire-4.0.4-1.i386.rpm |  cpio -iv --to-stdout ./opt/openfire/lib/openfire.jar > openfire.jar
mvn install:install-file -DgroupId=org.igniterealtime.openfire -DartifactId=openfire -Dversion=4.0.4 -Dpackaging=jar -DgeneratePom=true -Dfile=openfire.jar
# you'll have to build jtapi from official Oracle sources in Eclipse, and then copy to your home directory here
mvn install:install-file -DgroupId=javax.telephony -DartifactId=jtapi -Dversion=1.3.1 -Dpackaging=jar -DgeneratePom=true -Dfile=jtapi-1.3.1.jar
wget http://www.java2s.com/Open-Source/Java_Free_CodeDownload/m/maven-openfire-plugin-master.zip
unzip maven-openfire-plugin-master.zip
cd maven-openfire-plugin-master
mvn clean install
mvn install:install-file -Dfile=target/maven-openfire-plugin-1.0.2-SNAPSHOT.jar -DpomFile=pom.xml
cd ..
git clone https://github.com/igniterealtime/asterisk-im.git
cd asterisk-im/
mvn clean install

The plugin is now in ./server/target/asterisk-im.jar
```


## Upgrading an existing installation

### The plugin's schema version table

The plugin records the version of its own database schema in Openfire's version table. Openfire
renamed that table from `jiveVersion` to `ofVersion` in version 3.7, and this plugin's install
and upgrade scripts now name `ofVersion` directly.

**No manual action is required.** Openfire's `SchemaManager` rewrites `jiveVersion` to
`ofVersion` in every plugin script it executes, and has done so since 3.7, so installations made
with earlier releases of this plugin already carry their `asterisk-im` row in `ofVersion`. This
change makes the scripts say what actually reaches the database; it does not change what the
database ends up holding.

To confirm, on any Openfire 3.7 or later:

```sql
SELECT name, version FROM ofVersion WHERE name = 'asterisk-im';
```

That should report version 2. If it reports nothing at all while the `phoneServer`,
`phoneDevice` and `phoneUser` tables do exist, an earlier install left the schema half-recorded,
and Openfire will try to create those tables again on the next start. Record the version by
hand instead:

```sql
INSERT INTO ofVersion (name, version) VALUES ('asterisk-im', 2);
```

Only Openfire releases older than 3.7 still have a `jiveVersion` table. This plugin does not
support those servers, and its scripts should not be run against one unmodified.

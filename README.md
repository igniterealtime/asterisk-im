# Asterisk-IM

The Asterisk-IM project integrates the Asterisk PBX and Openfire XMPP (Jabber) server to create a unified communication platform for telephony and instant messaging.

Asterisk-IM is easily deployed as a plugin for Openfire and is fully supported in the Spark IM client. 

Read more about Asterisk-IM's architecture or find out more about client compatability.


## How to build on a clean CentOS7 machine

```
sudo yum -y install java-1.8.0-openjdk-devel java-1.8.0-openjdk-headless java-1.8.0-openjdk git maven unzip
wget https://www.igniterealtime.org/downloadServlet?filename=openfire/openfire-4.0.4-1.i386.rpm -O openfire-4.0.4-1.i386.rpm
rpm2cpio openfire-4.0.4-1.i386.rpm |  cpio -iv --to-stdout ./opt/openfire/lib/openfire.jar > openfire.jar
# you'll have to build jtapi from official Oracle sources in Eclipse, and then copy to your home directory here
mvn install:install-file -DgroupId=javax.telephony -DartifactId=jtapi -Dversion=1.3.1 -Dpackaging=jar -DgeneratePom=true -Dfile=jtapi-1.3.1.jar
mvn install:install-file -DgroupId=org.igniterealtime.openfire -DartifactId=openfire -Dversion=4.0.4 -Dpackaging=jar -DgeneratePom=true -Dfile=openfire.jar
wget http://www.java2s.com/Open-Source/Java_Free_CodeDownload/m/maven-openfire-plugin-master.zip
unzip maven-openfire-plugin-master.zip
cd maven-openfire-plugin-master
mvn clean install
mvn install:install-file -Dfile=target/openfire-maven-plugin-1.0.2-SNAPSHOT.jar -DpomFile=pom.xml
cd ..
git clone https://github.com/combird/asterisk-im.git
cd asterisk-im/
mvn clean install

The plugin is now in ./server/target/asterisk-im.jar
```

# STATUS

The project is discontinued.


[![Build Status](https://travis-ci.org/slub/qucosa-webapi.png?branch=master)](https://travis-ci.org/slub/qucosa-webapi)

# Qucosa Webapi

A Java web application that provides a RESTful interface to the Qucosa repository
backend, a [Fedora Commons](http://www.fedora-commons.org/) service. It also provides
a less RESTful interface for supporting first version Qucosa clients.

## Description


## Building

The qucosa-webapi program is a Maven project and as such can be build with the Maven package command:

```
$ mvn package
```

This will generate a deployable WAR file ```target/qucosa-webapi-<VERSION>.jar``` for Java Servlet
Containers like Tomcat or Jetty.

## Usage

After building and deploying you should be able to access the APIs resources via their URLs.

## Licence

The program is licenced under [GPLv3](http://www.gnu.org/licenses/gpl.html). See the COPYING file for details.


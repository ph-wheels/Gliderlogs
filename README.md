# Gliderlogs
An Android based Glider flight logging tool intended to be used on 10" android tablet.
This android app requires an web server back-end which make some required tables
available to this app with an REST api which also should handle the DB security,
which inour case where implemented within our main Drupal CRM.

Used tabels are: flights, members, gliders, reservation, rooster the required fields 
names and their type by table can be found in the GliderLogTables.java file.

Typically these tables reside on an web server which also host an DB engine like
MySQL or MariaDB. Additional to the DB tables a couple of pages, most likely in PHP,
will make up the interface to connect from the internet to the database tables as
to provide access and security, more info can be found on this topic, link:
http://wiki.servicenow.com/index.php?title=REST_API#gsc.tab=0
One can also use a really nice framework called Slim 3 which is extensively documented
and has lot's of implementation examples available like this one, link:
https://github.com/pabloroca/slim3-simple-rest-skeleton





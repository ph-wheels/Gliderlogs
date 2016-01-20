# Gliderlogs
An Android based Glider startlogging tool intended to be used on 10" tablet.
This android app requires an web server back-end which make some required tables
available to this app with REST api which also should handle the DB security.
  needed tabels are: flights, members, gliders, reservation, rooster
the required fields by table can be found in the GliderLogTables.java file
Typically these tables reside on an web server which also host an DB engine like
MySQL or MariaDB.




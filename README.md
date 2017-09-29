# fort_w

### Requirements
* fort_j jar should be installed in a local maven repo
* Currently httpclient:4.5 is used as I was unsure of what was previously used in it's place.

### Info
* Version is currently set to 0.0.1
* Group is currently wellsb1 

### Helpful Commands
###### (in no particular order)
* 'gradle eclipse' to convert this project into an eclipse project
* 'gradle install' to install this project's jar into a local maven repository for use by other projects
* 'gradle build' to build and test the project.  *NOTE* this task will fail if you are not running the Toxiproxy application prior to starting the build.  
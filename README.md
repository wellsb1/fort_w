# fort_w

### Requirements
* fort_j jar 
* Currently httpclient:4.5 is used as I was unsure of what was previously used in it's place.

### Info
* Version is currently set to 0.0.1
* Group is currently com.github.wellsb1 

### Helpful Commands
###### (in no particular order)
* ``gradle eclipse`` convert this project into an eclipse project
* ``gradle install`` install this project's jar into a local maven repository for use by other projects
* ``gradle build`` build and test the project.  *NOTE* this task will fail if you are not running the Toxiproxy application prior to starting the build.
* ``gradle uploadArchives`` start the process of pushing a build into the Maven Repositiory.  Takes a few minutes to complete.  For a more detailed explanation as to how this works and it's setup, view the 'fort_j' project's README  
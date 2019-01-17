# fort_w

### Requirements
* fort_j jar should be installed in a local maven repo
* Currently httpclient:4.5 is used as I was unsure of what was previously used in it's place.

### Info
* Version is currently set to ???
* Group is currently wellsb1 

### Helpful Commands
###### (in no particular order)
* 'gradle eclipse' to convert this project into an eclipse project
* 'gradle install' to install this project's jar into a local maven repository for use by other projects
* 'gradle build' to build and test the project.  *NOTE* this task will fail if you are not running the Toxiproxy application prior to starting the build.  



### Change Log

5/09/18 - 0.1.6
 
 * Added default 30s timeout HttpClients build via Web.getHttpClient()

04/26/18 - 0.1.5

 * Added UrlBuilder

12/20/17 - 0.1.1 
 * Added io.forty11.web.js packates and jackson dependency.  
 * Fixed bug where 503 and other exception content was added as resumable download content.
 * Changed retry logic to stop retrying on 404

.

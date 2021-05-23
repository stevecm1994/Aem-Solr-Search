# AEM Solr Project for Indexing/Updating/Deleting Data from Solr 

This is a project which implements end to end flow for indexing/deleting/updating/retrieving data from solr server integrating with AEM. This project majorly contains backend OSGI modules to cater the above functionalities.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates, runmode specific configs as well as Hobbes-tests
* ui.content: contains sample content using the components from the ui.apps

## Servlet Proxy Layer

* Fetching Data from English Solr collection : http://localhost:4502/apps/solrsearchApi/GET.servlet?searchType=search&collectionName=solrsearch_en_collection&q=*
* Fetching Data from French Solr collection : 
http://localhost:4502/apps/solrsearchApi/GET.servlet?searchType=search&collectionName=solrsearch_fr_collection&q=*

## Solr Config files

solrconfig folder contain necessary solr configuration files for creation of both French and English collections .
How to Use  ? : After setting up solr server , copy the configuration files (managed-schema,solrconfig) to the conf folder of the respective core and reload the core for the changes to reflect.
*managed-schema : Defines data variables we are indexing to Solr using OSGI service
*solrconfig : Defines the default as well as custom handlers in Solr like Search Handler , Spell Check Handler and so on.


## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    
Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish
    
Or alternatively

    mvn clean install -PautoInstallPackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

## Testing

There are three levels of testing contained in the project:

* unit test in core: this show-cases classic unit testing of the code contained in the bundle. To test, execute:

    mvn clean test

* server-side integration tests: this allows to run unit-like tests in the AEM-environment, ie on the AEM server. To test, execute:

    mvn clean verify -PintegrationTests

* client-side Hobbes.js tests: JavaScript-based browser-side tests that verify browser-side behavior. To test:

    in the browser, open the page in 'Developer mode', open the left panel and switch to the 'Tests' tab and find the generated 'MyName Tests' and run them.


## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

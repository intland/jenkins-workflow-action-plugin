# jenkins-workflow-action-plugin

## Prerequisites
CodeBeamer version: v20.11+
Copy the cb.jar from your codebeamer installation under the /libs directory

## Build
./gradlew jar

## Docker
You can add the jenkins-workflow-action-plugin.jar to the /home/appuser/codebeamer/repository/config/libs/ directory with a new volumne configuration

```yml
volumes:
  - <path>/jenkins-workflow-action-plugin.jar:/home/appuser/codebeamer/repository/config/libs/jenkins-workflow-action-plugin.jar
```
## Native instanlation
Copy the jenkins-workflow-action-plugin.jar into the <codebeamer directory>/tomcat/webapps/cb/WEB-INF/lib/ and restart the application

## How to use it

### Request a security token for your user
See: [API Token](https://www.jenkins.io/blog/2018/07/02/new-api-token-system/)

### Create a new node in the application configuration

```json
    "jenkins" : {
        "token" : "<token>",
        "url" : "<job url>/build",
        "userName" : "<userName>"
    }
```
Please note that url of your job must end with /build

### Add a new workfow action to your state transition
See: [Workflow Events](https://codebeamer.com/cb/wiki/1721267)

### How to call my jenkins job with parameter
"Compute as" must be used for job parameter type, job parameter must be a Map<String, Object>.

```
map().add("Parameter", "árvíztűrő tükörfúrógép")
```




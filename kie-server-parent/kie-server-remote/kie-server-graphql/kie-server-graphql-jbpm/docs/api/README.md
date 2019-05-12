# GraphQL jBPM API v1

## Schema

Schema documentation is located in different file called `jbpm-schema.md`

## Queries
Queries return only data that you specify. 
To create a query, you must specify fields within fields, until you return only scalars.
```JSON
query {
  # JSON object you want to return - this is a comment
  instances { #object
    processDefinition(definitionId: "Hello.Hello"){
      id
    }
  }
}
```

## Mutations
Mutations are used to modify processes.
To create a mutation you need to do this:
1. `Mutation name`. The type of the modification you want to perform.
2. `Input object`. The data you want to send to server. It contains input fields. These need to be passed as argument.
3. `Payload object`. The data that you want back from the server, this will be a bode of the mutation name.

Here is the final structure of mutation:
```JSON
mutation {
  instances {
    startProcesses(input: $input) { # Mutation name and Input object
      id # Payload object of the mutation
    }
  }
}
```

## Authorization

In order ot use the API. You need to have a user with roles registered in KIE Server - `rest-all, kie-server`.
Authorization with tokens is suggested.

## Variables

With variables you can make your queries dynamic and powerful.
Additionally, they reduce complexity with mutation input objects.

In order to use variables you should:
1. Define the variables in variables object. It must be a valid JSON.
    ```JSON
      {
          "input": {
              "batchSize": 10,
              "id": "Hello.Hello",
              "containerId":"Hello_1.0.0-SNAPSHOT"
          }
      }
    ```
1. Use it in a query or mutation
    ```JSON
    mutation StartedProcessInstance($input: StartProcessesInput!) {
    ```
3. Use the variable within the operation
    ```JSON
    mutation StartedProcessInstance($input: StartProcessesInput!) {
      instances {
        startProcesses(input: $input) {
          ...StartedProcessInstances
        }
      }
    }
    
    fragment StartedProcessInstances on ProcessInstance {
      id
      processId
      processName
      processVersion
      state
      containerId
      initiator
      date
      processInstanceDescription
      correlationKey
      parentId
      slaCompliance
      slaDueDate
      activeUserTasks {
        tasks {
          id
          name
          processId
        }
      }
      variables
    }
    
    ```
    In this example we substitute `StartProcessesInput` fields - `id`, `containerId` and `batchSize`


## How to use the GraphQL API of jBPM

API is available at localhost:8080/kie-server/services/rest/server/graphql. In order to use it you need
to communicate it with using REST.
For mutation use `POST` requests.
For queries use `GET` request.

For quick browsing and familiarization we suggest you use Altair GraphQL Client - https://altair.sirmuel.design/
It offers very nice user interface and allows you to browse the schema. All you need to do is provide
URL to the running service. It also comes as an browser extension and auto-completion based on the implemented schema,
 which makes it a great tool to start with.
 
Below you can find example commands for CLI tool [cURL](curl.haxx.se).
These commands work best with the quickstart running on local machine.
 
# Example CURL Commands

### Mutation command skeleton
CURL skeleton for mutations
```
curl -X POST 'http://tester:tester123;@localhost:8080/kie-server/services/rest/server/graphql' 
        -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' 
        -H 'Connection: keep-alive' -H 'Au: ' --data-binary '<HERE_GOES_THE_BODY_DATA>' 
        --compresed`
```

### Query command skeleton
CURL skeleton for queries
```
curl -X GET 'http://tester:tester123;@localhost:8080/kie-server/services/rest/server/graphql?operationName=&query=<ENCODED_QUERY_HERE>&variables=<VARIABLES_HERE>' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' --compressed`
```

### Mutation examples

For mutation queries, it is a best practice to use POST request where everything is stored in body - query, operationName and variables

* Command to start the 100 process instances with some variables:
    ```
    curl -X POST 'http://localhost:8080/kie-server/services/rest/server/graphql' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'Authorization: Basic dGVzdGVyOnRlc3RlcjEyMzs=' --data-binary '{"query":"mutation startProcesses($input:StartProcessesInput!){instances{startProcesses(input:$input){...StartedProcessInstances}}}fragment StartedProcessInstances on ProcessInstance{id processId processName processVersion state containerId initiator date processInstanceDescription correlationKey parentId slaCompliance slaDueDate activeUserTasks{tasks{id name processId}}variables}","variables":{"input":{"id":"Hello.Hello","containerId":"Hello_1.0.0-SNAPSHOT","batchSize":100,"variables":{"marinka":"ahoj","person":{"name":"Dominik","surname":"Hanak"}}}}}' --compressed
    ```

* Command to abort process instances instances with ids 1,2,3,4,5,6,7,8,9:
    ```
    curl -X GET 'http://localhost:8080/kie-server/services/rest/server/graphql' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' --data-binary '{"query":"mutation abortProcessInstances($input:AbortProcessInstancesInput!){instances{abortProcessInstances(input:$input){id}}}","variables":{"input":{"ids":[1,2,3,4,5,6,7,8,9],"containerId":"Hello_1.0.0-SNAPSHOT"}}}' --compressed
    ```

### Queries examples

For queries, it is a best practice to use GET requests where where `query`, `operationName` and `variables` are sent as query parameters.

* Command to get the process definition:
    ```
    curl -X GET 'http://localhost:8080/kie-server/services/rest/server/graphql?operationName=&query=query%20processDefinition(%24processDefinitionId%3AString!%2C%24containerId%3AString!)%7Bdefinitions%7BprocessDefinition(processDefinitionId%3A%24processDefinitionId%20containerId%3A%24containerId)%7B...DefinitionFragment%7D%7D%7Dfragment%20DefinitionFragment%20on%20ProcessDefinition%7Bid%20name%20version%20packageName%20containerId%20associatedEntities%20serviceTasks%20processVariables%20reusableSubProcesses%20nodes%7Bid%7Dtimers%7Bid%7Ddynamic%7D&variables=%7B%22processDefinitionId%22%3A%22Hello.Hello%22%2C%22containerId%22%3A%22Hello_1.0.0-SNAPSHOT%22%7D' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Authorization: Basic dGVzdGVyOnRlc3RlcjEyMzs=' -H 'Connection: keep-alive' --compressed```
    ```

* Command to get all tasks that are Ready and have empty potential owner:
    ```
    curl -X GET 'http://localhost:8080/kie-server/services/rest/server/graphql?operationName=&query=query%7Binstances%7BallTasks(batchSize%3A90%20filter%3A%7BstatesIn%3A%5B%22Ready%22%5D%2CpotentialOwnerId%3A%22%22%7D)%7Bid%20actualOwner%20createdBy%7D%7D%7D&variables=%7B%7D' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'Authorization: Basic dGVzdGVyOnRlc3RlcjEyMzs=' --compressed
    ```

* Command to get 100 process instances with containerId in all states:
    ```
    curl -X GET 'http://localhost:8080/kie-server/services/rest/server/graphql?operationName=&query=query%7Binstances%7BallProcessInstances(batchSize%3A100%20filter%3A%7BcontainerId%3A%22Hello_1.0.0-SNAPSHOT%22%20statesIn%3A%5B1%2C2%2C3%2C4%2C5%2C6%5D%7D)%7Bid%20processId%20processName%20processVersion%20state%20containerId%20initiator%20date%20processInstanceDescription%20correlationKey%20parentId%20slaCompliance%20slaDueDate%20activeUserTasks%7Btasks%7Bid%7D%7Dvariables%7D%7D%7D&variables=%7B%7D' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Authorization: Basic dGVzdGVyOnRlc3RlcjEyMzs=' -H 'Connection: keep-alive' --compressed```
    ```
* Command to get task instance with specific id:
    ```
    curl 'http://localhost:8080/kie-server/services/rest/server/graphql?operationName=&query=query%7Binstances%7BtaskInstance(taskId%3A10)%7Bid%20name%20subject%20description%20status%20priority%20skipable%20actualOwner%20createdBy%20createdOn%20activationTime%20expirationDate%20workItemId%20processInstanceId%20parentId%20processId%20containerId%20potentialOwners%20excludedOwners%20businessAdmins%20inputData%20outputData%7D%7D%7D&variables=%7B%7D' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'Authorization: Basic dGVzdGVyOnRlc3RlcjEyMzs=' --compressed
    ```
# Schema Types

<details>
  <summary><strong>Table of Contents</strong></summary>

  * [Query](#query)
  * [Mutation](#mutation)
  * [Objects](#objects)
    * [DefinitionQuery](#definitionquery)
    * [InstanceMutation](#instancemutation)
    * [InstanceQuery](#instancequery)
    * [NodeDefinition](#nodedefinition)
    * [ProcessDefinition](#processdefinition)
    * [ProcessInstance](#processinstance)
    * [TaskInputsDefinition](#taskinputsdefinition)
    * [TaskInstance](#taskinstance)
    * [TaskOutputsDefinition](#taskoutputsdefinition)
    * [TaskSummary](#tasksummary)
    * [TaskSummaryList](#tasksummarylist)
    * [TimerDefinition](#timerdefinition)
    * [UserTaskDefinition](#usertaskdefinition)
  * [Inputs](#inputs)
    * [AbortProcessInstancesInput](#abortprocessinstancesinput)
    * [ProcessInstanceFilter](#processinstancefilter)
    * [SignalProcessInstancesInput](#signalprocessinstancesinput)
    * [StartProcessesInput](#startprocessesinput)
    * [TaskInstanceFilter](#taskinstancefilter)
  * [Scalars](#scalars)
    * [Boolean](#boolean)
    * [Date](#date)
    * [Int](#int)
    * [Long](#long)
    * [Object](#object)
    * [String](#string)

</details>

## Query
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>definitions</strong></td>
<td valign="top"><a href="#definitionquery">DefinitionQuery</a></td>
<td>

Root for process definition related queries

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>instances</strong></td>
<td valign="top"><a href="#instancequery">InstanceQuery</a></td>
<td>

Root for process instance related queries

</td>
</tr>
</tbody>
</table>

## Mutation
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>instances</strong></td>
<td valign="top"><a href="#instancemutation">InstanceMutation</a></td>
<td>

Root for process instance related mutations

</td>
</tr>
</tbody>
</table>

## Objects

### DefinitionQuery

Collection of every operations for ProcessDefinitions

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>processDefinition</strong></td>
<td valign="top"><a href="#processdefinition">ProcessDefinition</a>!</td>
<td>

Gets a single ProcessDefinition with provided processDefinitionId and containerId

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">containerId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Identifier of container the definition belongs to

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">definitionId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Identifier of process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userTaskDefinitions</strong></td>
<td valign="top">[<a href="#usertaskdefinition">UserTaskDefinition</a>!]!</td>
<td>

Gets list of UserTaskDefinition's for process definition with provided processDefinitionId and containerId

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">containerId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Identifier of container the definitions belongs to

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">definitionId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Identifier of process definition task definitions belong to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>allProcessDefinitions</strong></td>
<td valign="top">[<a href="#processdefinition">ProcessDefinition</a>!]!</td>
<td>

Gets all available process definitions from runtime from all containers

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">batchSize</td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Size of the returned list

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processDefinitions</strong></td>
<td valign="top">[<a href="#processdefinition">ProcessDefinition</a>]</td>
<td>

Gets list of ProcessDefinition objects with concrete processDefinitionId OR containerId, not both

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">containerId</td>
<td valign="top"><a href="#string">String</a></td>
<td>

Identifier of container the definition belongs to

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">definitionId</td>
<td valign="top"><a href="#string">String</a></td>
<td>

Identifier of process definition task definitions belong to

</td>
</tr>
</tbody>
</table>

### InstanceMutation

Collection of mutation operations for instances

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>startProcesses</strong></td>
<td valign="top">[<a href="#processinstance">ProcessInstance</a>!]!</td>
<td>

Creates/Starts process instances defined by the input. Can be also used to start one process instance, just set batchSize to 1

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#startprocessesinput">StartProcessesInput</a>!</td>
<td>

Input object for startProcesses

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>abortProcessInstances</strong></td>
<td valign="top">[<a href="#processinstance">ProcessInstance</a>]!</td>
<td>

Aborts process instances with specified id or if specified, also with containerId

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#abortprocessinstancesinput">AbortProcessInstancesInput</a>!</td>
<td>

Input object for abortProcessInstances.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>signalProcessInstances</strong></td>
<td valign="top">[<a href="#processinstance">ProcessInstance</a>]</td>
<td>

Signals process instances with specified ids or if specified also with containerId

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#signalprocessinstancesinput">SignalProcessInstancesInput</a>!</td>
<td>

Input object for signalProcessInstances

</td>
</tr>
</tbody>
</table>

### InstanceQuery

Collection of query operations for instances

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>processInstance</strong></td>
<td valign="top"><a href="#processinstance">ProcessInstance</a>!</td>
<td>

Gets a ProcessInstance with id and containerId

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">id</td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the process instance

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">containerId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Id of the container that this instance belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>allProcessInstances</strong></td>
<td valign="top">[<a href="#processinstance">ProcessInstance</a>!]!</td>
<td>

Gets all available process instances from runtime based on the filter combination

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">filter</td>
<td valign="top"><a href="#processinstancefilter">ProcessInstanceFilter</a>!</td>
<td>

Filter for returned instances. Can be used to retrieve instances with specific fields.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">batchSize</td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Size of the returned list

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variables</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Gets list of ProcessVariable for process instance with id and containerId.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">id</td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the process instance that variables belong to

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">containerId</td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Id of the container that process instance belong to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>allTasks</strong></td>
<td valign="top">[<a href="#tasksummary">TaskSummary</a>!]!</td>
<td>

Gets all available tasks from runtime based on the filter combination

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">filter</td>
<td valign="top"><a href="#taskinstancefilter">TaskInstanceFilter</a>!</td>
<td>

Filter for returned tasks. Can be used to retrieve tasks with specific fields

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">batchSize</td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Size of the returned list

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>taskInstance</strong></td>
<td valign="top"><a href="#taskinstance">TaskInstance</a></td>
<td>

Gets task instance with specific ID.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">taskId</td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the task instance

</td>
</tr>
</tbody>
</table>

### NodeDefinition

KIE API NodeDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Id of the node

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the node

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>uniqueId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Unique identifier located in bpmn2 definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Type of the node

</td>
</tr>
</tbody>
</table>

### ProcessDefinition

KIE API ProcessDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Id of the process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>version</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Versions of the process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>packageName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the package the process definition is located in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the container the process definition belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>associatedEntities</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Entities associated with this process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>serviceTasks</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Service tasks this process definition contains

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processVariables</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Process variables defined in this process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>reusableSubProcesses</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

Reusable sub processes defined in this process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>nodes</strong></td>
<td valign="top">[<a href="#nodedefinition">NodeDefinition</a>]</td>
<td>

Nodes defined in this process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>timers</strong></td>
<td valign="top">[<a href="#timerdefinition">TimerDefinition</a>]</td>
<td>

Timers defined in this process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>dynamic</strong></td>
<td valign="top"><a href="#boolean">Boolean</a></td>
<td>

Whether this definition is dynamic or not

</td>
</tr>
</tbody>
</table>

### ProcessInstance

KIE API ProcessInstance.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the process definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the process

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processVersion</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Version of the process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>state</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

State this process instance is in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Id of the container that this process instance belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>initiator</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Initiator of this process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>date</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date of this process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processInstanceDescription</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Description of this process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>correlationKey</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Correlation key of this process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>parentId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the parent process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>slaCompliance</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

SLA compliance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>slaDueDate</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Due date for SLA

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>activeUserTasks</strong></td>
<td valign="top"><a href="#tasksummarylist">TaskSummaryList</a></td>
<td>

User tasks that are currently active

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variables</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Variables of this process instance

</td>
</tr>
</tbody>
</table>

### TaskInputsDefinition

KIE API TaskInputsDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>taskInputs</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Task inputs of this task inputs definition

</td>
</tr>
</tbody>
</table>

### TaskInstance

KIE API TaskInstance

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>subject</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Subject of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>description</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Description of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>status</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Status of the task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>priority</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Priority of the task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>skipable</strong></td>
<td valign="top"><a href="#boolean">Boolean</a></td>
<td>

Whether the task is skipable or not

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>actualOwner</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Actual owner of the task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createdBy</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

User id of creator of task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createdOn</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date when this instance was created

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>activationTime</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date when this instance was activated

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>expirationDate</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date when this instance expires

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>workItemId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the work item of this task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processInstanceId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the process instance this task belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>parentId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the parent process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of process definition this task belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the container that this task belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>potentialOwners</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

List of users that can own this task - potential owners

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>excludedOwners</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

List of users that cant own this task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>businessAdmins</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

List of users that are business admins for this task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>inputData</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Input data of this task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>outputData</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Output data of this task

</td>
</tr>
</tbody>
</table>

### TaskOutputsDefinition

KIE API TaskOutputsDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>taskOutputs</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Task outputs of this task outputs definition

</td>
</tr>
</tbody>
</table>

### TaskSummary

KIE API TaskSummary.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#long">Long</a>!</td>
<td>

Id of the task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>subject</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Subject of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>description</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Description of the task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>status</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Status that this task is currently in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>priority</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Priority of this task

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>skipable</strong></td>
<td valign="top"><a href="#boolean">Boolean</a></td>
<td>

Whether this task can be skipped or not

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>actualOwner</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Actual owner of this task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createdBy</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Creator of this task instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createdOn</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date of tasks creation

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>activationTime</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date of when this task was activated

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>expirationTime</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Date of when this task expires

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processInstanceId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the process instance this task belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the process definition this task belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the container this tasks belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>parentId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the parent process instance

</td>
</tr>
</tbody>
</table>

### TaskSummaryList

KIE API TaskSummaryList.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>tasks</strong></td>
<td valign="top">[<a href="#tasksummary">TaskSummary</a>]</td>
<td>

List of task instance summaries

</td>
</tr>
</tbody>
</table>

### TimerDefinition

KIE API TimerDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the timer

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>nodeName</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Node name of the timer

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>uniqueId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Unique identifier located in bpmn2 definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>nodeId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the node in process definition

</td>
</tr>
</tbody>
</table>

### UserTaskDefinition

KIE API UserTaskDefinition.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Id of this user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>priority</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Priority this user task definition has

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createdBy</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Creator of this user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>skippable</strong></td>
<td valign="top"><a href="#boolean">Boolean</a></td>
<td>

Whether this user task definition is skippable or not

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>formName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the form for this user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>associatedEntities</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

Entities associated with this user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>taskInputMappings</strong></td>
<td valign="top"><a href="#taskinputsdefinition">TaskInputsDefinition</a></td>
<td>

Task input mappings for this user task definition

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>taskOutputMappings</strong></td>
<td valign="top"><a href="#taskoutputsdefinition">TaskOutputsDefinition</a></td>
<td>

Task output mappings for this user task definition

</td>
</tr>
</tbody>
</table>

## Inputs

### AbortProcessInstancesInput

Input for abortProcessInstances mutation.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>ids</strong></td>
<td valign="top">[<a href="#long">Long</a>!]!</td>
<td>

List of IDs of the instances that you want to abort

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of a container that instances should belong to, optional argument,
in case there is Id that does not belong to it returned list is going to
contain null on its position

</td>
</tr>
</tbody>
</table>

### ProcessInstanceFilter

Input object for ProcessInstances.
Allows users to select subset of its fields to
query process instances.
Process instances are matched based on the selected
fields. Not every combination is supported.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>statesIn</strong></td>
<td valign="top">[<a href="#int">Int</a>]</td>
<td>

List of states that process instances should be in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>initiator</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Initiator of the process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of container of the process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of process definition of the process instance

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>correlationKey</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Correlation key that process instance has

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the process instance's process

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variableName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the variable that process instance has

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variableValue</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Value of the variable that process instance has

</td>
</tr>
</tbody>
</table>

### SignalProcessInstancesInput

Input for signalProcessInstances mutation

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>ids</strong></td>
<td valign="top">[<a href="#long">Long</a>!]!</td>
<td>

List of IDs of the instances that you want to signal

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>signalName</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Name of the signal

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>event</strong></td>
<td valign="top"><a href="#object">Object</a>!</td>
<td>

An event object to be passed in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

String identified of the container of the process instances
optional, if not set it won't be used

</td>
</tr>
</tbody>
</table>

### StartProcessesInput

Input for startProcesses mutation.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>batchSize</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>

Size of the started batch.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

Identifier of the process

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>containerId</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>

String identifier of the container of the process

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>correlationKey</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

A correlationKey that should be assigned to a started process instances
optional, must be unique

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variables</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Variables of the process

</td>
</tr>
</tbody>
</table>

### TaskInstanceFilter

Input object for Tasks
Allows users to select subset of its fields to
query tasks. If more fields are selected an AND
operation will be enforced.
Tasks are matched based on the selected
fields. Not every combination is supported.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>statesIn</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

States that tasks should be in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>businessAdminId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of business admin that tasks should have

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>potentialOwnerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of potential owner

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>groupIds</strong></td>
<td valign="top">[<a href="#string">String</a>]</td>
<td>

List of groupIds that tasks belongs to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>fromExpirationalDate</strong></td>
<td valign="top"><a href="#date">Date</a></td>
<td>

Expirational date that task should have

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>processInstanceId</strong></td>
<td valign="top"><a href="#long">Long</a></td>
<td>

Id of the process instance that tasks should belong to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variableName</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Name of the variable that task should have

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>variableValue</strong></td>
<td valign="top"><a href="#object">Object</a></td>
<td>

Value of the variable that task should have

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>ownerId</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>

Id of the tasks owner

</td>
</tr>
</tbody>
</table>

## Scalars

### Boolean

Built-in Boolean

### Date

### Int

Built-in Int

### Long

Long type

### Object

### String

Built-in String

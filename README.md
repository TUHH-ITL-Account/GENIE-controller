# Genie-Controller

Controller for the generator. Handles communication to other processes.

## Installation & Execution

1) Load and install all prerequisites
2) Run `gradle fatJar`, which will produce a .jar in `/build/libs/`
3) Execute via `java -jar controller-1.0-SNAPSHOT.jar [flags]`

## Execution flags

| flag | description | default |
|---------|-------------|---------|
|-c / --config | configPath | genie.config |
| -e / --env | Load configuration from environment variables | not present |

## Config

If no config-path is given the controller uses `genie.config` in the root directory

| setting | description | default |
|---------|-------------|---------|
|udsockets_use_temp|if set to `true` udsockets_path will be ignored and socket files will be created and used inside the OS' temp directory|`true`|
|udsockets_dir|sets the directory in which socket files will be created and used| - |
|log_dir|sets the directory in which log files will be created|`./logs`|
|max_threads|sets the number of generator threads|`10`|
|queue_size|sets the maximum task-queue size|`1000`|
|model_dir|sets the directory for (knowledge-)models|`./models`|
|preloaded_models|list of models to be loaded upon process start. E.g. `TechnischeLogistikSS22`|-|
|connection_type|sets the used socket connection type. Can be `tcp` for TCP/IP or `uds` for Unix Domain Sockets |uds|
|regulator_port|sets the port on which the controller will host the server for the regulator|4242|
|frontend_port|sets the port on which the controller will host the server for the frontend|4243|

## Environment Variables

If the -e flag is passed on execution, the config will be loaded from the system's environment
variables instead of a config file. The env vars are the same as the config settings, but fully
capitalized and with a "GENIE_" prefix.

Example: `log_dir` as a config setting is equivalent to the `GENIE_LOG_DIR` environment variable


## Messaging Protocol

message terminator (to be sent after every individual message) `$$$`

intra-message delimiter (to separate different parts of the same message) `##`

### Directives

Start with a `##` and contain two pieces of information, the number of the directive and any additional
information for it.

| Directive | description | Additional Var |
|---------|-------------|---------|
|0 (Reload)|Reloads the knowledge model with the given name, if one with the same name is already cached. Does nothing otherwise.|Name of the knowledge model|
|1 (Cache Model)|Caches the knowledge model with the given name. If already in cache it is instead reloaded.|Name of the knowledge model.|
|2 (Shutdown)|Performs a clean shutdown by stopping all threads|-|

Example: `##0##Technische Logistik SS22` to reload the KM 'Technische Logistik SS22'

### Exercise Requests

Start with 'normal' characters instead of `##`. 

`requestId##userId##courseName##parameters##exerciseType##solutionType##taskType`

| exerciseType | description |
|---------|-------------|
|0 (Full)|Produces all exercise types.|
|1 (Full HTML)|Produces a complete HTML page for the exercise.|
|2 (Part HTML)|Produces only the exercise part of the HTML page as exercise.|
|3 (TEX)|PLACEHOLDER FOR PDF|

| solutionType | description |
|---------|-------------|
|0 (Full)|Produces all solution types.|
|1 (Full HTML)|Produces a complete HTML page for the solution.|
|2 (Part HTML)|Produces only the exercise part of the HTML page as solution.|
|3 (TEX)|PLACEHOLDER FOR PDF|
|4 (JSON)|Only produces a JSON object holding the solution.|

| taskType | description |
|---------|-------------|
|0 (Course)|Generate an exercise from any FDL in any topic of the course.|
|1 (Sub tree)|Generate an exercise from any FDL from any topic of the given topic or any of its sub topics.|
|2 (Topic)|Generate an exercise from any FDL from the given topic.|
|3 (FDL)|Generate an exercise using the given FDL.|

Example: `76g3f7##asd0245##Technische Logistik SS22##{difficulty:5}##1##1##2`
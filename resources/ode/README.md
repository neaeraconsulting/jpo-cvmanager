# ODE Log Offload Processing

Documentation on ODE usage and log processing can be found in the [ODE README](https://github.com/usdot-jpo-ode/jpo-ode#1-usage-example).

OBU logs can be automatically processed by the ODE simply by placing them into the `./uploads/bsmlog` directory (all files should be .gz). The ODE will automatically process any new files that are placed into this directory. 

Messages will be processed and stored in the database and files which fail to parse will be placed into `./uploads/failed` for later inspection.
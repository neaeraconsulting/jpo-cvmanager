# ODE Log Offload Processing

OBU logs can be automatically processed by the ODE simply by placing them into the `./uploads/obulog` directory (all files should be .gz). The ODE will automatically process any new files that are placed into this directory. 

Messages will be processed and stored in the database and files which fail to parse will be placed into `./uploads/failed` for later inspection.
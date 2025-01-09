#!/bin/bash

# Path to the plugin file
PLUGIN_PATH=~/MinecraftServer/plugins/BuildingWandsPlus-1.0-SNAPSHOT.jar

# Delete the specific plugin if it exists
if [ -f "$PLUGIN_PATH" ]; then
    echo "Deleting old plugin: $PLUGIN_PATH"
    rm "$PLUGIN_PATH"
else
    echo "No old plugin found, continuing..."
fi

# Compile the plugin using Maven
mvn clean package

# Copy the built plugin to the Minecraft server plugins folder
cp target/BuildingWandsPlus-1.0-SNAPSHOT.jar ~/MinecraftServer/plugins/

# Start the Minecraft server
cd ~/MinecraftServer
./start.sh


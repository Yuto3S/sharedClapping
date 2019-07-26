package com.google.location.nearby.apps.rockpaperscissors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

class Connections {
    final private HashMap<String, String> connections = new HashMap<>();

    void addConnection(String endpointId, String endpointName){
        connections.put(endpointId, endpointName);
    }

    void removeConnection(String endpointId){
        connections.remove(endpointId);
    }

    String getConnectionsName(){
        ArrayList<String> connectionsNames = new ArrayList<>();
        Set<String> connectionIds = connections.keySet();
        for(String connectionId: connectionIds){
            connectionsNames.add(connections.get(connectionId));
        }
        return connectionsNames.toString();
    }

    Set<String> getEndpointIds(){
        return connections.keySet();
    }

    int getNumberOfConnections(){
        return connections.size();
    }

    boolean isEmpty(){
        return getNumberOfConnections() == 0;
    }
}

package com.example.ghosty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a chat room with a name and port.
 * Made serializable to support transfer between server and clients.
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int port;
    
    public Room(String name, int port) {
        this.name = name;
        this.port = port;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return port == room.port || 
               Objects.equals(name, room.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, port);
    }
    
    @Override
    public String toString() {
        return name + " (Port: " + port + ")";
    }
}
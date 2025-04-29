package com.example.ghosty;

public class Room {
    private int port;
    private String name;
    private boolean isTrendRoom;

    public int getPort() {
        return port;
    }

    public Room(String name, int port, boolean isTrendRoom) {
        this.port = port;
        this.name = name;
        this.isTrendRoom = isTrendRoom;
    }

    public String getName() {
        return name;
    }

    public boolean isTrend(){
        return isTrendRoom;
    }
}

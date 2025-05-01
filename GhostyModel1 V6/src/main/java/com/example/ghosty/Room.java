package com.example.ghosty;

public class Room {
    private int port;
    private String name;

    public int getPort() {
        return port;
    }

    public Room(String name, int port) {
        this.port = port;
        this.name = name;
    }

    public String getName() {
        return name;
    }

}

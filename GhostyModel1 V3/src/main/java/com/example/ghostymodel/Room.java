package com.example.ghostymodel;

public class Room {
    private final String name;
    private final boolean isTrendRoom; // burda trend olanların değişmeyeceği şeyini yaptık

    public Room(String name, boolean isTrendRoom) {
        this.name = name;
        this.isTrendRoom = isTrendRoom;
    }

    public String getName() {
        return name;
    }

    public boolean isTrendRoom() {
        return isTrendRoom;
    }

    @Override
    public String toString() {
        return name;
    }
}

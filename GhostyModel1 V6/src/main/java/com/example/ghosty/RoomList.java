package com.example.ghosty;

import java.util.ArrayList;
import java.util.List;

public class RoomList {
    private List<Room> rooms;
    private List<MainGUI> observers;  // List of observers (clients)

    public RoomList() {
        rooms = new ArrayList<>();
        observers = new ArrayList<>();
    }

    // Add observer (MainGUI instance)
    public void addObserver(MainGUI observer) {
        observers.add(observer);
    }

    // Remove observer
    public void removeObserver(MainGUI observer) {
        observers.remove(observer);
    }

    // Notify all observers
    public void notifyObservers() {
        for (MainGUI observer : observers) {
            observer.updateRoomList(rooms);
        }
    }

    // Add a new room to the list and notify observers
    public void addRoom(Room room) {
        rooms.add(room);
        notifyObservers();  // Notify all clients about the new room
    }

    public void removeRoom(Room room) 
    {
    	rooms.remove(room);
    	notifyObservers();
    }
    // Get all rooms
    public List<Room> getRooms() {
        return rooms;
    }
}

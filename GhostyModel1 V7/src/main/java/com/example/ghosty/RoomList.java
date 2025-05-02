package com.example.ghosty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a list of chat rooms and notifies observers of changes.
 * Made thread-safe for multiple clients accessing it.
 */
public class RoomList implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Using CopyOnWriteArrayList for thread safety
    private final List<Room> rooms = new CopyOnWriteArrayList<>();
    private final List<RoomListObserver> observers = new CopyOnWriteArrayList<>();
    
    /**
     * Adds a new room to the list
     * @param room The room to add
     */
    public synchronized void addRoom(Room room) {
        if (!containsRoom(room)) {
            rooms.add(room);
            notifyObservers();
            System.out.println("Room added: " + room.getName() + " on port " + room.getPort());
        } else {
            System.out.println("Room already exists: " + room.getName());
        }
    }
    
    /**
     * Removes a room from the list
     * @param room The room to remove
     */
    public synchronized void removeRoom(Room room) {
        rooms.remove(room);
        notifyObservers();
        System.out.println("Room removed: " + room.getName());
    }
    
    /**
     * Remove a room by its name
     * @param roomName The name of the room to remove
     */
    public synchronized void removeRoomByName(String roomName) {
        rooms.removeIf(room -> room.getName().equalsIgnoreCase(roomName));
        notifyObservers();
        System.out.println("Room removed: " + roomName);
    }
    
    /**
     * Check if the list contains a specific room
     * @param room The room to check for
     * @return true if the room already exists
     */
    public boolean containsRoom(Room room) {
        // Check if a room with the same name or port already exists
        for (Room existingRoom : rooms) {
            if (existingRoom.getName().equals(room.getName()) ||
                existingRoom.getPort() == room.getPort()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a port is already in use by a room
     * @param port The port to check
     * @return true if the port is in use
     */
    public boolean isPortInUse(int port) {
        for (Room room : rooms) {
            if (room.getPort() == port) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get a list of all rooms
     * @return Unmodifiable list of all rooms
     */
    public List<Room> getRooms() {
        return Collections.unmodifiableList(new ArrayList<>(rooms));
    }
    
    /**
     * Prints all rooms to the console (for debugging)
     */
    public void printRooms() {
        System.out.println("Current rooms:");
        for (Room room : rooms) {
            System.out.println("- " + room.getName() + " (Port: " + room.getPort() + ")");
        }
    }
    
    /**
     * Find a room by its port number
     * @param port The port number to search for
     * @return The room with the specified port, or null if not found
     */
    public Room findRoomByPort(int port) {
        for (Room room : rooms) {
            if (room.getPort() == port) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Find a room by its name
     * @param name The name to search for
     * @return The room with the specified name, or null if not found
     */
    public Room findRoomByName(String name) {
        for (Room room : rooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Add an observer to be notified of changes
     * @param observer The observer to add
     */
    public void addObserver(RoomListObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Remove an observer from the notification list
     * @param observer The observer to remove
     */
    public void removeObserver(RoomListObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notify all observers of changes to the room list
     */
    private void notifyObservers() {
        for (RoomListObserver observer : observers) {
            observer.updateRoomList(getRooms());
        }
    }
    
    /**
     * Interface for classes that want to observe changes to the room list
     */
    public interface RoomListObserver {
        void updateRoomList(List<Room> rooms);
    }
}
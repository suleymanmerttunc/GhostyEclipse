package com.example.ghosty;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

/**
 * Utility functions for the Ghosty chat application
 */
public class UtilFunctions {
    
    /**
     * Get the top trending topics from the web
     * 
     * @return Array of trending topic strings
     * @throws IOException If there's an error fetching trends
     */
    public static String[] getTopTrends() throws IOException {
        String[] trends = new String[4];
        
        try {
            // Use JSoup to fetch trending topics (this is a placeholder - actual implementation might differ)
	        Document doc = Jsoup.connect("https://trends24.in/turkey/").get(); 	        
	        Elements trendElements = doc.select(".trend-card__list li a");
            
            for (int i = 0; i < Math.min(4, trendElements.size()); i++) {
                trends[i] = trendElements.get(i).text();
            }
            
            // Fill any remaining slots with placeholders
            for (int i = trendElements.size(); i < 4; i++) {
                trends[i] = "Trending Topic " + (i + 1);
            }
        } catch (IOException e) {
            // If web fetch fails, provide default trending topics
            trends[0] = "Technology";
            trends[1] = "Sports";
            trends[2] = "Entertainment";
            trends[3] = "Science";
            throw e;
        }
        
        return trends;
    }
    
    /**
     * Find a room by its name in a list of rooms
     * 
     * @param roomName The name of the room to find
     * @param rooms The list of rooms to search
     * @return The found room, or null if not found
     */
    public static Room findRoom(String roomName, List<Room> rooms) {
        for (Room room : rooms) {
            if (room.getName().equals(roomName)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Find a room by its port number in a list of rooms
     * 
     * @param port The port number to search for
     * @param rooms The list of rooms to search
     * @return The found room, or null if not found
     */
    public static Room findRoom(int port, List<Room> rooms) {
        for (Room room : rooms) {
            if (room.getPort() == port) {
                return room;
            }
        }
        return null;
    }
    public static Room findRoom(Room roomToFind, List<Room> rooms) {
        for (Room room : rooms) {
            if (room.equals(roomToFind)) {
                return room;
            }
        }
        return null;
    }







}
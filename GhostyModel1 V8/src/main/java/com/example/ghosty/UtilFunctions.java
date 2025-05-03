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
     * İnternetten twitter trendlerini çeken metod
     * 
     * @return Türkiyeki trendlerin listesini
     */
    public static String[] getTopTrends() throws IOException {
        String[] trends = new String[4];
        
        try {
	        Document doc = Jsoup.connect("https://trends24.in/turkey/").get(); 	        
	        Elements trendElements = doc.select(".trend-card__list li a");
            
            for (int i = 0; i < Math.min(4, trendElements.size()); i++) {
                trends[i] = trendElements.get(i).text();
            }
            

            for (int i = trendElements.size(); i < 4; i++) {
                trends[i] = "Trending Topic " + (i + 1);
            }
        } catch (IOException e) {
            // Eğer trendleri çekerken bir hata yaşandıysa placeholder değerler kullanır
            trends[0] = "Technology";
            trends[1] = "Sports";
            trends[2] = "Entertainment";
            trends[3] = "Science";
            throw e;
        }
        
        return trends;
    }
    
    /**
     * İsim olarak uyan bir odayı verilen odalar listesinden bulur
     * 
     * @param bulunmak istenen odanın ismi
     * @param odalar listesi (Room objeleri tutan bir List)
     * @return uyan room yoksa null, varsa Room objesi
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
     * port numarası olarak uyan bir odayı verilen odalar listesinden bulur
     * 
     * @param bulunmak istenen odanın portu
     * @param odalar listesi (Room objeleri tutan bir List)
     * @return uyan room yoksa null, varsa Room objesi
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
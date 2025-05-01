package com.example.ghosty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class UtilFunctions 
{

	
	 public static String[] getTopTrends() throws IOException {
	        Document doc = Jsoup.connect("https://trends24.in/turkey/").get();
	        Elements trendElements = doc.select(".trend-card__list li a");

	        String[] trends = new String[5];
	        for (int i = 0; i < 5; i++) {
	            trends[i] = trendElements.get(i).text();
	        }
	        return trends;
	    }
	 
	 public static Room findRoom(String roomName, List<Room> roomList) 
	 {
		 
		 Room roomToReturn = null;
		 for (Room room : roomList) 
		 {
			 if (room.getName().equals(roomName)) 
			 {
				 roomToReturn = room;
			 }
		}
		 return roomToReturn;
	 }
}

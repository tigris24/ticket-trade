package com.ticket.Ticketing.service;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.ticket.Ticketing.config.EventConfig;
import com.ticket.Ticketing.config.SeatConfig;
import com.ticket.Ticketing.config.UserConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

import static com.ticket.Ticketing.Ticketing.cluster;

@Service
@AllArgsConstructor
public class SeatService {

    public void createSeatDocuments(String eventId, Integer totalSeats) {
        Bucket seatBucket = cluster.bucket(SeatConfig.getStaticBucketName());
        Collection seatCollection = seatBucket.defaultCollection();

        for (int i = 1; i <= totalSeats; i++) {
            String documentId = eventId + "-" + String.format("%03d", i);
            if (!documentExists(seatCollection, documentId)) {   // don't make initial documents if already exist
                JsonObject jsonData = JsonObject.create()
                        .put("id", String.valueOf(i))
                        .put("sold", false);
                seatCollection.insert(documentId, jsonData);
            }
        }
    }

    public void deleteSeatByEventDocuments(String eventId, Integer seatNum) {
        Bucket seatBucket = cluster.bucket(SeatConfig.getStaticBucketName());
        Collection seatCollection = seatBucket.defaultCollection();

        for (int i = 1; i <= seatNum; i++) {
            String documentId = eventId + "-" + String.format("%03d", i);
            if (documentExists(seatCollection, documentId)) {
                seatCollection.remove(documentId);
            }
        }
    }

    private boolean documentExists(Collection collection, String documentId) {
        try {
            collection.get(documentId);
            return true; // Document exists
        } catch (DocumentNotFoundException e) {
            return false; // Document not exists
        }
    }

    public List<String> seatList(String eventId, String loginUser) {
        // access to buckets
        Bucket userBucket = cluster.bucket(UserConfig.getStaticBucketName());
        Collection userCollection = userBucket.defaultCollection();

        Bucket eventBucket = cluster.bucket(EventConfig.getStaticBucketName());
        Collection eventCollection = eventBucket.defaultCollection();

        Bucket seatBucket = cluster.bucket(SeatConfig.getStaticBucketName());
        Collection seatCollection = seatBucket.defaultCollection();

        // add reserved seats info to model
        List<String> seatReserved = new ArrayList<>();
        JsonArray seatUserReserved;
        Integer seatNumOfEvent;

        // whole seat list of event
        if (loginUser == null) {
            seatNumOfEvent = eventCollection.get(eventId).contentAsObject().getInt("seatNum");
            for (int i = 1; i <= seatNumOfEvent; i++) {
                JsonObject seatInfo = seatCollection.get(eventId + "-" + String.format("%03d", i)).contentAsObject();
                if (seatInfo.getBoolean("sold")) {
                    seatReserved.add(String.valueOf(seatInfo.get("id")).split("-")[1]);
                }
            }
        } else { // user seat list of event
            seatUserReserved = userCollection.get(loginUser).contentAsObject().getArray("seat");

            // check event of reserved seat whether it is same with eventId
            for (Object seatInfo : seatUserReserved) {
                String[] tempUserSeat = String.valueOf(seatInfo).split("-");
                // tempUserSeat is composed of "eventName-seatNum"
                if (tempUserSeat[0].equals(eventId)) {
                    seatReserved.add(tempUserSeat[1]);
                }
            }
        }

        return seatReserved;
    }


}

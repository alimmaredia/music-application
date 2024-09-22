package com.csc301.songmicroservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.text.Document;

import org.bson.types.ObjectId;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.json.JSONObject;
import org.json.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Variable;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.ws.RealWebSocket.Message;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// TODO Auto-generated method stub
		String songName = songToAdd.getSongName();
		String songArtistFullName = songToAdd.getSongArtistFullName();
		String songAlbum = songToAdd.getSongAlbum();
		long songAmountFavourites = songToAdd.getSongAmountFavourites();
		
        JSONObject obj = new JSONObject();
        obj.put("songName", songName);
        obj.put("songArtistFullName", songArtistFullName);
        obj.put("songAlbum", songAlbum);
        obj.put("songAmountFavourites", songAmountFavourites);
        
        org.bson.Document ab = org.bson.Document.parse(obj.toString());
        
        MongoCollection<org.bson.Document> collections = db.getCollection("songs");
        
        
        collections.insertOne(ab);
        
        String idString = ab.get("_id").toString();
        
        ab.replace("_id", idString);
        
        songToAdd.setId(new ObjectId(idString));
        
        
        DbQueryStatus resultDbQueryStatus = new DbQueryStatus("Added song into the DB", DbQueryExecResult.QUERY_OK);
        resultDbQueryStatus.setData(ab);
        
		return resultDbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		
        MongoCollection<org.bson.Document> collections = db.getCollection("songs");
		BasicDBObject query = new BasicDBObject();
        DbQueryExecResult messagExecResult;
		String messageString;
		org.bson.Document doc = null;
		
		if (ObjectId.isValid(songId)) {
	        query.put("_id", new ObjectId(songId));
	        
	        doc = collections.find(query).first();
	        
	        if(doc == null) {
	        	messagExecResult = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
	    		messageString = "Song is not in the database";
	        } else {	
	        	messagExecResult = DbQueryExecResult.QUERY_OK;
	    		messageString = "Found song from DB";
	            doc.replace("_id", songId);
	        }
		} else {
			messagExecResult = DbQueryExecResult.QUERY_ERROR_GENERIC;
    		messageString = "Not a valid SongId";
		}
		
        
        DbQueryStatus resultDbQueryStatus = new DbQueryStatus(messageString, messagExecResult);
		resultDbQueryStatus.setData(doc);
		
        return resultDbQueryStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
        MongoCollection<org.bson.Document> collections = db.getCollection("songs");
		BasicDBObject query = new BasicDBObject();
        DbQueryExecResult messagExecResult;
		String messageString;
		String titleString = null;
		
		if (ObjectId.isValid(songId)) {
	        query.put("_id", new ObjectId(songId));
	        
	        org.bson.Document doc = collections.find(query).first();
	        
	        if(doc == null) {
	        	messagExecResult = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
	    		messageString = "Song is not in the database";
	        } else {	
	        	messagExecResult = DbQueryExecResult.QUERY_OK;
	    		messageString = "Found song from DB";
	            titleString = doc.getString("songName");
	        }
		} else {
			messagExecResult = DbQueryExecResult.QUERY_ERROR_GENERIC;
    		messageString = "Not a valid SongId";
		}

        
        DbQueryStatus resultDbQueryStatus = new DbQueryStatus(messageString, messagExecResult);

		resultDbQueryStatus.setData(titleString);
		
        return resultDbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
        MongoCollection<org.bson.Document> collections = db.getCollection("songs");
		BasicDBObject query = new BasicDBObject();
        DbQueryExecResult messagExecResult;
		String messageString;
		
		if (ObjectId.isValid(songId)) {
	        query.put("_id", new ObjectId(songId));

	        org.bson.Document doc = collections.find(query).first();
	        
	        if(doc == null) {
	        	messagExecResult = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
	    		messageString = "Song is not in the database";
	        } else {
	        	messagExecResult = DbQueryExecResult.QUERY_OK;
	    		messageString = "Deleted song from the DB";
	            collections.deleteOne(doc);
	        }
	        
		} else {
			messagExecResult = DbQueryExecResult.QUERY_ERROR_GENERIC;
    		messageString = "Not a valid SongId";
		}
        DbQueryStatus resultDbQueryStatus = new DbQueryStatus(messageString, messagExecResult);
		return resultDbQueryStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		MongoCollection<org.bson.Document> collections = db.getCollection("songs");
		
		BasicDBObject query = new BasicDBObject();
		BasicDBObject updatedquery = new BasicDBObject();
		
		query.put("_id", new ObjectId(songId));
		
		if (shouldDecrement) {
			BasicDBObject checking = new BasicDBObject();
			checking.put("_id", new ObjectId(songId));
			org.bson.Document document = collections.find(query).first();
			if (document.getInteger("songAmountFavourites") != 0) {
				updatedquery.append("$inc", new BasicDBObject().append("songAmountFavourites", -1));
				collections.updateOne(query,updatedquery);
			}			
		} else {
			updatedquery.append("$inc", new BasicDBObject().append("songAmountFavourites", 1));
			collections.updateOne(query,updatedquery);
			
		}
        
        DbQueryStatus resultDbQueryStatus = new DbQueryStatus("Updated Song Favourites Count", DbQueryExecResult.QUERY_OK);
		return resultDbQueryStatus;
	}
}
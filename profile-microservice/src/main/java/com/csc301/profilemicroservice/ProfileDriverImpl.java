package com.csc301.profilemicroservice;

import java.security.Policy.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;


import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;


@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
        Session session = driver.session();
		DbQueryExecResult messagExecResult;
		String messageString;
		if(userName.isEmpty() || fullName.isEmpty()|| password.isEmpty()) {
			messagExecResult = DbQueryExecResult.QUERY_ERROR_GENERIC;
			messageString = "Enter a valid userName, fullName and password";
		} else {
	        StatementResult checking = session.run("MATCH (a:profile {userName: $userName}) RETURN a.userName", org.neo4j.driver.v1.Values.parameters( "userName", userName));

			
			if(!checking.hasNext()) {
				messagExecResult = DbQueryExecResult.QUERY_OK;
				messageString = "Created new User Profile";
		        String usersFavString = userName+"-favorites";
				StatementResult result = session.run( "CREATE (p:profile {userName: $userName, fullName: $fullName, password: $password})", org.neo4j.driver.v1.Values.parameters( "userName", userName, "fullName", fullName, "password", password));
		        StatementResult favourites = session.run( "CREATE (a:playlist {plName: $usersFav})", org.neo4j.driver.v1.Values.parameters("usersFav",usersFavString));
		        StatementResult created = session.run( "MATCH (a:profile),(b:playlist) WHERE a.userName = $userName AND b.plName = $usersFavString CREATE (a)-[r:created]->(b)", org.neo4j.driver.v1.Values.parameters( "userName", userName, "usersFavString", usersFavString));

			} else {
				messagExecResult = DbQueryExecResult.QUERY_ERROR_GENERIC;
				messageString = "User already exist in the DB";
			}
		}
		
        DbQueryStatus status = new DbQueryStatus(messageString,messagExecResult);
        return status;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		Session session = driver.session();
		
        StatementResult result = session.run( "MATCH (a:profile),(b:profile) WHERE a.userName = $userName AND b.userName = $frndUserName CREATE UNIQUE (a)-[r:follows]->(b)", org.neo4j.driver.v1.Values.parameters( "userName", userName, "frndUserName", frndUserName));
		
        DbQueryStatus status = new DbQueryStatus("First user follows second user", DbQueryExecResult.QUERY_OK);
        return status;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		Session session = driver.session();
		
		DbQueryExecResult messagExecResult;
		String messageString;
		
		if(checkFollowing(userName, frndUserName)) {
			messagExecResult = DbQueryExecResult.QUERY_OK;
			messageString = String.format("User %s succesfully unfollowed User %s", userName, frndUserName);
	        StatementResult result = session.run( "MATCH (a { userName: $userName })-[r:follows]->(b { userName: $frndUserName }) DELETE r", org.neo4j.driver.v1.Values.parameters( "userName", userName, "frndUserName", frndUserName));
		} else {
			messagExecResult = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
			messageString = String.format("%s or %s are not friends :'(", userName, frndUserName);
		}
        DbQueryStatus status = new DbQueryStatus(messageString,messagExecResult);
        return status;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		Session session = driver.session();
		Map<String, Object> jsonMap = new HashMap<>();
		
		DbQueryExecResult messagExecResult;
		String messageString;
        StatementResult checking = session.run("MATCH (a:profile {userName: $userName}) RETURN a.userName", org.neo4j.driver.v1.Values.parameters( "userName", userName));

		if(checking.hasNext()) {
			StatementResult result = session.run( "MATCH (:profile { userName: $userName })-->(p:profile) RETURN p.userName", org.neo4j.driver.v1.Values.parameters( "userName", userName));
			
	        while(result.hasNext()) {
	            Record record = result.next();
	            StatementResult gettingNames = session.run( "MATCH (:playlist { plName: $plName })--(s:song) RETURN s.songName", org.neo4j.driver.v1.Values.parameters( "plName", record.get(0).asString()+"-favorites"));
	            List<String> songNames = new ArrayList<>();
	            
	            while(gettingNames.hasNext()) {
	                Record itt = gettingNames.next();
	                songNames.add(itt.get(0).asString());
	            }
	            jsonMap.put(record.get(0).asString(), songNames);
	        }
	        messageString = "Got all songs friends likes";
	        messagExecResult = DbQueryExecResult.QUERY_OK;
		} else {
			messageString = "User not found";
	        messagExecResult = DbQueryExecResult.QUERY_ERROR_NOT_FOUND;
		}
		
		
        DbQueryStatus status = new DbQueryStatus(messageString,messagExecResult);
        status.setData(jsonMap);

        return status;
	}
	
	@Override
	public boolean checkFollowing(String userName, String frndUserName) {
		Session session = driver.session();
		
		StatementResult checking = session.run("RETURN EXISTS( (:profile {userName:$userName})-[:follows]->(:profile {userName:$frndUserName}))", org.neo4j.driver.v1.Values.parameters( "userName", userName, "frndUserName", frndUserName));
		boolean checking2 = checking.single().get(0).asBoolean();
		
        return checking2;
	}
}

package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		String playListString = userName+"-favorites";
		Session session = driver.session();
		StatementResult result = session.run( "MATCH (p:playlist { plName: $playListString }) MATCH (s:song {songId: $songId}) CREATE UNIQUE (p)-[:includes]->(s)", org.neo4j.driver.v1.Values.parameters( "playListString", playListString, "songId", songId));

        DbQueryStatus status = new DbQueryStatus("Song liked succesfully",DbQueryExecResult.QUERY_OK);
        		
        return status;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		// need to finish
		String playListString = userName+"-favorites";
		Session session = driver.session();
        StatementResult result = session.run( "MATCH (a { plName: $playListString })-[r:includes]->(b { songId: $songId }) DELETE r", org.neo4j.driver.v1.Values.parameters( "playListString", playListString, "songId", songId));

        DbQueryStatus status = new DbQueryStatus("Song unliked succesfully", DbQueryExecResult.QUERY_OK);
        return status;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		Session session = driver.session();
        StatementResult result = session.run( "MATCH (a { songId: $songId }) DETACH DELETE a", org.neo4j.driver.v1.Values.parameters( "songId", songId));

        DbQueryStatus status = new DbQueryStatus("Song deleted succesfully", DbQueryExecResult.QUERY_OK);
        return status;
	}
	
	@Override
	public DbQueryStatus addSong(String songId, String songName) {
		Session session = driver.session();
        StatementResult result = session.run( "MERGE (s:song {songId: $songId, songName: $songName})", org.neo4j.driver.v1.Values.parameters( "songId", songId, "songName", songName));

        DbQueryStatus status = new DbQueryStatus("Added song into the DB", DbQueryExecResult.QUERY_OK);
        return status;
	}
	
	@Override
	public boolean songAlreadyliked(String userName, String songId) {
		String playListString = userName+"-favorites";
		Session session = driver.session();
		
		StatementResult checking = session.run("RETURN EXISTS( (:playlist {plName: $playListString})-[:includes]->(:song {songId: $songId}))", org.neo4j.driver.v1.Values.parameters( "playListString", playListString, "songId", songId));
		boolean checking2 = checking.single().get(0).asBoolean();

        return checking2;
	}
}

package com.BattleBuilder.adapter;

/*
 *  Copyright (C) 2010  Alex Badion
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.lang.System;

import com.BattleBuilder.adapter.ListDbAdapter;
import com.BattleBuilder.adapter.ModelAdapter.Model;

public class GameDBAdapter{
/**
 * Simple Game Manager
 * Each entry is a model that is part of a game
 * A game is retrieved by asking for all the entries that match a game id
 */
    public static final String KEY_MODEL_ID = "model";
    public static final String KEY_NAME = "name";
    public static final String KEY_DAMAGE = "damage";
    public static final String KEY_GAME_ID = "game";
    public static final String KEY_ARMY_ID = "army";
    public static final String KEY_ROWID = "_id";

    private static final String TAG = "ListDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	private ModelAdapter mModelAdapter;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table armies (_id integer primary key autoincrement, "
                    + "model integer not null, " 
                    + "damage text not null, "
                    + "game text not null, "
                    + "name text not null, "
                    + "army integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String GAMES_TABLE = "games";
    private static final String LISTS_TABLE = ListDbAdapter.getTableName();
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + GAMES_TABLE);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public GameDBAdapter(Context ctx) {
        this.mCtx = ctx;
        mModelAdapter = ModelAdapter.getAdapter(ctx);
    }

    /**
     * Open the games database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public GameDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }

    /**
     * Starting a game will find the army list id for the army being played
     * and create an entry in the database for each model in the army.
     * 
     * @param title the army list that is going to play the game
     * @param body the body of the note
     * @return gameID the id of the game that was created.
     */
    public long createGame(long armyListID) {
    	
    	Cursor mCursor =
            mDb.query(true, LISTS_TABLE, new String[] {
	            		ListDbAdapter.KEY_ROWID,
	            		ListDbAdapter.KEY_TITLE,
	            		ListDbAdapter.KEY_ARMY
            		},
            		ListDbAdapter.KEY_ROWID + "=" + armyListID, null,
                    null, null, null, null);
	    if (mCursor != null) {
	        mCursor.moveToFirst();
	        
	        long timeStamp = System.currentTimeMillis();
	        int army_id = mCursor.getInt(mCursor.getColumnIndexOrThrow(ListDbAdapter.KEY_ROWID));
	        String[] models = mCursor.getString(mCursor.getColumnIndexOrThrow(ListDbAdapter.KEY_ARMY)).split(";");
	        
			Model m;
			for( int i=0; i < models.length; i++){
				String [] byComma= models[i].split(",");
				m = mModelAdapter.getItemByName( byComma[0] );
				int total = m.getTotal();
				for( int j=0; j < total; j++){
					ContentValues initialValues = new ContentValues();
			        initialValues.put(KEY_ARMY_ID, army_id);
			        initialValues.put(KEY_MODEL_ID, m.row_id);
			        initialValues.put(KEY_GAME_ID, timeStamp);
			        initialValues.put(KEY_DAMAGE, "");
			        if( total > 1){
				        initialValues.put(KEY_NAME, m.name + " " + j);
			        }else{
				        initialValues.put(KEY_NAME, m.name);
			        }
			        mDb.insert(GAMES_TABLE, null, initialValues);
				}
			}				
		    return timeStamp;
	    }else{
	    	//error stuff
	    	return 0l;
	    }
    }

    /**
     * 
     * @param rowId id of not to delete
     * @return the number of rows deleted
     */
    public int deleteGame(long gameID) {
    	return mDb.delete(GAMES_TABLE, KEY_GAME_ID + "=" + gameID, null);
    }

    /**
     * Return a Cursor over the list of armies in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllGames() {
        return mDb.query(GAMES_TABLE,
        				 new String[] {KEY_ROWID, KEY_MODEL_ID, KEY_NAME, KEY_DAMAGE, KEY_ARMY_ID, KEY_GAME_ID },
        				 null, null, null, null, KEY_GAME_ID);
    }
    
    /**
     * Return a Cursor over the list of armies in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchGame(long gameID) {
        return mDb.query(GAMES_TABLE, new String[] { KEY_ROWID, KEY_MODEL_ID, KEY_NAME, KEY_DAMAGE }, KEY_GAME_ID + "=" + gameID,
        		null, null, null, KEY_MODEL_ID);
    }

    /**
     * Return a Cursor at a specific model that is part of a game that matches the given rowId
     * 
     * @param rowId id of specific model that is part of a game
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchGameItem(long rowId) throws SQLException {
        Cursor mCursor =
                mDb.query(true, GAMES_TABLE, new String[] {KEY_ROWID },
                        KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param damage the updated damage for the model
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateModelDamage(long rowId, String name, String damage) {
        ContentValues args = new ContentValues();
        args.put(KEY_DAMAGE, damage);
        args.put(KEY_NAME, name);

        return mDb.update(GAMES_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * All damage for a game is kept as string in each model entry. To restart a game
     * all the damages just need to be reset to empty strings.
     * 
     * @param gameID the game to be restarted
     * @return whether any rows were changed to restart the game
     */
    public boolean restartGame(long gameID){
        ContentValues args = new ContentValues();
        args.put(KEY_DAMAGE, "");

        return mDb.update(GAMES_TABLE, args, KEY_GAME_ID + "=" + gameID, null) > 0;
    }
}

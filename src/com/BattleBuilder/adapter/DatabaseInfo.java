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

public class DatabaseInfo {
	public static String[] getUpgrade(){
		return new String[] {"DROP TABLE IF EXISTS games;",
		"DROP TABLE IF EXISTS armies;"};
	}
	
	public static String[] getCreate(){
		return new String[] {"create table armies (_id integer primary key autoincrement, "
        + "title text not null, " 
        + "faction text not null, "
        + "army text not null, "
        + "points integer not null);",
        "create table games (_id integer primary key autoincrement, "
        + "model integer not null, " 
        + "type integer not null, " 
        + "damage text not null, "
        + "name text not null, "
        + "army integer not null);"};
	}
	
	public static int getVersion(){
		return 7;
	}

}

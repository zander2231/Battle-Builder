package com.BattleBuilder;

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

import com.BattleBuilder.adapter.DamageGridAdapter;
import com.BattleBuilder.adapter.GameDBAdapter;
import com.BattleBuilder.adapter.ListDbAdapter;
import com.BattleBuilder.adapter.ModelAdapter;
import com.BattleBuilder.adapter.DamageGridAdapter.DamageGrid;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlayingList extends ListActivity {
    private static final int ACTIVITY_EDIT=0;
    
    private static final int VIEW_LOG_ID = Menu.FIRST;
    private static final int RESTART_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;

    private GameDBAdapter mDbHelper=null;
    private GameModelAdapter mGameModels=null;
    private long mGameID=0l;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_army);
        mDbHelper = new GameDBAdapter(this);
        mDbHelper.open();
        
        long armyID = 0l;
        if( savedInstanceState != null ){
        	armyID = savedInstanceState.getLong(ListDbAdapter.KEY_ROWID);
        	mGameID = savedInstanceState.getLong(GameDBAdapter.KEY_ARMY_ID);
        }else{
        	Bundle extras = getIntent().getExtras();
        	if( extras != null ){
        		armyID = extras.getLong(ListDbAdapter.KEY_ROWID);
        		mGameID = extras.getLong(GameDBAdapter.KEY_ARMY_ID);
        	}else{
        		//show some error and go back
        		finish();
        		
        	}
        }
        
        Cursor game = mDbHelper.fetchGame(armyID);
        if( mGameID == 0 && armyID != 0 && game.getCount() <= 0){
        	mGameID = mDbHelper.createGame(armyID);
        }else{
        	mGameID = armyID;
        	if( mDbHelper.needReload(mGameID) ){
            	mDbHelper.deleteGame(mGameID);
            	mDbHelper.createGame(mGameID);
            }
        }
        game.close();
        
        mGameModels = new GameModelAdapter(this);        
        setListAdapter(mGameModels);
    }
        
    private void fillData() {
    	this.onContentChanged();
    }

    @Override
    protected void onSaveInstanceState( Bundle outState){
    	super.onSaveInstanceState(outState);
    	if( mGameID != 0l){
        	outState.putLong(GameDBAdapter.KEY_ARMY_ID, mGameID);
    	}
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	fillData();
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        menu.add(0, VIEW_LOG_ID, 0, R.string.view_log);
        menu.add(0, RESTART_ID, 1, R.string.restart_game);
        menu.add(0, DELETE_ID, 2, R.string.end_game);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.option_menu, menu);
    	return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case VIEW_LOG_ID:
            return true;
	    case RESTART_ID:
	    	// add are you sure
	        fillData();
	    	mDbHelper.restartGame(mGameID);
	        return true;
	    case DELETE_ID:
	    	// add are you sure
	    	mDbHelper.deleteGame(mGameID);
	        this.finish();
	        return true;
	    }
       
        return super.onMenuItemSelected(featureId, item);
    }
		    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ModelDamage.class);
        i.putExtra(GameDBAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
    
    public class GameModelAdapter implements ListAdapter{
    	
    	private Cursor mModels = null;
    	private ModelAdapter mModelsMaster = null;
    	private LayoutInflater mInflater;
    	
    	public GameModelAdapter(Context context){
    		mInflater = LayoutInflater.from(context);
            // Get all of the rows from the database and create the item list
            mModels = mDbHelper.fetchGame(mGameID);
            startManagingCursor(mModels);
            
            mModelsMaster = ModelAdapter.getAdapter(context);
    	}

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int position) {
			// all items are always enabled
			return true;
		}

		public int getCount() {
			return mModels.getCount();
		}

		/**
		 * Method always returns null;
		 * There is no object that represents a row in this adapter
		 */
		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			mModels.moveToPosition(position);
			return mModels.getLong(mModels.getColumnIndex(GameDBAdapter.KEY_ROWID));
		}

		public int getItemViewType(int position) {
			return R.layout.play_row;
		}
		
		public class ViewHolder{
			public TextView name;
			public TextView num;
			public TextView damageRemaining;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if( convertView == null){
				convertView	= mInflater.inflate(R.layout.play_row, null);
				
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.play_model_name);
				holder.num = (TextView) convertView.findViewById(R.id.play_model_num);
				holder.damageRemaining = (TextView) convertView.findViewById(R.id.damage_remaining);
				convertView.setTag(holder);	
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			
			mModels.moveToPosition(position);
			ModelAdapter.Model model = 
				(ModelAdapter.Model)mModelsMaster.getItem(mModels.getInt(mModels.getColumnIndex(GameDBAdapter.KEY_MODEL_ID)) );

			holder.name.setText(mModels.getString(mModels.getColumnIndex(GameDBAdapter.KEY_NAME)));

			int modelType = mModels.getInt(mModels.getColumnIndex(GameDBAdapter.KEY_MODEL_TYPE));
			int numModels = 1;
			if( (model.damage == null || model.damage.type == DamageGridAdapter.UNIT) && model.num_models.length > modelType){
				numModels = model.num_models[modelType];
				holder.num.setText( "Num:" + numModels );
			}else{
				holder.num.setText("");
			}
			
			String damages =  mModels.getString(mModels.getColumnIndex(GameDBAdapter.KEY_DAMAGE));
			int damage = 0;
			if( !damages.equalsIgnoreCase("")){
				damage = damages.split(";").length;
			}

			if( model.damage == null){
				holder.damageRemaining.setText( R.string.no_damage );
			}else if(model.damage.type == DamageGridAdapter.UNIT){
				int enabled = (model.damage.getSizeX() -1) * numModels;
				holder.damageRemaining.setText(enabled - damage + "/" + enabled );
			}
			else{
				holder.damageRemaining.setText(model.damage.totalEnabled()-damage + "/" + model.damage.totalEnabled());
			}
			
			return convertView;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mModels.getCount() == 0;
		}

		public void registerDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub
		}
    }
    
}
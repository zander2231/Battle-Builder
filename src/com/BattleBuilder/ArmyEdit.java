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

import com.BattleBuilder.adapter.FactionAdapter;
import com.BattleBuilder.adapter.ListDbAdapter;
import com.BattleBuilder.adapter.ModelAdapter;
import com.BattleBuilder.adapter.PointLevelAdapter;
import com.BattleBuilder.adapter.ModelAdapter.Model;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.inputmethodservice.Keyboard.Key;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class ArmyEdit extends ListActivity
	implements OnItemSelectedListener, ModelAdapter.OnFaUpdateListener{

    public static final String ARMY_CHANGED = "armyChanged";
	
	private EditText mArmyName;
	private TextView mPointsRemaining;
	private Spinner mFactionName;
	private FactionAdapter mFactionAdapter;
	private Spinner mPoints;
	private PointLevelAdapter mPointAdapter;
	private ModelAdapter mModelAdapter;
    private Long mRowId=0l;

    private ListDbAdapter mDbHelper=null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        mDbHelper = new ListDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.army_edit);
       
        mArmyName = (EditText) findViewById(R.id.army_name);
        mPointsRemaining = (TextView) findViewById(R.id.remaining_amount);
        
        mFactionName = (Spinner) findViewById(R.id.faction_chooser);
        mFactionAdapter = FactionAdapter.getAdapter(this);
        mFactionName.setAdapter(mFactionAdapter);
        mFactionName.setOnItemSelectedListener(this);

        mPoints = (Spinner) findViewById(R.id.points_chooser);
        mPointAdapter = PointLevelAdapter.getAdapter( this );
        mPoints.setAdapter(mPointAdapter);
        mPoints.setOnItemSelectedListener(this);
        
        mModelAdapter = ModelAdapter.getAdapter(this);
		mModelAdapter.setFaction(mFactionAdapter.getNameAt(0));
		mModelAdapter.setPointsLevel(mPointAdapter.getPointsAt(0), mPointAdapter.getCastersAt(0));
		mModelAdapter.setOnFaUpdateListener(this);
        setListAdapter(mModelAdapter);

        if( savedInstanceState != null ){
        	mRowId = savedInstanceState.getLong(ListDbAdapter.KEY_ROWID);
        }else{
        	Bundle extras = getIntent().getExtras();
        	if( extras != null ){
        		mRowId = extras.getLong(ListDbAdapter.KEY_ROWID);
        	}else{
        		mRowId = 0l;
        	}
        }
        
        populateFields();
    }
    
    private void populateFields(){
    	if ( mRowId != 0l){
    		Cursor army = mDbHelper.fetchArmy(mRowId);
    		startManagingCursor(army);
    		mArmyName.setText(army.getString(army.getColumnIndexOrThrow(ListDbAdapter.KEY_TITLE)));
    		String factionName = army.getString(army.getColumnIndexOrThrow(ListDbAdapter.KEY_FACTION));
			mModelAdapter.setFaction(factionName);
    		mFactionName.setSelection(mFactionAdapter.getNameIndex(factionName));
    		int pointLevel = army.getInt(army.getColumnIndexOrThrow(ListDbAdapter.KEY_POINTS));
    		int pointIndex = mPointAdapter.getPointsIndex(pointLevel);
			mModelAdapter.setPointsLevel(pointLevel, mPointAdapter.getCastersAt(pointIndex));
    		mPoints.setSelection(pointIndex);
    		mModelAdapter.setUpArmy(army.getString(army.getColumnIndexOrThrow(ListDbAdapter.KEY_ARMY)));
    	}
		updatePointsTotal();
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState){
    	super.onSaveInstanceState(outState);
    	if( mRowId != 0l){
        	outState.putLong(ListDbAdapter.KEY_ROWID, mRowId);
    	}
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	saveState();
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	populateFields();
    }
    
    private void saveState(){
    	String title = mArmyName.getText().toString();
    	String faction = mFactionAdapter.getNameAt(mFactionName.getSelectedItemPosition());
    	int points = mPointAdapter.getPointsAt(mPoints.getSelectedItemPosition());
    	
    	if( mRowId == 0l){
    		long id = mDbHelper.createArmy(title, faction, points, mModelAdapter.packUpArmy());
    		if( id > 0){
    			mRowId = id;
    		}
    	} else{
    		mDbHelper.updateArmy(mRowId, title, faction, points, mModelAdapter.packUpArmy());
    	}	
    }

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if( parent == mFactionName){
			mModelAdapter.setFaction(mFactionAdapter.getNameAt(position));
			updatePointsTotal();
			onContentChanged();
		}else if( parent == mPoints){
			int casters = mPointAdapter.getCastersAt(position);
			int points = mPointAdapter.getPointsAt(position);
			mModelAdapter.setPointsLevel(points, casters);
			updatePointsTotal();
			onContentChanged();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
		mModelAdapter.setFaction("");
	}

	public void updateFAs() {
		ListView lv = getListView();
		for( int i=0; i<lv.getChildCount(); i++){
			ModelAdapter.ViewHolder holder = (ModelAdapter.ViewHolder)lv.getChildAt(i).getTag();
			Model model = (Model)mModelAdapter.getItem(holder.faMin.getDataIndex());

			//setup whether the add/sub buttons works and current value showing
			holder.faMin.setCountNoUpdate(model.num_used[ModelAdapter.FA_INDEX_MIN]);
			holder.faMin.setEnabled(
					model.canPlus(ModelAdapter.FA_INDEX_MIN), 
					model.canMinus(ModelAdapter.FA_INDEX_MIN));
			if(model.cost[ModelAdapter.FA_INDEX_MAX] > 0){
				holder.faMax.setCountNoUpdate(model.num_used[ModelAdapter.FA_INDEX_MAX]);
				holder.faMax.setEnabled(
						model.canPlus(ModelAdapter.FA_INDEX_MAX), 
						model.canMinus(ModelAdapter.FA_INDEX_MAX));
				if(model.cost[ModelAdapter.FA_INDEX_WEAPON] > 0){
					holder.faWeapon.setCountNoUpdate(model.num_used[ModelAdapter.FA_INDEX_WEAPON]);
					holder.faWeapon.setEnabled(
							model.canPlus(ModelAdapter.FA_INDEX_WEAPON), 
							model.canMinus(ModelAdapter.FA_INDEX_WEAPON));
				}
			}			
		}
		updatePointsTotal();
	}
	
	public void updatePointsTotal(){
		mPointsRemaining.setText( mModelAdapter.getRemainingPoints() + "" );
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // TODO does clicking on an item in this view do anything
//        Intent i = new Intent(this, ModelDamage.class);
//        i.putExtra("model_id", position);
//        startActivity(i);
    }
    
    public static final int PLAY_ARMY_ID = 1;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PLAY_ARMY_ID, 0, R.string.play_army);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case PLAY_ARMY_ID:
            playArmy();
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void playArmy(){
        Intent i = new Intent(this, PlayingList.class);
        i.putExtra(ListDbAdapter.KEY_ROWID, mRowId);
        startActivity(i);
    }
}

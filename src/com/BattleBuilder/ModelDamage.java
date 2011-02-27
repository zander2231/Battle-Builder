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

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.BattleBuilder.adapter.DamageGridAdapter;
import com.BattleBuilder.adapter.GameDBAdapter;
import com.BattleBuilder.adapter.ListDbAdapter;
import com.BattleBuilder.adapter.ModelAdapter;
import com.BattleBuilder.adapter.DamageGridAdapter.DamageGrid;
import com.BattleBuilder.adapter.DamageGridAdapter.GridLocation;
import com.BattleBuilder.adapter.DamageGridAdapter.PlayableDamageGrid;
import com.BattleBuilder.widget.DamageGridView;
import com.BattleBuilder.widget.NumberPicker;

public class ModelDamage extends Activity implements
  OnClickListener{

    private static final int VIEW_LOG_ID = Menu.FIRST;
    private static final int CLEAR_ALL_ID = Menu.FIRST + 1;
    
	private final static String TAG = "ModelDamageScreen";
	private PlayableDamageGrid mGrid=null;
	private ModelAdapter.Model mModel;
	private long mGameModelID;
	private TextView mName;
	private TextView mPageNum;
	private NumberPicker mDamageAmount;
	private DamageGridView mDamageGrid;
	private Spinner mColumns;
	private Button mFinalize;
	
	private GameDBAdapter mDBHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        mDBHelper = new GameDBAdapter(this);
        //TODO make sure I am closing database on Activity EXIT
        mDBHelper.open();
        
        setContentView(R.layout.model_damage);
        mName = (TextView)findViewById(R.id.model_name);
        mPageNum = (TextView)findViewById(R.id.page_number);
        
        mDamageAmount = (NumberPicker)findViewById(R.id.amount_damage);
        mDamageAmount.setOnClickListener(this);
        
        mDamageGrid = (DamageGridView)findViewById(R.id.damage_grid);
        
        mColumns = (Spinner)findViewById(R.id.which_column);
        
        mFinalize = (Button)findViewById(R.id.finalize_damage);
        mFinalize.setOnClickListener(this);
                
    	Bundle extras = getIntent().getExtras();
    	if( extras != null ){
    		mGameModelID = extras.getLong(GameDBAdapter.KEY_ROWID);
        	Cursor gameModel = mDBHelper.fetchGameItem(mGameModelID);
        	int modelId = gameModel.getInt(gameModel.getColumnIndex(GameDBAdapter.KEY_MODEL_ID));
    		mModel = (ModelAdapter.Model)ModelAdapter.getAdapter(this).getItem(modelId);
    		
    		if( mModel.damage != null){
    			int type = gameModel.getInt(gameModel.getColumnIndex(GameDBAdapter.KEY_MODEL_TYPE));
    			mGrid = new PlayableDamageGrid(mModel.damage, mModel.num_models[type]);
    			mGrid.unpack(gameModel.getString(gameModel.getColumnIndex(GameDBAdapter.KEY_DAMAGE)));
    			mDamageGrid.setGrid( mGrid );

    			ArrayList<String> display = new ArrayList<String>();
        		for( int i=0; i< mDamageGrid.getNumTracks(); i++){
        			display.add( "" + (i+1) );
        		}
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, display);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mColumns.setAdapter(adapter);
    		}else{
    			mDamageGrid.setVisibility(View.INVISIBLE);
    			mColumns.setVisibility(View.INVISIBLE);
    			mDamageAmount.setVisibility(View.INVISIBLE);
    			mFinalize.setVisibility(View.INVISIBLE);
    			findViewById(R.id.damage_text).setVisibility(View.INVISIBLE);
    			findViewById(R.id.column_text).setVisibility(View.INVISIBLE);
    		}
    		mName.setText(mModel.name);
    		mPageNum.setText("Page Num: " + mModel.page_num);
    	}else{
    		//TODO Error think of correct action to take
    		this.finish();
    	}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
		if( mModel.damage != null){
			menu.add(0, VIEW_LOG_ID, 0, R.string.view_log);
			menu.add(0, CLEAR_ALL_ID, 1, R.string.remove_all);
		}
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.option_menu, menu);
    	return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case VIEW_LOG_ID:
            return true;
	    case CLEAR_ALL_ID:
	    	// add are you sure
			mDamageGrid.removeAllDamage();
	        return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }    

	public void onClick(View v) {
		if(mGrid == null){
			return;
		}
		
		if( v == mFinalize){
			mDamageGrid.finalizeDamage();
			int c = (int)mColumns.getSelectedItemId();
			Log.d(TAG, "SelectedItemId: " + c);
			mGrid.dealDamage(c, mDamageAmount.getCount());
			mDamageAmount.setCount(0);
			mDamageGrid.setEnabled(true);
			
			mDBHelper.updateModelDamage(mGameModelID, mName.getText().toString(), mGrid.packUp());
			
		}else if( v == mDamageAmount){
			mDamageGrid.setEnabled(mDamageAmount.getCount() > 0);
		}
	}
}

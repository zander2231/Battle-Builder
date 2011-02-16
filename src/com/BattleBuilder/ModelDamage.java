package com.BattleBuilder;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.BattleBuilder.adapter.DamageGridAdapter;
import com.BattleBuilder.adapter.ListDbAdapter;
import com.BattleBuilder.adapter.ModelAdapter;
import com.BattleBuilder.adapter.DamageGridAdapter.DamageGrid;
import com.BattleBuilder.adapter.DamageGridAdapter.GridLocation;
import com.BattleBuilder.adapter.DamageGridAdapter.PlayableDamageGrid;
import com.BattleBuilder.widget.DamageGridView;
import com.BattleBuilder.widget.NumberPicker;

public class ModelDamage extends Activity implements
  OnClickListener{

	private final static String TAG = "ModelDamageScreen";
	private PlayableDamageGrid mGrid=null;
	private ModelAdapter.Model mModel;
	private TextView mName;
	private TextView mPageNum;
	private NumberPicker mDamageAmount;
	private DamageGridView mDamageGrid;
	private Spinner mColumns;
	private Button mFinalize;
	private Button mRemoveAll;
	private Button mShowLog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.model_damage);
        mName = (TextView)findViewById(R.id.model_name);
        mPageNum = (TextView)findViewById(R.id.page_number);
        
        mDamageAmount = (NumberPicker)findViewById(R.id.amount_damage);
        mDamageAmount.setOnClickListener(this);
        
        mDamageGrid = (DamageGridView)findViewById(R.id.damage_grid);
        
        mColumns = (Spinner)findViewById(R.id.which_column);
        
        mFinalize = (Button)findViewById(R.id.finalize_damage);
        mFinalize.setOnClickListener(this);
        
        mRemoveAll = (Button)findViewById(R.id.remove_all);
        mRemoveAll.setOnClickListener(this);
        
        mShowLog = (Button)findViewById(R.id.show_log);
        mShowLog.setOnClickListener(this);
        
    	Bundle extras = getIntent().getExtras();
    	if( extras != null ){
        	int modelId = extras.getInt("model_id");
    		mModel = (ModelAdapter.Model)ModelAdapter.getAdapter(this).getItem(modelId);
    		if( mModel.damage != null){
    			mGrid = new PlayableDamageGrid(mModel.damage);
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
    			mRemoveAll.setVisibility(View.INVISIBLE);
    			mFinalize.setVisibility(View.INVISIBLE);
    			mShowLog.setVisibility(View.INVISIBLE);
    			findViewById(R.id.damage_text).setVisibility(View.INVISIBLE);
    			findViewById(R.id.column_text).setVisibility(View.INVISIBLE);
    		}
    		mName.setText(mModel.name);
    		mPageNum.setText("Page Num: " + mModel.page_num);
    	}else{
    		mModel = null;
    	}
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
			
		}else if( v == mDamageAmount){
			mDamageGrid.setEnabled(mDamageAmount.getCount() > 0);
		}else if( v == mRemoveAll){
			mDamageGrid.removeAllDamage();
		}
	}
}

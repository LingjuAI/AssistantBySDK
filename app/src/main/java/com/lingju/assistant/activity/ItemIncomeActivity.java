package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.model.Item;
import com.lingju.model.dao.AccountItemDao;

import java.util.ArrayList;
import java.util.List;

public class ItemIncomeActivity extends GoBackActivity {
	private final static String TAG="ItemExpenseActivity";
	
	public final static int FOR_SELECT_ITEM=9;
	public final static String ITEM="Income";
	
	private AccountItemDao dao;
	private ListView itemListView;
	private List<Item> items= new ArrayList<>();
	private int seletectedItemPosition=-1;
	private Intent intent;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_income);
		dao=AccountItemDao.getInstance();
		itemListView=(ListView) findViewById(R.id.aii_item_list);
		items=dao.findAllByExpense(0);
		itemListView.setAdapter(itemAdapter);
		itemListView.setOnItemClickListener(itemItemListener);
		
		findViewById(R.id.aii_back).setOnClickListener(clickListener);
		findViewById(R.id.aii_edit).setOnClickListener(clickListener);
		
		intent=getIntent();
		if(intent!=null){
			String item=intent.getStringExtra(ITEM);
			if(!TextUtils.isEmpty(item)){
					boolean r=false;
					for(int i=0;i<items.size();i++){
						if(items.get(i).getItem().equals(item)){
							seletectedItemPosition=i;
							break;
						}
					}
			}
		}
		itemListView.setSelection(seletectedItemPosition);
	}
	
	private int dp2px(int dp){
		return (int)(getResources().getDisplayMetrics().density*dp+0.5f);
	}
	
	
	
	private OnItemClickListener itemItemListener=new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			seletectedItemPosition=position;
			itemAdapter.notifyDataSetChanged();
			if(intent!=null&&seletectedItemPosition>-1){
				intent.putExtra(ITEM,items.get(seletectedItemPosition).getItem());
				if(intent.getIntExtra(MainActivity.RESULT_CODE, 0)==0){
					setResult(FOR_SELECT_ITEM, intent);
				}
				else{
					intent.setClass(ItemIncomeActivity.this, MainActivity.class);
					startActivity(intent);
				}
			}
			goBack();
		}
		
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(data!=null){
			if(resultCode==ItemEditActivity.FOR_ITEM_EDIT){
				if(data.getBooleanExtra(ItemEditActivity.UPDATE, false)){
					items=dao.findAllByExpense(0);
					itemAdapter.notifyDataSetChanged();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private View.OnClickListener clickListener=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.aii_back:
				if(intent!=null&&seletectedItemPosition>-1){
					intent.putExtra(ITEM,items.get(seletectedItemPosition).getItem());
					if(intent.getIntExtra(MainActivity.RESULT_CODE, 0)==0){
						setResult(FOR_SELECT_ITEM, intent);
					}
					else{
						intent.setClass(ItemIncomeActivity.this, MainActivity.class);
						startActivity(intent);
					}
				}
				goBack();
				break;
			case R.id.aii_edit:
				Intent it=new Intent(ItemIncomeActivity.this, ItemEditActivity.class);
				it.putExtra(ItemEditActivity.TYPE, ItemEditActivity.INCOME);
				startActivityForResult(it, ItemEditActivity.FOR_ITEM_EDIT);
				goInto();
				break;
			}
			
		}
	};
	
	private BaseAdapter itemAdapter=new BaseAdapter() {
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView=new LinearLayout(ItemIncomeActivity.this);
				
				AbsListView.LayoutParams lp=new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,dp2px(64));
				convertView.setLayoutParams(lp);
				((LinearLayout)convertView).setOrientation(LinearLayout.HORIZONTAL);
				TextView tv=new TextView(ItemIncomeActivity.this);
				tv.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				tv.setTextColor(getResources().getColorStateList(R.color.new_text_color_first));
				LinearLayout.LayoutParams llpt=new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
				llpt.weight=1.0f;
				
				LinearLayout.LayoutParams llpi=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				ImageView iv=new ImageView(ItemIncomeActivity.this);
				iv.setBackgroundResource(R.drawable.ic_green_checkbox);
				iv.setVisibility(View.GONE);
				llpi.gravity=Gravity.CENTER_VERTICAL;
				
				((LinearLayout)convertView).addView(tv,0,llpt);
				((LinearLayout)convertView).addView(iv,1,llpi);
			}
			((TextView)((LinearLayout)convertView).getChildAt(0)).setText(items.get(position).getItem());
			if(position==seletectedItemPosition){
				((LinearLayout)convertView).getChildAt(1).setVisibility(View.VISIBLE);
			}
			else{
				((LinearLayout)convertView).getChildAt(1).setVisibility(View.GONE);
			}
			return convertView;
		}
		
		
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Object getItem(int position) {
			return position;
		}
		
		@Override
		public int getCount() {
			return items.size();
		}
	};

}

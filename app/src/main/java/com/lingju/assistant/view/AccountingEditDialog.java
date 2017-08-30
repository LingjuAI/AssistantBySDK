package com.lingju.assistant.view;

import android.app.Activity;
import android.graphics.drawable.LevelListDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.view.base.BaseEditDialog;
import com.lingju.model.Accounting;
import com.lingju.model.dao.AssistDao;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccountingEditDialog extends BaseEditDialog implements View.OnClickListener{
	private OnAccountingEditListener defaultListener;
	private Accounting[] accounts;

	private int checkedType[];
	private double amount[];
	private long time;

	private View typeViews[]=new View[3];
	private TextView proTexts[]=new TextView[3];
	private TextView amountTexts[]=new TextView[3];
	private EditText memoText;
	private TextView timeText;
	private TextView ecText;
	private TextView icText;
	private SimpleDateFormat sf=new SimpleDateFormat("yyyy年MM月dd日");

	private int count;
	private int expenseCount=0;
	private int incomeCount=0;
	private int amountEditIndex=0;
	private int projectEditIndex=0;
	private View mTaskView;

	public AccountingEditDialog(Activity context, Accounting[] accounts, OnAccountingEditListener defaultListener) {
		super(context, R.style.lingju_dialog1);
		setCancelable(false);
		this.accounts=accounts;
		this.count=this.accounts.length;
		this.defaultListener=defaultListener;
	}

	/*public AccoutingEditDialog(Context context, int theme) {
		super(context, theme);
	}

	public AccoutingEditDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}*/

	public void setAccount(Accounting[] account) {
		this.accounts = account;
	}

	public Accounting[] getAccount() {
		return accounts;
	}

	@Override
	public void setAmount(double amount) {
		this.amount[amountEditIndex] = amount;
		amountTexts[amountEditIndex].setText(Double.toString(amount/100)+(amount%100==0?"":"."+amount%100));
		resetCount();
	}

	/** 重置收支统计 **/
	private void resetCount(){
		expenseCount=0;
		incomeCount=0;
		for(int i=0;i<count;i++){
			if(accounts[i]==null)continue;
			if(checkedType[i]==0){
				expenseCount+=amount[i];
			}
			else{
				incomeCount+=amount[i];
			}
		}
		ecText.setText(traslate(expenseCount));
		icText.setText(traslate(incomeCount));
	}

	@Override
	public void setTime(long time) {
		this.time = time;
		timeText.setText(sf.format(new Date(time)));
	}

	@Override
	public void setProject(String proText) {
		Log.e("AccountingEditDialog", "setProject proText="+proText+",projectEditIndex="+projectEditIndex+",this.proTexts[projectEditIndex]"+this.proTexts[projectEditIndex]);
		if(accounts[projectEditIndex]!=null)
			accounts[projectEditIndex].setEtype(proText);
		if(this.proTexts[projectEditIndex]!=null)
		this.proTexts[projectEditIndex].setText(proText);
	}

	public void setDefaultEditListener(OnAccountingEditListener listener) {
		this.defaultListener=listener;
	}

	@Override
	public boolean confirm(){
		int index=1;
		for(int i=0;i<count;i++){
			if(accounts[i]==null)continue;
			if(TextUtils.isEmpty(proTexts[i].getText())){
				if(defaultListener!=null){
					defaultListener.onError("第"+(index++)+"个记账项目不能为空");
					return false;
				}
			}
			accounts[i].setAmount(amount[i]);
			accounts[i].setAtype(checkedType[i]);
			accounts[i].setMemo(memoText.getText().toString());
			accounts[i].setEtype(proTexts[i].getText().toString());
			accounts[i].setCreated(new Timestamp(time));
			if(accounts[i].getId()!=null&&accounts[i].getId()>0){
				AssistDao.getInstance().updateAccount(accounts[i]);
			}
			else
			AssistDao.getInstance().insertAccount(accounts[i]);
		}
		cancel();
		return true;
	}

	@Override
	protected void initTaskView(LinearLayout llTaskContainer) {
		mTaskView = View.inflate(mContext, R.layout.accounting_edit_dialog, null);
		typeViews[0]= mTaskView.findViewById(R.id.aib_type1);
		typeViews[1]= mTaskView.findViewById(R.id.aib_type2);
		typeViews[2]= mTaskView.findViewById(R.id.aib_type3);
		proTexts[0]=(TextView) mTaskView.findViewById(R.id.aib_project1);
		proTexts[1]=(TextView) mTaskView.findViewById(R.id.aib_project2);
		proTexts[2]=(TextView) mTaskView.findViewById(R.id.aib_project3);
		amountTexts[0]=(TextView) mTaskView.findViewById(R.id.aib_amount1);
		amountTexts[1]=(TextView) mTaskView.findViewById(R.id.aib_amount2);
		amountTexts[2]=(TextView) mTaskView.findViewById(R.id.aib_amount3);
		memoText=(EditText) mTaskView.findViewById(R.id.aib_memo);
		timeText=(TextView) mTaskView.findViewById(R.id.aib_time);
		ecText=(TextView) mTaskView.findViewById(R.id.aib_expense);
		icText=(TextView) mTaskView.findViewById(R.id.aib_income);

		mTaskView.findViewById(R.id.aed_close).setOnClickListener(this);
		mTaskView.findViewById(R.id.aed_cancel).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_amount_box1).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_amount_box2).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_amount_box3).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_project_box1).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_project_box2).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_project_box3).setOnClickListener(this);
		timeText.setOnClickListener(this);
		mTaskView.findViewById(R.id.aed_confirm).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_type1).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_type2).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_type3).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_delete1).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_delete2).setOnClickListener(this);
		mTaskView.findViewById(R.id.aib_delete3).setOnClickListener(this);

		if(accounts!=null){
			checkedType=new int[count];
			amount=new double[count];
			LevelListDrawable ld;
			for(int i=0;i<count;i++){
				checkedType[i]=accounts[i].getAtype();
				amount[i]=accounts[i].getAmount();
				ld=(LevelListDrawable) typeViews[i].getBackground();
				ld.setLevel(checkedType[i]);
				if(checkedType[i]==0){
					expenseCount+=amount[i];
				}
				else{
					incomeCount+=amount[i];
				}
				amountTexts[i].setText(Double.toString(amount[i]/100)+(amount[i]%100==0?"":"."+amount[i]%100));
				proTexts[i].setText(accounts[i].getEtype());
			}
			time=accounts[0].getCreated()!=null?accounts[0].getCreated().getTime():System.currentTimeMillis();
			if(count>=2){
				mTaskView.findViewById(R.id.aib_count_box).setVisibility(View.VISIBLE);
				mTaskView.findViewById(R.id.aib_sub_box2).setVisibility(View.VISIBLE);
				mTaskView.findViewById(R.id.aib_delete1).setVisibility(View.VISIBLE);
				if(count==3){
					mTaskView.findViewById(R.id.aib_sub_box3).setVisibility(View.VISIBLE);
				}
			}
			ecText.setText(traslate(expenseCount));
			icText.setText(traslate(incomeCount));
			memoText.setText(accounts[0].getMemo());
			timeText.setText(sf.format(accounts[0].getCreated()));
		}

		llTaskContainer.addView(mTaskView);
	}

	private String traslate(int v){
		return Integer.toString(v/100)+(v%100==0?"":"."+v%100);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.aed_cancel:
		case R.id.aed_close:
			cancel();
			if(defaultListener!=null){
				defaultListener.onCancel();
			}
			break;
		case R.id.aed_confirm:
			if(confirm()&&defaultListener!=null){
				defaultListener.onConfirm();
			}
			break;
		case R.id.aib_amount_box1:
			if(defaultListener!=null){
				amountEditIndex=0;
				defaultListener.changeAmount(amount[0]);
			}
			break;
		case R.id.aib_amount_box2:
			if(defaultListener!=null){
				amountEditIndex=1;
				defaultListener.changeAmount(amount[1]);
			}
			break;
		case R.id.aib_amount_box3:
			if(defaultListener!=null){
				amountEditIndex=2;
				defaultListener.changeAmount(amount[2]);
			}
			break;
		case R.id.aib_time:
			if(defaultListener!=null){
				defaultListener.changeDate(time>0?time:System.currentTimeMillis());
			}
			break;
		case R.id.aib_project_box1:
			if(defaultListener!=null){
				projectEditIndex=0;
				defaultListener.changePro(proTexts[0].getText().toString(),checkedType[0]);
			}
			break;
		case R.id.aib_project_box2:
			if(defaultListener!=null){
				projectEditIndex=1;
				defaultListener.changePro(proTexts[1].getText().toString(),checkedType[1]);
			}
			break;
		case R.id.aib_project_box3:
			if(defaultListener!=null){
				projectEditIndex=2;
				defaultListener.changePro(proTexts[2].getText().toString(),checkedType[2]);
			}
			break;
		case R.id.aib_type1:
			checkedType[0]=checkedType[0]==1?0:1;
			((LevelListDrawable)typeViews[0].getBackground()).setLevel(checkedType[0]);
			resetCount();
			break;
		case R.id.aib_type2:
			checkedType[1]=checkedType[1]==1?0:1;
			((LevelListDrawable)typeViews[1].getBackground()).setLevel(checkedType[1]);
			resetCount();
			break;
		case R.id.aib_type3:
			checkedType[2]=checkedType[2]==1?0:1;
			((LevelListDrawable)typeViews[2].getBackground()).setLevel(checkedType[2]);
			resetCount();
			break;
		case R.id.aib_delete1:
			mTaskView.findViewById(R.id.aib_sub_box1).setVisibility(View.GONE);
			accounts[0]=null;
			if(mTaskView.findViewById(R.id.aib_sub_box2).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete3).setVisibility(View.GONE);
			}
			else if(mTaskView.findViewById(R.id.aib_sub_box3).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete2).setVisibility(View.GONE);
			}
			resetCount();
			break;
		case R.id.aib_delete2:
			mTaskView.findViewById(R.id.aib_sub_box2).setVisibility(View.GONE);
			accounts[1]=null;
			if(mTaskView.findViewById(R.id.aib_sub_box1).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete3).setVisibility(View.GONE);
			}
			else if(mTaskView.findViewById(R.id.aib_sub_box3).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete1).setVisibility(View.GONE);
			}
			resetCount();
			break;
		case R.id.aib_delete3:
			mTaskView.findViewById(R.id.aib_sub_box3).setVisibility(View.GONE);
			accounts[2]=null;
			if(mTaskView.findViewById(R.id.aib_sub_box2).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete1).setVisibility(View.GONE);
			}
			else if(mTaskView.findViewById(R.id.aib_sub_box1).getVisibility()==View.GONE){
				mTaskView.findViewById(R.id.aib_delete2).setVisibility(View.GONE);
			}
			resetCount();
			break;
		}
	}
	
	 public interface OnAccountingEditListener {
		 	public void onError(String msg);
	        public void onConfirm();
	        public void onCancel();
	        public void changeAmount(double amount);
	        public void changeDate(long date);
	        public void changePro(String pro, int type);
	 }

}

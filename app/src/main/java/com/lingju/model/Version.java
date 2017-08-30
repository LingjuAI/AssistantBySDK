package com.lingju.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Version{
	private boolean update=false;
	private String new_version;
	private String apk_path;
	private boolean updateApk=false;
	private String[] dbs;
	
	
	/**
	 * {"last_version":"v0.9.12","update_app":"http://www.360008.com/software/app_music/v0.9.12/,LingjuMusicv0.9.12.apk"}
	 */
	public Version(String jsonString){
		try {
			JSONObject jobject=new JSONObject(jsonString);
			if(jobject.getInt("status")==0) {
				this.new_version = jobject.getString("last_version");
				if (this.new_version != null && this.new_version.startsWith("v"))
					this.new_version = this.new_version.substring(1);
				this.apk_path = jobject.has("update_app")?jobject.getString("update_app"):"";
				this.update = this.new_version != null && this.new_version.length() > 0 ? true : false;
				if (!update) return;
				if (this.apk_path.trim().length() == 0) {
					this.updateApk = false;
					JSONArray ja = jobject.getJSONArray("update_files");
					if(ja.length()==0){
						this.update=false;
						return;
					}
					dbs = new String[ja.length()];
					for (int i = 0; i < dbs.length; i++) {
						dbs[i] = ja.getJSONObject(i).getString("update_file");
					}
				} else {
					this.updateApk = true;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isUpdateApk() {
		return updateApk;
	}
	
	public void setUpdateApk(boolean updateApk) {
		this.updateApk = updateApk;
	}
	
	public String[] getDbs() {
		return dbs;
	}
	
	public void setDbs(String[] dbs) {
		this.dbs = dbs;
	}
	
	public boolean isUpdate() {
		return update;
	}


	public void setUpdate(boolean update) {
		this.update = update;
	}


	public String getNew_version() {
		return new_version;
	}
	public void setNew_version(String new_version) {
		this.new_version = new_version;
	}
	public String getApk_path() {
		return apk_path;
	}
	public void setApk_path(String apk_path) {
		this.apk_path = apk_path;
	}


	
	

	
}

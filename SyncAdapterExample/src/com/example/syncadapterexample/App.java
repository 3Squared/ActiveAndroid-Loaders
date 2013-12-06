package com.example.syncadapterexample;

import android.app.Application;

import com.activeandroid.ActiveAndroid;


public class App extends Application
{
	public static final String AUTHORITY = "com.example.syncadapterexample.datasync.provider";


	@Override
	public void onCreate()
	{
		super.onCreate();
		ActiveAndroid.initialize(this);
		// ContentResolver.setSyncAutomatically(App.getOrCreateSyncAccount(this), App.AUTHORITY,
		// true);
	}


}

package com.example.syncadapterexample.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.TimeUtils;
import android.text.format.Time;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.example.syncadapterexample.Post;


public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	ContentResolver mContentResolver;
	Context mContext;


	public SyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		mContext = context;
		mContentResolver = context.getContentResolver();
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs)
	{
		super(context, autoInitialize, allowParallelSyncs);
		mContentResolver = context.getContentResolver();
	}


	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult)
	{
		try
		{
			AccountManager manager = AccountManager.get(mContext);
			String authToken = manager.blockingGetAuthToken(account,
					AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);

			String userData = manager.getUserData(account, AuthenticatorActivity.PARAM_USER_PASS);
			String pass = manager.getPassword(account);
			Log.e("", "authToken:  " + authToken);
			Log.e("", "pass:  " + pass);
			Log.e("", "username:  " + account.name);


			// Simulate network
			try
			{
				Thread.sleep(TimeUnit.SECONDS.toMillis(3));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			List<Post> postsNeedingSync = new Select().from(Post.class)
					.where("needsSync == 1")
					.execute();

			ActiveAndroid.beginTransaction();
			try
			{
				for (Post post : postsNeedingSync)
				{
					post.needsSync = false;
					post.save();
				}
				ActiveAndroid.setTransactionSuccessful();
			}
			finally
			{
				ActiveAndroid.endTransaction();
			}
		}
		catch (OperationCanceledException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (AuthenticatorException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

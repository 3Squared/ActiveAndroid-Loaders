package com.example.syncadapterexample;

import java.util.List;
import java.util.UUID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.loaders.ModelLoader;
import com.activeandroid.query.Select;
import com.example.syncadapterexample.example.AccountGeneral;


public class MainActivity extends FragmentActivity
{
	AccountManager mManager;
	Account mAccount;
	PostLoader mLoader;
	Button mSyncButton;
	Button mAddButton;
	Button mDeleteOneButton;
	Button mDeleteAllButton;
	ListView mPostsView;
	Button mTagButton;
	private ContentResolver mResolver;
	private ContentObserver mObserver;
	private PostAdapter mAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mManager = AccountManager.get(this);

		mAddButton = (Button) findViewById(R.id.button1);
		mTagButton = (Button) findViewById(R.id.button3);
		mSyncButton = (Button) findViewById(R.id.button2);
		mDeleteOneButton = (Button) findViewById(R.id.button4);
		mDeleteAllButton = (Button) findViewById(R.id.button5);
		mPostsView = (ListView) findViewById(R.id.listView1);
		mAdapter = new PostAdapter(this);
		mPostsView.setAdapter(mAdapter);

		mAddButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Post post = new Post();
				post.title = UUID.randomUUID()
						.toString();
				post.needsSync = true;
				post.save();
			}
		});

		mTagButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Post post = new Select().from(Post.class)
						.orderBy("ID DESC")
						.executeSingle();
				Tag tag = new Tag();
				tag.name = UUID.randomUUID()
						.toString();
				tag.needsSync = true;
				tag.save();
				tag.post = post;
				tag.save();
				post.needsSync = true;
				post.save();
			}
		});

		mSyncButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Bundle settingsBundle = new Bundle();
				settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
				settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
				ContentResolver.requestSync(mAccount, App.AUTHORITY, settingsBundle);
			}
		});

		mDeleteOneButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Post post = new Select().from(Post.class)
						.executeSingle();
				if (post != null)
				{
					post.delete();
				}
			}
		});


		mDeleteAllButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				List<Post> posts = new Select().from(Post.class)
						.execute();
				ActiveAndroid.beginTransaction();
				try
				{
					for (Post post : posts)
					{
						post.delete();
					}
					ActiveAndroid.setTransactionSuccessful();
				}
				finally
				{
					ActiveAndroid.endTransaction();
				}
			}
		});

		final AccountManagerFuture<Bundle> future = mManager.getAuthTokenByFeatures(
				AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, null, this,
				null, null, new AccountManagerCallback<Bundle>()
				{
					@Override
					public void run(AccountManagerFuture<Bundle> future)
					{
						Bundle bnd = null;
						try
						{
							bnd = future.getResult();
							final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);

						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}, null);


		mLoader = new PostLoader();
		getSupportLoaderManager().initLoader(0, null, mLoader);


		mResolver = getContentResolver();
		mObserver = new ContentObserver(new Handler(getMainLooper()))
		{
			@Override
			public void onChange(boolean selfChange)
			{
				onChange(selfChange, null);
			}


			@Override
			public void onChange(boolean selfChange, Uri changeUri)
			{
				Bundle settingsBundle = new Bundle();
				ContentResolver.requestSync(mAccount, App.AUTHORITY, settingsBundle);
			}
		};
		mResolver.registerContentObserver(createUri(), true, mObserver);


	}


	public Uri createUri()
	{
		StringBuilder uri = new StringBuilder();
		uri.append("content://");
		uri.append(getPackageName());
		uri.append("/");
		return Uri.parse(uri.toString());
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	class PostLoader implements LoaderManager.LoaderCallbacks<List<Post>>
	{

		@Override
		public Loader<List<Post>> onCreateLoader(int id, Bundle args)
		{
			return new ModelLoader<Post>(MainActivity.this, Post.class, true);
		}


		@Override
		public void onLoadFinished(Loader<List<Post>> loader, List<Post> data)
		{
			mAdapter.clear();
			mAdapter.addAll(data);
			mAdapter.notifyDataSetChanged();

			mTagButton.setEnabled(!data.isEmpty());
			mDeleteAllButton.setEnabled(!data.isEmpty());
			mDeleteOneButton.setEnabled(!data.isEmpty());
		}


		@Override
		public void onLoaderReset(Loader<List<Post>> loader)
		{
			mAdapter.clear();
			mAdapter.notifyDataSetChanged();
		}

	}

	private static class PostAdapter extends ArrayAdapter<Post>
	{

		public PostAdapter(Context context)
		{
			super(context, android.R.layout.simple_list_item_2);
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder;

			if (convertView == null)
			{
				LayoutInflater li = (LayoutInflater) getContext().getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				convertView = li.inflate(android.R.layout.simple_list_item_2, parent, false);
				viewHolder = ViewHolder.create((TwoLineListItem) convertView);
				convertView.setTag(viewHolder);
			}
			else
			{
				viewHolder = (ViewHolder) convertView.getTag();
			}

			Post post = getItem(position);
			viewHolder.text1.setText(post.title.substring(0, 5) + " ("
					+ (post.needsSync ? "needs sync" : "synced") + ")");
			viewHolder.text2.setText(post.tags()
					.toString());
			return convertView;
		}

		private static class ViewHolder
		{
			public final TextView text1;
			public final TextView text2;


			private ViewHolder(TextView text1, TextView text2)
			{
				this.text1 = text1;
				this.text2 = text2;
			}


			public static ViewHolder create(TwoLineListItem rootView)
			{
				TextView text1 = (TextView) rootView.findViewById(android.R.id.text1);
				TextView text2 = (TextView) rootView.findViewById(android.R.id.text2);
				return new ViewHolder(text1, text2);
			}
		}
	}


	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mResolver.unregisterContentObserver(mObserver);
	}
}

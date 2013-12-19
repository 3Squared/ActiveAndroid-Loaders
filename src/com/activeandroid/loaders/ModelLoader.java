package com.activeandroid.loaders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;

import com.activeandroid.Model;
import com.activeandroid.content.ContentProvider;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;


/**
 * The Class ModelLoader.
 * 
 * @param <T>
 *            the generic type
 */
public class ModelLoader<T extends Model> extends AsyncTaskLoader<List<T>>
{
	private ContentResolver mContentResolver;
	private ContentObserver mContentObserver;
	private From mQuery;
	private List<T> mResults;
	private Class<T> mClass;
	private Object mHandle;
	private boolean mUpdateOnRelationshipChanges;


	/**
	 * Instantiates a new model loader. Will retrieve all models of the specified subclass. Will not
	 * be reloaded on relationship changes.
	 * 
	 * @param context
	 *            the model subclass you wish to query
	 * @param clazz
	 *            the clazz
	 */
	public ModelLoader(Context context, Class<T> clazz)
	{
		this(context, clazz, null, false);
	}


	/**
	 * Instantiates a new model loader. Will retrieve all models of the specified subclass.
	 * 
	 * @param context
	 *            the context
	 * @param clazz
	 *            the model subclass you wish to query
	 * @param updateOnRelationshipChanges
	 *            if true, loader will updated when tables related to the one detected are changed
	 */
	public ModelLoader(Context context, Class<T> clazz, boolean updateOnRelationshipChanges)
	{
		this(context, clazz, null, updateOnRelationshipChanges);
	}


	/**
	 * Instantiates a new model loader.
	 * 
	 * @param context
	 *            the context
	 * @param clazz
	 *            the model subclass you wish to query
	 * @param from
	 *            a select/from statement that will be executed to retrieve the objects
	 * @param updateOnRelationshipChanges
	 *            if true, loader will updated when tables related to the one detected are changed
	 */
	public ModelLoader(Context context, Class<T> clazz, From from,
			boolean updateOnRelationshipChanges)
	{
		super(context);
		mQuery = from;
		mClass = clazz;
		mContentResolver = context.getContentResolver();
		mUpdateOnRelationshipChanges = updateOnRelationshipChanges;
	}


	/**
	 * Called when there is new data to deliver to the client. The super class
	 * will take care of delivering it; the implementation here just adds a
	 * little more logic.
	 * 
	 * @param models
	 *            the new models to be delivered
	 */
	@Override
	public void deliverResult(List<T> models)
	{
		if (isReset())
		{
			// An async query came in while the loader is stopped. We
			// don't need the result.
			return;
		}

		mResults = models;

		if (isStarted())
		{
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(models);
		}

	}


	/**
	 * This is where the bulk of our work is done. This function is called in a
	 * background thread and should generate a new set of data to be published
	 * by the loader.
	 * 
	 * @return the list
	 */
	@Override
	public List<T> loadInBackground()
	{
		List<T> results;

		if (mQuery == null)
		{
			results = new Select().from(mClass)
					.execute();
		}
		else
		{
			results = mQuery.execute();
		}

		return results;
	}


	/**
	 * Handles a request to completely reset the Loader.
	 */
	@Override
	protected void onReset()
	{
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();

		mResults = null;

		// Stop monitoring for changes.
		if (mContentObserver != null)
		{
			mContentResolver.unregisterContentObserver(mContentObserver);
			ContentResolver.removeStatusChangeListener(mHandle);
			mHandle = null;
			mContentObserver = null;
		}
	}


	/**
	 * Handles a request to start the Loader.
	 */
	@Override
	protected void onStartLoading()
	{
		if (mResults != null)
		{
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(mResults);
		}

		// Start watching for changes in the job data.
		if (mContentObserver == null)
		{
			mContentObserver = new ContentObserver(new Handler(getContext().getMainLooper()))
			{

				@Override
				public void onChange(boolean selfChange)
				{
					onChange(selfChange, null);
				}


				@Override
				public void onChange(boolean selfChange, Uri uri)
				{
					onContentChanged();
				}
			};

			SyncStatusObserver statusObserver = new SyncStatusObserver()
			{

				@Override
				public void onStatusChanged(int which)
				{
					Handler handler = new Handler(getContext().getMainLooper());
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							onContentChanged();
						}
					};
					handler.post(runnable);
				}
			};

			mHandle = ContentResolver.addStatusChangeListener(
					ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, statusObserver);

			mContentResolver.registerContentObserver(ContentProvider.createUri(mClass, null), true,
					mContentObserver);

			// Register for updates from related tables
			if (mUpdateOnRelationshipChanges)
			{
				Field[] fields = mClass.getDeclaredFields();
				for (Field field : fields)
				{
					Class<?> clazz = field.getType();
					if (Model.class.isAssignableFrom(clazz))
					{
						mContentResolver.registerContentObserver(
								ContentProvider.createUri(clazz.asSubclass(Model.class), null),
								true, mContentObserver);
					}
				}

				Method[] methods = mClass.getDeclaredMethods();
				for (Method method : methods)
				{
					Class<?> returnClass = method.getReturnType();
					if (Model.class.isAssignableFrom(returnClass))
					{
						mContentResolver.registerContentObserver(
								ContentProvider.createUri(returnClass.asSubclass(Model.class), null),
								true, mContentObserver);
					}
					else
					{
						Type type = method.getGenericReturnType();
						if (type instanceof ParameterizedType)
						{
							ParameterizedType parameterizedType = (ParameterizedType) type;
							for (Type actualType : parameterizedType.getActualTypeArguments())
							{
								Class<T> genericClass = (Class<T>) actualType;
								if (Model.class.isAssignableFrom(genericClass))
								{
									mContentResolver.registerContentObserver(
											ContentProvider.createUri(
													genericClass.asSubclass(Model.class), null),
											true, mContentObserver);
								}
							}
						}
					}
				}

			}
		}

		if (takeContentChanged() || (mResults == null))
		{
			// If the data has changed since the last time it was loaded
			// or is not currently available, start a load.
			forceLoad();
		}
	}


	/**
	 * Handles a request to stop the Loader.
	 */
	@Override
	protected void onStopLoading()
	{
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}
}
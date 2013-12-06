ActiveAndroid-Loaders
====================

About
-----
This library extends [ActiveAndroid](https://github.com/pardom/ActiveAndroid) to add support for fetcing results 
using a [Loader](http://developer.android.com/reference/android/content/Loader.html). 

It adds one class, `ModelLoader`.

Also see [this pull request](https://github.com/pardom/ActiveAndroid/pull/35).

Example
-------

Say we have a model named `Post`. We can create a loader for this class which loads its results into an 
`ArrayAdapter` (which could be used to back a `ListView`) and keeps it up-to-date like so:

```java
ArrayAdapter<Post> mAdapter;

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
	}

	@Override
	public void onLoaderReset(Loader<List<Post>> loader)
	{
	  mAdapter.clear();
		mAdapter.notifyDataSetChanged();
	}
}
```
This loader will query the database for all `Post` records and add them to the adapter. By using a `ContentObserver`
the `Loader` is updated whenever a change occurs to the `Model` table.

There are 3 constructors available for creating your `ModelLoader`.

```java
/**
 * Instantiates a new model loader. Will retrieve all models of the specified subclass. Will not
 * be reloaded on relationship changes.
 * 
 * @param context
 *            the model subclass you wish to query
 * @param clazz
 *            the clazz
 */
public ModelLoader(Context context, Class<T> clazz);
```
```java
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
public ModelLoader(Context context, Class<T> clazz, boolean updateOnRelationshipChanges);
```
```java
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
public ModelLoader(Context context, Class<T> clazz, From from, boolean updateOnRelationshipChanges);
```

Sor for example you could create a loader that would only load `Posts` where a member `needsSync` was `true` like so:

```java
return new ModelLoader<Post>(MainActivity.this, 
  Post.class, 
  new Select()
    .from(Post.class)
    .where("needsSync == true"), 
  false);
```

By setting `updateOnRelationshipChanges` to `true`, the loader will also be updated if a table that the main class has
a reference to is updated - for instance, if `Post` had a 1-M relationship with a table `Tag`, the model would be updated
when members of the `Tag` class are changed. This adds some overhead, so leave it off unless you need it.



Example Project
-------

An example project is included that shows the use of `ModelLoaders` with a custom `SyncAdpater` to keep a list of data up-to-date and syncronised with a fake webservice (no network stuff occurs for real). The credentials are included in the code - you can login with:
'''
email: foo@example.com 
password: hello
'''

License
=======
This project made available under the MIT License.

Copyright (C) 2013 3Squared Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

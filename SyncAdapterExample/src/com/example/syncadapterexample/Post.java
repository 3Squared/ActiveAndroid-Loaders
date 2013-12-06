package com.example.syncadapterexample;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;


@Table(name = "Posts")
public class Post extends Model
{
	@Column(name = "title")
	public String title;

	@Column(name = "needsSync")
	public boolean needsSync;


	@Override
	public String toString()
	{
		return "\nP: " + (needsSync ? "needsSync" : "") + " / " + title.substring(0, 5) + "\n"
				+ tags();
	}


	public List<Tag> tags()
	{
		return getMany(Tag.class, "post");
	}


	public Tag getMainTag()
	{
		return tags().get(0);
	}
}

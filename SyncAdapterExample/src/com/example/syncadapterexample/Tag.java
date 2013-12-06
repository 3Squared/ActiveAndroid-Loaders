package com.example.syncadapterexample;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;


@Table(name = "Tags")
public class Tag extends Model
{
	@Column(name = "name")
	public String name;

	@Column(name = "needsSync")
	public boolean needsSync;

	@Column(name = "post", onDelete = ForeignKeyAction.CASCADE)
	public Post post;


	@Override
	public String toString()
	{
		return name.substring(0, 5);
	}

}

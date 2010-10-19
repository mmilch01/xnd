package org.nrg.fileserver;
import java.util.Collection;
import java.util.LinkedList;

public class Context extends LinkedList<ItemTag> implements TagSet
{
	public Context(ItemTag... par)
	{
		for (ItemTag it : par)
			add(it);
	}
	public Context(Collection<ItemTag> par)
	{
		addAll(par);
	}
	public ItemTag getTag(String tag)
	{
		for (ItemTag it : this)
			if (it.GetName().compareTo(tag) == 0)
				return it;
		return null;
	}
	public Context getBroaderContext(String lowestLevelName)
	{
		Context newC = new Context();
		for (ItemTag it : this)
		{
			newC.addLast(new ItemTag(it.GetName(), it.GetValues()));
			if (it.GetName().compareTo(lowestLevelName) == 0)
				break;
		}
		return newC;
	}

	public static Context fromString(String s)
	{
		String[] tags = s.split("/");
		String tmp;
		Collection<ItemTag> cit = new LinkedList<ItemTag>();
		for (int i = 1; i < tags.length; i += 2)
		{
			tmp = tags[i].substring(0, tags[i].length() - 1);
			cit.add(new ItemTag(tmp, tags[i + 1]));
		}
		return new Context(cit);
	}

	@Override
	public String toString()
	{
		String res = "";
		for (ItemTag it : this)
		{
			res += "/" + it.GetName().toLowerCase() + "s" + "/"
					+ it.GetFirstValue();
		}
		return res;
	}
}
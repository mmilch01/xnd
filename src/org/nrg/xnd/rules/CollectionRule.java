package org.nrg.xnd.rules;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.CollectionManager;
import org.nrg.fileserver.FileCollection;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.RootElement;
import org.nrg.xnd.model.TreeIterator;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.utils.MilliTimer;

public class CollectionRule extends Rule
{
	@Override
	protected boolean ParseRuleDescriptor()
	{
		return true;
	}

	private Collection<String> m_eqTags = null;
	private Collection<ResourceCollection> m_colClasses = new LinkedList<ResourceCollection>();

	public static CollectionRule DefaultCollectionRule(RepositoryViewManager rvm)
	{
		final Collection<String> tags = new LinkedList<String>();
		tags.add("Project");
		tags.add("Subject");
		tags.add("Experiment");
		tags.add("Scan");
		return new CollectionRule(tags, rvm);
	}
	public CollectionRule(Collection<String> tags, RepositoryViewManager rvm)
	{
		super(rvm);
		m_eqTags = new LinkedList<String>();
		m_eqTags.addAll(tags);
	}
	private boolean NeedToCollect(DBElement dbe)
	{
		String tv;
		for (String s : m_eqTags)
		{
			tv = dbe.GetIR().getTagValue(s);
			if (tv == null || tv.length() < 1)
				return false;
		}
		return true;
	}

	@Override
	public boolean ApplyRule(Collection<CElement> cce, IProgressMonitor ipm)
	{
		MilliTimer mt = new MilliTimer(ipm);

		boolean bMember = false;
		DBElement dbe;
		TreeIterator ti = new TreeIterator(new RootElement(cce, m_rvm),
				new TypeFilter(TypeFilter.RESOURCE, true));
		CElement ce;
		while ((ce = ti.Next()) != null)
		{
			if (!mt.Check("Analyzing selection", ce.GetLabel()))
				return false;
			if (ce instanceof DBElement)
			{
				dbe = (DBElement) ce;
				if (!NeedToCollect(dbe))
					continue;
				bMember = false;
				for (ResourceCollection rc : m_colClasses)
				{
					if (rc.Match(dbe))
					{
						bMember = true;
						rc.Add(dbe);
						break;
					}
				}
				if (!bMember)
					m_colClasses.add(new ResourceCollection(dbe));
			}
			ce.Invalidate();
		}
		Collection<DBElement> cmembers;
		ItemRecord ir, ir_first = null;
		int ind = 0;
		CollectionManager cm = m_rvm.getCM();
		FileCollection fc = null;
		for (ResourceCollection rc : m_colClasses)
		{
			ind = 0;
			if (rc.IsNonTrivial())
			{
				cmembers = rc.GetMembers();
				for (DBElement dbel : cmembers)
				{
					if (!mt.Check("Generating collections", dbel.GetLabel()))
						return false;
					ir = dbel.GetIR();
					if (ind > 0)
					{
						// ir_first.TagsMerge(ir.GetTagCollection());
						m_rvm.ItemRemove(ir);
						fc.AddFile(ir.getRelativePath());
					} else
					{
						ir_first = dbel.GetIR();
						fc = cm.CreateCollection(m_rvm.GetParentPath(ir_first
								.getRelativePath()), true);
						// new
						// LocalFileCollection(m_rvm.GetParentPath(ir_first.GetRelativePath()),true);
						fc.AddFile(ir_first.getRelativePath());
						// m_rvm.AttachCollection(ir_first, fc.GetID());
					}
					ind++;
					dbel.Invalidate();
				}
				m_rvm.AttachCollection(ir_first, fc.GetID());
				m_rvm.getCM().AddCollection(fc);
			}
		}
		return true;
	}

	class ResourceCollection
	{
		private Collection<ItemTag> m_Tags = null;
		private Collection<DBElement> m_members;

		public void ToFileCollection(FileCollection fc)
		{
			for (DBElement dbel : m_members)
				fc.AddFile(dbel.GetIR().getRelativePath());
		}

		public ResourceCollection(DBElement el)
		{
			ItemRecord ir = el.GetIR();
			m_Tags = new LinkedList<ItemTag>();
			String tv;
			for (String s : m_eqTags)
			{
				tv = ir.getTagValue(s);
				if (tv != null)
					m_Tags.add(new ItemTag(s, tv));
			}
			m_members = new LinkedList<DBElement>();
			m_members.add(el);
		}
		public boolean IsNonTrivial()
		{
			return (m_members.size() > 1);
		}
		public void Add(DBElement el)
		{
			m_members.add(el);
		}
		public Collection<DBElement> GetMembers()
		{
			return m_members;
		}
		public boolean Match(DBElement el)
		{
			ItemRecord ir = el.GetIR();
			ItemTag match;
			for (ItemTag it : m_Tags)
			{
				if ((match = ir.getTag(it.GetName())) == null)
					return false;
				if (it.CompareFully(match) != 0)
					return false;
			}
			return true;
		}
	}
}
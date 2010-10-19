package org.nrg.xnd.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class TreeIterator
{
	private TreeNode m_current = null;
	private TypeFilter m_tf = null;

	public TreeIterator(CElement el, TypeFilter tf)
	{
		m_current = new TreeNode(el, null);
		m_tf = tf;
		if (m_tf == null)
			m_tf = new TypeFilter();
	}
	public TreeIterator(Collection<CElement> cel, TypeFilter tf)
	{
		if (cel.size() < 1)
			return;
		m_tf = tf;
		Collection<CElement> celf;
		// if(m_tf!=null) celf=m_tf.Filter(cel);
		// else celf=cel;
		celf = cel;
		if (celf.size() < 1)
			return;

		if (m_tf == null)
			m_tf = new TypeFilter();

		if (celf.size() == 1)
		{
			for (CElement el : celf)
				m_current = new TreeNode(el, null);
			return;
		}
		CElement[] ch = celf.toArray(new CElement[0]);
		m_current = new TreeNode(new RootElement(celf, ch[0].getRVM()), null);
	}
	public CElement Next()
	{
		if (m_current == null)
			return null;
		if (!m_current.HasChildren())
		{
			TreeNode res = m_current;
			m_current = res.GetParent();
			return res.m_el;
		}
		if (m_current.IsIteratorExhausted())
		{
			TreeNode res = m_current;
			m_current = m_current.GetParent();
			// return Next();
			return res.m_el;
		}
		m_current = m_current.GetNextChild();
		return Next();
	}

	class TreeNode
	{
		public CElement m_el;
		private TreeNode m_parent;
		private Collection<TreeNode> m_children = null;
		private boolean m_bChildrenInited = false;
		private boolean m_bChildrenExhausted = false;
		private Iterator<TreeNode> m_iterator = null;
		// private int m_chInd=-1;

		public TreeNode GetParent()
		{
			return m_parent;
		}

		private void InitChildren()
		{
			try
			{
				if (m_bChildrenInited)
					return;
				// m_el.Invalidate();
				Collection<CElement> ch = m_el.GetChildren(m_tf, null);
				m_bChildrenInited = true;
				if (ch == null || ch.size() < 1)
				{
					m_children = null;
					return;
				}
				m_children = new LinkedList<TreeNode>();
				for (CElement ce : ch)
					m_children.add(new TreeNode(ce, this));
				m_iterator = m_children.iterator();
				// m_chInd=m_children.size()-1;
			} catch (Exception e)
			{
				return;
			}

		}
		public boolean IsIteratorExhausted()
		{
			if (m_iterator == null)
				return true;
			// if(m_chInd==-1) return true;
			return m_bChildrenExhausted;
		}
		public TreeNode GetNextChild()
		{
			// m_chInd--;
			// TreeNode res=m_children.get(m_chInd);
			TreeNode res = m_iterator.next();
			// if(m_chInd==0)
			if (!m_iterator.hasNext())
				m_bChildrenExhausted = true;
			return res;
		}
		public boolean HasChildren()
		{
			if (!m_bChildrenInited)
				InitChildren();
			if (m_children == null)
				return false;
			else
				return true;
		}

		TreeNode(CElement el, TreeNode parent)
		{
			m_el = el;
			m_parent = parent;
		}
	}
}
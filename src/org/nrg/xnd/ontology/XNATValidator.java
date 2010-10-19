package org.nrg.xnd.ontology;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpMethodBase;
import org.dom4j.io.SAXReader;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.TreeIterator;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.Validator;

public class XNATValidator extends Validator
{
	/**
	 * default modality used for fixes.
	 */
	public static String m_defaultMod = "MR";
	private XNATRestAdapter m_xre;
	public XNATValidator(Collection<CElement> ce, XNATRestAdapter xre)
	{
		super(ce);
		m_xre = xre;
	}

	@Override
	public boolean fix()
	{
		return validate(true) == null;
	}

	@Override
	public String validate(boolean bAutoFix)
	{
		String res1 = "", res2 = "";
		TreeIterator ti = new TreeIterator(m_elements, new TypeFilter(
				TypeFilter.RESOURCE, true));

		CElement el;
		DBElement dbe;
		TreeMap<String, P> corr_exp = null;
		TreeMap<String, P> fix_match = new TreeMap<String, P>();
		boolean bSubjErr = false, bModErr = false, bFixErfolg1 = bAutoFix, bFixErfolg2 = bAutoFix;
		ItemRecord ir;

		while ((el = ti.Next()) != null)
		{
			if (!(el instanceof DBElement))
				continue;
			dbe = (DBElement) el;
			ir = dbe.GetIR();
			if (ir == null)
				continue;

			// 1.check for the proj/experiment uniqueness.
			P cur_p = new P(dbe);
			if (cur_p.isExpOrBelow())
			{
				if (corr_exp == null) // init the experiment map from the remote
				// server
				{
					corr_exp = getSubjExpUIDs(cur_p.m_proj);
				}
				// this experiment value is encountered for the first time, add
				// to map and continue to next one.
				if (!corr_exp.containsKey(cur_p.m_PrExpPath))
				{
					corr_exp.put(cur_p.m_PrExpPath, cur_p);
				} else
				{
					P match_p = corr_exp.get(cur_p.m_PrExpPath);

					if (match_p.checkOverlap(cur_p) == P.SUBJ_DIFF) // problem.
					{
						if (!bSubjErr)
						{
							res1 += "- Some different subjects in project "
									+ cur_p.m_proj
									+ " have the same experiment label, which is not supported by XNAT. "
									+ "Ensure that all experiments within project have different labels before upload.\n";
							bSubjErr = true;
						}
						// check if the value is already matched; if not, create
						// a new value for experiment.
						P fix = fix_match.get(cur_p.m_PrSubjExpPath);
						if (fix == null)
						{
							fix = fixExp(cur_p, corr_exp);
							fix_match.put(cur_p.m_PrSubjExpPath, fix);
						}
						if (bAutoFix)
						{
							bFixErfolg1 &= XNDApp.app_localVM.TagAttach(ir,
									new ItemTag("Experiment", fix.m_exp));
							dbe.Invalidate();
						}
					}
				}
			}
			// 2.Check for modality.
			// update item record in case it was modified at previous step.
			ir = dbe.GetIR();
			String mod = ir.getTagValue("Modality");
			if (mod == null || mod.length() < 2)
			{
				if (!bModErr)
				{
					res2 += "- Some files don't have modality tag set. Modality tag is required for all files to upload.\n";
					bModErr = true;
				}
				if (bAutoFix)
					bFixErfolg2 &= XNDApp.app_localVM.TagAttach(ir,
							new ItemTag("Modality", m_defaultMod));
			}
		}

		if (bFixErfolg1 && bFixErfolg2)
			return null;
		String res = (bFixErfolg1 ? "" : res1) + (bFixErfolg2 ? "" : res2);
		return res.length() > 0 ? res : null;
	}

	private TreeMap<String, P> getSubjExpUIDs(String proj)
	{
		// a special request that would return experiment labels+subject labels
		// all in one:
		// http://cninds05l:8080/cnda_xnat/REST/experiments?
		// format=xml&xsiType=xnat:subjectAssessorData&project=test1

		TreeMap<String, P> res = new TreeMap<String, P>();
		HttpMethodBase get = null;
		try
		{
			String q = "experiments"
					+ "?xsiType=xnat:subjectAssessorData&project=" + proj;
			get = m_xre.PerformConnection(XNATRestAdapter.GET, q, null);
			if (get == null)
				return res;
			LinkedList<TreeMap<String, String>> row_map = XNATTableParser
					.GetRows(new SAXReader()
							.read(get.getResponseBodyAsStream()));
			get.releaseConnection();
			String subj, exp;
			for (TreeMap<String, String> row : row_map)
			{
				subj = row.get("subject_label");
				exp = row.get("label");
				if (subj == null || exp == null)
					continue;
				res.put(proj + "/" + exp, new P(proj, subj, exp));
			}
		} catch (Exception e)
		{
		} finally
		{
			if (get != null)
				get.releaseConnection();
		}

		return res;
	}

	private P fixExp(P p, TreeMap<String, P> tm)
	{
		int ind = 0;
		String new_exp = p.m_exp + "_0";
		while (tm.containsKey(p.m_proj + "/" + new_exp))
		{
			ind++;
			new_exp = p.m_exp + "_" + ind;
		}
		P res = new P(p.m_proj, p.m_subj, new_exp);
		tm.put(p.m_proj + "/" + new_exp, res);
		return res;
	}
	private String getUIDStr(DBElement dbe)
	{
		ItemRecord ir = dbe.GetIR();
		if (ir == null)
			return null;
		String pr = ir.getTagValue("Project"), exp = ir
				.getTagValue("Experiment");
		if (pr == null || exp == null)
			return null;
		if (pr.length() < 1 || exp.length() < 1)
			return null;
		return pr + "/" + exp;
	}
	private class P
	{
		public String m_subj = "";
		public String m_proj = "";
		public String m_exp = "";
		public String m_PrExpPath = "";
		public String m_PrSubjExpPath = "";
		public static final int NONE = 0, FULL = 1, SUBJ_DIFF = 2;

		public P(String proj, String sub, String exp)
		{
			m_proj = proj;
			m_subj = sub;
			m_exp = exp;
			init();
		}

		public P(DBElement dbe)
		{
			ItemRecord ir = dbe.GetIR();
			m_subj = ir.getTagValue("Subject");
			m_proj = ir.getTagValue("Project");
			m_exp = ir.getTagValue("Experiment");
			init();
		}
		private void init()
		{
			if (m_subj == null)
				m_subj = "";
			if (m_proj == null)
				m_proj = "";
			if (m_exp == null)
				m_exp = "";
			m_PrExpPath = m_proj + "/" + m_exp;
			m_PrSubjExpPath = m_proj + "/" + m_subj + "/" + m_exp;
		}
		public boolean isExpOrBelow()
		{
			if (m_subj == null || m_proj == null || m_exp == null)
				return false;
			if (m_subj.length() < 1 || m_proj.length() < 1
					|| m_exp.length() < 1)
				return false;
			return true;
		}
		public int checkOverlap(P p)
		{
			if (m_proj.compareTo(p.m_proj) != 0
					|| m_exp.compareTo(p.m_exp) != 0)
				return NONE;

			if (m_subj.compareTo(p.m_subj) == 0)
				return FULL;
			else
				return SUBJ_DIFF;
		}
	}
}
package org.nrg.xnd.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexExtractor
{
	public static void main(String[] args)
	{
		String txt = "rqnf123_t89_ab.24";// "range123_t89_ab.24";

		String re1 = ".*?"; // Non-greedy match on filler
		String re2 = "[a-z]"; // Uninteresting: w
		String re3 = ".*?"; // Non-greedy match on filler
		String re4 = "[a-z]"; // Uninteresting: w
		String re5 = ".*?"; // Non-greedy match on filler
		String re6 = "[a-z]"; // Uninteresting: w
		String re7 = ".*?"; // Non-greedy match on filler
		String re8 = "[a-z]"; // Uninteresting: w
		String re9 = "([a-z])"; // Any Single Word Character (Not Whitespace) 1

		Pattern p = Pattern.compile(re1 + re2 + re3 + re4 + re5 + re6 + re7
				+ re8 + re9, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = p.matcher(txt);
		if (m.find())
		{
			String w1 = m.group(1);
			System.out.print("(" + w1.toString() + ")" + "\n");
		}
	}

}
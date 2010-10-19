package org.nrg.xnd.tools.ImageViewer.ip;

public final class Interpolate
{
	static final byte InterpolateKeys = 0, InterpolateSinc = 1,
			InterpolateOleg = 2, InterpolateNone = 3;

	protected int[] m_clut = null;

	// interpolation initialization
	{
		// InterpolationType = InterpolateOleg; //??
		byte InterpolationType = InterpolateKeys; // set to
		final int sShift = 7, aShift = 10;
		final int sV = 1 << sShift, aV = 1 << aShift;
		final int sV2 = 2 * sV;
		m_clut = new int[sV2 + 1];
		// ??if(iType != InterpolationType) // New interpolation LUT
		double x, a = -0.5;
		// Default: coefficients for Moms cubic interpolation (too
		// smooth)
		double a0 = 0.5, a1 = -1.0, a2 = 1.0 / 14.0, a3 = 13.0 / 21.0;
		double b0 = -1.0 / 6.0, b1 = 1.0, b2 = -85.0 / 42.0, b3 = 29.0 / 21.0;
		if (InterpolationType == InterpolateKeys)
		{
			a0 = a + 2;
			a1 = -(a + 3);
			a2 = 0;
			a3 = 1;
			b0 = a;
			b1 = -5 * a;
			b2 = 8 * a;
			b3 = -4 * a;
		}
		// Scaled coefficients
		a0 *= aV;
		a1 *= aV;
		a2 *= aV;
		a3 *= aV;
		b0 *= aV;
		b1 *= aV;
		b2 *= aV;
		b3 *= aV;
		a3 += 0.5;
		b3 += 0.5; // before integer truncation

		// Lookup values
		for (int n = 0; n <= sV2; n++)
		{
			x = ((double) n) / sV;
			if (InterpolationType == InterpolateSinc)
			{
				double xpi = x * Math.PI;
				if (xpi < 0.02)
					x = 1.0 - (xpi * xpi) / 6.0;
				else
					x = Math.sin(xpi) / xpi;
				m_clut[n] = (int) (0.5 + aV * x);
			} else if (InterpolationType == InterpolateOleg)
			{
				if (n < sV)
					x = 1.0 + 0.2428771 * x - 0.2345586328E1 * x * x
							+ 0.1102709226E1 * x * x * x;
				else
					x = 0.2464148272E1 - 0.4416957938E1 * x + 0.2313177431E1
							* x * x - 0.360367765 * x * x * x;
				m_clut[n] = (int) (0.5 + aV * x);
			} else
			// Cubic polynomial
			{
				if (n < sV)
					m_clut[n] = (int) (((a0 * x + a1) * x + a2) * x + a3);
				else
					m_clut[n] = (int) (((b0 * x + b1) * x + b2) * x + b3);
			}
		}
		// iType = InterpolationType;
	}

	public int[] getClut()
	{
		return m_clut;
	}
}

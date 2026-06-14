import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Java implementation of SchemeHIBME.
 * Converted from SchemeHIBME.py line by line.
 */
@SuppressWarnings("unchecked")
public class SchemeHIBME
{
	private static final int DefaultL = 30;
	
	private int l;
	private Pairing group;
	private Field<Element> G1, G2, ZR;
	private int operand;
	private Object[] mpk, msk;
	private boolean flag = false;
	
	public SchemeHIBME(Pairing pairing)
	{
		this.group = pairing;
		this.G1 = pairing.getG1();
		this.G2 = pairing.getG2();
		this.ZR = pairing.getZr();
		this.operand = (1 << 512) - 1;
		this.l = DefaultL;
	}
	
	public SchemeHIBME()
	{
		this(initPairing());
	}
	
	private static Pairing initPairing()
	{
		TypeACurveGenerator pg = new TypeACurveGenerator(512, 512);
		PairingParameters params = pg.generate();
		return PairingFactory.getPairing(params);
	}
	
	private Element product(Element[] elements)
	{
		try
		{
			if (elements != null && elements.length > 0)
			{
				Element result = elements[0].duplicate().getImmutable();
				for (int i = 1; i < elements.length; i++)
					result = result.mul(elements[i]).getImmutable();
				return result;
			}
			else
			{
				return ZR.newOneElement();
			}
		}
		catch (Exception e)
		{
			return ZR.newOneElement();
		}
	}
	
	@SuppressWarnings("unused")
	private Element productG1(Element[] elements)
	{
		try
		{
			if (elements != null && elements.length > 0)
			{
				Element result = elements[0].duplicate().getImmutable();
				for (int i = 1; i < elements.length; i++)
					result = result.mul(elements[i]).getImmutable();
				return result;
			}
			else
			{
				return G1.newOneElement();
			}
		}
		catch (Exception e)
		{
			return G1.newOneElement();
		}
	}
	
	private Element productG2(Element[] elements)
	{
		try
		{
			if (elements != null && elements.length > 0)
			{
				Element result = elements[0].duplicate().getImmutable();
				for (int i = 1; i < elements.length; i++)
					result = result.mul(elements[i]).getImmutable();
				return result;
			}
			else
			{
				return G2.newOneElement();
			}
		}
		catch (Exception e)
		{
			return G2.newOneElement();
		}
	}
	
	// HHat: hash Element to int
	private int HHat(Element x)
	{
		try
		{
			MessageDigest sha = MessageDigest.getInstance("SHA-512");
			byte[] digest = sha.digest(x.toBytes());
			int result = 0;
			for (int i = 0; i < Math.min(4, digest.length); i++)
				result = (result << 8) | (digest[i] & 0xFF);
			return result & operand;
		}
		catch (NoSuchAlgorithmException e)
		{
			return 0;
		}
	}
	
	public Object[][] Setup(int l)
	{
		flag = false;
		if (isInt(l) && l >= 3)
		{
			this.l = l;
		}
		else
		{
			this.l = DefaultL;
			System.out.println("Setup: The variable l should be a positive integer not smaller than 3, but it is not, which has been defaulted to " + DefaultL + ".");
		}
		
		Element g = G1.newOneElement().getImmutable();
		Element alpha = ZR.newRandomElement().getImmutable();
		Element b1 = ZR.newRandomElement().getImmutable();
		Element b2 = ZR.newRandomElement().getImmutable();
		
		Element[] s = new Element[l];
		Element[] a = new Element[l];
		for (int i = 0; i < l; i++)
		{
			s[i] = ZR.newRandomElement().getImmutable();
			a[i] = ZR.newRandomElement().getImmutable();
		}
		
		Element g2 = G1.newRandomElement().getImmutable();
		Element g3 = G1.newRandomElement().getImmutable();
		
		Element[] h = new Element[l];
		for (int i = 0; i < l; i++)
			h[i] = G1.newRandomElement().getImmutable();
		
		Element g1 = g.powZn(alpha).getImmutable();
		Element A = group.pairing(g1, g2).getImmutable();
		Element gBar = g.powZn(b1).getImmutable();
		Element gTilde = g.powZn(b2).getImmutable();
		Element g3Bar = g3.powZn(b1.duplicate().invert()).getImmutable();
		Element g3Tilde = g3.powZn(b2.duplicate().invert()).getImmutable();
		
		// Build mpk: g, g1, g2, g3, gBar, gTilde, g3Bar, g3Tilde, h[0..l-1], A
		Object[] mpkParts = new Object[8 + l + 1];
		mpkParts[0] = g;
		mpkParts[1] = g1;
		mpkParts[2] = g2;
		mpkParts[3] = g3;
		mpkParts[4] = gBar;
		mpkParts[5] = gTilde;
		mpkParts[6] = g3Bar;
		mpkParts[7] = g3Tilde;
		for (int i = 0; i < l; i++)
			mpkParts[8 + i] = h[i];
		mpkParts[8 + l] = A;
		mpk = mpkParts;
		
		// Build msk: g2^alpha, b1, b2, s[0..l-1], a[0..l-1]
		Object[] mskParts = new Object[3 + l + l];
		mskParts[0] = g2.powZn(alpha).getImmutable();
		mskParts[1] = b1;
		mskParts[2] = b2;
		for (int i = 0; i < l; i++)
			mskParts[3 + i] = s[i];
		for (int i = 0; i < l; i++)
			mskParts[3 + l + i] = a[i];
		msk = mskParts;
		
		flag = true;
		return new Object[][]{mpk, msk};
	}
	
	public Object[] EKGen(Element[] ID_k)
	{
		if (!flag) Setup(l);
		
		@SuppressWarnings("unused")
		Element[] H1 = null; // H1 is not saved; we compute hash on the fly
		Element[] sArr = new Element[l];
		Element[] aArr = new Element[l];
		for (int i = 0; i < l; i++)
		{
			sArr[i] = (Element) msk[3 + i];
			aArr[i] = (Element) msk[3 + l + i];
		}
		int k = ID_k.length;
		
		Element Ak = ZR.newOneElement().getImmutable();
		for (int j = 0; j < k; j++)
			Ak = Ak.mul(aArr[j]).getImmutable();
		
		Element[] ek1 = new Element[k];
		for (int i = 0; i < k; i++)
		{
			byte[] idBytes = ID_k[i].toBytes();
			Element H1_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
			ek1[i] = H1_val.powZn(sArr[i].mul(Ak)).getImmutable();
		}
		
		Element[] ek2 = new Element[l - k];
		for (int i = 0; i < l - k; i++)
			ek2[i] = sArr[k + i].mul(Ak).getImmutable();
		
		Element[] ek3 = new Element[l - k];
		for (int i = 0; i < l - k; i++)
			ek3[i] = aArr[k + i].getImmutable();
		
		return new Object[]{ek1, ek2, ek3};
	}
	
	public Object[] DKGen(Element[] ID_k)
	{
		if (!flag) Setup(l);
		
		Element g = (Element) mpk[0];
		Element g3Bar = (Element) mpk[6];
		Element g3Tilde = (Element) mpk[7];
		Element[] h = new Element[l];
		for (int i = 0; i < l; i++)
			h[i] = (Element) mpk[8 + i];
		
		Element g2ToThePowerOfAlpha = (Element) msk[0];
		Element b1 = (Element) msk[1];
		Element b2 = (Element) msk[2];
		Element[] sArr = new Element[l];
		Element[] aArr = new Element[l];
		for (int i = 0; i < l; i++)
		{
			sArr[i] = (Element) msk[3 + i];
			aArr[i] = (Element) msk[3 + l + i];
		}
		int k = ID_k.length;
		
		Element r = ZR.newRandomElement().getImmutable();
		
		// HI = prod h[i]^{ID_k[i]}
		Element HI = G1.newOneElement().getImmutable();
		for (int i = 0; i < k; i++)
			HI = HI.mul(h[i].powZn(ID_k[i])).getImmutable();
		
		Element a0 = g2ToThePowerOfAlpha.powZn(b1.duplicate().invert())
			.mul(HI.powZn(r.duplicate().mul(b1.duplicate().invert())))
			.mul(g3Bar.powZn(r)).getImmutable();
		Element a1 = g2ToThePowerOfAlpha.powZn(b2.duplicate().invert())
			.mul(HI.powZn(r.duplicate().mul(b2.duplicate().invert())))
			.mul(g3Tilde.powZn(r)).getImmutable();
		Element gr = g.powZn(r).getImmutable();
		
		Element Ak = ZR.newOneElement().getImmutable();
		for (int j = 0; j < k; j++)
			Ak = Ak.mul(aArr[j]).getImmutable();
		
		// dk1 components
		Element[] h_k_b1inv = new Element[l - k];
		Element[] h_k_b2inv = new Element[l - k];
		Element[] h_b1inv = new Element[l - k];
		Element[] h_b2inv = new Element[l - k];
		for (int i = 0; i < l - k; i++)
		{
			h_k_b1inv[i] = h[k + i].powZn(r.duplicate().mul(b1.duplicate().invert())).getImmutable();
			h_k_b2inv[i] = h[k + i].powZn(r.duplicate().mul(b2.duplicate().invert())).getImmutable();
			h_b1inv[i] = h[k + i].powZn(b1.duplicate().invert()).getImmutable();
			h_b2inv[i] = h[k + i].powZn(b2.duplicate().invert()).getImmutable();
		}
		Element HI_b1inv = HI.powZn(b1.duplicate().invert()).getImmutable();
		Element HI_b2inv = HI.powZn(b2.duplicate().invert()).getImmutable();
		
		// Build dk1 tuple
		Element[] dk1 = new Element[((l - k) << 2) + 5];
		dk1[0] = a0;
		dk1[1] = a1;
		dk1[2] = gr;
		int idx = 3;
		for (int i = 0; i < l - k; i++) dk1[idx++] = h_k_b1inv[i];
		for (int i = 0; i < l - k; i++) dk1[idx++] = h_k_b2inv[i];
		for (int i = 0; i < l - k; i++) dk1[idx++] = h_b1inv[i];
		for (int i = 0; i < l - k; i++) dk1[idx++] = h_b2inv[i];
		dk1[idx++] = HI_b1inv;
		dk1[idx++] = HI_b2inv;
		
		// dk2[i] = H2(ID_k[i])^{s[i] * Ak}
		Element[] dk2 = new Element[k];
		for (int i = 0; i < k; i++)
		{
			byte[] idBytes = ID_k[i].toBytes();
			Element H2_val = G2.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
			dk2[i] = H2_val.powZn(sArr[i].mul(Ak)).getImmutable();
		}
		
		Element[] dk3 = new Element[l - k];
		for (int i = 0; i < l - k; i++)
			dk3[i] = sArr[k + i].mul(Ak).getImmutable();
		
		Element[] dk4 = new Element[l - k];
		for (int i = 0; i < l - k; i++)
			dk4[i] = aArr[k + i].getImmutable();
		
		return new Object[]{dk1, dk2, dk3, dk4};
	}
	
	public Object[] Enc(Object[] ek_ID_S, Element[] ID_Snd, Element[] ID_Rev, int message)
	{
		if (!flag) Setup(l);
		
		Element g = (Element) mpk[0];
		Element g3 = (Element) mpk[3];
		Element gBar = (Element) mpk[4];
		Element gTilde = (Element) mpk[5];
		Element[] h = new Element[l];
		for (int i = 0; i < l; i++)
			h[i] = (Element) mpk[8 + i];
		Element A = (Element) mpk[8 + l];
		
		Element[] sArr = new Element[l];
		Element[] aArr = new Element[l];
		for (int i = 0; i < l; i++)
		{
			sArr[i] = (Element) msk[3 + i];
			aArr[i] = (Element) msk[3 + l + i];
		}
		
		int n = ID_Snd.length;
		int m = ID_Rev.length;
		
		Element s1 = ZR.newRandomElement().getImmutable();
		Element s2 = ZR.newRandomElement().getImmutable();
		Element eta = ZR.newRandomElement().getImmutable();
		
		Element T = A.powZn(s1.duplicate().add(s2)).getImmutable();
		
		Element K;
		if (m == n)
		{
			Element[] ek1 = (Element[]) ek_ID_S[0];
			Element[] pairTerms = new Element[n];
			for (int i = 0; i < n; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				Element H2Rev = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
				Element gEta = g.powZn(eta);
				Element left = gEta.mul(ek1[i]).getImmutable();
				pairTerms[i] = group.pairing(left, H2Rev).getImmutable();
			}
			K = product(pairTerms);
		}
		else if (m > n)
		{
			Element An = ZR.newOneElement().getImmutable();
			for (int j = 0; j < n; j++) An = An.mul(aArr[j]).getImmutable();
			Element Bmn = ZR.newOneElement().getImmutable();
			for (int j = n; j < m; j++) Bmn = Bmn.mul(aArr[j]).getImmutable();
			
			Element[] ek1 = (Element[]) ek_ID_S[0];
			Element[] pairTerms1 = new Element[n];
			Element[] H2RevFirstN = new Element[n];
			for (int i = 0; i < n; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				H2RevFirstN[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
				pairTerms1[i] = group.pairing(ek1[i], H2RevFirstN[i]).getImmutable();
			}
			Element first = product(pairTerms1);
			
			Element[] pairTerms2 = new Element[m - n];
			for (int i = n; i < m; i++)
			{
				byte[] snBytes = ID_Snd[n - 1].toBytes();
				Element H1Sn = G1.newElementFromHash(snBytes, 0, snBytes.length).getImmutable();
				byte[] revBytes = ID_Rev[i].toBytes();
				Element H2RevI = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
				pairTerms2[i - n] = group.pairing(H1Sn, H2RevI).powZn(sArr[i].mul(An)).getImmutable();
			}
			Element second = product(pairTerms2);
			
			Element inner = first.mul(second).powZn(Bmn).getImmutable();
			
			Element[] allH2Rev = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				allH2Rev[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
			}
			Element prodH2Rev = productG2(allH2Rev);
			Element third = group.pairing(g.powZn(eta), prodH2Rev);
			
			K = inner.mul(third).getImmutable();
		}
		else // m < n
		{
			Element[] ek1 = (Element[]) ek_ID_S[0];
			Element[] pairTerms1 = new Element[m];
			Element[] H2RevFirstM = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				H2RevFirstM[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
				pairTerms1[i] = group.pairing(ek1[i], H2RevFirstM[i]).getImmutable();
			}
			Element first = product(pairTerms1);
			
			byte[] revLastBytes = ID_Rev[m - 1].toBytes();
			Element H2RevLast = G2.newElementFromHash(revLastBytes, 0, revLastBytes.length).getImmutable();
			
			Element[] pairTerms2 = new Element[n - m];
			for (int i = m; i < n; i++)
			{
				pairTerms2[i - m] = group.pairing(ek1[i], H2RevLast).getImmutable();
			}
			Element second = product(pairTerms2);
			
			Element[] allH2Rev = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				allH2Rev[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
			}
			Element prodH2Rev = productG2(allH2Rev);
			Element third = group.pairing(g.powZn(eta), prodH2Rev);
			
			K = first.mul(second).mul(third).getImmutable();
		}
		
		int C1_val = message ^ HHat(T) ^ HHat(K);
		Element C1 = ZR.newElement(C1_val & operand).getImmutable();
		Element C2 = gBar.powZn(s1).getImmutable();
		Element C3 = gTilde.powZn(s2).getImmutable();
		
		Element HI_Snd = G1.newOneElement().getImmutable();
		for (int i = 0; i < n; i++)
			HI_Snd = HI_Snd.mul(h[i].powZn(ID_Snd[i])).getImmutable();
		Element C4 = (HI_Snd.mul(g3)).powZn(s1.duplicate().add(s2)).getImmutable();
		Element C5 = g.powZn(eta).getImmutable();
		
		return new Object[]{C1, C2, C3, C4, C5};
	}
	
	public int Dec(Object[] dk_ID_R, Element[] ID_Rev, Element[] ID_Snd, Object[] CT)
	{
		if (!flag) Setup(l);
		
		Element[] sArr = new Element[l];
		Element[] aArr = new Element[l];
		for (int i = 0; i < l; i++)
		{
			sArr[i] = (Element) msk[3 + i];
			aArr[i] = (Element) msk[3 + l + i];
		}
		
		Element C1 = (Element) CT[0];
		Element C2 = (Element) CT[1];
		Element C3 = (Element) CT[2];
		Element C4 = (Element) CT[3];
		Element C5 = (Element) CT[4];
		Element[] dk1 = (Element[]) dk_ID_R[0];
		Element[] dk2 = (Element[]) dk_ID_R[1];
		
		int m = ID_Rev.length;
		int n = ID_Snd.length;
		
		Element TPrime = group.pairing(dk1[2], C4)
			.div(group.pairing(C2, dk1[0]).mul(group.pairing(C3, dk1[1]))).getImmutable();
		
		Element KPrime;
		if (m == n)
		{
			Element[] pairTerms = new Element[n];
			for (int i = 0; i < n; i++)
			{
				byte[] sndBytes = ID_Snd[i].toBytes();
				Element H1Sn = G1.newElementFromHash(sndBytes, 0, sndBytes.length).getImmutable();
				pairTerms[i] = group.pairing(H1Sn, dk2[i]).getImmutable();
			}
			Element first = product(pairTerms);
			
			Element[] H2Rev = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				H2Rev[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
			}
			Element second = group.pairing(C5, productG2(H2Rev));
			
			KPrime = first.mul(second).getImmutable();
		}
		else if (m > n)
		{
			Element[] pairTerms = new Element[m];
			for (int i = 0; i < n; i++)
			{
				byte[] sndBytes = ID_Snd[i].toBytes();
				Element H1Sn = G1.newElementFromHash(sndBytes, 0, sndBytes.length).getImmutable();
				pairTerms[i] = group.pairing(H1Sn, dk2[i]).getImmutable();
			}
			for (int i = n; i < m; i++)
			{
				byte[] sndN_1Bytes = ID_Snd[n - 1].toBytes();
				Element H1Sn_1 = G1.newElementFromHash(sndN_1Bytes, 0, sndN_1Bytes.length).getImmutable();
				pairTerms[i] = group.pairing(H1Sn_1, dk2[i]).getImmutable();
			}
			Element first = product(pairTerms);
			
			Element[] H2Rev = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				H2Rev[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
			}
			Element second = group.pairing(C5, productG2(H2Rev));
			
			KPrime = first.mul(second).getImmutable();
		}
		else // m < n
		{
			Element Am = ZR.newOneElement().getImmutable();
			for (int j = 0; j < m; j++) Am = Am.mul(aArr[j]).getImmutable();
			Element Bnm = ZR.newOneElement().getImmutable();
			for (int j = m; j < n; j++) Bnm = Bnm.mul(aArr[j]).getImmutable();
			
			Element[] pairTerms1 = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] sndBytes = ID_Snd[i].toBytes();
				Element H1Sn = G1.newElementFromHash(sndBytes, 0, sndBytes.length).getImmutable();
				pairTerms1[i] = group.pairing(H1Sn, dk2[i]).getImmutable();
			}
			Element first = product(pairTerms1);
			
			byte[] revLastBytes = ID_Rev[m - 1].toBytes();
			Element H2RevLast = G2.newElementFromHash(revLastBytes, 0, revLastBytes.length).getImmutable();
			
			Element[] pairTerms2 = new Element[n - m];
			for (int i = m; i < n; i++)
			{
				byte[] sndBytes = ID_Snd[i].toBytes();
				Element H1Sn = G1.newElementFromHash(sndBytes, 0, sndBytes.length).getImmutable();
				pairTerms2[i - m] = group.pairing(H1Sn, H2RevLast).powZn(sArr[i].mul(Am)).getImmutable();
			}
			Element second = product(pairTerms2);
			
			Element inner = first.mul(second).powZn(Bnm).getImmutable();
			
			Element[] H2Rev = new Element[m];
			for (int i = 0; i < m; i++)
			{
				byte[] revBytes = ID_Rev[i].toBytes();
				H2Rev[i] = G2.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
			}
			Element third = group.pairing(C5, productG2(H2Rev));
			
			KPrime = inner.mul(third).getImmutable();
		}
		
		int C1_int = 0;
		byte[] c1Bytes = C1.toBytes();
		for (int i = 0; i < Math.min(4, c1Bytes.length); i++)
			C1_int = (C1_int << 8) | (c1Bytes[i] & 0xFF);
		int M = C1_int ^ HHat(TPrime) ^ HHat(KPrime);
		return M & operand;
	}
	
	public long getLengthOf(Object obj)
	{
		if (obj instanceof Element)
			return ((Element) obj).toBytes().length;
		else if (obj instanceof Object[])
		{
			long total = 0;
			for (Object o : (Object[]) obj)
				total += getLengthOf(o);
			return total;
		}
		else if (obj instanceof Integer)
			return 64;
		else
			return 0;
	}
	
	private boolean isInt(Object x)
	{
		return x instanceof Integer || x instanceof BigInteger;
	}
}
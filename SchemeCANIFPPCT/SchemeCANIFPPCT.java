import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import java.math.BigInteger;
import java.util.List;

/**
 * Java implementation of SchemeCANIFPPCT.
 * Converted from SchemeCANIFPPCT.py line by line.
 */
@SuppressWarnings("unchecked")
public class SchemeCANIFPPCT
{
	private static final int DefaultN = 30;
	private static final int DefaultM = 10;
	
	private int n, m;
	private Pairing group;
	private Field<Element> G1, G2, ZR;
	private Object[] bpk, bsk;
	private boolean bFlag = false;
	private Object[] mpk, msk;
	private boolean flag = false;
	
	public SchemeCANIFPPCT(Pairing pairing)
	{
		this.group = pairing;
		this.G1 = pairing.getG1();
		this.G2 = pairing.getG2();
		this.ZR = pairing.getZr();
		this.n = DefaultN;
		this.m = DefaultM;
	}
	
	public SchemeCANIFPPCT()
	{
		this(initPairing());
	}
	
	private static Pairing initPairing()
	{
		TypeACurveGenerator pg = new TypeACurveGenerator(512, 512);
		PairingParameters params = pg.generate();
		return PairingFactory.getPairing(params);
	}
	
	@SuppressWarnings("unused")
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
	
	private byte[] concatBytes(byte[]... parts)
	{
		int total = 0;
		for (byte[] p : parts) total += p.length;
		byte[] result = new byte[total];
		int offset = 0;
		for (byte[] p : parts)
		{
			System.arraycopy(p, 0, result, offset, p.length);
			offset += p.length;
		}
		return result;
	}
	
	// computeCoefficients: given roots VVec, compute aVec such that
	// f(x) = prod_i (x - V_i) = a_0 + a_1*x + ... + a_{n-1}*x^{n-1} + x^n
	private Element[] computeCoefficients(Element[] roots)
	{
		int n = roots.length;
		Element[] coefficients = new Element[n + 1];
		coefficients[n - 1] = roots[0].duplicate().getImmutable();
		coefficients[n] = ZR.newOneElement().getImmutable();
		
		int cnt = n - 2;
		for (int rIdx = 1; rIdx < n; rIdx++)
		{
			Element r = roots[rIdx];
			coefficients[cnt] = r.duplicate().mul(coefficients[cnt + 1]).getImmutable();
			for (int i = cnt + 1; i < n - 1; i++)
			{
				coefficients[i] = coefficients[i].add(r.duplicate().mul(coefficients[i + 1])).getImmutable();
			}
			coefficients[n - 1] = coefficients[n - 1].add(r).getImmutable();
			cnt--;
		}
		for (int i = n - 1; i >= 0; i -= 2)
		{
			coefficients[i] = coefficients[i].negate().getImmutable();
		}
		return coefficients;
	}
	
	private Element computePolynomial(Element x, Element[] coefficients)
	{
		if (coefficients != null && coefficients.length > 0)
		{
			int n = coefficients.length - 1;
			Element eleResult = coefficients[0].duplicate().getImmutable();
			for (int i = 1; i < n; i++)
			{
				Element eResult = x.duplicate().getImmutable();
				for (int k = 1; k < i; k++)
					eResult = eResult.mul(x).getImmutable();
				eleResult = eleResult.add(coefficients[i].duplicate().mul(eResult)).getImmutable();
			}
			Element eResult = x.duplicate().getImmutable();
			for (int k = 1; k < n; k++)
				eResult = eResult.mul(x).getImmutable();
			eleResult = eleResult.add(eResult).getImmutable();
			return eleResult;
		}
		else
		{
			return null;
		}
	}
	
	public Object[][] BSetup(int n, int m)
	{
		bFlag = false;
		if (isInt(n) && isInt(m) && 1 <= m && m <= n)
		{
			this.n = n;
			this.m = m;
		}
		else
		{
			this.n = DefaultN;
			this.m = DefaultM;
			System.out.println("BSetup: The variables n and m should be two positive integers satisfying 1 <= m <= n but they are not, which have been defaulted to " + DefaultN + " and " + DefaultM + ", respectively.");
		}
		
		Element g = G1.newOneElement().getImmutable();
		Element g1 = G1.newRandomElement().getImmutable();
		Element omega = ZR.newRandomElement().getImmutable();
		Element t1 = ZR.newRandomElement().getImmutable();
		Element t2 = ZR.newRandomElement().getImmutable();
		Element t3 = ZR.newRandomElement().getImmutable();
		Element t4 = ZR.newRandomElement().getImmutable();
		
		Element Omega = group.pairing(g, g).powZn(t1.mul(t2).mul(omega)).getImmutable();
		Element v1 = g.powZn(t1).getImmutable();
		Element v2 = g.powZn(t2).getImmutable();
		Element v3 = g.powZn(t3).getImmutable();
		Element v4 = g.powZn(t4).getImmutable();
		
		bpk = new Object[]{g1, Omega, v1, v2, v3, v4};
		bsk = new Object[]{omega, t1, t2, t3, t4};
		
		bFlag = true;
		return new Object[][]{bpk, bsk};
	}
	
	public Element BKGen()
	{
		if (!bFlag) BSetup(n, m);
		
		Element k_i = ZR.newRandomElement().getImmutable();
		Element bsk_ID_i = k_i;
		return bsk_ID_i;
	}
	
	public Object[] BEncryption(byte[] TP_i, Element[] s, Element s_i)
	{
		if (!bFlag) BSetup(n, m);
		
		if (TP_i == null)
		{
			byte[] randBytes = new byte[64];
			new java.security.SecureRandom().nextBytes(randBytes);
			TP_i = randBytes;
			System.out.println("BEncryption: The variable TP_i should be a bytes object, but it is not, which has been generated randomly.");
		}
		
		Element g1 = (Element) bpk[0];
		Element Omega = (Element) bpk[1];
		Element v1 = (Element) bpk[2];
		Element v2 = (Element) bpk[3];
		Element v3 = (Element) bpk[4];
		Element v4 = (Element) bpk[5];
		
		Element s1_i = ZR.newRandomElement().getImmutable();
		Element s2_i = ZR.newRandomElement().getImmutable();
		
		Element[] VVec = new Element[n];
		for (int i = 0; i < n; i++)
		{
			byte[] omegaBytes = Omega.powZn(s[i]).toBytes();
			VVec[i] = ZR.newElementFromHash(omegaBytes, 0, omegaBytes.length).getImmutable();
		}
		
		Element H1_TP = G1.newElementFromHash(TP_i, 0, TP_i.length).getImmutable();
		Element C0_i = (g1.duplicate().mul(H1_TP)).powZn(s_i).getImmutable();
		Element C1_i = v1.powZn(s_i.duplicate().sub(s1_i)).getImmutable();
		Element C2_i = v2.powZn(s1_i).getImmutable();
		Element C3_i = v3.powZn(s_i.duplicate().sub(s2_i)).getImmutable();
		Element C4_i = v4.powZn(s2_i).getImmutable();
		
		Element[] aVec = computeCoefficients(VVec);
		
		return new Object[]{new Element[]{C0_i, C1_i, C2_i, C3_i, C4_i}, aVec};
	}
	
	public Object[] BTrapdoorGen(byte[] QTP_i, Element bsk_ID_i)
	{
		if (!bFlag) BSetup(n, m);
		
		if (QTP_i == null)
		{
			byte[] randBytes = new byte[64];
			new java.security.SecureRandom().nextBytes(randBytes);
			QTP_i = randBytes;
			System.out.println("BTrapdoorGen: The variable QTP_i should be a bytes object, but it is not, which has been generated randomly.");
		}
		if (bsk_ID_i == null || !bsk_ID_i.getField().equals(ZR))
		{
			bsk_ID_i = BKGen();
			System.out.println("BTrapdoorGen: The variable bsk_ID_i should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element g1 = (Element) bpk[0];
		Element v1 = (Element) bpk[2];
		Element v2 = (Element) bpk[3];
		Element omega = (Element) bsk[0];
		Element t1 = (Element) bsk[1];
		Element t2 = (Element) bsk[2];
		Element t3 = (Element) bsk[3];
		Element t4 = (Element) bsk[4];
		
		Element r1_i = ZR.newRandomElement().getImmutable();
		Element r2_i = ZR.newRandomElement().getImmutable();
		
		Element g = G1.newOneElement().getImmutable();
		Element H1_QTP = G1.newElementFromHash(QTP_i, 0, QTP_i.length).getImmutable();
		Element g1H1 = g1.duplicate().mul(H1_QTP).getImmutable();
		
		Element T0_i = g.powZn(r1_i.duplicate().mul(t1).mul(t2).add(r2_i.duplicate().mul(t3).mul(t4))).getImmutable();
		Element T1_i = v2.powZn(omega).mul(g1H1.powZn(r1_i.duplicate().negate().mul(t2))).getImmutable();
		Element T2_i = v1.powZn(omega).mul(g1H1.powZn(r1_i.duplicate().negate().mul(t1))).getImmutable();
		Element T3_i = g1H1.powZn(r2_i.duplicate().negate().mul(t4)).getImmutable();
		Element T4_i = g1H1.powZn(r2_i.duplicate().negate().mul(t3)).getImmutable();
		
		return new Object[]{T0_i, T1_i, T2_i, T3_i, T4_i};
	}
	
	public boolean BQuery(Object[] BCT_TP_i, Object[] btrapdoor_i)
	{
		if (!bFlag) BSetup(n, m);
		
		Element[] CVec_i = (Element[]) BCT_TP_i[0];
		Element[] aVec = (Element[]) BCT_TP_i[1];
		Element C0_i = CVec_i[0], C1_i = CVec_i[1], C2_i = CVec_i[2], C3_i = CVec_i[3], C4_i = CVec_i[4];
		Element T0_i = (Element) btrapdoor_i[0];
		Element T1_i = (Element) btrapdoor_i[1];
		Element T2_i = (Element) btrapdoor_i[2];
		Element T3_i = (Element) btrapdoor_i[3];
		Element T4_i = (Element) btrapdoor_i[4];
		
		byte[] pairBytes = concatBytes(
			group.pairing(T0_i, C0_i).toBytes(),
			group.pairing(T1_i, C1_i).toBytes(),
			group.pairing(T2_i, C2_i).toBytes(),
			group.pairing(T3_i, C3_i).toBytes(),
			group.pairing(T4_i, C4_i).toBytes()
		);
		Element VPrime_i = ZR.newElementFromHash(pairBytes, 0, pairBytes.length).getImmutable();
		
		Element result = computePolynomial(VPrime_i, aVec);
		return result.isZero();
	}
	
	public Object[][] Setup(int n, int m)
	{
		flag = false;
		if (isInt(n) && isInt(m) && 1 <= m && m <= n)
		{
			this.n = n;
			this.m = m;
		}
		else
		{
			this.n = DefaultN;
			this.m = DefaultM;
			System.out.println("Setup: The variables n and m should be two positive integers satisfying 1 <= m <= n but they are not, which have been defaulted to " + DefaultN + " and " + DefaultM + ", respectively.");
		}
		
		Element g1 = G1.newOneElement().getImmutable();
		Element g2 = G2.newOneElement().getImmutable();
		Element g3 = G1.newRandomElement().getImmutable();
		
		Element r = ZR.newRandomElement().getImmutable();
		Element s = ZR.newRandomElement().getImmutable();
		Element t = ZR.newRandomElement().getImmutable();
		Element omega = ZR.newRandomElement().getImmutable();
		Element t1 = ZR.newRandomElement().getImmutable();
		Element t2 = ZR.newRandomElement().getImmutable();
		Element t3 = ZR.newRandomElement().getImmutable();
		Element t4 = ZR.newRandomElement().getImmutable();
		
		Element R = g1.powZn(r).getImmutable();
		Element S = g2.powZn(s).getImmutable();
		Element T = g1.powZn(t).getImmutable();
		Element Omega = group.pairing(g1, g2).powZn(t1.mul(t2).mul(omega)).getImmutable();
		Element v1 = g2.powZn(t1).getImmutable();
		Element v2 = g2.powZn(t2).getImmutable();
		Element v3 = g2.powZn(t3).getImmutable();
		Element v4 = g2.powZn(t4).getImmutable();
		
		mpk = new Object[]{g1, g2, g3, R, S, T, Omega, v1, v2, v3, v4};
		msk = new Object[]{r, s, t, omega, t1, t2, t3, t4};
		
		flag = true;
		return new Object[][]{mpk, msk};
	}
	
	public Element[][] KGen(List<Object[]> L)
	{
		if (!flag) Setup(n, m);
		
		Element g1 = (Element) mpk[0];
		Element r = (Element) msk[0];
		Element s = (Element) msk[1];
		
		Element k_i = ZR.newRandomElement().getImmutable();
		Element x_i = ZR.newRandomElement().getImmutable();
		Element z_i = r.duplicate().sub(x_i).mul(s.duplicate().mul(x_i).invert()).getImmutable();
		Element Z_i = g1.powZn(z_i).getImmutable();
		
		Element sk_ID_i = k_i;
		Element[] ek_ID_i = new Element[]{x_i, Z_i};
		
		byte[] xZBytes = x_i.duplicate().mul(Z_i).toBytes();
		Element tag_i = ZR.newElementFromHash(xZBytes, 0, xZBytes.length).getImmutable();
		if (L != null) L.add(new Object[]{sk_ID_i, k_i, tag_i});
		
		return new Element[][]{{sk_ID_i}, ek_ID_i};
	}
	
	public Object[] Encryption(byte[] TP_i, Element sk_ID_i, Element[] ek_ID_i, Element[] s, Element s_i)
	{
		if (!flag) Setup(n, m);
		
		Element g1 = (Element) mpk[0];
		Element g3 = (Element) mpk[2];
		Element S = (Element) mpk[4];
		Element T = (Element) mpk[5];
		Element Omega = (Element) mpk[6];
		Element v1 = (Element) mpk[7];
		Element v2 = (Element) mpk[8];
		Element v3 = (Element) mpk[9];
		Element v4 = (Element) mpk[10];
		
		Element x_i = ek_ID_i[0];
		Element Z_i = ek_ID_i[1];
		
		Element s1_i = ZR.newRandomElement().getImmutable();
		Element s2_i = ZR.newRandomElement().getImmutable();
		
		Element[] VVec = new Element[n];
		for (int i = 0; i < n; i++)
		{
			byte[] omegaBytes = Omega.powZn(s[i]).toBytes();
			VVec[i] = ZR.newElementFromHash(omegaBytes, 0, omegaBytes.length).getImmutable();
		}
		
		Element H1_TP = G1.newElementFromHash(TP_i, 0, TP_i.length).getImmutable();
		Element C0_i = (g3.duplicate().mul(H1_TP)).powZn(s_i).getImmutable();
		Element C1_i = v1.powZn(s_i.duplicate().sub(s1_i)).getImmutable();
		Element C2_i = v2.powZn(s1_i).getImmutable();
		Element C3_i = v3.powZn(s_i.duplicate().sub(s2_i)).getImmutable();
		Element C4_i = v4.powZn(s2_i).getImmutable();
		
		Element[] aVec = computeCoefficients(VVec);
		
		Element alpha = ZR.newRandomElement().getImmutable();
		Element C1 = g1.powZn(alpha).getImmutable();
		Element C2 = Z_i.duplicate().powZn(x_i).mul(T.powZn(alpha)).getImmutable();
		Element C3 = group.pairing(T, S).powZn(alpha).getImmutable();
		
		byte[] c4Bytes = concatBytes(
			C0_i.toBytes(), C1_i.toBytes(), C2_i.toBytes(), C3_i.toBytes(), C4_i.toBytes()
		);
		for (int i = 0; i < n; i++)
			c4Bytes = concatBytes(c4Bytes, aVec[i].toBytes());
		c4Bytes = concatBytes(c4Bytes, C1.toBytes(), C2.toBytes(), C3.toBytes());
		Element C4 = ZR.newElementFromHash(c4Bytes, 0, c4Bytes.length).getImmutable();
		
		Element C5 = sk_ID_i.duplicate().mul(C4).add(x_i).getImmutable();
		
		return new Object[]{C0_i, C1_i, C2_i, C3_i, C4_i, C1, C2, C3, C4, C5};
	}
	
	public Object[] TrapdoorGen(byte[] QTP_i, Element sk_ID_i)
	{
		if (!flag) Setup(n, m);
		
		Element g3 = (Element) mpk[2];
		Element v1 = (Element) mpk[7];
		Element v2 = (Element) mpk[8];
		Element omega = (Element) msk[3];
		Element t1 = (Element) msk[4];
		Element t2 = (Element) msk[5];
		Element t3 = (Element) msk[6];
		Element t4 = (Element) msk[7];
		
		Element r1_i = ZR.newRandomElement().getImmutable();
		Element r2_i = ZR.newRandomElement().getImmutable();
		
		Element g = G1.newOneElement().getImmutable();
		Element H1_QTP = G1.newElementFromHash(QTP_i, 0, QTP_i.length).getImmutable();
		Element g3H1 = g3.duplicate().mul(H1_QTP).getImmutable();
		
		Element T0_i = g.powZn(r1_i.duplicate().mul(t1).mul(t2).add(r2_i.duplicate().mul(t3).mul(t4))).getImmutable();
		Element T1_i = v2.powZn(omega).mul(g3H1.powZn(r1_i.duplicate().negate().mul(t2))).getImmutable();
		Element T2_i = v1.powZn(omega).mul(g3H1.powZn(r1_i.duplicate().negate().mul(t1))).getImmutable();
		Element T3_i = g3H1.powZn(r2_i.duplicate().negate().mul(t4)).getImmutable();
		Element T4_i = g3H1.powZn(r2_i.duplicate().negate().mul(t3)).getImmutable();
		
		return new Object[]{T0_i, T1_i, T2_i, T3_i, T4_i};
	}
	
	public boolean Query(Object[] CT_TP_i, Object[] trapdoor_i, Element[] s)
	{
		if (!flag) Setup(n, m);
		
		Element Omega = (Element) mpk[6];
		
		Element C0_i = (Element) CT_TP_i[0];
		Element C1_i = (Element) CT_TP_i[1];
		Element C2_i = (Element) CT_TP_i[2];
		Element C3_i = (Element) CT_TP_i[3];
		Element C4_i = (Element) CT_TP_i[4];
		Element T0_i = (Element) trapdoor_i[0];
		Element T1_i = (Element) trapdoor_i[1];
		Element T2_i = (Element) trapdoor_i[2];
		Element T3_i = (Element) trapdoor_i[3];
		Element T4_i = (Element) trapdoor_i[4];
		
		Element[] VVec = new Element[n];
		for (int i = 0; i < n; i++)
		{
			byte[] omegaBytes = Omega.powZn(s[i]).toBytes();
			VVec[i] = ZR.newElementFromHash(omegaBytes, 0, omegaBytes.length).getImmutable();
		}
		Element[] aVec = computeCoefficients(VVec);
		
		byte[] pairBytes = concatBytes(
			group.pairing(C0_i, T0_i).toBytes(),
			group.pairing(C1_i, T1_i).toBytes(),
			group.pairing(C2_i, T2_i).toBytes(),
			group.pairing(C3_i, T3_i).toBytes(),
			group.pairing(C4_i, T4_i).toBytes()
		);
		Element VPrime_i = ZR.newElementFromHash(pairBytes, 0, pairBytes.length).getImmutable();
		
		Element result = computePolynomial(VPrime_i, aVec);
		return result.isZero();
	}
	
	public Object Trace(Object[] CT_TP_i, List<Object[]> L)
	{
		if (!flag) Setup(n, m);
		
		Element t = (Element) msk[2];
		Element C1 = (Element) CT_TP_i[5];
		Element C2 = (Element) CT_TP_i[6];
		
		byte[] tagBytes = C2.duplicate().sub(t.duplicate().mul(C1)).toBytes();
		Element tag_i = ZR.newElementFromHash(tagBytes, 0, tagBytes.length).getImmutable();
		
		Object identity = Boolean.FALSE;
		if (L != null)
		{
			for (Object[] element : L)
			{
				if (element.length >= 3 && tag_i.equals(element[2]))
				{
					identity = element;
					break;
				}
			}
		}
		return identity;
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
		else if (obj instanceof Object[][])
		{
			long total = 0;
			for (Object[] o : (Object[][]) obj)
				total += getLengthOf(o);
			return total;
		}
		else
		{
			return 0;
		}
	}
	
	private boolean isInt(Object x)
	{
		return x instanceof Integer || x instanceof BigInteger;
	}
}
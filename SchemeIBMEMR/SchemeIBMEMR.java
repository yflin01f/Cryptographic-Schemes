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
 * Java implementation of SchemeIBMEMR.
 * Converted from SchemeIBMEMR.py line by line.
 */
@SuppressWarnings("unchecked")
public class SchemeIBMEMR
{
	private static final int DefaultD = 30;
	
	private int d;
	private int seed;
	private Pairing group;
	private Field<Element> G1, ZR;
	private int operand;
	private Object[] mpk, msk;
	private boolean flag = false;
	
	public SchemeIBMEMR(Pairing pairing)
	{
		this.group = pairing;
		this.G1 = pairing.getG1();
		this.ZR = pairing.getZr();
		this.operand = (1 << 512) - 1;
		this.d = DefaultD;
	}
	
	public SchemeIBMEMR()
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
	
	private byte[] concatObjects(Object... objects)
	{
		byte[] result = new byte[0];
		for (Object obj : objects)
		{
			if (obj instanceof Element)
				result = concatBytes(result, ((Element) obj).toBytes());
			else if (obj instanceof byte[])
				result = concatBytes(result, (byte[]) obj);
			else if (obj instanceof Object[])
			{
				for (Object o : (Object[]) obj)
					result = concatBytes(result, ((Element) o).toBytes());
			}
		}
		return result;
	}
	
	private Element[] computeCoefficients(Element[] roots, Element k)
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
				coefficients[i] = coefficients[i].add(r.duplicate().mul(coefficients[i + 1])).getImmutable();
			coefficients[n - 1] = coefficients[n - 1].add(r).getImmutable();
			cnt--;
		}
		for (int i = n - 1; i >= 0; i -= 2)
			coefficients[i] = coefficients[i].negate().getImmutable();
		if (k != null)
			coefficients[0] = coefficients[0].add(k).getImmutable();
		return coefficients;
	}
	
	private Element[] computeCoefficients(Element[] roots)
	{
		return computeCoefficients(roots, null);
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
	
	public Object[][] Setup(int d)
	{
		flag = false;
		if (isInt(d) && d >= 1)
			this.d = d;
		else
		{
			this.d = DefaultD;
			System.out.println("Setup: The variable d should be a positive integer, but it is not, which has been defaulted to " + DefaultD + ".");
		}
		this.seed = (int)(Math.random() * this.d);
		
		Element g = G1.newOneElement().getImmutable();
		Element g0 = G1.newRandomElement().getImmutable();
		Element g1 = G1.newRandomElement().getImmutable();
		
		Element w = ZR.newRandomElement().getImmutable();
		Element alpha = ZR.newRandomElement().getImmutable();
		Element gamma = ZR.newRandomElement().getImmutable();
		Element k = ZR.newRandomElement().getImmutable();
		Element t1 = ZR.newRandomElement().getImmutable();
		Element t2 = ZR.newRandomElement().getImmutable();
		
		Element Omega = group.pairing(g, g).powZn(w).getImmutable();
		Element v1 = g.powZn(t1).getImmutable();
		Element v2 = g.powZn(t2).getImmutable();
		Element v3 = g.powZn(gamma).getImmutable();
		Element v4 = g.powZn(k).getImmutable();
		
		mpk = new Object[]{g, g0, g1, v1, v2, v3, v4, Omega};
		msk = new Object[]{w, alpha, gamma, k, t1, t2};
		
		flag = true;
		return new Object[][]{mpk, msk};
	}
	
	public Element EKGen(Element id_S)
	{
		if (!flag) Setup(d);
		
		if (id_S == null || !id_S.getField().equals(ZR))
		{
			id_S = ZR.newRandomElement().getImmutable();
			System.out.println("EKGen: The variable id_S should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element alpha = (Element) msk[1];
		byte[] idBytes = id_S.toBytes();
		Element H1_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
		Element ek_id_S = H1_val.powZn(alpha).getImmutable();
		return ek_id_S;
	}
	
	public Object[] DKGen(Element id_R)
	{
		if (!flag) Setup(d);
		
		if (id_R == null || !id_R.getField().equals(ZR))
		{
			id_R = ZR.newRandomElement().getImmutable();
			System.out.println("DKGen: The variable id_R should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		
		@SuppressWarnings("unused")
		Element w = (Element) msk[0];
		@SuppressWarnings("unused")
		Element alpha = (Element) msk[1];
		Element gamma = (Element) msk[2];
		Element t1 = (Element) msk[4];
		Element t2 = (Element) msk[5];
		
		byte[] idBytes = id_R.toBytes();
		Element H2_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
		
		Element dk1 = H2_val.powZn(alpha).getImmutable();
		Element g0g1id = g0.duplicate().mul(g1.powZn(id_R)).getImmutable();
		Element dk2 = g.powZn(w.duplicate().mul(t1.duplicate().invert()))
			.mul(g0g1id.powZn(gamma.duplicate().mul(t1.duplicate().invert()))).getImmutable();
		Element dk3 = g.powZn(w.duplicate().mul(t2.duplicate().invert()))
			.mul(g0g1id.powZn(gamma.duplicate().mul(t2.duplicate().invert()))).getImmutable();
		
		return new Object[]{dk1, dk2, dk3};
	}
	
	public Object[] TDKGen(Element id_R)
	{
		if (!flag) Setup(d);
		
		if (id_R == null || !id_R.getField().equals(ZR))
		{
			id_R = ZR.newRandomElement().getImmutable();
			System.out.println("TDKGen: The variable id_R should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		@SuppressWarnings("unused")
		Element w = (Element) msk[0];
		@SuppressWarnings("unused")
		Element alpha = (Element) msk[1];
		Element k = (Element) msk[3];
		Element t1 = (Element) msk[4];
		Element t2 = (Element) msk[5];
		
		Element g0g1id = g0.duplicate().mul(g1.powZn(id_R)).getImmutable();
		
		Element td1 = g.powZn(ZR.newOneElement().duplicate().negate().mul(t1.duplicate().invert()))
			.mul(g0g1id.powZn(k.duplicate().mul(t1.duplicate().invert()))).getImmutable();
		Element td2 = g.powZn(ZR.newOneElement().duplicate().negate().mul(t2.duplicate().invert()))
			.mul(g0g1id.powZn(k.duplicate().mul(t2.duplicate().invert()))).getImmutable();
		
		return new Object[]{td1, td2};
	}
	
	public Object[] Enc(Element ek_id_S, Element id_R, int message)
	{
		if (!flag) Setup(d);
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		Element v1 = (Element) mpk[3];
		Element v2 = (Element) mpk[4];
		Element v3 = (Element) mpk[5];
		Element v4 = (Element) mpk[6];
		@SuppressWarnings("unused")
		Element Omega = (Element) mpk[7];
		
		Element w = (Element) msk[0];
		
		int mVal = message & operand;
		
		// Generate S = (random, ..., id_R, ..., random)
		Element[] S = new Element[d];
		for (int i = 0; i < d; i++)
		{
			if (i == seed)
				S[i] = id_R;
			else
				S[i] = ZR.newRandomElement().getImmutable();
		}
		
		Element s1 = ZR.newRandomElement().getImmutable();
		Element s2 = ZR.newRandomElement().getImmutable();
		Element beta = ZR.newRandomElement().getImmutable();
		Element sigma = ZR.newRandomElement().getImmutable();
		Element K = ZR.newRandomElement().getImmutable();
		Element R = ZR.newRandomElement().getImmutable();
		
		byte[] sigmaBytes = sigma.toBytes();
		byte[] mBytes = BigInteger.valueOf(mVal).toByteArray();
		byte[] hashInput = concatBytes(sigmaBytes, mBytes);
		byte[] hashDigest = sha256(hashInput);
		Element r = ZR.newElementFromHash(hashDigest, 0, hashDigest.length).getImmutable();
		
		Element ct1 = g.powZn(beta).getImmutable();
		Element ct2 = v1.powZn(s1).getImmutable();
		Element ct3 = v2.powZn(s2).getImmutable();
		
		Element[] KArray = new Element[d];
		for (int i = 0; i < d; i++)
		{
			byte[] sBytes = S[i].toBytes();
			Element H2_val = G1.newElementFromHash(sBytes, 0, sBytes.length).getImmutable();
			KArray[i] = group.pairing(H2_val, ek_id_S.duplicate().mul(ct1)).getImmutable();
		}
		Element[] h4k = new Element[d];
		for (int i = 0; i < d; i++)
		{
			byte[] kBytes = KArray[i].toBytes();
			h4k[i] = ZR.newElementFromHash(kBytes, 0, kBytes.length).getImmutable();
		}
		Element[] aArray = computeCoefficients(h4k, K);
		
		Element s = s1.duplicate().add(s2).getImmutable();
		Element[] RArray = new Element[d];
		for (int i = 0; i < d; i++)
		{
			Element g0g1id = g0.duplicate().mul(g1.powZn(S[i])).getImmutable();
			RArray[i] = group.pairing(v3, g0g1id.powZn(s)).getImmutable();
		}
		Element[] h4r = new Element[d];
		for (int i = 0; i < d; i++)
		{
			Element rVal = RArray[i].duplicate().mul(group.pairing(g, g).powZn(w.duplicate().mul(s))).getImmutable();
			byte[] rBytes = rVal.toBytes();
			h4r[i] = ZR.newElementFromHash(rBytes, 0, rBytes.length).getImmutable();
		}
		Element[] bArray = computeCoefficients(h4r, R);
		
		int ct4_val = HHat(K) ^ HHat(R) ^ BigInteger.valueOf(mVal).xor(new BigInteger(sigmaBytes)).intValue();
		Element ct4_el = ZR.newElement(ct4_val & operand).getImmutable();
		
		Element[] VArray = new Element[d];
		for (int i = 0; i < d; i++)
		{
			Element g0g1id = g0.duplicate().mul(g1.powZn(S[i])).getImmutable();
			VArray[i] = group.pairing(v4, g0g1id.powZn(s)).getImmutable();
		}
		Element[] h4v = new Element[d];
		for (int i = 0; i < d; i++)
		{
			Element vVal = VArray[i].duplicate().mul(group.pairing(g, g).powZn(s.duplicate().negate())).getImmutable();
			byte[] vBytes = vVal.toBytes();
			h4v[i] = ZR.newElementFromHash(vBytes, 0, vBytes.length).getImmutable();
		}
		Element[] cArray = computeCoefficients(h4v);
		
		Element ct5 = g.powZn(r).getImmutable();
		
		byte[] h5Input = concatBytes(ct1.toBytes(), ct2.toBytes(), ct3.toBytes(), ct4_el.toBytes(), ct5.toBytes());
		h5Input = concatBytes(h5Input, concatObjects(aArray, bArray, cArray));
		Element H5_val = G1.newElementFromHash(h5Input, 0, h5Input.length).getImmutable();
		Element ct6 = H5_val.powZn(r).getImmutable();
		
		return new Object[]{ct1, ct2, ct3, ct4_el, ct5, ct6, aArray, bArray, cArray};
	}
	
	public Object Dec(Object[] dk_id_R, Element id_R, Element id_S, Object[] ct)
	{
		if (!flag) Setup(d);
		
		Element g = (Element) mpk[0];
		
		Element dk1 = (Element) dk_id_R[0];
		Element dk2 = (Element) dk_id_R[1];
		Element dk3 = (Element) dk_id_R[2];
		
		Element ct1 = (Element) ct[0];
		Element ct2 = (Element) ct[1];
		Element ct3 = (Element) ct[2];
		Element ct4_el = (Element) ct[3];
		Element ct5 = (Element) ct[4];
		Element ct6 = (Element) ct[5];
		Element[] aArray = (Element[]) ct[6];
		Element[] bArray = (Element[]) ct[7];
		Element[] cArray = (Element[]) ct[8];
		
		byte[] h5Input = concatBytes(ct1.toBytes(), ct2.toBytes(), ct3.toBytes(), ct4_el.toBytes(), ct5.toBytes());
		h5Input = concatBytes(h5Input, concatObjects(aArray, bArray, cArray));
		Element H5_val = G1.newElementFromHash(h5Input, 0, h5Input.length).getImmutable();
		
		if (group.pairing(ct5, H5_val).equals(group.pairing(ct6, g)))
		{
			byte[] idSBytes = id_S.toBytes();
			Element H1_val = G1.newElementFromHash(idSBytes, 0, idSBytes.length).getImmutable();
			byte[] idRBytes = id_R.toBytes();
			Element H2_val = G1.newElementFromHash(idRBytes, 0, idRBytes.length).getImmutable();
			
			Element KPrimePrime_val = group.pairing(dk1, H1_val).mul(group.pairing(H2_val, ct1)).getImmutable();
			byte[] kpBytes = KPrimePrime_val.toBytes();
			Element KPrimePrime = ZR.newElementFromHash(kpBytes, 0, kpBytes.length).getImmutable();
			
			Element RPrimePrime_val = group.pairing(dk2, ct2).mul(group.pairing(dk3, ct3)).getImmutable();
			byte[] rpBytes = RPrimePrime_val.toBytes();
			Element RPrimePrime = ZR.newElementFromHash(rpBytes, 0, rpBytes.length).getImmutable();
			
			Element KPrime = computePolynomial(KPrimePrime, aArray);
			Element RPrime = computePolynomial(RPrimePrime, bArray);
			
			int ct4_int = 0;
			byte[] c4Bytes = ct4_el.toBytes();
			for (int i = 0; i < Math.min(4, c4Bytes.length); i++)
				ct4_int = (ct4_int << 8) | (c4Bytes[i] & 0xFF);
			
			int m_sigma_val = ct4_int ^ HHat(KPrime) ^ HHat(RPrime);
			BigInteger m_val = BigInteger.valueOf(m_sigma_val);
			
			byte[] m_sigmaBytes = m_val.toByteArray();
			byte[] rCheckDigest = sha256(m_sigmaBytes);
			Element r_check = ZR.newElementFromHash(rCheckDigest, 0, rCheckDigest.length).getImmutable();
			
			if (ct5.equals(g.powZn(r_check)))
				return m_val.intValue() & operand;
			else
				return Boolean.FALSE;
		}
		else
			return Boolean.FALSE;
	}
	
	public boolean ReceiverVerify(Object[] ct, Object[] td_id_R)
	{
		Element ct1 = (Element) ct[0];
		Element ct2 = (Element) ct[1];
		Element ct3 = (Element) ct[2];
		Element td1 = (Element) td_id_R[0];
		Element td2 = (Element) td_id_R[1];
		
		Element left = group.pairing(ct1, td1).mul(group.pairing(ct2, td2));
		Element right = group.pairing(ct3, ct1);
		return left.equals(right);
	}
	
	private byte[] sha256(byte[] input)
	{
		try
		{
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			return sha.digest(input);
		}
		catch (NoSuchAlgorithmException e)
		{
			return input;
		}
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
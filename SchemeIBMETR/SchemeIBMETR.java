import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import it.unisa.dia.gas.plaf.jpbc.field.gt.GTFiniteField;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Java implementation of SchemeIBMETR.
 * Converted from SchemeIBMETR.py line by line.
 */
@SuppressWarnings("unchecked")
public class SchemeIBMETR
{
	private Pairing group;
	private Field<Element> G1, ZR;
	private int operand;
	private Object[] mpk, msk;
	private boolean flag = false;
	
	public SchemeIBMETR(Pairing pairing)
	{
		this.group = pairing;
		this.G1 = pairing.getG1();
		this.ZR = pairing.getZr();
		this.operand = (1 << 512) - 1;
	}
	
	public SchemeIBMETR()
	{
		this(initPairing());
	}
	
	private static Pairing initPairing()
	{
		TypeACurveGenerator pg = new TypeACurveGenerator(512, 512);
		PairingParameters params = pg.generate();
		return PairingFactory.getPairing(params);
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
	
	public Object[][] Setup()
	{
		flag = false;
		
		Element g = G1.newOneElement().getImmutable();
		Element g0 = G1.newRandomElement().getImmutable();
		Element g1 = G1.newRandomElement().getImmutable();
		Element w = ZR.newRandomElement().getImmutable();
		Element alpha = ZR.newRandomElement().getImmutable();
		Element t1 = ZR.newRandomElement().getImmutable();
		Element t2 = ZR.newRandomElement().getImmutable();
		Element Omega = group.pairing(g, g).powZn(w).getImmutable();
		Element v1 = g.powZn(t1).getImmutable();
		Element v2 = g.powZn(t2).getImmutable();
		
		mpk = new Object[]{g, g0, g1, v1, v2, Omega};
		msk = new Object[]{w, alpha, t1, t2};
		
		flag = true;
		return new Object[][]{mpk, msk};
	}
	
	public Element EKGen(Element id_S)
	{
		if (!flag) Setup();
		if (id_S == null || !id_S.getField().equals(ZR))
		{
			id_S = ZR.newRandomElement().getImmutable();
			System.out.println("EKGen: The variable id_S should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element alpha = (Element) msk[1];
		byte[] idBytes = id_S.toBytes();
		Element H1_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
		return H1_val.powZn(alpha).getImmutable();
	}
	
	public Object[] DKGen(Element id_R)
	{
		if (!flag) Setup();
		if (id_R == null || !id_R.getField().equals(ZR))
		{
			id_R = ZR.newRandomElement().getImmutable();
			System.out.println("DKGen: The variable id_R should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		Element w = (Element) msk[0];
		Element alpha = (Element) msk[1];
		Element t1 = (Element) msk[2];
		Element t2 = (Element) msk[3];
		
		Element r = ZR.newRandomElement().getImmutable();
		
		byte[] idBytes = id_R.toBytes();
		Element H2_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
		
		Element dk0 = H2_val.powZn(alpha).getImmutable();
		Element dk1 = g.powZn(r).getImmutable();
		Element g0g1id = g0.duplicate().mul(g1.powZn(id_R)).getImmutable();
		
		Element neg_w_t1 = w.duplicate().negate().mul(t1.duplicate().invert());
		Element neg_r_t1 = r.duplicate().negate().mul(t1.duplicate().invert());
		Element dk2 = g.powZn(neg_w_t1).mul(g0g1id.powZn(neg_r_t1)).getImmutable();
		
		Element neg_w_t2 = w.duplicate().negate().mul(t2.duplicate().invert());
		Element neg_r_t2 = r.duplicate().negate().mul(t2.duplicate().invert());
		Element dk3 = g.powZn(neg_w_t2).mul(g0g1id.powZn(neg_r_t2)).getImmutable();
		
		return new Object[]{dk0, dk1, dk2, dk3};
	}
	
	public Object[] TKGen(Element id_R)
	{
		if (!flag) Setup();
		if (id_R == null || !id_R.getField().equals(ZR))
		{
			id_R = ZR.newRandomElement().getImmutable();
			System.out.println("TKGen: The variable id_R should be an element of ZR, but it is not, which has been generated randomly.");
		}
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		Element t1 = (Element) msk[2];
		Element t2 = (Element) msk[3];
		
		Element k = ZR.newRandomElement().getImmutable();
		Element g0g1id = g0.duplicate().mul(g1.powZn(id_R)).getImmutable();
		
		Element tk1 = g.powZn(k).getImmutable();
		Element tk2 = g.powZn(t1.duplicate().invert())
			.mul(g0g1id.powZn(k.duplicate().negate().mul(t1.duplicate().invert()))).getImmutable();
		Element tk3 = g.powZn(t2.duplicate().invert())
			.mul(g0g1id.powZn(k.duplicate().negate().mul(t2.duplicate().invert()))).getImmutable();
		
		return new Object[]{tk1, tk2, tk3};
	}
	
	public Object[] Enc(Element ek_id_S, Element id_Rev, int message)
	{
		if (!flag) Setup();
		
		Element g = (Element) mpk[0];
		Element g0 = (Element) mpk[1];
		Element g1 = (Element) mpk[2];
		Element v1 = (Element) mpk[3];
		Element v2 = (Element) mpk[4];
		Element Omega = (Element) mpk[5];
		
		int m = message & operand;
		
		Element s1 = ZR.newRandomElement().getImmutable();
		Element s2 = ZR.newRandomElement().getImmutable();
		Element beta = ZR.newRandomElement().getImmutable();
		Element s = s1.duplicate().add(s2).getImmutable();
		
		Element R = Omega.powZn(s.duplicate().negate()).getImmutable();
		Element T = g.powZn(beta).getImmutable();
		
		byte[] idBytes = id_Rev.toBytes();
		Element H2_val = G1.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
		Element K = group.pairing(H2_val, ek_id_S.duplicate().mul(T)).getImmutable();
		
		int ct0 = HHat(R) ^ HHat(K) ^ m;
		
		Element g0g1id = g0.duplicate().mul(g1.powZn(id_Rev)).getImmutable();
		Element ct1 = g0g1id.powZn(s).getImmutable();
		Element ct2 = v1.powZn(s1).getImmutable();
		Element ct3 = v2.powZn(s2).getImmutable();
		Element V = group.pairing(g, g).powZn(s).getImmutable();
		
		return new Object[]{Integer.valueOf(ct0), ct1, ct2, ct3, T, V};
	}
	
	public int Dec(Object[] dk_id_R, Element id_Rev, Element id_Snd, Object[] ct)
	{
		if (!flag) Setup();
		
		Element dk0 = (Element) dk_id_R[0];
		Element dk1 = (Element) dk_id_R[1];
		Element dk2 = (Element) dk_id_R[2];
		Element dk3 = (Element) dk_id_R[3];
		
		int ct0 = ((Integer) ct[0]).intValue();
		Element ct1 = (Element) ct[1];
		Element ct2 = (Element) ct[2];
		Element ct3 = (Element) ct[3];
		Element T = (Element) ct[4];
		
		byte[] sndBytes = id_Snd.toBytes();
		Element H1_val = G1.newElementFromHash(sndBytes, 0, sndBytes.length).getImmutable();
		
		byte[] revBytes = id_Rev.toBytes();
		Element H2_val = G1.newElementFromHash(revBytes, 0, revBytes.length).getImmutable();
		
		Element RPrime = group.pairing(dk1, ct1).mul(group.pairing(dk2, ct2)).mul(group.pairing(dk3, ct3));
		Element KPrime = group.pairing(dk0, H1_val).mul(group.pairing(H2_val, T));
		
		int m_val = ct0 ^ HHat(RPrime) ^ HHat(KPrime);
		return m_val & operand;
	}
	
	public boolean TVerify(Object[] tk_id_R, Object[] ct)
	{
		Element tk1 = (Element) tk_id_R[0];
		Element tk2 = (Element) tk_id_R[1];
		Element tk3 = (Element) tk_id_R[2];
		Element ct1 = (Element) ct[1];
		Element ct2 = (Element) ct[2];
		Element ct3 = (Element) ct[3];
		Element V = (Element) ct[5];
		
		Element left = group.pairing(tk1, ct1).mul(group.pairing(tk2, ct2)).mul(group.pairing(tk3, ct3));
		return V.equals(left);
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
}
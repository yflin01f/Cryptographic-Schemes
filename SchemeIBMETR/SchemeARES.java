import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * Java implementation of the ARES cryptographic scheme.
 * Converted from SchemeARES.py based on JPBC library.
 * This scheme is only applicable to symmetric groups of prime orders.
 */
@SuppressWarnings("unchecked")
public class SchemeARES
{
    private Pairing pairing;
    private Field<Element> G1, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;

    public SchemeARES(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1();
        this.Zp = pairing.getZr();
    }

    public Object[][] Setup()
    {
        flag = false;
        Element g = G1.newOneElement().getImmutable();
        Element g0 = G1.newRandomElement().getImmutable();
        Element g1 = G1.newRandomElement().getImmutable();
        Element w = Zp.newRandomElement().getImmutable();
        Element t1 = Zp.newRandomElement().getImmutable();
        Element t2 = Zp.newRandomElement().getImmutable();
        Element t3 = Zp.newRandomElement().getImmutable();
        Element t4 = Zp.newRandomElement().getImmutable();

        Element Omega = pairing.pairing(g, g).powZn(t1.duplicate().mul(t2).mul(w)).getImmutable();
        Element v1 = g.powZn(t1).getImmutable();
        Element v2 = g.powZn(t2).getImmutable();
        Element v3 = g.powZn(t3).getImmutable();
        Element v4 = g.powZn(t4).getImmutable();

        mpk = new Object[]{Omega, g, g0, g1, v1, v2, v3, v4};
        msk = new Object[]{w, t1, t2, t3, t4};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Object[] Extract(Element identity)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[1];
        Element g0 = (Element) mpk[2];
        Element g1 = (Element) mpk[3];
        Element w = (Element) msk[0];
        Element t1 = (Element) msk[1];
        Element t2 = (Element) msk[2];
        Element t3 = (Element) msk[3];
        Element t4 = (Element) msk[4];

        Element r1 = Zp.newRandomElement().getImmutable();
        Element r2 = Zp.newRandomElement().getImmutable();

        Element d0 = g.powZn(
            r1.duplicate().mul(t1).mul(t2).add(
            r2.duplicate().mul(t3).mul(t4))).getImmutable();

        Element g0g1Id = g0.duplicate().mul(g1.powZn(identity)).getImmutable();

        Element d1 = g.powZn(w.duplicate().mul(t2).negate())
            .mul(g0g1Id.powZn(r1.duplicate().mul(t2).negate())).getImmutable();
        Element d2 = g.powZn(w.duplicate().mul(t1).negate())
            .mul(g0g1Id.powZn(r1.duplicate().mul(t1).negate())).getImmutable();
        Element d3 = g0g1Id.powZn(r2.duplicate().mul(t4).negate()).getImmutable();
        Element d4 = g0g1Id.powZn(r2.duplicate().mul(t3).negate()).getImmutable();

        return new Object[]{d0, d1, d2, d3, d4};
    }

    public Object[] TSK(Element identity)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[1];
        Element g0 = (Element) mpk[2];
        Element g1 = (Element) mpk[3];
        Element t1 = (Element) msk[1];
        Element t2 = (Element) msk[2];
        Element t3 = (Element) msk[3];
        Element t4 = (Element) msk[4];

        Element r1 = Zp.newRandomElement().getImmutable();
        Element r2 = Zp.newRandomElement().getImmutable();

        Element d0 = g.powZn(
            r1.duplicate().mul(t1).mul(t2).add(
            r2.duplicate().mul(t3).mul(t4))).getImmutable();

        Element g0g1Id = g0.duplicate().mul(g1.powZn(identity)).getImmutable();

        // TSK: no w term in d1, d2
        Element d1 = g0g1Id.powZn(r1.duplicate().mul(t2).negate()).getImmutable();
        Element d2 = g0g1Id.powZn(r1.duplicate().mul(t1).negate()).getImmutable();
        Element d3 = g0g1Id.powZn(r2.duplicate().mul(t4).negate()).getImmutable();
        Element d4 = g0g1Id.powZn(r2.duplicate().mul(t3).negate()).getImmutable();

        return new Object[]{d0, d1, d2, d3, d4};
    }

    public Object[] Encrypt(Element identity, Element message)
    {
        if (!flag) Setup();
        Element Omega = (Element) mpk[0];
        Element g0 = (Element) mpk[2];
        Element g1 = (Element) mpk[3];
        Element v1 = (Element) mpk[4];
        Element v2 = (Element) mpk[5];
        Element v3 = (Element) mpk[6];
        Element v4 = (Element) mpk[7];

        Element s = Zp.newRandomElement().getImmutable();
        Element s1 = Zp.newRandomElement().getImmutable();
        Element s2 = Zp.newRandomElement().getImmutable();

        Element CPi = Omega.powZn(s).mul(message).getImmutable();
        Element C0 = g0.duplicate().mul(g1.powZn(identity)).powZn(s).getImmutable();
        Element C1 = v1.powZn(s.duplicate().sub(s1)).getImmutable();
        Element C2 = v2.powZn(s1).getImmutable();
        Element C3 = v3.powZn(s.duplicate().sub(s2)).getImmutable();
        Element C4 = v4.powZn(s2).getImmutable();

        return new Object[]{CPi, C0, C1, C2, C3, C4};
    }

    public Element Decrypt(Object[] PvkId, Object[] cipherText)
    {
        if (!flag) Setup();
        Element d0 = (Element) PvkId[0];
        Element d1 = (Element) PvkId[1];
        Element d2 = (Element) PvkId[2];
        Element d3 = (Element) PvkId[3];
        Element d4 = (Element) PvkId[4];

        Element CPi = (Element) cipherText[0];
        Element C0 = (Element) cipherText[1];
        Element C1 = (Element) cipherText[2];
        Element C2 = (Element) cipherText[3];
        Element C3 = (Element) cipherText[4];
        Element C4 = (Element) cipherText[5];

        Element M = CPi.duplicate()
            .mul(pairing.pairing(C0, d0))
            .mul(pairing.pairing(C1, d1))
            .mul(pairing.pairing(C2, d2))
            .mul(pairing.pairing(C3, d3))
            .mul(pairing.pairing(C4, d4)).getImmutable();
        return M;
    }

    public boolean TVerify(Object[] PvkId, Object[] cipherText)
    {
        if (!flag) Setup();
        Element d0 = (Element) PvkId[0];
        Element d1 = (Element) PvkId[1];
        Element d2 = (Element) PvkId[2];
        Element d3 = (Element) PvkId[3];
        Element d4 = (Element) PvkId[4];

        Element C0 = (Element) cipherText[1];
        Element C1 = (Element) cipherText[2];
        Element C2 = (Element) cipherText[3];
        Element C3 = (Element) cipherText[4];
        Element C4 = (Element) cipherText[5];

        Element result = pairing.pairing(C0, d0)
            .mul(pairing.pairing(C1, d1))
            .mul(pairing.pairing(C2, d2))
            .mul(pairing.pairing(C3, d3))
            .mul(pairing.pairing(C4, d4)).getImmutable();

        return result.isOne();
    }

    public long getLengthOf(Object obj)
    {
        if (obj instanceof Element)
			return pairing.getG1().getLengthInBytes();
		else if (obj instanceof Integer || obj instanceof Long)
			return pairing.getG1().getLengthInBytes();
		else if (obj instanceof byte[])
			return ((byte[]) obj).length;
		else if (obj instanceof Object[])
        {
            Object[] arr = (Object[]) obj;
            long sum = 0;
            for (Object o : arr)
            {
                long size = getLengthOf(o);
                if (size < 1) return -1;
                sum += size;
            }
            return sum;
        }
        else
            return -1;
    }
}
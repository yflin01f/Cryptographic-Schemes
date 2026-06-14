import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import java.math.BigInteger;

/**
 * Java implementation of the IB-ME cryptographic scheme.
 * Converted from SchemeIBME.py based on JPBC library.
 * This scheme is only applicable to symmetric groups of prime orders.
 */
@SuppressWarnings("unchecked")
public class SchemeIBME
{
    private Pairing pairing;
    private Field<Element> G1, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;
    private int operand;

    public SchemeIBME(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1();
        this.Zp = pairing.getZr();
        this.operand = (1 << 512) - 1;
    }

    private byte[] concatBytes(byte[] a, byte[] b)
    {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public Object[][] Setup()
    {
        flag = false;
        Element P = G1.newOneElement().getImmutable();
        Element r = Zp.newRandomElement().getImmutable();
        Element s = Zp.newRandomElement().getImmutable();

        Element P0 = P.powZn(r).getImmutable();

        // mask: random bytes of length equal to field degree
        byte[] mask = new byte[pairing.getG1().getLengthInBytes()];
        for (int i = 0; i < mask.length; i++) mask[i] = (byte) (Math.random() * 256);

        mpk = new Object[]{P, P0, mask};
        msk = new Object[]{r, s};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Element SKGen(Element sender)
    {
        if (!flag) Setup();
        Element HPrime_S = hashWithMask(sender);
        Element s_val = (Element) msk[1];
        return HPrime_S.powZn(s_val).getImmutable();
    }

    public Object[] RKGen(Element receiver)
    {
        if (!flag) Setup();
        Element P = (Element) mpk[0];
        Element r_val = (Element) msk[0];
        Element s_val = (Element) msk[1];

        Element HR = hashToG1(receiver);
        Element dk1 = HR.powZn(r_val).getImmutable();
        Element dk2 = HR.powZn(s_val).getImmutable();
        Element dk3 = HR.duplicate().getImmutable();
        return new Object[]{dk1, dk2, dk3};
    }

    public Object[] Enc(Element ekS, Element receiver, int message)
    {
        if (!flag) Setup();
        Element P = (Element) mpk[0];
        Element P0 = (Element) mpk[1];

        int M = message & operand;

        Element u = Zp.newRandomElement().getImmutable();
        Element t = Zp.newRandomElement().getImmutable();

        Element T = P.powZn(t).getImmutable();
        Element U = P.powZn(u).getImmutable();

        Element HR = hashToG1(receiver);

        // k_R = pair(HR, u * P0) = pair(HR, P0^u)
        Element k_R = pairing.pairing(HR, P0.powZn(u)).getImmutable();
        // k_S = pair(HR, T + ekS) = pair(HR, T.mul(ekS))
        Element k_S = pairing.pairing(HR, T.duplicate().mul(ekS)).getImmutable();

        // V = M ^ hash(k_R) ^ hash(k_S)
        int kR_int = new BigInteger(1, k_R.toCanonicalRepresentation()).intValue() & operand;
        int kS_int = new BigInteger(1, k_S.toCanonicalRepresentation()).intValue() & operand;
        int V = M ^ kR_int ^ kS_int;

        return new Object[]{T, U, V};
    }

    public int Dec(Object[] dkR, Element sender, Object[] cipher)
    {
        if (!flag) Setup();
        Element dk1 = (Element) dkR[0];
        Element dk2 = (Element) dkR[1];
        Element dk3 = (Element) dkR[2];

        Element T = (Element) cipher[0];
        Element U = (Element) cipher[1];
        int V = (Integer) cipher[2];

        // k_R = pair(dk1, U)
        Element k_R = pairing.pairing(dk1, U).getImmutable();

        // k_S = pair(dk3, T) * pair(H'(S), dk2)
        Element HPrime_S = hashWithMask(sender);
        Element k_S = pairing.pairing(dk3, T).mul(pairing.pairing(HPrime_S, dk2)).getImmutable();

        int kR_int = new BigInteger(1, k_R.toCanonicalRepresentation()).intValue() & operand;
        int kS_int = new BigInteger(1, k_S.toCanonicalRepresentation()).intValue() & operand;
        int M = V ^ kR_int ^ kS_int;

        return M;
    }

    private Element hashToG1(Element zrElem)
    {
        byte[] bytes = zrElem.toCanonicalRepresentation();
        return G1.newElementFromHash(bytes, 0, bytes.length).getImmutable();
    }

    private Element hashWithMask(Element zrElem)
    {
        byte[] mask = (byte[]) mpk[2];
        byte[] bytes = zrElem.toCanonicalRepresentation();
        byte[] xored = new byte[Math.min(bytes.length, mask.length)];
        for (int i = 0; i < xored.length; i++)
            xored[i] = (byte) (bytes[i] ^ mask[i]);
        return G1.newElementFromHash(xored, 0, xored.length).getImmutable();
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
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Java implementation of the VL-PSI-CA cryptographic scheme.
 * Converted from SchemeVLPSICA.py based on JPBC library.
 * This scheme is applicable to symmetric and asymmetric groups of prime orders.
 */
@SuppressWarnings("unchecked")
public class SchemeVLPSICA
{
    private static final int DEFAULT_M = 10, DEFAULT_N = 10, DEFAULT_D = 10;
    private int m, n, d;
    private Pairing pairing;
    private Field<Element> G1, G2, Zp;
    private Object[] mpk, msk;
    @SuppressWarnings("unused")
    private boolean flag = false;

    public SchemeVLPSICA(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
        this.Zp = pairing.getZr();
        this.m = DEFAULT_M; this.n = DEFAULT_N; this.d = DEFAULT_D;
    }

    private Element product(Element[] elements)
    {
        Element r = elements[0].duplicate().getImmutable();
        for (int i = 1; i < elements.length; i++) r = r.mul(elements[i]).getImmutable();
        return r;
    }

    private Element computeLagrangeCoefficients(Element[] xPoints, Element[] yPoints, Element x)
    {
        int nPts = xPoints.length;
        Element result = Zp.newZeroElement().getImmutable();
        for (int i = 0; i < nPts; i++)
        {
            Element L_i = Zp.newOneElement().getImmutable();
            for (int j = 0; j < nPts; j++)
            {
                if (i != j)
                {
                    Element num = x.duplicate().sub(xPoints[j]).getImmutable();
                    Element den = xPoints[i].duplicate().sub(xPoints[j]).getImmutable();
                    L_i = L_i.mul(num.div(den)).getImmutable();
                }
            }
            result = result.add(yPoints[i].duplicate().mul(L_i)).getImmutable();
        }
        return result;
    }

    private long hashToLong(Element elem)
    {
        byte[] bytes = elem.toCanonicalRepresentation();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(bytes);
            return new BigInteger(1, digest).longValue();
        } catch (NoSuchAlgorithmException e) {
            return new BigInteger(1, bytes).longValue();
        }
    }

    public Object[][] Setup(int m, int n, int d)
    {
        flag = false;
        if (m >= 1) this.m = m; else this.m = DEFAULT_M;
        if (n >= 1) this.n = n; else this.n = DEFAULT_N;
        if (d >= 1) this.d = d; else this.d = DEFAULT_D;

        Element g1 = G1.newOneElement().getImmutable();
        Element g2 = G2.newOneElement().getImmutable();
        Element s = Zp.newRandomElement().getImmutable();

        Element[] SVec = new Element[this.m + this.d + 1];
        for (int i = 0; i <= this.m + this.d; i++)
        {
            Element sPow = s.duplicate();
            for (int p = 1; p < i; p++) sPow = sPow.mul(s).getImmutable();
            SVec[i] = g2.powZn(sPow).getImmutable();
        }

        Element SPrime = g1.powZn(s).getImmutable();

        mpk = new Object[]{g1, SPrime};
        msk = new Object[]{g2, SVec};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Element[][] Sender(Element[] vVec, Element[] YVec)
    {
        if (!flag) Setup(m, n, d);
        Element g1 = (Element) mpk[0];
        Element SPrime = (Element) mpk[1];

        int k = (int)(Math.random() * n);
        Element[] tVec = new Element[n];
        Element[] TVec = new Element[n];
        for (int j = 0; j < n; j++)
        {
            tVec[j] = Zp.newRandomElement().getImmutable();
            TVec[j] = g1.powZn(tVec[j]).getImmutable();
        }

        Element[] UVec = new Element[n];
        for (int j = 0; j < n; j++)
        {
            int piJ = (j + k) % n;
            UVec[j] = SPrime.duplicate().mul(g1.powZn(YVec[piJ].duplicate().negate())).getImmutable();
        }

        Element[] tPrimeVec = new Element[d];
        Element[] TPrimeVec = new Element[d];
        for (int j = 0; j < d; j++)
        {
            tPrimeVec[j] = Zp.newRandomElement().getImmutable();
            TPrimeVec[j] = g1.powZn(tPrimeVec[j]).getImmutable();
        }

        Element[] UPrimeVec = new Element[d];
        for (int j = 0; j < d; j++)
            UPrimeVec[j] = SPrime.duplicate().mul(g1.powZn(vVec[j].duplicate().negate()).powZn(tPrimeVec[j])).getImmutable();

        // Combine: T||T', U||U'
        Element[] TTPrime = new Element[n + d];
        Element[] UUPrime = new Element[n + d];
        System.arraycopy(TVec, 0, TTPrime, 0, n);
        System.arraycopy(TPrimeVec, 0, TTPrime, n, d);
        System.arraycopy(UVec, 0, UUPrime, 0, n);
        System.arraycopy(UPrimeVec, 0, UUPrime, n, d);

        return new Element[][]{TTPrime, UUPrime};
    }

    public Object[] Receiver(Element[] vVec, Element[] XVec)
    {
        if (!flag) Setup(m, n, d);
        Element[] SVec = (Element[]) msk[1];

        // X' = X || v
        Element[] XPrimeVec = new Element[m + d];
        System.arraycopy(XVec, 0, XPrimeVec, 0, m);
        System.arraycopy(vVec, 0, XPrimeVec, m, d);

        Element r = Zp.newRandomElement().getImmutable();

        // xPoints = [1, 2, ..., m+d], yPoints = XPrimeVec
        Element[] xPoints = new Element[m + d];
        for (int j = 0; j < m + d; j++)
            xPoints[j] = Zp.newElement(BigInteger.valueOf(j + 1)).getImmutable();

        Element[] lagrangeTerms = new Element[m + d + 1];
        for (int j = 0; j <= m + d; j++)
        {
            Element x_j = Zp.newElement(BigInteger.valueOf(Math.max(j, 1))).getImmutable();
            lagrangeTerms[j] = SVec[j].powZn(computeLagrangeCoefficients(xPoints, XPrimeVec, x_j)).getImmutable();
        }
        Element R = product(lagrangeTerms).powZn(r).getImmutable();

        Element[] RPrimeVec = new Element[m + d];
        for (int i = 0; i < m + d; i++)
        {
            Element[] subTerms = new Element[m + d];
            for (int j = 0; j < m + d; j++)
            {
                Element x_j = Zp.newElement(BigInteger.valueOf(Math.max(j, 1))).getImmutable();
                subTerms[j] = SVec[j].powZn(computeLagrangeCoefficients(xPoints, XPrimeVec, x_j)).getImmutable();
            }
            RPrimeVec[i] = product(subTerms).powZn(r).getImmutable();
        }

        return new Object[]{R, RPrimeVec};
    }

    public long[] Cloud1(Element[] TTPrime, Element R)
    {
        if (!flag) Setup(m, n, d);
        long[] WVec = new long[n + d];
        for (int j = 0; j < n + d; j++)
            WVec[j] = hashToLong(pairing.pairing(TTPrime[j], R));

        int k1 = (int)(Math.random() * (n + d));
        long[] permuted = new long[n + d];
        for (int j = 0; j < n + d; j++)
            permuted[j] = WVec[(j + k1) % (n + d)];

        return permuted;
    }

    public long[] Cloud2(Element[] UUPrime, Element[] RPrimeVec)
    {
        if (!flag) Setup(m, n, d);
        long[] KVec = new long[(m + d) * (n + d)];
        int idx = 0;
        for (int i = 0; i < m + d; i++)
            for (int j = 0; j < n + d; j++)
                KVec[idx++] = hashToLong(pairing.pairing(UUPrime[j], RPrimeVec[i]));
        return KVec;
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
            for (Object o : arr) { long s = getLengthOf(o); if (s < 1) return -1; sum += s; }
            return sum;
        }
        else return -1;
    }
}
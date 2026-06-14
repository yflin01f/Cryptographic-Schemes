import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import java.math.BigInteger;

/**
 * Java implementation of the IBPRME cryptographic scheme.
 * Converted from SchemeIBPRME.py based on JPBC library.
 * This scheme is only applicable to symmetric groups of prime orders.
 */
@SuppressWarnings("unchecked")
public class SchemeIBPRME
{
    private Pairing pairing;
    private Field<Element> G1, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;
    private int operand;

    public SchemeIBPRME(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.Zp = pairing.getZr();
        this.operand = (1 << 512) - 1;
    }

    public Object[][] Setup()
    {
        flag = false;
        Element g = G1.newOneElement().getImmutable();
        Element h = G1.newRandomElement().getImmutable();
        Element x = Zp.newRandomElement().getImmutable();
        Element alpha = Zp.newRandomElement().getImmutable();

        Element y = g.powZn(x).getImmutable();

        mpk = new Object[]{g, h, y}; // H1-H7 replaced by newElementFromHash
        msk = new Object[]{x, alpha};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Object[] DKGen(byte[] id_R)
    {
        if (!flag) Setup();
        Element x_val = (Element) msk[0];
        Element alpha = (Element) msk[1];

        Element H1_id = G1.newElementFromHash(id_R, 0, id_R.length).getImmutable();
        Element dk1 = H1_id.powZn(x_val).getImmutable();
        Element dk2 = H1_id.powZn(alpha).getImmutable();
        return new Object[]{dk1, dk2};
    }

    public Element EKGen(byte[] id_S)
    {
        if (!flag) Setup();
        Element alpha = (Element) msk[1];
        Element H2_id = G1.newElementFromHash(id_S, 0, id_S.length).getImmutable();
        return H2_id.powZn(alpha).getImmutable();
    }

    public Object[] ReEKGen(Element ekId2, Object[] dkId2, byte[] id1, byte[] id2, byte[] id3)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[0];
        Element h = (Element) mpk[1];
        Element y = (Element) mpk[2];

        Element N = Zp.newRandomElement().getImmutable();
        Element xBar = Zp.newRandomElement().getImmutable();

        Element rk1 = g.powZn(xBar).getImmutable();
        Element dk1 = (Element) dkId2[0];
        Element H1_id3 = G1.newElementFromHash(id3, 0, id3.length).getImmutable();
        Element pair_y_H1 = pairing.pairing(y, H1_id3).getImmutable();
        Element H6_input = pair_y_H1.powZn(xBar).getImmutable();
        byte[] H6_bytes = H6_input.toCanonicalRepresentation();
        Element H6 = G1.newElementFromHash(H6_bytes, 0, H6_bytes.length).getImmutable();
        Element rk2 = dk1.duplicate().mul(h.powZn(xBar)).mul(H6).getImmutable();

        Element ek_id_2 = ekId2;
        Element H1_id3b = G1.newElementFromHash(id3, 0, id3.length).getImmutable();
        Element K = pairing.pairing(ek_id_2, H1_id3b).getImmutable();

        Element H2_id1 = G1.newElementFromHash(id1, 0, id1.length).getImmutable();
        byte[] K_bytes = K.toCanonicalRepresentation();
        byte[] concat = new byte[K_bytes.length + id2.length + id3.length + 32];
        System.arraycopy(K_bytes, 0, concat, 0, K_bytes.length);
        System.arraycopy(id2, 0, concat, K_bytes.length, id2.length);
        System.arraycopy(id3, 0, concat, K_bytes.length + id2.length, id3.length);
        byte[] N_bytes = N.toCanonicalRepresentation();
        System.arraycopy(N_bytes, 0, concat, K_bytes.length + id2.length + id3.length, N_bytes.length);
        Element H7 = G1.newElementFromHash(concat, 0, concat.length).getImmutable();

        Element dk2 = (Element) dkId2[1];
        Element rk3_inner = H7.duplicate().mul(dk2).getImmutable();
        Element rk3 = pairing.pairing(H2_id1, rk3_inner).getImmutable();

        return new Object[]{N, rk1, rk2, rk3};
    }

    public Object[] Enc(Element ekId1, byte[] id2Bytes, int message)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[0];
        Element h = (Element) mpk[1];
        Element y = (Element) mpk[2];

        int m = message & operand;

        Element sigma = G1.newRandomElement().getImmutable();
        Element eta = pairing.pairing(g, g).duplicate().getImmutable(); // GT random

        byte[] m_bytes = BigInteger.valueOf(m).toByteArray();
        byte[] sigma_bytes = sigma.toCanonicalRepresentation();
        byte[] eta_bytes = eta.toCanonicalRepresentation();
        byte[] hashInput = new byte[m_bytes.length + sigma_bytes.length + eta_bytes.length];
        System.arraycopy(m_bytes, 0, hashInput, 0, m_bytes.length);
        System.arraycopy(sigma_bytes, 0, hashInput, m_bytes.length, sigma_bytes.length);
        System.arraycopy(eta_bytes, 0, hashInput, m_bytes.length + sigma_bytes.length, eta_bytes.length);
        Element r = Zp.newElementFromHash(hashInput, 0, hashInput.length).getImmutable();

        Element ct1 = h.powZn(r).getImmutable();
        Element ct2 = g.powZn(r).getImmutable();

        Element H1_id2 = G1.newElementFromHash(id2Bytes, 0, id2Bytes.length).getImmutable();
        Element pair_yr = pairing.pairing(y, H1_id2).powZn(r).getImmutable();
        byte[] pair_yr_bytes = pair_yr.toCanonicalRepresentation();
        Element H4_pair = G1.newElementFromHash(pair_yr_bytes, 0, pair_yr_bytes.length).getImmutable();
        byte[] H4_pair_bytes = H4_pair.toCanonicalRepresentation();

        byte[] eta_ser = eta.toCanonicalRepresentation();
        Element H4_eta = G1.newElementFromHash(eta_ser, 0, eta_ser.length).getImmutable();
        byte[] H4_eta_bytes = H4_eta.toCanonicalRepresentation();

        byte[] m_sigma = new byte[m_bytes.length + sigma_bytes.length];
        System.arraycopy(m_bytes, 0, m_sigma, 0, m_bytes.length);
        System.arraycopy(sigma_bytes, 0, m_sigma, m_bytes.length, sigma_bytes.length);

        BigInteger m_sigma_int = new BigInteger(1, m_sigma);
        BigInteger H4_pair_int = new BigInteger(1, H4_pair_bytes);
        BigInteger H4_eta_int = new BigInteger(1, H4_eta_bytes);
        BigInteger ct3_big = m_sigma_int.xor(H4_pair_int).xor(H4_eta_int);
        int ct3 = ct3_big.intValue() & operand;

        Element ct4 = eta.duplicate().mul(pairing.pairing(ekId1, H1_id2)).getImmutable();

        byte[] ct1_bytes = ct1.toCanonicalRepresentation();
        byte[] ct2_bytes = ct2.toCanonicalRepresentation();
        byte[] ct3_bytes = BigInteger.valueOf(ct3).toByteArray();
        byte[] ct4_bytes = ct4.toCanonicalRepresentation();
        byte[] H5_input = new byte[ct1_bytes.length + ct2_bytes.length + ct3_bytes.length + ct4_bytes.length];
        System.arraycopy(ct1_bytes, 0, H5_input, 0, ct1_bytes.length);
        System.arraycopy(ct2_bytes, 0, H5_input, ct1_bytes.length, ct2_bytes.length);
        System.arraycopy(ct3_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length, ct3_bytes.length);
        System.arraycopy(ct4_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length + ct3_bytes.length, ct4_bytes.length);
        Element H5 = G1.newElementFromHash(H5_input, 0, H5_input.length).getImmutable();
        Element ct5 = H5.powZn(r).getImmutable();

        return new Object[]{ct1, ct2, ct3, ct4, ct5};
    }

    public Object ReEnc(Object[] cipherText, Object[] reKey)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[0];
        Element h = (Element) mpk[1];

        Element ct1 = (Element) cipherText[0];
        Element ct2 = (Element) cipherText[1];
        int ct3 = (Integer) cipherText[2];
        Element ct4 = (Element) cipherText[3];
        Element ct5 = (Element) cipherText[4];

        Element N_rk = (Element) reKey[0];
        Element rk1 = (Element) reKey[1];
        Element rk2 = (Element) reKey[2];
        Element rk3 = (Element) reKey[3];

        // Verify: e(ct1, g) == e(h, ct2)
        boolean valid1 = pairing.pairing(ct1, g).isEqual(pairing.pairing(h, ct2));

        // Verify: e(ct1, H5(...)) == e(h, ct5)
        byte[] ct1_bytes = ct1.toCanonicalRepresentation();
        byte[] ct2_bytes = ct2.toCanonicalRepresentation();
        byte[] ct3_bytes = BigInteger.valueOf(ct3).toByteArray();
        byte[] ct4_bytes = ct4.toCanonicalRepresentation();
        byte[] H5_input = new byte[ct1_bytes.length + ct2_bytes.length + ct3_bytes.length + ct4_bytes.length];
        System.arraycopy(ct1_bytes, 0, H5_input, 0, ct1_bytes.length);
        System.arraycopy(ct2_bytes, 0, H5_input, ct1_bytes.length, ct2_bytes.length);
        System.arraycopy(ct3_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length, ct3_bytes.length);
        System.arraycopy(ct4_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length + ct3_bytes.length, ct4_bytes.length);
        Element H5 = G1.newElementFromHash(H5_input, 0, H5_input.length).getImmutable();
        boolean valid2 = pairing.pairing(ct1, H5).isEqual(pairing.pairing(h, ct5));

        if (valid1 && valid2)
        {
            Element ct4Prime = ct4.duplicate().div(rk3).getImmutable();
            Element ct6 = rk1;
            Element ct7 = pairing.pairing(rk2, ct2).div(pairing.pairing(ct1, rk1)).getImmutable();
            return new Object[]{ct2, ct3, ct4Prime, ct6, ct7, N_rk};
        }
        else
        {
            return false;
        }
    }

    public Object Dec1(Object[] dkId2, byte[] id1Bytes, Object[] cipherText)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[0];
        Element h = (Element) mpk[1];

        Element dk2 = (Element) dkId2[1];

        Element ct1 = (Element) cipherText[0];
        Element ct2 = (Element) cipherText[1];
        int ct3 = (Integer) cipherText[2];
        Element ct4 = (Element) cipherText[3];
        Element ct5 = (Element) cipherText[4];

        // Verify
        boolean valid1 = pairing.pairing(ct1, g).isEqual(pairing.pairing(h, ct2));
        byte[] ct1_bytes = ct1.toCanonicalRepresentation();
        byte[] ct2_bytes = ct2.toCanonicalRepresentation();
        byte[] ct3_bytes = BigInteger.valueOf(ct3).toByteArray();
        byte[] ct4_bytes = ct4.toCanonicalRepresentation();
        byte[] H5_input = new byte[ct1_bytes.length + ct2_bytes.length + ct3_bytes.length + ct4_bytes.length];
        System.arraycopy(ct1_bytes, 0, H5_input, 0, ct1_bytes.length);
        System.arraycopy(ct2_bytes, 0, H5_input, ct1_bytes.length, ct2_bytes.length);
        System.arraycopy(ct3_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length, ct3_bytes.length);
        System.arraycopy(ct4_bytes, 0, H5_input, ct1_bytes.length + ct2_bytes.length + ct3_bytes.length, ct4_bytes.length);
        Element H5 = G1.newElementFromHash(H5_input, 0, H5_input.length).getImmutable();
        boolean valid2 = pairing.pairing(ct1, H5).isEqual(pairing.pairing(h, ct5));

        if (valid1 && valid2)
        {
            Element H2_id1 = G1.newElementFromHash(id1Bytes, 0, id1Bytes.length).getImmutable();
            Element V = pairing.pairing(dk2, H2_id1).getImmutable();
            Element etaPrime = ct4.duplicate().div(V).getImmutable();

            Element dk1 = (Element) dkId2[0];
            Element pair_dk1_ct2 = pairing.pairing(dk1, ct2).getImmutable();
            byte[] pair_bytes = pair_dk1_ct2.toCanonicalRepresentation();
            Element H4_pair = G1.newElementFromHash(pair_bytes, 0, pair_bytes.length).getImmutable();

            byte[] etaPrime_bytes = etaPrime.toCanonicalRepresentation();
            Element H4_eta = G1.newElementFromHash(etaPrime_bytes, 0, etaPrime_bytes.length).getImmutable();

            byte[] ct3_ser = BigInteger.valueOf(ct3).toByteArray();
            BigInteger ct3_int = new BigInteger(1, ct3_ser);
            BigInteger H4_pair_int = new BigInteger(1, H4_pair.toCanonicalRepresentation());
            BigInteger H4_eta_int = new BigInteger(1, H4_eta.toCanonicalRepresentation());
            BigInteger decoded = ct3_int.xor(H4_pair_int).xor(H4_eta_int);

            byte[] decoded_bytes = decoded.toByteArray();
            int token1 = 32; // approximate secparam/8
            byte[] r_input;
            if (decoded_bytes.length > token1)
            {
                byte[] m_sigma = new byte[token1 + decoded_bytes.length - token1];
                System.arraycopy(decoded_bytes, 0, m_sigma, 0, token1);
                System.arraycopy(decoded_bytes, token1, m_sigma, token1, decoded_bytes.length - token1);
                r_input = new byte[m_sigma.length + etaPrime_bytes.length];
                System.arraycopy(m_sigma, 0, r_input, 0, m_sigma.length);
                System.arraycopy(etaPrime_bytes, 0, r_input, m_sigma.length, etaPrime_bytes.length);
            }
            else
            {
                r_input = new byte[decoded_bytes.length + etaPrime_bytes.length];
                System.arraycopy(decoded_bytes, 0, r_input, 0, decoded_bytes.length);
                System.arraycopy(etaPrime_bytes, 0, r_input, decoded_bytes.length, etaPrime_bytes.length);
            }
            Element r_check = Zp.newElementFromHash(r_input, 0, r_input.length).getImmutable();

            if (g.powZn(r_check).isEqual(ct2))
            {
                int m = new BigInteger(1, decoded_bytes).intValue() & operand;
                return m;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public Object Dec2(Object[] dkId3, byte[] id1Bytes, byte[] id2Bytes, byte[] id3Bytes, Object cipherTextPrime)
    {
        if (!flag) Setup();
        if (cipherTextPrime instanceof Boolean)
            return false;

        Object[] ctPrime = (Object[]) cipherTextPrime;
        Element g = (Element) mpk[0];
        Element y = (Element) mpk[2];

        Element dk2 = (Element) dkId3[1];

        Element ct2 = (Element) ctPrime[0];
        int ct3 = (Integer) ctPrime[1];
        Element ct4Prime = (Element) ctPrime[2];
        Element ct6 = (Element) ctPrime[3];
        Element ct7 = (Element) ctPrime[4];
        Element N = (Element) ctPrime[5];

        Element H2_id2 = G1.newElementFromHash(id2Bytes, 0, id2Bytes.length).getImmutable();
        Element V = pairing.pairing(dk2, H2_id2).getImmutable();

        Element H2_id1 = G1.newElementFromHash(id1Bytes, 0, id1Bytes.length).getImmutable();
        byte[] V_bytes = V.toCanonicalRepresentation();
        byte[] N_bytes = N.toCanonicalRepresentation();
        byte[] concat = new byte[V_bytes.length + id2Bytes.length + id3Bytes.length + N_bytes.length];
        System.arraycopy(V_bytes, 0, concat, 0, V_bytes.length);
        System.arraycopy(id2Bytes, 0, concat, V_bytes.length, id2Bytes.length);
        System.arraycopy(id3Bytes, 0, concat, V_bytes.length + id2Bytes.length, id3Bytes.length);
        System.arraycopy(N_bytes, 0, concat, V_bytes.length + id2Bytes.length + id3Bytes.length, N_bytes.length);
        Element H7 = G1.newElementFromHash(concat, 0, concat.length).getImmutable();
        Element etaPrime = ct4Prime.duplicate().mul(pairing.pairing(H2_id1, H7)).getImmutable();

        Element dk1 = (Element) dkId3[0];
        Element pair_dk1_ct6 = pairing.pairing(dk1, ct6).getImmutable();
        byte[] pair_bytes = pair_dk1_ct6.toCanonicalRepresentation();
        Element H6 = G1.newElementFromHash(pair_bytes, 0, pair_bytes.length).getImmutable();
        Element R = ct7.duplicate().div(pairing.pairing(H6, ct2)).getImmutable();

        byte[] R_bytes = R.toCanonicalRepresentation();
        Element H4_R = G1.newElementFromHash(R_bytes, 0, R_bytes.length).getImmutable();
        byte[] etaPrime_bytes = etaPrime.toCanonicalRepresentation();
        Element H4_eta = G1.newElementFromHash(etaPrime_bytes, 0, etaPrime_bytes.length).getImmutable();

        byte[] ct3_ser = BigInteger.valueOf(ct3).toByteArray();
        BigInteger ct3_int = new BigInteger(1, ct3_ser);
        BigInteger H4_R_int = new BigInteger(1, H4_R.toCanonicalRepresentation());
        BigInteger H4_eta_int = new BigInteger(1, H4_eta.toCanonicalRepresentation());
        BigInteger decoded = ct3_int.xor(H4_R_int).xor(H4_eta_int);

        byte[] decoded_bytes = decoded.toByteArray();
        int token1 = 32;
        byte[] r_input;
        if (decoded_bytes.length > token1)
        {
            byte[] m_sigma = new byte[token1 + decoded_bytes.length - token1];
            System.arraycopy(decoded_bytes, 0, m_sigma, 0, token1);
            System.arraycopy(decoded_bytes, token1, m_sigma, token1, decoded_bytes.length - token1);
            r_input = new byte[m_sigma.length + etaPrime_bytes.length];
            System.arraycopy(m_sigma, 0, r_input, 0, m_sigma.length);
            System.arraycopy(etaPrime_bytes, 0, r_input, m_sigma.length, etaPrime_bytes.length);
        }
        else
        {
            r_input = new byte[decoded_bytes.length + etaPrime_bytes.length];
            System.arraycopy(decoded_bytes, 0, r_input, 0, decoded_bytes.length);
            System.arraycopy(etaPrime_bytes, 0, r_input, decoded_bytes.length, etaPrime_bytes.length);
        }
        Element r_check = Zp.newElementFromHash(r_input, 0, r_input.length).getImmutable();

        if (g.powZn(r_check).isEqual(ct2))
        {
            return new BigInteger(1, decoded_bytes).intValue() & operand;
        }
        else
        {
            return false;
        }
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
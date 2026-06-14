import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Java implementation of the IBPME cryptographic scheme.
 * Converted from SchemeIBPME.py based on JPBC library.
 * This scheme is applicable to symmetric and asymmetric groups of prime orders.
 */
@SuppressWarnings("unchecked")
public class SchemeIBPME
{
    private Pairing pairing;
    private Field<Element> G1, G2, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;
    private int operand;

    public SchemeIBPME(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
        this.Zp = pairing.getZr();
        this.operand = (1 << 512) - 1;
    }

    private byte[] sha256(byte[] input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) { return new byte[32]; }
    }

    public Object[][] Setup()
    {
        flag = false;
        Element g = G1.newOneElement().getImmutable();
        Element gHat = G2.newOneElement().getImmutable();
        Element s = Zp.newRandomElement().getImmutable();
        Element alpha = Zp.newRandomElement().getImmutable();
        Element beta0 = Zp.newRandomElement().getImmutable();
        Element beta1 = Zp.newRandomElement().getImmutable();

        Element g1 = g.powZn(alpha).getImmutable();
        Element f = g.powZn(beta0).getImmutable();
        Element fHat = gHat.powZn(beta0).getImmutable();
        Element h = g.powZn(beta1).getImmutable();
        Element hHat = gHat.powZn(beta1).getImmutable();

        mpk = new Object[]{g, gHat, g1, f, h, fHat, hHat};
        msk = new Object[]{s, alpha};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Element SKGen(byte[] sigma)
    {
        if (!flag) Setup();
        Element H1_sigma = G1.newElementFromHash(sigma, 0, sigma.length).getImmutable();
        Element s_val = (Element) msk[0];
        return H1_sigma.powZn(s_val).getImmutable();
    }

    public Object[] RKGen(byte[] rho)
    {
        if (!flag) Setup();
        Element H2_rho = G2.newElementFromHash(rho, 0, rho.length).getImmutable();
        Element s_val = (Element) msk[0];
        Element alpha = (Element) msk[1];
        Element d1 = H2_rho.powZn(s_val).getImmutable();
        Element d2 = H2_rho.powZn(alpha).getImmutable();
        return new Object[]{d1, d2};
    }

    public Object[] PKGen(Object[] dkRho, byte[] sigma)
    {
        if (!flag) Setup();
        Element gHat = (Element) mpk[1];
        Element fHat = (Element) mpk[5];
        Element hHat = (Element) mpk[6];
        Element d1 = (Element) dkRho[0];
        Element d2 = (Element) dkRho[1];

        Element H1_sigma = G1.newElementFromHash(sigma, 0, sigma.length).getImmutable();
        Element eta = pairing.pairing(H1_sigma, d1).getImmutable();

        byte[] eta_bytes = eta.toCanonicalRepresentation();
        Element H_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();
        Element H3_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();

        Element y = Zp.newRandomElement().getImmutable();
        Element y1 = d2.powZn(H3_eta).mul(fHat.duplicate().mul(hHat.powZn(H_eta)).powZn(y)).getImmutable();
        Element y2 = gHat.powZn(y).getImmutable();

        return new Object[]{y1, y2};
    }

    public Object[] Enc(Element ekSigma, byte[] rcv, int message)
    {
        if (!flag) Setup();
        Element g = (Element) mpk[0];
        Element g1 = (Element) mpk[2];
        Element f = (Element) mpk[3];
        Element h = (Element) mpk[4];

        int m = message & operand;

        Element H2_rho = G2.newElementFromHash(rcv, 0, rcv.length).getImmutable();
        Element r = Zp.newRandomElement().getImmutable();
        Element eta = pairing.pairing(ekSigma, H2_rho).getImmutable();

        byte[] eta_bytes = eta.toCanonicalRepresentation();
        Element H_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();
        Element H3_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();

        Element K_R = pairing.pairing(g1, H2_rho).powZn(r.duplicate().mul(H3_eta)).getImmutable();
        Element C1 = g.powZn(r).getImmutable();
        Element C2 = f.duplicate().mul(h.powZn(H_eta)).powZn(r).getImmutable();

        byte[] m_bytes = BigInteger.valueOf(m).toByteArray();
        byte[] K_R_bytes = K_R.toCanonicalRepresentation();
        byte[] K_C_input = new byte[m_bytes.length + eta_bytes.length + K_R_bytes.length];
        System.arraycopy(m_bytes, 0, K_C_input, 0, m_bytes.length);
        System.arraycopy(eta_bytes, 0, K_C_input, m_bytes.length, eta_bytes.length);
        System.arraycopy(K_R_bytes, 0, K_C_input, m_bytes.length + eta_bytes.length, K_R_bytes.length);
        byte[] K_C = sha256(K_C_input);

        byte[] C1_bytes = C1.toCanonicalRepresentation();
        byte[] C2_bytes = C2.toCanonicalRepresentation();
        byte[] Y_input = new byte[m_bytes.length + K_C.length + K_R_bytes.length + C1_bytes.length + C2_bytes.length];
        System.arraycopy(m_bytes, 0, Y_input, 0, m_bytes.length);
        System.arraycopy(K_C, 0, Y_input, m_bytes.length, K_C.length);
        System.arraycopy(K_R_bytes, 0, Y_input, m_bytes.length + K_C.length, K_R_bytes.length);
        System.arraycopy(C1_bytes, 0, Y_input, m_bytes.length + K_C.length + K_R_bytes.length, C1_bytes.length);
        System.arraycopy(C2_bytes, 0, Y_input, m_bytes.length + K_C.length + K_R_bytes.length + C1_bytes.length, C2_bytes.length);
        byte[] Y = sha256(Y_input);

        byte[] H6_K_R = sha256(K_R_bytes);
        byte[] m_KC_Y = new byte[m_bytes.length + K_C.length + Y.length];
        System.arraycopy(m_bytes, 0, m_KC_Y, 0, m_bytes.length);
        System.arraycopy(K_C, 0, m_KC_Y, m_bytes.length, K_C.length);
        System.arraycopy(Y, 0, m_KC_Y, m_bytes.length + K_C.length, Y.length);

        BigInteger m_KC_Y_int = new BigInteger(1, m_KC_Y);
        BigInteger H6_int = new BigInteger(1, H6_K_R);
        BigInteger C3_big = m_KC_Y_int.xor(H6_int);
        int C3 = C3_big.intValue() & operand;

        return new Object[]{C1, C2, C3};
    }

    public Object ProxyDec(Object[] pdk, Object[] cipher)
    {
        if (!flag) Setup();
        Element y1 = (Element) pdk[0];
        Element y2 = (Element) pdk[1];
        Element C1 = (Element) cipher[0];
        Element C2 = (Element) cipher[1];
        int C3 = (Integer) cipher[2];

        Element K_R = pairing.pairing(C1, y1).div(pairing.pairing(C2, y2)).getImmutable();
        byte[] K_R_bytes = K_R.toCanonicalRepresentation();
        byte[] H6_K_R = sha256(K_R_bytes);

        byte[] C3_bytes = BigInteger.valueOf(C3).toByteArray();
        BigInteger C3_int = new BigInteger(1, C3_bytes);
        BigInteger H6_int = new BigInteger(1, H6_K_R);
        BigInteger m_KC_Y_int = C3_int.xor(H6_int);

        byte[] m_KC_Y = m_KC_Y_int.toByteArray();
        int token = 32; // approx secparam/8
        if (m_KC_Y.length < token * 3) return false;

        byte[] m_KC = new byte[token * 2];
        byte[] Y_from = new byte[token];
        System.arraycopy(m_KC_Y, 0, m_KC, 0, token * 2);
        System.arraycopy(m_KC_Y, m_KC_Y.length - token, Y_from, 0, token);

        byte[] Y_check_input = new byte[m_KC.length + K_R_bytes.length + C1.toCanonicalRepresentation().length + C2.toCanonicalRepresentation().length];
        System.arraycopy(m_KC, 0, Y_check_input, 0, m_KC.length);
        System.arraycopy(K_R_bytes, 0, Y_check_input, m_KC.length, K_R_bytes.length);
        System.arraycopy(C1.toCanonicalRepresentation(), 0, Y_check_input, m_KC.length + K_R_bytes.length, C1.toCanonicalRepresentation().length);
        System.arraycopy(C2.toCanonicalRepresentation(), 0, Y_check_input, m_KC.length + K_R_bytes.length + C1.toCanonicalRepresentation().length, C2.toCanonicalRepresentation().length);
        byte[] Y_check = sha256(Y_check_input);

        // Check Y
        boolean yMatch = true;
        for (int i = 0; i < token; i++)
            if (Y_from[i] != Y_check[i]) { yMatch = false; break; }

        if (yMatch)
        {
            byte[] H7_K_R = sha256(K_R_bytes);
            byte[] CT2_input = new byte[m_KC.length + H7_K_R.length];
            System.arraycopy(m_KC, 0, CT2_input, 0, m_KC.length);
            System.arraycopy(H7_K_R, 0, CT2_input, m_KC.length, H7_K_R.length);
            BigInteger m_KC_int = new BigInteger(1, m_KC);
            BigInteger H7_int = new BigInteger(1, H7_K_R);
            int CT2 = m_KC_int.xor(H7_int).intValue() & operand;
            return new Object[]{C1, CT2};
        }
        else
        {
            return false;
        }
    }

    public Object Dec1(Object[] dkRho, byte[] snd, Object[] cipher)
    {
        if (!flag) Setup();
        Element d1 = (Element) dkRho[0];
        Element d2 = (Element) dkRho[1];
        Element C1 = (Element) cipher[0];
        Element C2 = (Element) cipher[1];
        int C3 = (Integer) cipher[2];

        Element H1_sigma = G1.newElementFromHash(snd, 0, snd.length).getImmutable();
        Element eta = pairing.pairing(H1_sigma, d1).getImmutable();
        byte[] eta_bytes = eta.toCanonicalRepresentation();
        Element H3_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();

        Element K_R = pairing.pairing(C1, d2.powZn(H3_eta)).getImmutable();
        byte[] K_R_bytes = K_R.toCanonicalRepresentation();
        byte[] H6_K_R = sha256(K_R_bytes);

        byte[] C3_bytes = BigInteger.valueOf(C3).toByteArray();
        BigInteger C3_int = new BigInteger(1, C3_bytes);
        BigInteger H6_int = new BigInteger(1, H6_K_R);
        BigInteger m_KC_Y_int = C3_int.xor(H6_int);

        byte[] m_KC_Y = m_KC_Y_int.toByteArray();
        int token = 32;
        if (m_KC_Y.length < token * 3) return false;

        byte[] m_bytes = new byte[token];
        byte[] K_C_from = new byte[token];
        byte[] Y_from = new byte[token];
        System.arraycopy(m_KC_Y, 0, m_bytes, 0, token);
        System.arraycopy(m_KC_Y, token, K_C_from, 0, token);
        System.arraycopy(m_KC_Y, token * 2, Y_from, 0, token);

        byte[] K_C_check_input = new byte[m_bytes.length + eta_bytes.length + K_R_bytes.length];
        System.arraycopy(m_bytes, 0, K_C_check_input, 0, m_bytes.length);
        System.arraycopy(eta_bytes, 0, K_C_check_input, m_bytes.length, eta_bytes.length);
        System.arraycopy(K_R_bytes, 0, K_C_check_input, m_bytes.length + eta_bytes.length, K_R_bytes.length);
        byte[] K_C_check = sha256(K_C_check_input);

        byte[] C1_bytes = C1.toCanonicalRepresentation();
        byte[] C2_bytes = C2.toCanonicalRepresentation();
        byte[] Y_check_input = new byte[m_bytes.length + K_C_check.length + K_R_bytes.length + C1_bytes.length + C2_bytes.length];
        System.arraycopy(m_bytes, 0, Y_check_input, 0, m_bytes.length);
        System.arraycopy(K_C_check, 0, Y_check_input, m_bytes.length, K_C_check.length);
        System.arraycopy(K_R_bytes, 0, Y_check_input, m_bytes.length + K_C_check.length, K_R_bytes.length);
        System.arraycopy(C1_bytes, 0, Y_check_input, m_bytes.length + K_C_check.length + K_R_bytes.length, C1_bytes.length);
        System.arraycopy(C2_bytes, 0, Y_check_input, m_bytes.length + K_C_check.length + K_R_bytes.length + C1_bytes.length, C2_bytes.length);
        byte[] Y_check = sha256(Y_check_input);

        boolean kcMatch = true, yMatch = true;
        for (int i = 0; i < token; i++)
        {
            if (K_C_from[i] != K_C_check[i]) kcMatch = false;
            if (Y_from[i] != Y_check[i]) yMatch = false;
        }

        if (!kcMatch || !yMatch)
            return false;
        else
            return new BigInteger(1, m_bytes).intValue() & operand;
    }

    public Object Dec2(Object[] dkRho, byte[] snd, Object cipherText)
    {
        if (!flag) Setup();
        if (cipherText instanceof Boolean) return false;

        Object[] CT = (Object[]) cipherText;
        Element d1 = (Element) dkRho[0];
        Element d2 = (Element) dkRho[1];
        Element CT1 = (Element) CT[0];
        int CT2 = (Integer) CT[1];

        Element H1_sigma = G1.newElementFromHash(snd, 0, snd.length).getImmutable();
        Element eta = pairing.pairing(H1_sigma, d1).getImmutable();
        byte[] eta_bytes = eta.toCanonicalRepresentation();
        Element H3_eta = Zp.newElementFromHash(eta_bytes, 0, eta_bytes.length).getImmutable();

        Element K_R = pairing.pairing(CT1, d2.powZn(H3_eta)).getImmutable();
        byte[] K_R_bytes = K_R.toCanonicalRepresentation();

        byte[] H7_K_R = sha256(K_R_bytes);
        byte[] CT2_bytes = BigInteger.valueOf(CT2).toByteArray();
        BigInteger CT2_int = new BigInteger(1, CT2_bytes);
        BigInteger H7_int = new BigInteger(1, H7_K_R);
        BigInteger m_KC_int = CT2_int.xor(H7_int);

        byte[] m_KC = m_KC_int.toByteArray();
        int token = 32;
        if (m_KC.length < token * 2) return false;

        byte[] m_bytes = new byte[token];
        byte[] K_C_from = new byte[token];
        System.arraycopy(m_KC, 0, m_bytes, 0, token);
        System.arraycopy(m_KC, token, K_C_from, 0, token);

        byte[] K_C_check_input = new byte[m_bytes.length + eta_bytes.length + K_R_bytes.length];
        System.arraycopy(m_bytes, 0, K_C_check_input, 0, m_bytes.length);
        System.arraycopy(eta_bytes, 0, K_C_check_input, m_bytes.length, eta_bytes.length);
        System.arraycopy(K_R_bytes, 0, K_C_check_input, m_bytes.length + eta_bytes.length, K_R_bytes.length);
        byte[] K_C_check = sha256(K_C_check_input);

        boolean kcMatch = true;
        for (int i = 0; i < token; i++)
            if (K_C_from[i] != K_C_check[i]) { kcMatch = false; break; }

        if (!kcMatch) return false;
        return new BigInteger(1, m_bytes).intValue() & operand;
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
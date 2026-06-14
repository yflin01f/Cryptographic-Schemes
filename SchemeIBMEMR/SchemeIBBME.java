import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import java.math.BigInteger;

/**
 * Java implementation of the IBB-ME cryptographic scheme.
 * Converted from SchemeIBBME.py based on JPBC library.
 */
@SuppressWarnings("unchecked")
public class SchemeIBBME
{
    private static final int DEFAULT_L = 30;
    private int l;
    private Pairing pairing;
    private Field<Element> G1, G2, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;

    public SchemeIBBME(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
        this.Zp = pairing.getZr();
        this.l = DEFAULT_L;
    }

    private Element product(Element[] elements)
    {
        Element r = elements[0].duplicate().getImmutable();
        for (int i = 1; i < elements.length; i++) r = r.mul(elements[i]).getImmutable();
        return r;
    }

    private Element[] computeCoefficients(Element[] roots)
    {
        int n = roots.length;
        Element[] coefficients = new Element[n + 1];
        for (int i = 0; i < n - 1; i++) coefficients[i] = null;
        coefficients[n - 1] = roots[0].duplicate().getImmutable();
        coefficients[n] = Zp.newOneElement().getImmutable();

        int cnt = n - 2;
        for (int idx = 1; idx < n; idx++)
        {
            Element r = roots[idx];
            coefficients[cnt] = r.duplicate().mul(coefficients[cnt + 1]).getImmutable();
            for (int i = cnt + 1; i < n - 1; i++)
                coefficients[i] = coefficients[i].duplicate().add(r.duplicate().mul(coefficients[i + 1])).getImmutable();
            coefficients[n - 1] = coefficients[n - 1].duplicate().add(r).getImmutable();
            cnt--;
        }
        for (int i = n - 1; i >= 0; i -= 2)
            coefficients[i] = coefficients[i].duplicate().negate().getImmutable();

        return coefficients;
    }

    private Element[] computeCoefficientsWithOffset(Element[] roots, Element k)
    {
        Element[] coeffs = computeCoefficients(roots);
        coeffs[0] = coeffs[0].duplicate().add(k).getImmutable();
        return coeffs;
    }

    private Element computePolynomial(Element x, Element[] coefficients)
    {
        int n = coefficients.length - 1;
        Element result = coefficients[0].duplicate();
        for (int i = 1; i < n; i++)
        {
            Element xPow = x.duplicate();
            for (int j = 1; j < i; j++) xPow = xPow.mul(x).getImmutable();
            result = result.add(coefficients[i].duplicate().mul(xPow)).getImmutable();
        }
        Element xPowN = x.duplicate();
        for (int j = 1; j < n; j++) xPowN = xPowN.mul(x).getImmutable();
        result = result.add(xPowN).getImmutable();
        return result;
    }

    public Object[][] Setup(int l)
    {
        flag = false;
        this.l = (l >= 3) ? l : DEFAULT_L;

        Element g = G1.newOneElement().getImmutable();
        Element h = G2.newRandomElement().getImmutable();
        Element v = G1.newRandomElement().getImmutable();
        Element beta = Zp.newRandomElement().getImmutable();
        Element beta1 = Zp.newRandomElement().getImmutable();
        Element beta2 = Zp.newRandomElement().getImmutable();
        Element alpha = Zp.newRandomElement().getImmutable();
        Element rho = Zp.newRandomElement().getImmutable();
        Element b = Zp.newRandomElement().getImmutable();
        Element tau = Zp.newRandomElement().getImmutable();
        Element t = Zp.newRandomElement().getImmutable();
        Element t1 = Zp.newRandomElement().getImmutable();
        Element t2 = Zp.newRandomElement().getImmutable();

        Element[] rVec = new Element[this.l + 1];
        Element[] rVec1 = new Element[this.l + 1];
        Element[] rVec2 = new Element[this.l + 1];
        for (int i = 0; i <= this.l; i++)
        {
            rVec[i] = Zp.newRandomElement().getImmutable();
            rVec1[i] = Zp.newRandomElement().getImmutable();
            rVec2[i] = Zp.newRandomElement().getImmutable();
        }

        Element vRho = v.powZn(rho).getImmutable();
        Element gb = g.powZn(b).getImmutable();
        Element[] RVec = new Element[this.l + 1];
        for (int i = 0; i <= this.l; i++) RVec[i] = g.powZn(rVec[i]).getImmutable();
        Element T = g.powZn(t).getImmutable();
        Element egg_beta = pairing.pairing(g, h).powZn(beta).getImmutable();

        Element[] hRVec1 = new Element[this.l + 1];
        Element[] hRVec2 = new Element[this.l + 1];
        for (int i = 0; i <= this.l; i++)
        {
            hRVec1[i] = h.powZn(rVec1[i]).getImmutable();
            hRVec2[i] = h.powZn(rVec2[i]).getImmutable();
        }
        Element hT1 = h.powZn(t1).getImmutable();
        Element hT2 = h.powZn(t2).getImmutable();
        Element gTauBeta = g.powZn(tau.mul(beta)).getImmutable();
        Element hTauBeta1 = h.powZn(tau.mul(beta1)).getImmutable();
        Element hTauBeta2 = h.powZn(tau.mul(beta2)).getImmutable();
        Element hInvTau = h.powZn(tau.duplicate().invert()).getImmutable();

        mpk = new Object[]{v, vRho, g, gb, RVec, T, egg_beta, h, hRVec1, hRVec2, hT1, hT2, gTauBeta, hTauBeta1, hTauBeta2, hInvTau};
        msk = new Object[]{h.powZn(beta1), h.powZn(beta2), alpha, rho};
        flag = true;
        return new Object[][]{mpk, msk};
    }

    public Element EKGen(byte[] idStar)
    {
        if (!flag) Setup(l);
        Element alpha = (Element) msk[2];
        Element H1_id = G1.newElementFromHash(idStar, 0, idStar.length).getImmutable();
        return H1_id.powZn(alpha).getImmutable();
    }

    public Object[] DKGen(byte[] identity)
    {
        if (!flag) Setup(l);
        Element h = (Element) mpk[7];
        Element[] hRVec1 = (Element[]) mpk[8];
        Element[] hRVec2 = (Element[]) mpk[9];
        Element hT1 = (Element) mpk[10];
        Element hT2 = (Element) mpk[11];

        Element hBeta1 = (Element) msk[0];
        Element hBeta2 = (Element) msk[1];
        Element alpha = (Element) msk[2];
        Element rho = (Element) msk[3];

        Element H0_id = G2.newElementFromHash(identity, 0, identity.length).getImmutable();
        Element z = Zp.newRandomElement().getImmutable();
        Element[] rtags = new Element[l];
        for (int i = 0; i < l; i++) rtags[i] = Zp.newRandomElement().getImmutable();

        Element dk1 = H0_id.powZn(rho).getImmutable();
        Element dk2 = H0_id.powZn(alpha).getImmutable();
        Element dk3 = H0_id.duplicate().getImmutable();
        Element dk4 = hBeta1.duplicate().mul(hT1.powZn(z)).getImmutable();
        Element dk5 = hBeta2.duplicate().mul(hT2.powZn(z)).getImmutable();
        Element dk6 = h.powZn(z).getImmutable();

        byte[] idBytes = identity;
        Element H2_id = Zp.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();

        Element[] dk7 = new Element[l];
        Element[] dk8 = new Element[l];
        for (int j = 1; j <= l; j++)
        {
            BigInteger jBig = BigInteger.valueOf(j);
            Element base7 = hT1.powZn(rtags[j - 1]).mul(hRVec1[j]).div(hRVec1[0].powZn(H2_id.pow(jBig)));
            dk7[j - 1] = base7.powZn(z).getImmutable();
            Element base8 = hT2.powZn(rtags[j - 1]).mul(hRVec2[j]).div(hRVec2[0].powZn(H2_id.pow(jBig)));
            dk8[j - 1] = base8.powZn(z).getImmutable();
        }

        return new Object[]{dk1, dk2, dk3, dk4, dk5, dk6, dk7, dk8, rtags};
    }

    public Object[] Enc(byte[][] S, Element ekIdStar, Element message)
    {
        if (!flag) Setup(l);
        int n = S.length;

        // Unpack mpk (indices based on Python)
        Element v = (Element) mpk[0];
        Element vRho = (Element) mpk[1];
        Element g = (Element) mpk[2];
        Element gb = (Element) mpk[3];
        Element[] R = (Element[]) mpk[4];
        Element T = (Element) mpk[5];
        Element eggBeta = (Element) mpk[6];

        // Hash S to Zp elements
        Element[] H2_S = new Element[n];
        for (int i = 0; i < n; i++)
            H2_S[i] = Zp.newElementFromHash(S[i], 0, S[i].length).getImmutable();

        // y = computeCoefficients(H2(S))
        Element[] y = computeCoefficients(H2_S);
        // yVec = y + (0,...,0) padded to l+1 length
        Element[] yVec = new Element[l + 1];
        for (int i = 0; i <= l; i++)
            yVec[i] = (i < y.length) ? y[i].duplicate().getImmutable() : Zp.newZeroElement().getImmutable();

        Element s = Zp.newRandomElement().getImmutable();
        Element d2 = Zp.newRandomElement().getImmutable();
        Element ctag = Zp.newRandomElement().getImmutable();

        Element C0 = message.duplicate().mul(eggBeta.powZn(s)).getImmutable();
        Element C1 = g.powZn(s).getImmutable();
        Element C2 = gb.powZn(s).getImmutable();

        // C3 = (T^ctag * prod(R[i]^yVec[i]))^(d2*s)
        Element term = T.powZn(ctag).getImmutable();
        for (int i = 0; i <= n; i++)
            term = term.mul(R[i].powZn(yVec[i])).getImmutable();
        Element C3 = term.powZn(d2.duplicate().mul(s)).getImmutable();
        Element C4 = v.powZn(s).getImmutable();

        // V_id[i] = H3(pair(H0(S[i]), ekIdStar * gb^s * vRho^s))
        Element[] V_id = new Element[n];
        for (int i = 0; i < n; i++)
        {
            Element H0_Si = G2.newElementFromHash(S[i], 0, S[i].length).getImmutable();
            Element pairingInput = ekIdStar.duplicate().mul(gb.powZn(s)).mul(vRho.powZn(s)).getImmutable();
            Element pairResult = pairing.pairing(H0_Si, pairingInput).getImmutable();
            // H3: hash to Zp
            byte[] pairBytes = pairResult.toCanonicalRepresentation();
            V_id[i] = Zp.newElementFromHash(pairBytes, 0, pairBytes.length).getImmutable();
        }

        // bVec = computeCoefficients(V_id, k=d2)
        Element[] bVec = computeCoefficientsWithOffset(V_id, d2);

        return new Object[]{C0, C1, C2, C3, C4, ctag, yVec, bVec};
    }

    public Object Dec(byte[][] S, Object[] dkIdI, byte[] idStar, Object[] cipherText)
    {
        if (!flag) Setup(l);
        int n = S.length;

        // Unpack dk
        Element dk1 = (Element) dkIdI[0];
        Element dk2 = (Element) dkIdI[1];
        Element dk3 = (Element) dkIdI[2];
        Element dk4 = (Element) dkIdI[3];
        Element dk5 = (Element) dkIdI[4];
        Element dk6 = (Element) dkIdI[5];
        Element[] dk7 = (Element[]) dkIdI[6];
        Element[] dk8 = (Element[]) dkIdI[7];
        Element[] rtags = (Element[]) dkIdI[8];

        // Unpack ct
        Element C0 = (Element) cipherText[0];
        Element C1 = (Element) cipherText[1];
        Element C2 = (Element) cipherText[2];
        Element C3 = (Element) cipherText[3];
        Element C4 = (Element) cipherText[4];
        Element ctag = (Element) cipherText[5];
        Element[] yVec = (Element[]) cipherText[6];
        Element[] bVec = (Element[]) cipherText[7];

        // H1(idStar)
        Element H1_idStar = G1.newElementFromHash(idStar, 0, idStar.length).getImmutable();

        // V_id_i = H3(pair(dk3, C2) * pair(dk2, H1(idStar)) * pair(dk1, C4))
        Element pair1 = pairing.pairing(dk3, C2).getImmutable();
        Element pair2 = pairing.pairing(dk2, H1_idStar).getImmutable();
        Element pair3 = pairing.pairing(dk1, C4).getImmutable();
        Element pairProduct = pair1.mul(pair2).mul(pair3).getImmutable();
        byte[] pairBytes = pairProduct.toCanonicalRepresentation();
        Element V_id_i = Zp.newElementFromHash(pairBytes, 0, pairBytes.length).getImmutable();

        // d2 = computePolynomial(V_id_i, bVec)
        Element d2 = computePolynomial(V_id_i, bVec);

        // rtag = sum(yVec[i+1] * rtags[i]) for i=0..l-1
        Element rtag = yVec[1].duplicate().mul(rtags[0]).getImmutable();
        for (int i = 1; i < l; i++)
            rtag = rtag.add(yVec[i + 1].duplicate().mul(rtags[i])).getImmutable();

        if (rtag.isEqual(ctag))
        {
            return false; // m = False (perl)
        }
        else
        {
            // A = pair(C1, prod(dk7[j]^yVec[j+1])) * pair(C2, prod(dk8[j]^yVec[j+1])) / pair(C3^(1/d2), dk6)
            Element[] prod1Terms = new Element[l];
            Element[] prod2Terms = new Element[l];
            for (int j = 0; j < l; j++)
            {
                prod1Terms[j] = dk7[j].powZn(yVec[j + 1]).getImmutable();
                prod2Terms[j] = dk8[j].powZn(yVec[j + 1]).getImmutable();
            }
            Element prod1 = product(prod1Terms);
            Element prod2 = product(prod2Terms);
            Element A = pairing.pairing(C1, prod1).mul(pairing.pairing(C2, prod2))
                .div(pairing.pairing(C3.powZn(d2.duplicate().invert()), dk6)).getImmutable();

            // B = pair(C1, dk4) * pair(C2, dk5)
            Element B = pairing.pairing(C1, dk4).mul(pairing.pairing(C2, dk5)).getImmutable();

            // m = C0 * A^(1/(rtag-ctag)) * B^(-1)
            Element diff = rtag.duplicate().sub(ctag).getImmutable();
            Element m = C0.duplicate().mul(A.powZn(diff.duplicate().invert())).mul(B.duplicate().invert()).getImmutable();
            return m;
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
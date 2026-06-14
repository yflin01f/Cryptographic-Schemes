import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import java.math.BigInteger;

/**
 * Java implementation of SchemeFuzzyME.
 * Converted from SchemeFuzzyME.py line by line.
 * Variable names kept consistent with Python (removing __ prefix).
 */
@SuppressWarnings("unchecked")
public class SchemeFuzzyME
{
    private static final int DefaultN = 30;
    private static final int DefaultD = 10;
    
    private int n, d;
    private Pairing group;
    private Field<Element> G1, GT, ZR;
    private Object[] mpk;
    private Object[] msk;
    private boolean flag = false;
    
    public SchemeFuzzyME(Pairing pairing)
    {
        this.group = pairing;
        this.G1 = pairing.getG1();
        this.GT = pairing.getGT();
        this.ZR = pairing.getZr();
        this.n = DefaultN;
        this.d = DefaultD;
    }
    
    public SchemeFuzzyME()
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
    
    public Object[][] Setup(int n, int d)
    {
        // Checks
        flag = false;
        if (isInt(n) && isInt(d) && 2 <= d && d <= n)
        {
            this.n = n;
            this.d = d;
        }
        else
        {
            this.n = DefaultN;
            this.d = DefaultD;
            System.out.println("Setup: The variables n and d should be two positive integers satisfying 2 <= d <= n but they are not, which have been defaulted to " + DefaultN + " and " + DefaultD + ", respectively.");
        }
        
        // Scheme
        Element g = G1.newOneElement().getImmutable();
        Element g2 = G1.newRandomElement().getImmutable();
        Element g3 = G1.newRandomElement().getImmutable();
        
        Element[] tVec = new Element[n + 1];
        Element[] lVec = new Element[n + 1];
        for (int i = 0; i <= n; i++)
        {
            tVec[i] = G1.newRandomElement().getImmutable();
            lVec[i] = G1.newRandomElement().getImmutable();
        }
        
        Element alpha = ZR.newRandomElement().getImmutable();
        Element beta = ZR.newRandomElement().getImmutable();
        Element theta1 = ZR.newRandomElement().getImmutable();
        Element theta2 = ZR.newRandomElement().getImmutable();
        Element theta3 = ZR.newRandomElement().getImmutable();
        Element theta4 = ZR.newRandomElement().getImmutable();
        
        Element g1 = g.powZn(alpha).getImmutable();
        Element eta1 = g.powZn(theta1).getImmutable();
        Element eta2 = g.powZn(theta2).getImmutable();
        Element eta3 = g.powZn(theta3).getImmutable();
        Element eta4 = g.powZn(theta4).getImmutable();
        
        Element Y1 = group.pairing(g1, g2).powZn(theta1.mul(theta2)).getImmutable();
        Element Y2 = group.pairing(g3, g.powZn(beta)).powZn(theta1.mul(theta2)).getImmutable();
        
        mpk = new Object[]{g1, g2, g3, Y1, Y2, tVec, lVec, eta1, eta2, eta3, eta4};
        msk = new Object[]{alpha, beta, theta1, theta2, theta3, theta4};
        
        flag = true;
        return new Object[][]{mpk, msk};
    }
    
    public Object[][] EKGen(Element[] SA)
    {
        // Checks
        if (!flag)
        {
            System.out.println("EKGen: The Setup procedure has not been called yet. The program will call the Setup first and finish the EKGen subsequently.");
            Setup(n, d);
        }
        
        Element[] S_A;
        if (isValidTuple(SA, n, ZR))
        {
            S_A = SA;
        }
        else
        {
            S_A = new Element[n];
            for (int i = 0; i < n; i++)
                S_A[i] = ZR.newRandomElement().getImmutable();
            System.out.println("EKGen: The variable S_A should be a tuple containing n elements of Zr, but it is not, which has been generated randomly.");
        }
        
        // Unpack
        Element g3 = (Element) mpk[2];
        Element[] lVec = (Element[]) mpk[6];
        Element beta = (Element) msk[1];
        Element theta1 = (Element) msk[2];
        Element theta2 = (Element) msk[3];
        
        // Scheme
        Element g = G1.newOneElement().getImmutable();
        
        Element[] coefficients = new Element[d];
        coefficients[0] = beta.duplicate().getImmutable();
        for (int i = 1; i < d - 1; i++)
            coefficients[i] = ZR.newRandomElement().getImmutable();
        coefficients[d - 1] = ZR.newOneElement().getImmutable();
        
        Element[] rVec = new Element[S_A.length];
        Element[] EVec = new Element[S_A.length];
        Element[] eVec = new Element[S_A.length];
        for (int i = 0; i < S_A.length; i++)
        {
            rVec[i] = ZR.newRandomElement().getImmutable();
            EVec[i] = g3.powZn(computePolynomial(S_A[i], coefficients).mul(theta1).mul(theta2))
                .mul(H(S_A[i], g3, lVec).powZn(rVec[i])).getImmutable();
            eVec[i] = g.powZn(rVec[i]).getImmutable();
        }
        Object[][] ek_S_A = new Object[][]{EVec, eVec};
        
        return ek_S_A;
    }
    
    public Object[][][] DKGen(Element[] SB, Element[] PA)
    {
        // Checks
        if (!flag)
        {
            System.out.println("DKGen: The Setup procedure has not been called yet. The program will call the Setup first and finish the DKGen subsequently.");
            Setup(n, d);
        }
        
        Element[] S_B;
        if (isValidTuple(SB, n, ZR))
        {
            S_B = SB;
        }
        else
        {
            S_B = new Element[n];
            for (int i = 0; i < n; i++)
                S_B[i] = ZR.newRandomElement().getImmutable();
            System.out.println("DKGen: The variable S_B should be a tuple containing n elements of Zr, but it is not, which has been generated randomly.");
        }
        
        Element[] P_A;
        if (isValidTuple(PA, n, ZR))
        {
            P_A = PA;
        }
        else
        {
            P_A = new Element[n];
            for (int i = 0; i < n; i++)
                P_A[i] = ZR.newRandomElement().getImmutable();
            System.out.println("DKGen: The variable P_A should be a tuple containing n elements of Zr, but it is not, which has been generated randomly.");
        }
        
        // Unpack
        Element g2 = (Element) mpk[1];
        Element g3 = (Element) mpk[2];
        Element[] tVec = (Element[]) mpk[5];
        Element[] lVec = (Element[]) mpk[6];
        Element alpha = (Element) msk[0];
        Element beta = (Element) msk[1];
        Element theta1 = (Element) msk[2];
        Element theta2 = (Element) msk[3];
        Element theta3 = (Element) msk[4];
        Element theta4 = (Element) msk[5];
        
        // Scheme
        Element g = G1.newOneElement().getImmutable();
        Element gamma = ZR.newRandomElement().getImmutable();
        Element G_ID = G1.newRandomElement().getImmutable();
        
        Element[] coefficientsForF = new Element[d];
        coefficientsForF[0] = alpha.duplicate().getImmutable();
        for (int i = 1; i < d - 1; i++)
            coefficientsForF[i] = ZR.newRandomElement().getImmutable();
        coefficientsForF[d - 1] = ZR.newOneElement().getImmutable();
        
        Element[] coefficientsForH = new Element[d];
        coefficientsForH[0] = gamma.duplicate().getImmutable();
        for (int i = 1; i < d - 1; i++)
            coefficientsForH[i] = ZR.newRandomElement().getImmutable();
        coefficientsForH[d - 1] = ZR.newOneElement().getImmutable();
        
        Element[] coefficientsForQPrime = new Element[d];
        coefficientsForQPrime[0] = beta.duplicate().getImmutable();
        for (int i = 1; i < d - 1; i++)
            coefficientsForQPrime[i] = ZR.newRandomElement().getImmutable();
        coefficientsForQPrime[d - 1] = ZR.newOneElement().getImmutable();
        
        Element[] k1Vec = new Element[n];
        Element[] k2Vec = new Element[n];
        Element[] rPrime1Vec = new Element[n];
        Element[] rPrime2Vec = new Element[n];
        for (int i = 0; i < n; i++)
        {
            k1Vec[i] = ZR.newRandomElement().getImmutable();
            k2Vec[i] = ZR.newRandomElement().getImmutable();
            rPrime1Vec[i] = ZR.newRandomElement().getImmutable();
            rPrime2Vec[i] = ZR.newRandomElement().getImmutable();
        }
        
        Element[] dk_S_B_0 = new Element[n];
        Element[] dk_S_B_1 = new Element[n];
        Element[] dk_S_B_2 = new Element[n];
        Element[] dk_S_B_3 = new Element[n];
        Element[] dk_S_B_4 = new Element[n];
        for (int i = 0; i < n; i++)
        {
            dk_S_B_0[i] = g.powZn(
                k1Vec[i].duplicate().mul(theta1).mul(theta2)
                .add(k2Vec[i].duplicate().mul(theta3).mul(theta4))
            ).getImmutable();
            
            Element f_val = computePolynomial(S_B[i], coefficientsForF);
            Element h_val = computePolynomial(S_B[i], coefficientsForH);
            Element T_val = T(S_B[i], g2, tVec);
            
            dk_S_B_1[i] = g2.powZn(f_val.duplicate().negate().mul(theta2))
                .mul(G_ID.powZn(h_val.duplicate().negate().mul(theta2)))
                .mul(T_val.powZn(k1Vec[i].duplicate().negate().mul(theta2)))
                .getImmutable();
            
            dk_S_B_2[i] = g2.powZn(f_val.duplicate().negate().mul(theta1))
                .mul(G_ID.powZn(h_val.duplicate().negate().mul(theta1)))
                .mul(T_val.powZn(k1Vec[i].duplicate().negate().mul(theta1)))
                .getImmutable();
            
            dk_S_B_3[i] = T_val.powZn(k2Vec[i].duplicate().negate().mul(theta4)).getImmutable();
            dk_S_B_4[i] = T_val.powZn(k2Vec[i].duplicate().negate().mul(theta3)).getImmutable();
        }
        Element[][] dk_S_B = new Element[][]{dk_S_B_0, dk_S_B_1, dk_S_B_2, dk_S_B_3, dk_S_B_4};
        
        Element[] dk_P_A_0 = new Element[n];
        Element[] dk_P_A_1 = new Element[n];
        Element[] dk_P_A_2 = new Element[n];
        Element[] dk_P_A_3 = new Element[n];
        Element[] dk_P_A_4 = new Element[n];
        for (int i = 0; i < n; i++)
        {
            dk_P_A_0[i] = g.powZn(
                rPrime1Vec[i].duplicate().mul(theta1).mul(theta2)
                .add(rPrime2Vec[i].duplicate().mul(theta3).mul(theta4))
            ).getImmutable();
            
            Element qPrime_val = computePolynomial(P_A[i], coefficientsForQPrime);
            Element h_val = computePolynomial(P_A[i], coefficientsForH);
            Element H_val = H(P_A[i], g3, lVec);
            
            Element negTwo = ZR.newElement(-2).getImmutable();
            dk_P_A_1[i] = g2.powZn(negTwo.duplicate().mul(qPrime_val).mul(theta2))
                .mul(G_ID.powZn(h_val.duplicate().mul(theta2)))
                .mul(H_val.powZn(rPrime1Vec[i].duplicate().negate().mul(theta2)))
                .getImmutable();
            
            dk_P_A_2[i] = g2.powZn(negTwo.duplicate().mul(qPrime_val).mul(theta1))
                .mul(G_ID.powZn(h_val.duplicate().mul(theta1)))
                .mul(H_val.powZn(rPrime1Vec[i].duplicate().negate().mul(theta1)))
                .getImmutable();
            
            dk_P_A_3[i] = H_val.powZn(rPrime2Vec[i].duplicate().negate().mul(theta4)).getImmutable();
            dk_P_A_4[i] = H_val.powZn(rPrime2Vec[i].duplicate().negate().mul(theta3)).getImmutable();
        }
        Element[][] dk_P_A = new Element[][]{dk_P_A_0, dk_P_A_1, dk_P_A_2, dk_P_A_3, dk_P_A_4};
        
        Element[][][] dk_SBPA = new Element[][][]{dk_S_B, dk_P_A};
        return dk_SBPA;
    }
    
    public Object[] Encryption(Object[][] ekSA, Element[] SA, Element[] PB, Element message)
    {
        // Checks
        if (!flag)
        {
            System.out.println("Encryption: The Setup procedure has not been called yet. The program will call the Setup first and finish the Encryption subsequently.");
            Setup(n, d);
        }
        
        Element[] S_A;
        Object[][] ek_S_A;
        if (isValidTuple(SA, n, ZR))
        {
            S_A = SA;
            if (ekSA != null && ekSA.length == 2 && ekSA[0] != null && ekSA[1] != null && ekSA[0].length == n && ekSA[1].length == n)
            {
                ek_S_A = ekSA;
            }
            else
            {
                ek_S_A = EKGen(S_A);
                System.out.println("Encryption: The variable ek_{S_A} should be a tuple containing 2 tuples, but it is not, which has been generated accordingly.");
            }
        }
        else
        {
            S_A = new Element[n];
            for (int i = 0; i < n; i++)
                S_A[i] = ZR.newRandomElement().getImmutable();
            System.out.println("Encryption: The variable S_A should be a tuple containing n elements of Zr, but it is not, which has been generated randomly.");
            ek_S_A = EKGen(S_A);
            System.out.println("Encryption: The variable ek_{S_A} has been generated accordingly.");
        }
        
        Element[] P_B;
        if (isValidTuple(PB, n, ZR))
        {
            P_B = PB;
        }
        else
        {
            P_B = new Element[n];
            for (int i = 0; i < n; i++)
                P_B[i] = ZR.newRandomElement().getImmutable();
            System.out.println("Encryption: The variable P_B should be a tuple containing n elements of Zr, but it is not, which has been generated randomly.");
        }
        
        Element M;
        if (message != null && message.getField().equals(GT))
        {
            M = message;
        }
        else
        {
            M = GT.newRandomElement().getImmutable();
            System.out.println("Encryption: The variable M should be an element of GT, but it is not, which has been generated randomly.");
        }
        
        // Unpack
        Element g2 = (Element) mpk[1];
        Element g3 = (Element) mpk[2];
        Element Y1 = (Element) mpk[3];
        Element Y2 = (Element) mpk[4];
        Element[] tVec = (Element[]) mpk[5];
        Element[] lVec = (Element[]) mpk[6];
        Element eta1 = (Element) mpk[7];
        Element eta2 = (Element) mpk[8];
        Element eta3 = (Element) mpk[9];
        Element eta4 = (Element) mpk[10];
        
        Element[] EVec = (Element[]) ek_S_A[0];
        Element[] eVec = (Element[]) ek_S_A[1];
        
        // Scheme
        Element g = G1.newOneElement().getImmutable();
        
        Element s = ZR.newRandomElement().getImmutable();
        Element s1 = ZR.newRandomElement().getImmutable();
        Element s2 = ZR.newRandomElement().getImmutable();
        Element tau = ZR.newRandomElement().getImmutable();
        
        Element K_s = Y1.powZn(s).getImmutable();
        Element K_l = Y2.powZn(s).mul(group.pairing(g3, g.powZn(tau.duplicate().negate()))).getImmutable();
        
        Element C0 = M.duplicate().mul(K_s).mul(K_l).getImmutable();
        Element C1 = eta1.powZn(s.duplicate().sub(s1)).getImmutable();
        Element C2 = eta2.powZn(s1).getImmutable();
        Element C3 = eta3.powZn(s.duplicate().sub(s2)).getImmutable();
        Element C4 = eta4.powZn(s2).getImmutable();
        
        Element[] C1Vec = new Element[P_B.length];
        for (int i = 0; i < P_B.length; i++)
            C1Vec[i] = T(P_B[i], g2, tVec).powZn(s).getImmutable();
        
        Element[] C2Vec = new Element[S_A.length];
        for (int i = 0; i < S_A.length; i++)
            C2Vec[i] = H(S_A[i], g3, lVec).powZn(s).getImmutable();
        
        Element[] coefficients = new Element[d];
        coefficients[0] = tau.duplicate().getImmutable();
        for (int i = 1; i < d - 1; i++)
            coefficients[i] = ZR.newRandomElement().getImmutable();
        coefficients[d - 1] = ZR.newOneElement().getImmutable();
        
        Element[] xiVec = new Element[n];
        Element[] chiVec = new Element[n];
        Element[] C3Vec = new Element[n];
        Element[] C4Vec = new Element[n];
        Element[] C5Vec = new Element[n];
        for (int i = 0; i < n; i++)
        {
            xiVec[i] = ZR.newRandomElement().getImmutable();
            chiVec[i] = ZR.newRandomElement().getImmutable();
            
            C3Vec[i] = eVec[i].duplicate().mul(g.powZn(xiVec[i])).getImmutable();
            C4Vec[i] = g.powZn(chiVec[i]).getImmutable();
            
            byte[] serialized = concatBytes(
                C0.toBytes(), C1.toBytes(), C2.toBytes(), C3.toBytes(), C4.toBytes(),
                C1Vec[i].toBytes(), C2Vec[i].toBytes(), C3Vec[i].toBytes(), C4Vec[i].toBytes()
            );
            Element H1_val = G1.newElementFromHash(serialized, 0, serialized.length).getImmutable();
            
            C5Vec[i] = EVec[i].duplicate().powZn(s)
                .mul(g3.powZn(computePolynomial(S_A[i], coefficients)))
                .mul(H(S_A[i], g3, lVec).powZn(s.duplicate().mul(xiVec[i])))
                .mul(H1_val.powZn(chiVec[i]))
                .getImmutable();
        }
        
        Object[] CT = new Object[]{C0, C1, C2, C3, C4, C1Vec, C2Vec, C3Vec, C4Vec, C5Vec};
        return CT;
    }
    
    public Object Decryption(Object[][][] dkSBPA, Element[] SA, Element[] PA, Element[] SB, Element[] PB, Object[] cipherText)
    {
        // Checks
        if (!flag)
        {
            System.out.println("Decryption: The Setup procedure has not been called yet. The program will call the Setup first and finish the Decryption subsequently.");
            Setup(n, d);
        }
        
        Element[] S_A, P_A, S_B, P_B;
        Object[][][] dk_SBPA;
        if (isValidTuple(SA, n, ZR) && isValidTuple(PA, n, ZR) && isValidTuple(SB, n, ZR) && isValidTuple(PB, n, ZR))
        {
            S_A = SA; P_A = PA; S_B = SB; P_B = PB;
            if (dkSBPA != null && dkSBPA.length == 2 && dkSBPA[0] != null && dkSBPA[1] != null && dkSBPA[0].length == 5 && dkSBPA[1].length == 5)
            {
                dk_SBPA = dkSBPA;
            }
            else
            {
                dk_SBPA = DKGen(S_B, P_A);
                System.out.println("Decryption: The variable dk_{S_B, P_A} should be a tuple containing 2 tuples, but it is not, which has been generated accordingly.");
            }
        }
        else
        {
            S_A = new Element[n]; for (int i = 0; i < n; i++) S_A[i] = ZR.newRandomElement().getImmutable();
            P_A = new Element[n]; for (int i = 0; i < n; i++) P_A[i] = ZR.newRandomElement().getImmutable();
            S_B = new Element[n]; for (int i = 0; i < n; i++) S_B[i] = ZR.newRandomElement().getImmutable();
            P_B = new Element[n]; for (int i = 0; i < n; i++) P_B[i] = ZR.newRandomElement().getImmutable();
            System.out.println("Decryption: Each of the variables S_A, P_A, S_B, P_B should be a tuple containing n elements of Zr, but at least one is not, all generated randomly.");
            dk_SBPA = DKGen(S_B, P_A);
            System.out.println("Decryption: The variable dk_{S_B, P_A} has been generated accordingly.");
        }
        
        Object[] CT;
        if (cipherText != null && cipherText.length == 10)
        {
            CT = cipherText;
        }
        else
        {
            CT = Encryption(EKGen(S_A), S_A, P_B, GT.newRandomElement());
            System.out.println("Decryption: The variable CT should be a tuple containing 10 elements, but it is not, which has been generated randomly.");
        }
        
        // Unpack
        Element[][] dk_S_B = (Element[][]) dk_SBPA[0];
        Element[][] dk_P_A = (Element[][]) dk_SBPA[1];
        Element[] dk_S_B_0 = dk_S_B[0], dk_S_B_1 = dk_S_B[1], dk_S_B_2 = dk_S_B[2], dk_S_B_3 = dk_S_B[3], dk_S_B_4 = dk_S_B[4];
        Element[] dk_P_A_0 = dk_P_A[0], dk_P_A_1 = dk_P_A[1], dk_P_A_2 = dk_P_A[2], dk_P_A_3 = dk_P_A[3], dk_P_A_4 = dk_P_A[4];
        
        Element C0 = (Element) CT[0];
        Element C1 = (Element) CT[1];
        Element C2 = (Element) CT[2];
        Element C3 = (Element) CT[3];
        Element C4 = (Element) CT[4];
        Element[] C1Vec = (Element[]) CT[5];
        Element[] C2Vec = (Element[]) CT[6];
        Element[] C3Vec = (Element[]) CT[7];
        Element[] C4Vec = (Element[]) CT[8];
        Element[] C5Vec = (Element[]) CT[9];
        
        // Scheme - use set intersection
        java.util.HashSet<Integer> waIndices = new java.util.HashSet<Integer>();
        java.util.HashSet<Integer> wbIndices = new java.util.HashSet<Integer>();
        
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
            {
                if (S_A[i].equals(P_A[j]))
                    waIndices.add(i);
                if (S_B[i].equals(P_B[j]))
                    wbIndices.add(i);
            }
        
        java.util.ArrayList<Element> WAPrime = new java.util.ArrayList<Element>();
        java.util.ArrayList<Element> WBPrime = new java.util.ArrayList<Element>();
        for (int idx : waIndices) WAPrime.add(S_A[idx]);
        for (int idx : wbIndices) WBPrime.add(S_B[idx]);
        
        if (WAPrime.size() >= d && WBPrime.size() >= d)
        {
            Element[] WA = WAPrime.subList(0, d).toArray(new Element[d]);
            Element[] WB = WBPrime.subList(0, d).toArray(new Element[d]);
            
            Element g = G1.newOneElement().getImmutable();
            
            // KsPrime = product over WB
            Element KsPrime = GT.newOneElement().getImmutable();
            for (int i = 0; i < n; i++)
            {
                // Check if S_B[i] is in WB
                boolean inWB = false;
                for (Element wb : WB)
                    if (S_B[i].equals(wb)) { inWB = true; break; }
                
                if (inWB)
                {
                    Element zero_zr = ZR.newZeroElement().getImmutable();
                    Element deltaVal = delta(S_B[i], zero_zr, WB);
                    Element term = group.pairing(C1Vec[i], dk_S_B_0[i])
                        .mul(group.pairing(C1, dk_S_B_1[i]))
                        .mul(group.pairing(C2, dk_S_B_2[i]))
                        .mul(group.pairing(C3, dk_S_B_3[i]))
                        .mul(group.pairing(C4, dk_S_B_4[i]))
                        .powZn(deltaVal).getImmutable();
                    KsPrime = KsPrime.mul(term).getImmutable();
                }
            }
            
            // CTVec and KlPrime
            Element KlPrime = GT.newOneElement().getImmutable();
            for (int i = 0; i < n; i++)
            {
                boolean inWA = false;
                for (Element wa : WA)
                    if (S_A[i].equals(wa)) { inWA = true; break; }
                
                if (inWA)
                {
                    byte[] ctBytes = concatBytes(
                        C0.toBytes(), C1.toBytes(), C2.toBytes(), C3.toBytes(), C4.toBytes(),
                        C1Vec[i].toBytes(), C2Vec[i].toBytes(), C3Vec[i].toBytes(), C4Vec[i].toBytes()
                    );
                    Element H1_ct = G1.newElementFromHash(ctBytes, 0, ctBytes.length).getImmutable();
                    
                    Element zero_zr2 = ZR.newZeroElement().getImmutable();
                    Element deltaVal = delta(S_A[i], zero_zr2, WA);
                    
                    Element numerator = group.pairing(C1Vec[i], dk_P_A_0[i])
                        .mul(group.pairing(C1, dk_P_A_1[i]))
                        .mul(group.pairing(C2, dk_P_A_2[i]));
                    
                    Element denominator = group.pairing(H1_ct, C4Vec[i])
                        .mul(group.pairing(C3Vec[i], C2Vec[i]))
                        .mul(group.pairing(C3, dk_P_A_3[i]))
                        .mul(group.pairing(C4, dk_P_A_4[i]))
                        .mul(group.pairing(C5Vec[i], g));
                    
                    Element term = numerator.div(denominator).powZn(deltaVal).getImmutable();
                    KlPrime = KlPrime.mul(term).getImmutable();
                }
            }
            
            Element M = C0.duplicate().mul(KsPrime).mul(KlPrime).getImmutable();
            return M;
        }
        else
        {
            return Boolean.FALSE;
        }
    }
    
    // Delta_{i,S}(x) = prod_{j in S, j != i} (x - j)/(i - j)
    // Python: lambda i, S, x: product(tuple((x - j) / (i - j) for j in S if j != i))
    private Element delta(Element i, Element x, Element[] S)
    {
        Element[] terms = new Element[S.length - 1];
        int idx = 0;
        for (Element j : S)
        {
            if (!i.equals(j))
            {
                terms[idx] = x.duplicate().sub(j).div(i.duplicate().sub(j));
                idx++;
            }
        }
        return product(terms);
    }
    
    // T(x) = g2^{x^n} * prod_{i=1}^{n+1} t_i^{Delta(i, N, x)}
    private Element T(Element x, Element g2, Element[] tVec)
    {
        Element[] N = new Element[n + 1];
        for (int idx = 0; idx <= n; idx++)
            N[idx] = ZR.newElement(idx + 1).getImmutable();
        
        Element result = g2.duplicate().powZn(x.duplicate().pow(BigInteger.valueOf(n))).getImmutable();
        for (int idx = 0; idx <= n; idx++)
        {
            Element iElem = ZR.newElement(idx + 1).getImmutable();
            Element coeff = delta(iElem, x, N);
            result = result.mul(tVec[idx].duplicate().powZn(coeff)).getImmutable();
        }
        return result;
    }
    
    // H(x) = g3^{x^n} * prod_{i=1}^{n+1} l_i^{Delta(i, N, x)}
    private Element H(Element x, Element g3, Element[] lVec)
    {
        Element[] N = new Element[n + 1];
        for (int idx = 0; idx <= n; idx++)
            N[idx] = ZR.newElement(idx + 1).getImmutable();
        
        Element result = g3.duplicate().powZn(x.duplicate().pow(BigInteger.valueOf(n))).getImmutable();
        for (int idx = 0; idx <= n; idx++)
        {
            Element iElem = ZR.newElement(idx + 1).getImmutable();
            Element coeff = delta(iElem, x, N);
            result = result.mul(lVec[idx].duplicate().powZn(coeff)).getImmutable();
        }
        return result;
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
    
    private boolean isInt(Object x)
    {
        return x instanceof Integer || x instanceof java.math.BigInteger;
    }
    
    private boolean isValidTuple(Element[] arr, int expectedLen, Field<Element> field)
    {
        if (arr == null || arr.length != expectedLen)
            return false;
        for (Element e : arr)
        {
            if (e == null || !e.getField().equals(field))
                return false;
        }
        return true;
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
        else if (obj instanceof Object[][][])
        {
            long total = 0;
            for (Object[][] o : (Object[][][]) obj)
                total += getLengthOf(o);
            return total;
        }
        else
        {
            return 0;
        }
    }
}
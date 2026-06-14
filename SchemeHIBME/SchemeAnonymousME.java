import java.util.ArrayList;
import java.util.List;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * Java implementation of the AnonymousME cryptographic scheme.
 * Converted from SchemeAnonymousME.py based on JPBC library.
 */
@SuppressWarnings("unchecked")
public class SchemeAnonymousME
{
    private static final int DEFAULT_L = 30;
    private int l;
    private Pairing pairing;
    private Field<Element> G1, G2, Zp;
    private Object[] mpk, msk;
    private boolean flag = false;
    
    public SchemeAnonymousME(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
        this.Zp = pairing.getZr();
        this.l = DEFAULT_L;
    }
    
    private Element product(Element[] elems)
    {
        Element r = elems[0].duplicate().getImmutable();
        for (int i = 1; i < elems.length; i++) r = r.mul(elems[i]).getImmutable();
        return r;
    }
    
    public Object[][] Setup(int l)
    {
        flag = false;
        this.l = (l >= 3) ? l : DEFAULT_L;
        
        Element g = G1.newOneElement().getImmutable();
        Element g2 = G2.newRandomElement().getImmutable();
        Element g3 = G2.newRandomElement().getImmutable();
        Element alpha = Zp.newRandomElement().getImmutable();
        Element b1 = Zp.newRandomElement().getImmutable();
        Element b2 = Zp.newRandomElement().getImmutable();
        
        Element[] s = new Element[this.l];
        Element[] a = new Element[this.l];
        Element[] h = new Element[this.l];
        for (int i = 0; i < this.l; i++)
        {
            s[i] = Zp.newRandomElement().getImmutable();
            a[i] = Zp.newRandomElement().getImmutable();
            h[i] = G2.newRandomElement().getImmutable();
        }
        
        Element g1 = g.powZn(alpha).getImmutable();
        Element gBar = g.powZn(b1).getImmutable();
        Element gTilde = g.powZn(b2).getImmutable();
        Element g3Bar = g3.powZn(b1.duplicate().invert()).getImmutable();
        Element g3Tilde = g3.powZn(b2.duplicate().invert()).getImmutable();
        
        List<Object> mpkList = new ArrayList<>();
        mpkList.add(g); mpkList.add(g1); mpkList.add(g2); mpkList.add(g3);
        mpkList.add(gBar); mpkList.add(gTilde); mpkList.add(g3Bar); mpkList.add(g3Tilde);
        for (Element hi : h) mpkList.add(hi);
        mpk = mpkList.toArray();
        
        List<Object> mskList = new ArrayList<>();
        mskList.add(g2.powZn(alpha)); mskList.add(b1); mskList.add(b2);
        for (Element si : s) mskList.add(si);
        for (Element ai : a) mskList.add(ai);
        msk = mskList.toArray();
        
        flag = true;
        return new Object[][]{mpk, msk};
    }
    
    private Element HI(Element[] h, Element[] ID_k, int k)
    {
        Element r = h[0].powZn(ID_k[0]).getImmutable();
        for (int i = 1; i < k; i++)
            r = r.mul(h[i].powZn(ID_k[i])).getImmutable();
        return r;
    }
    
    public Object[] KGen(Element[] ID_k)
    {
        if (!flag) Setup(l);
        int k = ID_k.length;
        Element[] h = new Element[l];
        for (int i = 0; i < l; i++) h[i] = (Element)mpk[8 + i];
        
        Element g2Alpha = (Element)msk[0];
        Element b1 = (Element)msk[1]; Element b2 = (Element)msk[2];
        Element[] s = new Element[l];
        Element[] a = new Element[l];
        for (int i = 0; i < l; i++) { s[i] = (Element)msk[3+i]; a[i] = (Element)msk[3+l+i]; }
        
        Element hi = HI(h, ID_k, k);
        Element r = Zp.newRandomElement().getImmutable();
        
        Element a0 = g2Alpha.powZn(b1.duplicate().invert())
            .mul(hi.powZn(r.duplicate().mul(b1.duplicate().invert())))
            .mul(((Element)mpk[6]).powZn(r)).getImmutable();
        Element a1 = g2Alpha.powZn(b2.duplicate().invert())
            .mul(hi.powZn(r.duplicate().mul(b2.duplicate().invert())))
            .mul(((Element)mpk[7]).powZn(r)).getImmutable();
        
        List<Element> dk1List = new ArrayList<>();
        dk1List.add(a0); dk1List.add(a1); dk1List.add(G1.newOneElement().powZn(r));
        for (int i = k; i < l; i++)
            dk1List.add(h[i].powZn(r.duplicate().mul(b1.duplicate().invert())));
        for (int i = k; i < l; i++)
            dk1List.add(h[i].powZn(r.duplicate().mul(b2.duplicate().invert())));
        for (int i = k; i < l; i++)
            dk1List.add(h[i].powZn(b1.duplicate().invert()));
        for (int i = k; i < l; i++)
            dk1List.add(h[i].powZn(b2.duplicate().invert()));
        dk1List.add(hi.powZn(b1.duplicate().invert()));
        dk1List.add(hi.powZn(b2.duplicate().invert()));
        
        Element Ak = product(a);
        Element[] dk2 = new Element[k];
        for (int i = 0; i < k; i++)
        {
            byte[] idBytes = ID_k[i].toBytes();
            Element H2_ID = G2.newElementFromHash(idBytes, 0, idBytes.length).getImmutable();
            dk2[i] = H2_ID.powZn(s[i].mul(Ak)).getImmutable();
        }
        
        Element[] dk3 = new Element[l - k];
        Element[] dk4 = new Element[l - k];
        for (int i = 0; i < l - k; i++)
        {
            dk3[i] = s[k+i].duplicate().mul(Ak).getImmutable();
            dk4[i] = a[k+i].duplicate().getImmutable();
        }
        
        return new Object[]{dk1List.toArray(), dk2, dk3, dk4};
    }
    
    public Object[] Enc(Object[] ek_ID_k, Element[] ID_Rev, int message)
    {
        if (!flag) Setup(l);
        int n = ID_Rev.length;
        Element g = (Element)mpk[0]; Element g1 = (Element)mpk[1];
        Element g2 = (Element)mpk[2]; Element g3 = (Element)mpk[3];
        Element[] h = new Element[l];
        for (int i = 0; i < l; i++) h[i] = (Element)mpk[8+i];
        Element A = pairing.pairing(g1, g2);
        
        Element z = Zp.newRandomElement().getImmutable();
        Element[] ek1 = (Element[])ek_ID_k[0];
        Element[] ek2 = (Element[])ek_ID_k[1];
        Element[] ek3 = (Element[])ek_ID_k[2];
        
        Element hi = HI(h, ID_Rev, n);
        Element C1 = g.powZn(z).getImmutable();
        Element C2 = g3.powZn(z).mul(hi.powZn(z)).getImmutable();
        Element C3 = A.powZn(z).getImmutable();
        Element C4 = g1.powZn(z).getImmutable();
        
        byte[] c3Bytes = C3.toBytes();
        int operand = (1 << 512) - 1;
        // XOR with first bytes of C3 converted to int
        int c3Int = 0;
        for (int i = 0; i < Math.min(4, c3Bytes.length); i++)
            c3Int = (c3Int << 8) | (c3Bytes[i] & 0xFF);
        int c5Val = message ^ (c3Int & operand);
        // Simplified: use XOR-like operation
        Element C5 = Zp.newElement(c5Val & operand).getImmutable();
        
        Element[] ek1Pow = new Element[n];
        for (int i = 0; i < n; i++)
            ek1Pow[i] = ek1[i].powZn(z).getImmutable();
        
        Element[] ek2Pow = new Element[l - n];
        for (int i = 0; i < l - n; i++)
            ek2Pow[i] = ek2[i].duplicate().mul(z).getImmutable();
        
        return new Object[]{C1, C2, C3, C4, C5, ek1Pow, ek2Pow, ek3};
    }
}
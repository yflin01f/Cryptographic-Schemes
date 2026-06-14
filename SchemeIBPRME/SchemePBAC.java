import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * Java implementation of the PBAC cryptographic scheme.
 * Converted from SchemePBAC.py based on JPBC library.
 */
@SuppressWarnings("unchecked")
public class SchemePBAC
{
    private Pairing pairing;
    private Field<Element> G1, G2, Zp;
    private Object[] mpk, msk;
    
    public SchemePBAC(Pairing pairing)
    {
        this.pairing = pairing;
        this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
        this.Zp = pairing.getZr();
    }
    
    public Object[][] Setup()
    {
        Element g = G1.newRandomElement().getImmutable();
        Element g2 = G2.newRandomElement().getImmutable();
        Element u = G1.newRandomElement().getImmutable();
        Element v = G1.newRandomElement().getImmutable();
        Element alpha = Zp.newRandomElement().getImmutable();
        Element beta = Zp.newRandomElement().getImmutable();
        
        Element g1 = g.powZn(alpha).getImmutable();
        Element egg = pairing.pairing(g, g2).getImmutable();
        Element eggAlpha = egg.powZn(alpha).getImmutable();
        
        mpk = new Object[]{g, g1, g2, u, v, egg, eggAlpha};
        msk = new Object[]{alpha, beta};
        return new Object[][]{mpk, msk};
    }
}
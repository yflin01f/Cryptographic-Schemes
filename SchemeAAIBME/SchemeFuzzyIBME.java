
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import java.util.Arrays;
import static jdk.nashorn.internal.ir.debug.ObjectSizeCalculator.getObjectSize;

public class Dec
{
	public static Element dec(int d, Element[] P_A, Element[] S_A, Element[] P_B, Element[] S_B, PARS pars, Element[][] Dd, Element[][] Dd_prime, Element C_0, Element C_1, Element C_2, Element C_3, Element C_4, Element[] C_1_i, Element[] C_2_i, Element[] C_3_i, Element[] C_4_i, Element[] C_5_i)
	{
		Element[] W_A_prime = Utils.intersect(P_A, S_A);
		Element[] W_B_prime = Utils.intersect(P_B, S_B);
		if (W_A_prime.length >= d && W_B_prime.length >= d)
		{
			Element[] W_A = new Element[d];
			System.arraycopy(W_A_prime, 0, W_A, 0, W_A.length);
			Element[] W_B = new Element[d];
			System.arraycopy(W_B_prime, 0, W_B, 0, W_B.length);

			Element K_s_prime = pars.getGT().newOneElement().getImmutable();
			for (int i = 0; i < W_B.length; ++i) {
				//K_s_prime = K_s_prime.mul(pars.getPairing().pairing(D_i[i].duplicate(), C_1.duplicate()).div(pars.getPairing().pairing(d_i[i].duplicate(), C_1_i[i].duplicate())).powZn(Utils.delta(pars.getZp().newZeroElement(), W_B[i], W_B, pars))).getImmutable();
				K_s_prime = K_s_prime.mul((pars.getPairing().pairing(Dd[0][i].duplicate(), C_1_i[i].duplicate())).mul(pars.getPairing().pairing(Dd[1][i].duplicate(), C_1.duplicate()))).getImmutable(); // Revised
			}
			Element K_l_prime = pars.getGT().newOneElement().getImmutable();
			for (int i = 0; i < W_A.length; ++i) {
				//K_l_prime = K_l_prime.mul(pars.getPairing().pairing(D_i_prime[i].duplicate(), C_1.duplicate()).duplicate().mul(
				//		pars.getPairing().pairing(pars.getPairing().getG1().newElementFromBytes(Utils.byteMergerAll(C_0.toBytes(), C_1.toBytes(), C_1_i[i].toBytes(), C_2_i[i].toBytes(), C_3_i[i].toBytes(), C_4_i[i].toBytes())).duplicate(),C_4_i[i].duplicate()).duplicate().mul(pars.getPairing().pairing(C_3_i[i].duplicate(), C_2_i[i].duplicate()).duplicate())).div(pars.getPairing().pairing(d_i_prime[i].duplicate(), C_2_i[i].duplicate()).duplicate().mul(pars.getPairing().pairing(C_5_i[i].duplicate(), pars.get_g().duplicate()).duplicate())).powZn(Utils.delta(pars.getZp().newZeroElement(), W_A[i], W_A, pars))).getImmutable();
				K_l_prime = K_l_prime.mul(
						pars.getPairing().pairing(Dd_prime[0][i].duplicate(), C_1_i[i].duplicate()).duplicate().mul(
								pars.getPairing().pairing(Dd_prime[1][i].duplicate(), C_1.duplicate()).duplicate().mul(
										pars.getPairing().pairing(Dd_prime[2][i], C_3).mul(
												pars.getPairing().pairing(Dd_prime[3][i], C_4).mul(
														pars.getPairing().pairing(pars.get_g(), Dd_prime[4][i]).mul(
																pars.getPairing().pairing(Dd_prime[2][i].duplicate(), C_2.duplicate()).duplicate().div(
																		pars.getPairing().pairing(C_4_i[i], Utils.H(pars.getG().newRandomElement(), pars)).div(
																				pars.getPairing().pairing(C_2_i[i], C_3_i[i])
																		)
																)
														)
												)
										)
								)
						)
				); // revised
			}
			Element M = C_1.duplicate().div(K_s_prime.duplicate().mul(K_l_prime.duplicate())).getImmutable();
			return M;
		}
		else
			return null;
	}
}

public class DKGen {
	public static Element[][] dKGen(int d, PARS pars, Element[] S_B, Element[] P_A) {
		Element gamma = pars.getZp().newRandomElement().getImmutable();
		Polynomial f = Utils.newRandomPolynomial(d, pars.getAlpha().duplicate(), pars);
		Polynomial h = Utils.newRandomPolynomial(d, gamma.duplicate(), pars);
		Polynomial q_prime = Utils.newRandomPolynomial(d, pars.getBeta().duplicate(), pars);
		Element G_ID = pars.getG().newRandomElement().duplicate().getImmutable();
		/*
		Element[] D_i = new Element[S_B.length];
		Element[] d_i = new Element[S_B.length];
		for (int i = 0; i < S_B.length; i++) {
			Element k_i = pars.getZp().newRandomElement().getImmutable();
			d_i[i] = pars.get_g().duplicate().powZn(k_i).getImmutable();
			D_i[i] = pars.getG2().duplicate().powZn(f.evaluate(S_B[i])).mul(G_ID.powZn(h.evaluate(S_B[i]))).mul(Utils.T(S_B[i], pars).powZn(k_i)).getImmutable();
		}
		Element[] D_i_prime = new Element[P_A.length];
		Element[] d_i_prime = new Element[P_A.length];
		for (int i = 0; i < P_A.length; i++) {
			Element r_i_prime = pars.getZp().newRandomElement().getImmutable();
			d_i_prime[i] = pars.get_g().duplicate().powZn(r_i_prime).getImmutable();
			D_i_prime[i] = pars.getG2().duplicate().powZn(q_prime.evaluate(P_A[i]).mul(BigInteger.valueOf(2))).mul(G_ID.powZn(h.evaluate(P_A[i]).negate())).mul(Utils.H(P_A[i], pars).powZn(r_i_prime)).getImmutable();
		}
		return new Element[][]{D_i, d_i, D_i_prime, d_i_prime};
		*/
		/* The following are revised */
		Element Dd[][] = new Element[5][S_B.length], Dd_prime[][] = new Element[5][S_B.length];
		for (int i = 0; i < S_B.length; i++) {
			Element k_i_1 = pars.getZp().newRandomElement().getImmutable(), k_i_2 = pars.getZp().newRandomElement().getImmutable();
			Dd[0][i] = pars.get_g().duplicate().powZn((k_i_1.mul(pars.getTheta()[0].mul(pars.getTheta()[1]))).add(k_i_2.mul(pars.getTheta()[2].mul(pars.getTheta()[3])))).getImmutable();
			Dd[1][i] = pars.getG2().duplicate().powZn(f.evaluate(S_B[i]).negate().mul(pars.getTheta()[1])).mul(G_ID.powZn(h.evaluate(S_B[i]).negate().mul(pars.getTheta()[1]))).mul(Utils.T(S_B[i], pars).powZn(k_i_1.negate().mul(pars.getTheta()[1]))).getImmutable();
			Dd[2][i] = pars.getG2().duplicate().powZn(f.evaluate(S_B[i]).negate().mul(pars.getTheta()[0])).mul(G_ID.powZn(h.evaluate(S_B[i]).negate().mul(pars.getTheta()[0]))).mul(Utils.T(S_B[i], pars).powZn(k_i_1.negate().mul(pars.getTheta()[0]))).getImmutable();
		   	//Dd[3][i] = Utils.T(S_B[i], pars).mul(k_i_2.negate().mul(pars.getTheta()[3].duplicate())).getImmutable();
		   	//Dd[4][i] = Utils.T(S_B[i], pars).mul(k_i_2.negate().mul(pars.getTheta()[2].duplicate())).getImmutable();
		   	Dd[3][i] = k_i_2.negate().mul(pars.getTheta()[3].duplicate()).getImmutable();
		   	Dd[4][i] = k_i_2.negate().mul(pars.getTheta()[2].duplicate()).getImmutable();
		}
		for (int i = 0; i < P_A.length; i++) {
			Element r_i_prime_1 = pars.getZp().newRandomElement().getImmutable(), r_i_prime_2 = pars.getZp().newRandomElement().getImmutable();
			Dd_prime[0][i] = pars.get_g().duplicate().powZn((r_i_prime_1.mul(pars.getTheta()[0].mul(pars.getTheta()[1]))).add(r_i_prime_2.mul(pars.getTheta()[2].mul(pars.getTheta()[3])))).getImmutable();
			Dd_prime[1][i] = pars.getG2().duplicate().powZn(q_prime.evaluate(P_A[i]).mul(2).negate().mul(pars.getTheta()[1])).mul(G_ID.powZn(h.evaluate(S_B[i]).negate().mul(pars.getTheta()[1]))).mul(Utils.H(P_A[i], pars).powZn(r_i_prime_1.negate().mul(pars.getTheta()[1]))).getImmutable();
			Dd_prime[2][i] = pars.getG2().duplicate().powZn(q_prime.evaluate(P_A[i]).mul(2).negate().mul(pars.getTheta()[0])).mul(G_ID.powZn(h.evaluate(S_B[i]).negate().mul(pars.getTheta()[0]))).mul(Utils.H(P_A[i], pars).powZn(r_i_prime_1.negate().mul(pars.getTheta()[0]))).getImmutable();
			//Dd_prime[3][i] = Utils.H(P_A[i], pars).mul(r_i_prime_2.negate().mul(pars.getTheta()[3])).getImmutable();
			//Dd_prime[4][i] = Utils.H(P_A[i], pars).mul(r_i_prime_2.negate().mul(pars.getTheta()[2])).getImmutable();
			Dd_prime[3][i] = r_i_prime_2.negate().mul(pars.getTheta()[3]).getImmutable();
			Dd_prime[4][i] = r_i_prime_2.negate().mul(pars.getTheta()[2]).getImmutable();
		}
		return new Element[][]{Dd[0], Dd[1], Dd[2], Dd[3], Dd[4], Dd_prime[0], Dd_prime[1], Dd_prime[2], Dd_prime[3], Dd_prime[4]};
	}
}

public class EKGen {
	public static Element[][] eKGen(int d, PARS pars, Element[] S_A) {
		Polynomial q = Utils.newRandomPolynomial(d, pars.getBeta(), pars);
		Element[] E_i = new Element[S_A.length];
		Element[] e_i = new Element[S_A.length];
		for (int i = 0; i < S_A.length; i++) {
			Element r_i = pars.getZp().newRandomElement().getImmutable();
			e_i[i] = pars.get_g().duplicate().powZn(r_i).getImmutable();
			E_i[i] = pars.getG2().duplicate().powZn(q.evaluate(S_A[i]).mul(pars.getTheta()[0].mul(pars.getTheta()[1]))).mul(Utils.H(S_A[i], pars).powZn(r_i)).getImmutable(); // Revised
		}
		return new Element[][]{E_i, e_i};
	}
}


public class Enc {
	public static List<Object> enc(int d, PARS pars, Element[] S_A, Element[] P_B, Element[] E_i, Element[] e_i, Element M){
		Element s = pars.getZp().newRandomElement().getImmutable(), s_1 = pars.getZp().newRandomElement().getImmutable(), s_2 = pars.getZp().newRandomElement().getImmutable();
		Element tau = pars.getZp().newRandomElement().getImmutable();
		Element K_s = pars.getPairing().pairing(pars.getG1().duplicate(), pars.getG2().duplicate()).powZn(s).getImmutable();
		Element K_l = pars.getY().duplicate().powZn(s).mul(pars.getPairing().pairing(pars.getG2().duplicate(), pars.get_g().duplicate().powZn(tau.negate()))).getImmutable();
		Element C_0 = M.duplicate().mul(K_s.duplicate()).duplicate().mul(K_l.duplicate()).getImmutable();
		Element C_1 = pars.getEta()[0].powZn(s.sub(s_1)).getImmutable(); // Revised
		Element C_2 = pars.getEta()[1].powZn(s_1).getImmutable(); // Revised
		Element C_3 = pars.getEta()[2].powZn(s.sub(s_2)).getImmutable(); // Revised
		Element C_4 = pars.getEta()[3].powZn(s_2).getImmutable(); // Revised
		
		Element[] C_1_i = new Element[P_B.length];
		for (int i = 0; i < P_B.length; i++) {
			C_1_i[i] = Utils.T(P_B[i], pars).powZn(s).getImmutable();
		}
		Element[] C_2_i = new Element[S_A.length];
		for (int i = 0; i < S_A.length; i++) {
			C_2_i[i] = Utils.H(S_A[i], pars).powZn(s).getImmutable();
		}

		Polynomial l = Utils.newRandomPolynomial(d, tau.duplicate(), pars);
		Element[] C_3_i = new Element[d];
		Element[] C_4_i = new Element[d];
		Element[] C_5_i = new Element[d];
		for (int i = 0; i < C_3_i.length; i++) {
			Element xi_i = pars.getZp().newRandomElement().getImmutable();
			Element chi_i = pars.getZp().newRandomElement().getImmutable();
			C_3_i[i] = e_i[i].duplicate().mul(pars.get_g().duplicate().powZn(xi_i.duplicate())).getImmutable();
			C_4_i[i] = pars.get_g().duplicate().powZn(chi_i.duplicate()).getImmutable();
			C_5_i[i] = E_i[i].duplicate().powZn(s.duplicate()).mul(pars.getG2().duplicate().powZn(l.evaluate(S_A[i]).duplicate())).duplicate().mul(Utils.H(S_A[i].duplicate(), pars).duplicate().powZn(xi_i.duplicate().mul(s.duplicate()))).duplicate().mul(pars.getPairing().getG1().newElementFromBytes(Utils.byteMergerAll(C_0.toBytes(), C_1.toBytes(), C_1_i[i].toBytes(), C_2_i[i].toBytes(), C_3_i[i].toBytes(), C_4_i[i].toBytes())).duplicate().powZn(chi_i.duplicate()));
		}

		List<Object> result = new ArrayList<>();
		result.add(C_0);
		result.add(C_1);
		result.add(C_2);
		result.add(C_3); // revised
		result.add(C_4); // revised
		result.add(C_1_i);
		result.add(C_2_i);
		result.add(C_3_i);
		result.add(C_4_i);
		result.add(C_5_i);
		return result;
	}
}

public class PARS {
    private Element g;
    private Element g1;
    private Element g2;
    private Element Y;
    private Element[] t;
    private Element[] l;
    private Element[] eta; // revised
    private Element[] theta; // revised
    private Element alpha;
    private Element beta;
    private Pairing pairing;
    private Field<Element> G;
    private Field<Element> GT;
    private Field<Element> Zp;

    public Element get_g() {
        return g;
    }
    public void set_g(Element g) {
        this.g = g;
    }

    public Element getG1() {
        return g1;
    }
    public void setG1(Element g1) {
        this.g1 = g1;
    }

    public Element getG2() {
        return g2;
    }
    public void setG2(Element g2) {
        this.g2 = g2;
    }

    public Element getY() {
        return Y;
    }
    public void setY(Element Y) {
        this.Y = Y;
    }

    public Element[] getT() {
        return t;
    }
    public void setT(Element[] t) {
        this.t = t;
    }

    public Element[] getL() {
        return l;
    }
    public void setL(Element[] l) {
        this.l = l;
    }

    public Element[] getEta() {
        return eta;
    } // revised
    public void setEta(Element[] eta) {
        this.eta = eta;
    } // revised
    
    public Element[] getTheta() {
        return theta;
    } // revised
    public void setTheta(Element[] theta) {
        this.theta = theta;
    } // revised
    
    public Element getAlpha() {
        return alpha;
    }
    public void setAlpha(Element alpha) {
        this.alpha = alpha;
    }

    public Element getBeta() {
        return beta;
    }
    public void setBeta(Element beta) {
        this.beta = beta;
    }

    public Pairing getPairing() {
        return pairing;
    }
    public void setPairing(Pairing pairing) {
        this.pairing = pairing;
    }

    public Field<Element> getG() {
        return G;
    }
    public void setG(Field<Element> G) { this.G = G; }

    public Field<Element> getGT() {
        return GT;
    }
    public void setGT(Field<Element> GT) {
        this.GT = GT;
    }

    public Field<Element> getZp() {
        return Zp;
    }
    public void setZp(Field<Element> zp) {
        Zp = zp;
    }

}

public class Polynomial {
	private Element[] coef; //coefficients 系数
	private int deg;//degree of polynomial (0 for the zero polynomial)
	private Element zero;
	private PARS pars;

	//a*x^b
	public Polynomial(Element a, int b, PARS pars) {
		coef = new Element[b + 1];
		zero = pars.getZp().newZeroElement().getImmutable();
		this.pars = pars;
		Arrays.fill(coef, zero);
		coef[b] = a.duplicate().getImmutable();
		deg = degree();
	}

	// return the degree of this polynomial (0 for the zero polynomial)
	public int degree() {
		int d = 0;
		for (int i = 0; i < coef.length; i++) {
			if (!coef[i].equals(zero)) {
				d = i;
			}
		}

		return d;
	}

	//return c = a+b
	public Polynomial plus(Polynomial b) {
		Polynomial a = this;
		Polynomial c = new Polynomial(zero, Math.max(a.deg, b.deg), pars);
		for (int i = 0; i <= a.deg; i++)
			c.coef[i] = c.coef[i].duplicate().add(a.coef[i].duplicate()).getImmutable();
		for (int i = 0; i <= b.deg; i++)
			c.coef[i] = c.coef[i].duplicate().add(b.coef[i].duplicate()).getImmutable();
		c.deg = c.degree();
		return c;
	}

	// return (a - b)
	public Polynomial minus(Polynomial b) {
		Polynomial a = this;
		Polynomial c = new Polynomial(zero, Math.max(a.deg, b.deg), pars);
		for (int i = 0; i <= a.deg; i++) c.coef[i] = c.coef[i].add(a.coef[i]);
		for (int i = 0; i <= b.deg; i++) c.coef[i] = c.coef[i].sub(b.coef[i]);
		c.deg = c.degree();
		return c;
	}

	// return (a * b)
	public Polynomial times(Polynomial b) {
		Polynomial a = this;
		Polynomial c = new Polynomial(zero, a.deg + b.deg, pars);
		for (int i = 0; i <= a.deg; i++)
			for (int j = 0; j <= b.deg; j++)
				c.coef[i + j] = c.coef[i + j].duplicate().add(a.coef[i].duplicate().mul(b.coef[j].duplicate())).getImmutable();
		c.deg = c.degree();
		return c;
	}

	// return a(b(x))  - compute using Horner's method
	public Polynomial compose(Polynomial b) {
		Polynomial a = this;
		Polynomial c = new Polynomial(zero, 0, pars);
		for (int i = a.deg; i >= 0; i--) {
			Polynomial term = new Polynomial(a.coef[i], 0, pars);
			c = term.plus(b.times(c));
		}
		return c;
	}


	// do a and b represent the same polynomial?
	public boolean eq(Polynomial b) {
		Polynomial a = this;
		if (a.deg != b.deg) return false;
		for (int i = a.deg; i >= 0; i--)
			if (a.coef[i] != b.coef[i]) return false;
		return true;
	}


	// use Horner's method to compute and return the polynomial evaluated at x
	public Element evaluate(Element x) {
		Element p = zero;
		for (int i = deg; i >= 0; i--)
			p = coef[i].duplicate().add(x.duplicate().mul(p)).getImmutable();
		return p;
	}

	// differentiate this polynomial and return it
	public Polynomial differentiate() {
		if (deg == 0) return new Polynomial(zero, 0, pars);
		Polynomial deriv = new Polynomial(zero, deg - 1, pars);
		deriv.deg = deg - 1;
		for (int i = 0; i < deg; i++)
//			deriv.coef[i] = (i + 1) * coef[i + 1];
			deriv.coef[i] = coef[i + 1].mul(BigInteger.valueOf(i + 1));
		return deriv;
	}

	public String toString() {
		if (deg == 0)
			return "" + coef[0];
		if (deg == 1)
			return coef[1] + "x + " + coef[0];

		String s = coef[deg] + "x^" + deg;
		for (int i = deg - 1; i >= 0; i--) {
			if (coef[i].equals(zero))
				continue;
			else
				s = s + " + " + (coef[i]);
			if (i == 1)
				s = s + "x";
			else if (i > 1)
				s = s + "x^" + i;
		}
		return s;
	}

	public static void main(String[] args) {
		int d = 10;
		int n = 10;
		PARS pars = Setup.setup(n);
		Element root = pars.getZp().newRandomElement();
		Polynomial poly = Utils.newRandomPolynomial(d, root, pars);
		System.out.println("root		= " + root);
		System.out.println("p(x)		= " + poly);
		System.out.println("p(3)		= " + poly.evaluate(pars.getZp().newZeroElement()));
	}
}


public class Setup {
	public static PARS setup(int n){
		int rBits = 160;
		int qBits = 512;
		TypeACurveGenerator pg = new TypeACurveGenerator(rBits, qBits);
		PairingParameters pairingParameters = pg.generate();
		Pairing pairing = PairingFactory.getPairing(pairingParameters);

		PARS pars = new PARS();
		pars.setPairing(pairing);
		pars.setG(pairing.getG1());
		pars.setGT(pairing.getGT());
		pars.setZp(pairing.getZr());
		Element g = pars.getG().newRandomElement().duplicate().getImmutable();
		Element alpha = pars.getZp().newRandomElement().getImmutable();
		Element beta = pars.getZp().newRandomElement().getImmutable();
		Element[] theta = {
				pars.getZp().newRandomElement().getImmutable(), 
				pars.getZp().newRandomElement().getImmutable(), 
				pars.getZp().newRandomElement().getImmutable(), 
				pars.getZp().newRandomElement().getImmutable()
		}; // revised
		Element g1 = g.powZn(alpha).getImmutable();
		Element g2 = pars.getG().newRandomElement().duplicate().getImmutable();
		Element[] eta = {
			g.powZn(theta[0]).getImmutable(), 
			g.powZn(theta[1]).getImmutable(), 
			g.powZn(theta[2]).getImmutable(), 
			g.powZn(theta[3]).getImmutable(),  
		}; // revised
		Element Y = pairing.pairing(g2, g).duplicate().powZn(beta).getImmutable();
		Element[] t = new Element[n + 1];
		Element[] l = new Element[n + 1];
		for (int i = 0; i < n + 1; ++i) {
			t[i] = pars.getG().newRandomElement().duplicate().getImmutable();
			l[i] = pars.getG().newRandomElement().duplicate().getImmutable();
		}
		pars.set_g(g);
		pars.setG1(g1);
		pars.setG2(g2);
		pars.setY(Y);
		pars.setT(t);
		pars.setL(l);
		pars.setEta(eta); // revised
		pars.setTheta(theta); // revised
		pars.setAlpha(alpha);
		pars.setBeta(beta);
		return pars;
	}
}


public class Start {
	public static void main(String[] args){
		int n = 10;
		int d = 10;
		PARS pars = Setup.setup(n);
		Element[] S_A = new Element[n];
		for (int i = 0; i < S_A.length; i++) {
			S_A[i] = pars.getZp().newElement(BigInteger.valueOf(100 + i)).getImmutable();
		}
		Element[] S_B = new Element[n];
		for (int i = 0; i < S_B.length; i++) {
			S_B[i] = pars.getZp().newElement(BigInteger.valueOf(100 + i)).getImmutable();
		}
		Element[] P_A = new Element[n];
		for (int i = 0; i < P_A.length; i++) {
			P_A[i] = pars.getZp().newElement(BigInteger.valueOf(100 + i)).getImmutable();
		}
		Element[] P_B = new Element[n];
		for (int i = 0; i < P_B.length; i++) {
			P_B[i] = pars.getZp().newElement(BigInteger.valueOf(100 + i)).getImmutable();
		}
		Element[][] temp = EKGen.eKGen(d, pars, P_A);
		Element[] E_i = temp[0];
		Element[] e_i = temp[1];
		temp = DKGen.dKGen(d, pars, P_B, P_A);
		Element[][] Dd = {temp[0], temp[1], temp[2], temp[3], temp[4]}; // Revised
		Element[][] Dd_prime = {temp[5], temp[6], temp[7], temp[8], temp[9]}; // Revised
		Element M = pars.getGT().newRandomElement().getImmutable();
		List<Object> list = Enc.enc(d, pars, S_A, P_B, E_i, e_i, M);
		Element C_0 = ((Element) list.get(0)).getImmutable();
		Element C_1 = ((Element) list.get(1)).getImmutable();
		Element C_2 = ((Element) list.get(2)).getImmutable();
		Element C_3 = ((Element) list.get(3)).getImmutable(); // Revised
		Element C_4 = ((Element) list.get(4)).getImmutable(); // Revised
		Element[] C_1_i = (Element[]) list.get(5); // Revised
		Element[] C_2_i = (Element[]) list.get(6); // Revised
		Element[] C_3_i = (Element[]) list.get(7); // Revised
		Element[] C_4_i = (Element[]) list.get(8); // Revised
		Element[] C_5_i = (Element[]) list.get(9); // Revised
		Element M_prime = Dec.dec(d, P_A, S_A, P_B, S_B, pars, Dd, Dd_prime, C_0, C_1, C_2, C_3, C_4, C_1_i, C_2_i, C_3_i, C_4_i, C_5_i); // Revised
		System.out.println("M = " + M);
		System.out.println("M_prime = " + M_prime);
		System.out.println("Result = " + M.equals(M_prime));
	}
}

public class Timer {
	public enum FORMAT{
		SECOND, MILLI_SECOND, MICRO_SECOND, NANO_SECOND,
	}

	public static final int DEFAULT_MAX_NUM_TIMER = 10;
	public final int MAX_NUM_TIMER;

	private long[] timeRecorder;
	private boolean[] isTimerStart;
	private FORMAT[] outFormat;

	public Timer(){
		this.MAX_NUM_TIMER = DEFAULT_MAX_NUM_TIMER;
		this.timeRecorder = new long[MAX_NUM_TIMER];
		this.isTimerStart = new boolean[MAX_NUM_TIMER];
		this.outFormat = new FORMAT[MAX_NUM_TIMER];

		//set default format as millisecond
		for (int i=0; i<outFormat.length; i++){
			outFormat[i] = FORMAT.MILLI_SECOND;
		}
	}

	public Timer(int max_num_timer){
		this.MAX_NUM_TIMER = max_num_timer;
		this.timeRecorder = new long[MAX_NUM_TIMER];
		this.isTimerStart = new boolean[MAX_NUM_TIMER];
	}

	public void setFormat(int num, FORMAT format){
		//Ensure num less than MAX_NUM_TIMER
		assert(num >=0 && num < MAX_NUM_TIMER);

		this.outFormat[num] = format;
	}

	public void start(int num) {
		//Ensure the timer now stops.
		assert(!isTimerStart[num]);
		//Ensure num less than MAX_NUM_TIMER
		assert(num >=0 && num < MAX_NUM_TIMER);

		isTimerStart[num] = true;
		timeRecorder[num] = System.nanoTime();
	}

	public long stop(int num) {
		//Ensure the timer now starts.
		assert(isTimerStart[num]);
		//Ensure num less than MAX_NUM_TIMER
		assert(num >=0 && num < MAX_NUM_TIMER);

		long result = System.nanoTime() - timeRecorder[num];
		isTimerStart[num] = false;

		switch(outFormat[num]){
			case SECOND:
				return result / 1000000000L;
			case MILLI_SECOND:
				return result / 1000000L;
			case MICRO_SECOND:
				return result / 1000L;
			case NANO_SECOND:
				return result;
			default:
				return result / 1000000L;
		}

	}
}

public class TimeTest
{
	public static void main(String[] args)
	{
		int timeToTest = 20;
		PARS pars;
		Element[] S_A, S_B, P_A, P_B, E_i, e_i, C_1_i, C_2_i, C_3_i, C_4_i, C_5_i;
		Element[][] temp = new Element[0][];
		Element M, C_0, C_1, C_2, C_3, C_4;
		List<Object> list = null;
		long runTime;
		Timer timer = new Timer();
		timer.setFormat(0, Timer.FORMAT.MICRO_SECOND);

		/* d from 10 to 50, n from 10 to 60 */
		for (int d = 10; d <= 50; d += 10)
		{
			System.out.println("---------------------------- d = " + d + " ----------------------------");
			for (int n = d; n <= 60; n += 10)
			{
				System.out.println("  ------------------------ [n = " + n + "] ------------------------  ");
				pars = Setup.setup(n);
				S_A = new Element[n];
				S_B = new Element[n];
				P_A = new Element[n];
				P_B = new Element[n];
				for (int i = 0; i < n; ++i)
				{
					S_A[i] = pars.getZp().newElement(BigInteger.valueOf(100 + i)).getImmutable();
					S_B[i] = S_A[i].getImmutable();
					P_A[i] = S_A[i].getImmutable();
					P_B[i] = S_A[i].getImmutable();
				}
	
				runTime = 0;
				for (int i = 0; i < timeToTest; ++i)
				{
					timer.start(0);
					temp = EKGen.eKGen(d, pars, P_A);
					runTime += timer.stop(0);
				}
				runTime = runTime / timeToTest;
				System.out.println("  [EKGen] Run Time = " + runTime + " us");
	
				E_i = temp[0];
				e_i = temp[1];
				System.out.println("  [EKGen] E_i Size = " + getObjectSize(E_i) + " bytes" + ", e_i Size = " + getObjectSize(e_i) + " bytes");
	
				runTime = 0;
				for (int i = 0; i < timeToTest; ++i)
				{
					timer.start(0);
					temp = DKGen.dKGen(d, pars, P_B, P_A);
					runTime += timer.stop(0);
				}
				runTime = runTime / timeToTest;
				System.out.println("  [DKGen] Run Time = " + runTime + " us");
	
				Element[][] Dd = { temp[0], temp[1], temp[2], temp[3], temp[4] }; // Revised
				Element[][] Dd_prime = { temp[5], temp[6], temp[7], temp[8], temp[9] }; // Revised
				M = pars.getGT().newRandomElement().getImmutable();
				System.out.println("  [DKGen] Dd Size = " + getObjectSize(Dd[0]) * Dd.length + " bytes" + ", Dd_prime Size = " + getObjectSize(Dd_prime[0]) * Dd_prime.length + " bytes");
	
				runTime = 0;
				for (int i = 0; i < timeToTest; ++i)
				{
					timer.start(0);
					list = Enc.enc(d, pars, S_A, P_B, E_i, e_i, M);
					runTime += timer.stop(0);
				}
				runTime /= timeToTest;
				System.out.println("  [Enc] Run Time = " + runTime + " us");
	
				C_0 = ((Element) list.get(0)).getImmutable();
				C_1 = ((Element) list.get(1)).getImmutable();
				C_2 = ((Element) list.get(2)).getImmutable();
				C_3 = ((Element) list.get(3)).getImmutable(); // revised
				C_4 = ((Element) list.get(4)).getImmutable(); // revised
				C_1_i = (Element[]) list.get(5); // revised
				C_2_i = (Element[]) list.get(6); // revised
				C_3_i = (Element[]) list.get(7); // revised
				C_4_i = (Element[]) list.get(8); // revised
				C_5_i = (Element[]) list.get(9); // revised
	
				System.out.println("  [Enc] C_0 Size = " + getObjectSize(C_0) + " bytes" + ", C_1 Size = " + getObjectSize(C_1) + " bytes"
					+ ", C_2 Size = " + getObjectSize(C_2) + " bytes" + ", C_3 Size = " + getObjectSize(C_3) + " bytes" + ", C_4 Size = " + getObjectSize(C_4) + " bytes"
					+ ", C_1_i Size = " + getObjectSize(C_1_i) + " bytes" + ", C_2_i Size = " + getObjectSize(C_2_i) + " bytes" + ", C_3_i Size = " + getObjectSize(C_3_i) + " bytes" + ", C_4_i Size = " + getObjectSize(C_4_i) + " bytes" + ", C_5_i Size = " + getObjectSize(C_5_i) + " bytes"
				 ); // revised
	
				runTime = 0;
				for (int i = 0; i < timeToTest; ++i)
				{
					timer.start(0);
					Dec.dec(d, P_A, S_A, P_B, S_B, pars, Dd, Dd_prime, C_0, C_1, C_2, C_3, C_4, C_1_i, C_2_i, C_3_i, C_4_i, C_5_i);
					runTime += timer.stop(0);
				}
				runTime = runTime / timeToTest;
				System.out.println("  [Dec] Run Time = " + runTime + " us");
			}
		}
		System.exit(0);
	}
}


public class Utils {
	public static Element delta(Element x, Element i, Element[] S, PARS pars){
		Element result = pars.getZp().newOneElement().getImmutable();
		for (Element j : S) {
			if (!i.duplicate().equals(j.duplicate())) {
				result = result.mul(x.duplicate().sub(j.duplicate()).div(i.duplicate().sub(j.duplicate()))).getImmutable();
			}
		}
		return result;
	}

	public static Element T(Element x, PARS pars){
		Element[] t = pars.getT();
		int n = t.length;
		Element result = pars.getG().newOneElement().getImmutable();
		Element[] N = new Element[n];
		for (int i = 0; i < n; i++) {
			N[i] = pars.getZp().newElement(BigInteger.valueOf(i + 1)).getImmutable();
		}
		for (int i = 0; i < n; i++) {
			result = result.duplicate().mul(t[i].duplicate().powZn(delta(x, pars.getZp().newElement(BigInteger.valueOf(i + 1)), N, pars))).getImmutable();
		}
		result = result.duplicate().mul(pars.getG2().duplicate().powZn(x.duplicate().pow(BigInteger.valueOf(n - 1))));
		return result;
	}

	public static Element H(Element x, PARS pars){
		Element[] l = pars.getL();
		int n = l.length;
		Element result = pars.getG().newOneElement().getImmutable();
		Element[] N = new Element[n];
		for (int i = 0; i < n; i++) {
			N[i] = pars.getZp().newElement(BigInteger.valueOf(i + 1)).getImmutable();
		}
		for (int i = 0; i < n; i++) {
			result = result.duplicate().mul(l[i].duplicate().powZn(delta(x, pars.getZp().newElement(BigInteger.valueOf(i + 1)), N, pars))).getImmutable();
		}
		result = result.duplicate().mul(pars.getG2().duplicate().powZn(x.duplicate().pow(BigInteger.valueOf(n - 1))));
		return result;
	}

	public static Polynomial newRandomPolynomial(int d, PARS pars){
		return newRandomPolynomial(d, pars.getZp().newRandomElement(), pars);
	}

	public static Polynomial newRandomPolynomial(int d, Element root, PARS pars){
		Polynomial poly = new Polynomial(pars.getZp().newZeroElement().getImmutable(), 0, pars);
		for (int i = 1; i < d; i++) {
			poly = poly.plus(new Polynomial(pars.getZp().newRandomElement(), i, pars));
		}
		poly = poly.plus(new Polynomial(root, 0, pars));
		return poly;
	}

	public static Element[] intersect(Element[] a, Element[] b){
		List<Element> result = new ArrayList<>();
		for (Element a0 : a) {
			for (Element b0 : b) {
				if (a0.isEqual(b0))
					result.add(a0);
			}
		}
		Element[] intersection = new Element[result.size()];
		for (int i = 0; i < intersection.length; i++) {
			intersection[i] = result.get(i);
		}
		return intersection;
	}

	public static byte[] byteMergerAll(byte[]... values) {
		int length_byte = 0;
		for (int i = 0; i < values.length; i++) {
			length_byte += values[i].length;
		}
		byte[] all_byte = new byte[length_byte];
		int countLength = 0;
		for (int i = 0; i < values.length; i++) {
			byte[] b = values[i];
			System.arraycopy(b, 0, all_byte, countLength, b.length);
			countLength += b.length;
		}
		return all_byte;
	}
}

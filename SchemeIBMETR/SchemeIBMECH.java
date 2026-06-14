import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * Java implementation of the IBMECH cryptographic scheme.
 * Converted from SchemeIBMECH.py based on JPBC library.
 */
@SuppressWarnings("unchecked")
public class SchemeIBMECH
{
	private Pairing pairing;
	private Field<Element> G1, G2, Zp;
	private Object[] mpk, msk;
	private boolean flag = false;

	public SchemeIBMECH(Pairing pairing)
	{
		this.pairing = pairing;
		this.G1 = pairing.getG1(); this.G2 = pairing.getG2();
		this.Zp = pairing.getZr();
	}

	private Element product(Element[] elems)
	{
		Element r = elems[0].duplicate().getImmutable();
		for (int i = 1; i < elems.length; i++) r = r.mul(elems[i]).getImmutable();
		return r;
	}

	public Object[][] Setup()
	{
		flag = false;
		Element g1 = G1.newOneElement().getImmutable();
		Element g2 = G2.newOneElement().getImmutable();
		Element alpha = Zp.newRandomElement().getImmutable();
		Element eta = Zp.newRandomElement().getImmutable();
		Element zero = Zp.newZeroElement().getImmutable();
		Element one = Zp.newRandomElement().getImmutable();

		Element[][] B = new Element[8][8];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				B[i][j] = Zp.newRandomElement().getImmutable();

		Element[][] D = new Element[4][8];
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 8; j++)
				D[i][j] = g1.powZn(B[i][j]).getImmutable();

		// Gauss elimination in groups: simplified matrix inversion
		// Python: DStar[i][j] = g2^{B^{-1} * [one if i==j else zero]}
		Element[][] DStar = new Element[4][8];
		for (int i = 0; i < 4; i++)
		{
			// Build augmented matrix [B | target] where target = [one if i==j else zero]
			Element[][] aug = new Element[8][9];
			for (int row = 0; row < 8; row++)
			{
				for (int col = 0; col < 8; col++)
					aug[row][col] = B[row][col].duplicate().getImmutable();
				aug[row][8] = (row == i) ? one.duplicate().getImmutable() : zero.duplicate().getImmutable();
			}

			// Forward elimination (Gaussian elimination in Zp)
			for (int col = 0; col < 8; col++)
			{
				// Find pivot
				int pivot = col;
				while (pivot < 8 && aug[pivot][col].isZero()) pivot++;
				if (pivot >= 8) continue;

				// Swap rows
				Element[] temp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = temp;

				// Normalize pivot row
				Element invPivot = aug[col][col].duplicate().invert().getImmutable();
				for (int j = col; j <= 8; j++)
					aug[col][j] = aug[col][j].mul(invPivot).getImmutable();

				// Eliminate other rows
				for (int row = 0; row < 8; row++)
				{
					if (row != col && !aug[row][col].isZero())
					{
						Element factor = aug[row][col].duplicate().getImmutable();
						for (int j = col; j <= 8; j++)
							aug[row][j] = aug[row][j].sub(factor.duplicate().mul(aug[col][j])).getImmutable();
					}
				}
			}

			// Extract solution: DStar[i][j] = g2^{solved[j]}
			for (int j = 0; j < 8; j++)
				DStar[i][j] = g2.powZn(aug[j][8]).getImmutable();
		}

		Element gT = pairing.pairing(g1, g2).getImmutable();
		Element gTAlpha = gT.powZn(alpha.duplicate().mul(one)).getImmutable();
		Element gTEta = gT.powZn(eta.duplicate().mul(one)).getImmutable();

		mpk = new Object[]{gTAlpha, gTEta, D[0], D[1]};
		msk = new Object[]{alpha, eta, g1, g2, D[2], D[3], DStar[0], DStar[1], DStar[2], DStar[3]};

		flag = true;
		return new Object[][]{mpk, msk};
	}

	public Object[] SKGen(Element sigma)
	{
		if (!flag) Setup();
		Element eta = (Element) msk[1];
		Element[] d3 = (Element[]) msk[4];
		Element[] d4 = (Element[]) msk[5];
		Element r = Zp.newRandomElement().getImmutable();

		Element[] ek = new Element[8];
		for (int i = 0; i < 8; i++)
			ek[i] = d3[i].powZn(eta.duplicate().add(r.duplicate().mul(sigma)))
				.div(d4[i].powZn(r)).getImmutable();
		return ek;
	}

	public Object[] RKGen(Element rho)
	{
		if (!flag) Setup();
		Element gTEta = (Element) mpk[1];
		Element alpha = (Element) msk[0];
		Element g2 = (Element) msk[3];
		Element[] DStar1 = (Element[]) msk[6];
		Element[] DStar2 = (Element[]) msk[7];
		Element[] DStar3 = (Element[]) msk[8];
		Element[] DStar4 = (Element[]) msk[9];

		Element s = Zp.newRandomElement().getImmutable();
		Element s1 = Zp.newRandomElement().getImmutable();
		Element s2 = Zp.newRandomElement().getImmutable();

		Element[] k1 = new Element[8];
		Element[] k2 = new Element[8];
		for (int i = 0; i < 8; i++)
		{
			k1[i] = g2.powZn(
				DStar1[i].duplicate().mul(alpha.duplicate().add(s1.duplicate().mul(rho)))
				.sub(s1.duplicate().mul(DStar2[i]))
				.add(s.duplicate().mul(DStar3[i]))
			).getImmutable();
			k2[i] = g2.powZn(
				s2.duplicate().mul(rho.duplicate().mul(DStar1[i]).sub(DStar2[i]))
				.add(s.duplicate().mul(DStar4[i]))
			).getImmutable();
		}
		Element k3 = gTEta.powZn(s).getImmutable();

		return new Object[]{k1, k2, k3};
	}

	public Object[] Enc(Object[] ekSigma, Element receiver, Element message)
	{
		if (!flag) Setup();
		Element[] ek = (Element[]) ekSigma;
		Element rcv = receiver;
		Element m = message;
		Element gTAlpha = (Element) mpk[0];
		Element[] D1 = (Element[]) mpk[2];
		Element[] D2 = (Element[]) mpk[3];

		Element z = Zp.newRandomElement().getImmutable();
		Element[] C = new Element[8];
		for (int i = 0; i < 8; i++)
			C[i] = D1[i].powZn(z).mul(D2[i].powZn(z.duplicate().mul(rcv)))
				.mul(ek[i]).getImmutable();
		Element C0 = gTAlpha.powZn(z).mul(m).getImmutable();

		return new Object[]{C, C0};
	}

	public Element Dec(Object[] dkRho, Element sender, Object[] cipherText)
	{
		if (!flag) Setup();
		Element[] k1 = (Element[]) dkRho[0];
		Element[] k2 = (Element[]) dkRho[1];
		Element k3 = (Element) dkRho[2];
		Element[] C = (Element[]) cipherText[0];
		Element C0 = (Element) cipherText[1];
		Element snd = sender;

		Element[] pairProducts = new Element[8];
		for (int i = 0; i < 8; i++)
			pairProducts[i] = pairing.pairing(C[i], k1[i].duplicate().mul(k2[i].powZn(snd))).getImmutable();
		Element numerator = C0.duplicate().mul(k3).getImmutable();
		Element denominator = product(pairProducts);
		Element m = numerator.div(denominator).getImmutable();
		return m;
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
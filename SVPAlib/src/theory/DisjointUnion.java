package theory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.sat4j.specs.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;

import utilities.Pair;

public class DisjointUnion<P1, S1, P2, S2> extends BooleanAlgebra<Pair<P1,P2>, Pair<S1,S2>> {

	private BooleanAlgebra<P1,S1> ba1;
	private BooleanAlgebra<P2,S2> ba2;
	
	public DisjointUnion(BooleanAlgebra<P1,S1> ba1, BooleanAlgebra<P2,S2> ba2) {
		this.ba1 = ba1;
		this.ba2 = ba2;
	}
	
	@Override
	public Pair<P1, P2> MkAtom(Pair<S1, S2> s) throws TimeoutException {
		return new Pair<P1, P2>(ba1.MkAtom(s.first), ba2.MkAtom(s.second));
	}

	@Override
	public Pair<P1, P2> MkNot(Pair<P1, P2> p) throws TimeoutException {
		return new Pair<P1, P2>(ba1.MkNot(p.first), ba2.MkNot(p.second));
	}

	@Override
	public Pair<P1, P2> MkOr(Collection<Pair<P1, P2>> pset) throws TimeoutException {
		Collection<P1> p1set = new ArrayList<P1>();
		Collection<P2> p2set = new ArrayList<P2>();
		for (Pair<P1, P2> p : pset) {
			p1set.add(p.first);
			p2set.add(p.second);
		}
		return new Pair<P1, P2>(ba1.MkOr(p1set), ba2.MkOr(p2set));
	}

	@Override
	public Pair<P1, P2> MkOr(Pair<P1, P2> p1, Pair<P1, P2> p2) throws TimeoutException {
		return new Pair<P1, P2>(ba1.MkOr(p1.first, p2.first), ba2.MkOr(p1.second, p2.second));
	}

	@Override
	public Pair<P1, P2> MkAnd(Collection<Pair<P1, P2>> pset) throws TimeoutException {
		Collection<P1> p1set = new ArrayList<P1>();
		Collection<P2> p2set = new ArrayList<P2>();
		for (Pair<P1, P2> p : pset) {
			p1set.add(p.first);
			p2set.add(p.second);
		}
		return new Pair<P1, P2>(ba1.MkAnd(p1set), ba2.MkAnd(p2set));
	}

	@Override
	public Pair<P1, P2> MkAnd(Pair<P1, P2> p1, Pair<P1, P2> p2) throws TimeoutException {
		return new Pair<P1, P2>(ba1.MkAnd(p1.first, p2.first), ba2.MkAnd(p1.second, p2.second));
	}

	@Override
	public Pair<P1, P2> True() {
		return new Pair<P1, P2>(ba1.True(), ba2.True());
	}

	@Override
	public Pair<P1, P2> False() {
		return new Pair<P1, P2>(ba1.False(), ba2.False());
	}

	@Override
	public boolean AreEquivalent(Pair<P1, P2> p1, Pair<P1, P2> p2) {
		return ba1.AreEquivalent(p1.first, p2.first) && ba2.AreEquivalent(p1.second, p2.second);
	}

	@Override
	public boolean IsSatisfiable(Pair<P1, P2> p) {
		return ba1.IsSatisfiable(p.first) && ba2.IsSatisfiable(p.second); 
	}

	@Override
	public boolean HasModel(Pair<P1, P2> p, Pair<S1, S2> s) {
		return ba1.HasModel(p.first, s.first) && ba2.HasModel(p.second, s.second);
	}

	@Override
	public boolean HasModel(Pair<P1, P2> p, Pair<S1, S2> s1, Pair<S1, S2> s2) {
		return ba1.HasModel(p.first, s1.first, s2.first) && ba2.HasModel(p.second, s1.second, s2.second);
	}

	@Override
	public Pair<S1, S2> generateWitness(Pair<P1, P2> p) {
		return new Pair<S1, S2>(ba1.generateWitness(p.first), ba2.generateWitness(p.second));
	}

	@Override
	public Pair<Pair<S1, S2>, Pair<S1, S2>> generateWitnesses(Pair<P1, P2> p) {
		Pair<S1, S1> ret1 = ba1.generateWitnesses(p.first);
		Pair<S2, S2> ret2 = ba2.generateWitnesses(p.second);
		return new Pair<Pair<S1, S2>, Pair<S1, S2>>(new Pair<S1, S2>(ret1.first, ret2.first), new Pair<S1, S2>(ret1.second, ret2.second));
	}

	@Override
	public ArrayList<Pair<P1, P2>> GetSeparatingPredicates(
			ArrayList<Collection<Pair<S1, S2>>> groups, long timeout) throws TimeoutException {
		ArrayList<Collection<S1>> g1 = new ArrayList<Collection<S1>>();
		ArrayList<Collection<S2>> g2 = new ArrayList<Collection<S2>>();
		for (Collection<Pair<S1, S2>> c : groups) {
			Collection<S1> s1set = new HashSet<S1>();
			Collection<S2> s2set = new HashSet<S2>();
			for(Pair<S1, S2> p : c) {
				s1set.add(p.first);
				s2set.add(p.second);
			}
			g1.add(s1set);
			g2.add(s2set);
		}
		ArrayList<P1> preds1 = ba1.GetSeparatingPredicates(g1, timeout);
		ArrayList<P2> preds2 = ba2.GetSeparatingPredicates(g2, timeout);
		checkArgument(preds1.size() == preds2.size());
		ArrayList<Pair<P1, P2>> ret = new ArrayList<Pair<P1, P2>>();
		for(int i = 0; i < preds1.size(); i++) {
			ret.add(new Pair<P1, P2>(preds1.get(i), preds2.get(i)));
		}
		return ret;
	}
}

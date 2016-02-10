package automata.safa;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SumOfProducts extends BooleanExpr {

	protected List<List<Integer>> dnf;

	public SumOfProducts(List<List<Integer>> dnf) {
		super();
		this.dnf = dnf;
	}

	public Set<Integer> getStates() {
		HashSet<Integer> acc = new HashSet<Integer>();
		dnf.stream().forEach(acc::addAll);
		return acc;
	}

	public static BooleanExpr empty() {
		List<List<Integer>> emptydnf = new LinkedList<>();
		return new SumOfProducts(emptydnf);
	}

	@Override
	public Object clone() {
		List<List<Integer>> newDnf = new LinkedList<>();
		for (List<Integer> l : dnf)
			newDnf.add(new LinkedList<>(l));
		return new SumOfProducts(newDnf);
	}

	@Override
	public boolean hasModel(Collection<Integer> elements) {
		for (List<Integer> l : dnf)
			if (elements.containsAll(l))
				return true;

		return false;
	}

	@Override
	public BooleanExpr unionWith(BooleanExpr p1) {
		if (!(p1 instanceof SumOfProducts))
			throw new IllegalArgumentException("can only interesect SumOfProducts with SumOfProducts");

		SumOfProducts p1c = (SumOfProducts) p1;
		List<List<Integer>> newDnf = new LinkedList<>(dnf);
		newDnf.addAll(p1c.dnf);

		return new SumOfProducts(newDnf);
	}

	@Override
	public BooleanExpr interesectWith(BooleanExpr p1) {
		if (!(p1 instanceof SumOfProducts))
			throw new IllegalArgumentException("can only interesect SumOfProducts with SumOfProducts");

		SumOfProducts p1c = (SumOfProducts) p1;

		List<List<Integer>> newDnf = new LinkedList<>();
		for (List<Integer> l1 : dnf)
			for (List<Integer> l2 : p1c.dnf) {
				List<Integer> l1concl2 = new LinkedList<>(l1);
				l1concl2.addAll(l2);
				newDnf.add(l1concl2);
			}

		return new SumOfProducts(newDnf);
	}

	@Override
	public BooleanExpr offset(int offset) {
		List<List<Integer>> newDnf = new LinkedList<>();
		for (List<Integer> l1 : dnf) {
			List<Integer> newl1 = new LinkedList<>();
			for(Integer s: l1){
				newl1.add(s+offset);
			}
			newDnf.add(newl1);
		}
		return new SumOfProducts(newDnf);
	}

	// TODO equals clone...
}

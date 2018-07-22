/**
 * NOTICE: This file has been modified by Sophie Lathouwers
 *
 * Original file by:
 * SVPAlib
 * transducers.sft
 * Mar 5, 2018
 * @author Loris D'Antoni
 */
package transducers.sft;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.sat4j.specs.TimeoutException;
import static com.google.common.base.Preconditions.checkNotNull;
import static sftlearning.TestAutomaticEquivalenceOracle.getEncodeSpec;

import automata.Automaton;
import automata.Move;
import automata.sfa.SFA;
import automata.sfa.SFAMove;
import automata.sfa.SFAEpsilon;
import automata.sfa.SFAInputMove;
import specifications.CyberchefSpecifications;
import theory.BooleanAlgebraSubst;
import theory.characters.*;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;


/**
 * A symbolic finite state transducer
 * modified from the model given by the paper named after Symbolic Finite State Transducers: Algorithms And Applications
 * To be more specific, SFT here could has 0 or more tails for every final state.
 *
 * @param <P>
 *			The type of predicates forming the Boolean algebra
 * @param <F>
 *			The type of functions S->S in the Boolean Algebra
 * @param <S>
 *			The domain of the Boolean algebra
 */
public class SFT<P extends CharPred, F extends TermInterface, S> extends Automaton<P, S> {

	// SFT properties
	protected Collection<Integer> states;
	protected Integer initialState;
	protected Map<Integer, Set<List<S>>> finalStatesAndTails;
	protected Integer maxStateId;

	// Moves are inputs or epsilon
	protected Map<Integer, Collection<SFTInputMove<P, F, S>>> transitionsFrom;
	protected Map<Integer, Collection<SFTInputMove<P, F, S>>> transitionsTo;

	protected Map<Integer, Collection<SFTEpsilon<P, F, S>>> epsTransitionsFrom;
	protected Map<Integer, Collection<SFTEpsilon<P, F, S>>> epsTransitionsTo;

	public Integer stateCount() {
		return states.size();
	}

	public Integer transitionCount() {
		return getTransitions().size();
	}

	protected SFT() {
		super();
		states = new HashSet<>();
		transitionsFrom = new HashMap<>();
		transitionsTo = new HashMap<>();
		epsTransitionsFrom = new HashMap<>();
		epsTransitionsTo = new HashMap<>();
		finalStatesAndTails = new HashMap<>();
		maxStateId = 0;
		initialState = 0;
	}

	/*
	* Create a SFT (removes unreachable states)
	* Page 3, left column, the last 4 lines, definition 2
	*/
	public static <P extends CharPred, F extends TermInterface, S> SFT<P, F, S> MkSFT(Collection<SFTMove<P, F, S>> transitions, Integer initialState,
																					  Map<Integer, Set<List<S>>> finalStatesAndTails,
																					  BooleanAlgebraSubst<P, F, S> ba) {
		SFT<P, F, S> aut = new SFT<P, F, S>();

		// Initialize state set
		aut.initialState = initialState;

		for (Integer state: finalStatesAndTails.keySet()) {
			Set<List<S>> tails = new HashSet<List<S>>();
			for (List<S> tail: finalStatesAndTails.get(state)) {
				if (tail.size() != 0) // remove the empty tail which is just an empty List<S> for brevity
					tails.add(tail);
			}
			aut.finalStatesAndTails.put(state, tails);
		}
		// Now all tails are not empty so that if a final state <code>state</code> does not have any tails, the size of
		// finalStatesAndTails.get(state) must be 0.

		aut.states = new HashSet<Integer>();
		aut.states.add(initialState);
		aut.states.addAll(finalStatesAndTails.keySet());

		try {
			for (SFTMove<P, F, S> t : transitions)
				aut.addTransition(t, ba, false);
			return aut;
		} catch (TimeoutException toe) {
			return null;
		}
	}

	public static <P extends CharPred, S> SFT<P, CharFunc, S> MkSFT(List<SFTMove<P, CharFunc, S>> transitions, Integer initialState,
																	HashMap<Integer, Set<List<S>>> finalStatesAndTails,
																	BooleanAlgebraSubst<P, CharFunc, S> ba) {
		SFT<P, CharFunc, S> aut = new SFT<>();

		// Initialize state set
		aut.initialState = initialState;

		for (Integer state: finalStatesAndTails.keySet()) {
			Set<List<S>> tails = new HashSet<>();
			for (List<S> tail: finalStatesAndTails.get(state)) {
				if (tail.size() != 0) // remove the empty tail which is just an empty List<S> for brevity
					tails.add(tail);
			}
			aut.finalStatesAndTails.put(state, tails);
		}
		// Now all tails are not empty so that if a final state <code>state</code> does not have any tails, the size of
		// finalStatesAndTails.get(state) must be 0.

		aut.states = new HashSet<>();
		aut.states.add(initialState);
		aut.states.addAll(finalStatesAndTails.keySet());

		try {
			for (SFTMove<P, CharFunc, S> t : transitions) {
				aut.addTransition(t, ba, false);
			}
			return aut;
		} catch (TimeoutException toe) {
			return null;
		}
	}

	/**
	 * Returns the empty SFT
	 */
	public static <P extends CharPred, F extends TermInterface, S> SFT<P, F, S> getEmptySFT(BooleanAlgebraSubst<P, F, S> ba) {
		SFT<P, F, S> aut = new SFT<>();
		aut.states = new HashSet<Integer>();
		aut.states.add(0);
		aut.initialState = 0;
		aut.isDeterministic = true;
		aut.isEmpty = true;
		aut.isEpsilonFree = true;
		aut.maxStateId = 1;
		return aut;
	}

	public List<S> outputOn(List<S> input, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		return outputOn(this, input, ba);
	}

	/**
	 * Computes one of the outputs produced when reading input. Null if no such output exists
	 *
	 * @param input
	 * @param ba
	 * @return one output sequence, null if undefined
	 * @throws TimeoutException
	 */
	public static <P extends CharPred, F extends TermInterface, S> List<S> outputOn(SFT<P, F, S> sftWithEps, List<S> input,
												 BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {

		// codes for nondeterministic but single-valued symbolic finite transducers
		List<List<S>> outputs = new ArrayList<List<S>>();

		SFT<P, F, S> sft = sftWithEps.removeEpsilonMoves(ba);
		// guarantee that there are no epsilon transitions for now

		backtrack(outputs, new ArrayList<S>(), sft, sft.getInitialState(), input, 0, ba);

		// if you want the method to be adapt to non single-valued symbolic finite transducers, you could just return
		// the whole outputs.
		if (outputs.size() != 0)
			return outputs.get(0);
		else
			return null;
	}

	// use backtrack method to get all possible outputs
	private static <P extends CharPred, F extends TermInterface, S> void backtrack(List<List<S>> outputs, List<S> tempList, SFT<P, F, S> sft,
											Integer currentState, List<S> input, int position,
											BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {

		if (position > input.size())
			return;
		else if (position == input.size()) {
			if (sft.isFinalState(currentState)) {
				if (sft.getFinalStatesAndTails().get(currentState).size() == 0) {
					outputs.add(new ArrayList<S>(tempList));
				} else {
					for (List<S> tail: sft.getFinalStatesAndTails().get(currentState)) {
						List<S> finalResult = new ArrayList<S>(tempList);
						finalResult.addAll(tail);
						outputs.add(new ArrayList<S>(finalResult));
					}
				}
			}
			return;
		} else {
			Collection<SFTInputMove<P, F, S>> transitions = sft.getInputMovesFrom(currentState);
			boolean canMove = false;
			for (SFTInputMove<P, F, S> transition: transitions) {
				if (ba.HasModel(transition.guard, input.get(position))) {
					for (F outputFunc: transition.outputFunctions)
						tempList.add(ba.MkSubstFuncConst(outputFunc, input.get(position)));
					backtrack(outputs, tempList, sft, transition.to, input, position + 1, ba);
					for (int i = 0; i < transition.outputFunctions.size(); i++)
						tempList.remove(tempList.size() - 1);
					canMove = true;
				}
			}
			if (!canMove)
				return;
		}
	}

	/**
	 * Computes the composition with <code>sftWithEps</code> as a new SFT
	 * Page 4, right column, start from the first line, part 3.1
	 *
	 * @throws TimeoutException
	 */
	public SFT<P, F, S> composeWith(SFT<P, F, S> sftWithEps, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		return compose(this, sftWithEps, ba);
	}

	/**
	 * Computes the composition of <code>sft1withEps</code> and <code>sft2withEps</code>
	 * Page 4, right column, start from the first line, part 3.1
	 *
	 * @throws TimeoutException
	 */
	public static <P extends CharPred, F extends TermInterface, S> SFT<P, F, S> compose(SFT<P, F, S> sft1withEps, SFT<P, F, S> sft2withEps,
													   BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		// Remove epsilons
		SFT<P, F, S> sft1 = sft1withEps.removeEpsilonMoves(ba);
		SFT<P, F, S> sft2 = sft2withEps.removeEpsilonMoves(ba);

		Collection<SFTMove<P, F, S>> transitions = new ArrayList<>();
		Integer initialState;
		Map<Integer, Set<List<S>>> sft1FinalStatesAndTails = new HashMap<Integer, Set<List<S>>>(sft1.getFinalStatesAndTails());
		Map<Integer, Set<List<S>>> sft2FinalStatesAndTails = new HashMap<Integer, Set<List<S>>>(sft2.getFinalStatesAndTails());
		Map<Integer, Set<List<S>>> finalStatesAndTails = new HashMap<Integer, Set<List<S>>>();

		Map<Pair<Integer, Integer>, Integer> reached = new HashMap<Pair<Integer, Integer>, Integer>();
		LinkedList<Pair<Integer, Integer>> toVisit = new LinkedList<Pair<Integer, Integer>>();

		// Add initial state
		Pair<Integer, Integer> p = new Pair<Integer, Integer>(sft1.initialState, sft2.initialState);
		initialState = 0;
		reached.put(p, initialState);
		toVisit.push(p);

		// depth first search
		while (!toVisit.isEmpty()) {
			Pair<Integer, Integer> currState = toVisit.pop();
			int currStateId = reached.get(currState);

			if (sft1.isFinalState(currState.first)) {
				if (sft1FinalStatesAndTails.get(currState.first).size() == 0) {
					// Since I have removed all empty tails during making the SFT in method MkSFT, we do not have to
					// worry about it now. So a final state has no tails is equivalent to the size of
					// sft1FinalStatesAndTails.get(currState.first) is 0.
					if (sft2.isFinalState(currState.second))
						// the composed sft accepts a string iff sft1 and sft2 both accept the string
						finalStatesAndTails.put(currStateId, sft2FinalStatesAndTails.get(currState.second));
				} else { // currState.first is a final state and it has non-empty tails
					for (List<S> tail: sft1FinalStatesAndTails.get(currState.first)) {
						List<List<SFTMove<P, F, S>>> chains = sft2.getPossibleTransitionChains(currState.second, tail.size());
						for (List<SFTMove<P, F, S>> chain : chains) {
							// according to the algorithm of method getPossibleTransitionChains, there is at least one
							// SFTMove in a chain.
							SFTInputMove<P, F, S> t2 = (SFTInputMove<P, F, S>) chain.get(0);
							P intersGuard = ba.MkSubstFuncPred(ba.MkFuncConst(tail.get(0)), t2.guard);
							List<S> output = new LinkedList<S>();
							for (F t2OutputFunction : t2.outputFunctions)
								output.add(ba.MkSubstFuncConst(t2OutputFunction, tail.get(0)));
							// initialize the composed guard.
							// consider the tail p --[c1 c2]-->| in sft1 and the transition
							// q --psi/[g]--> q' --gamma/[h] --> q'' --[c3 c4]-->| in sft2, where q'' is a final state
							// the composed tail should be
							// (p, q) --[g(c1), h(c2) c3 c4]-->| if psi is satisfied by c1 and gamma is satisfied by c2
							for (int i = 1; i < chain.size(); i++) {
								t2 = (SFTInputMove<P, F, S>) chain.get(i);
								intersGuard = ba.MkAnd(intersGuard, ba.MkSubstFuncPred(ba.MkFuncConst(tail.get(i)), t2.guard));
								for (F t2OutputFunction : t2.outputFunctions)
									output.add(ba.MkSubstFuncConst(t2OutputFunction, tail.get(i)));
							}
							// now t2 is the last transition in the chain
							if (ba.IsSatisfiable(intersGuard) && sft2.isFinalState(t2.to)) {
								Set<List<S>> tails = new HashSet<List<S>>();
								if (sft2FinalStatesAndTails.get(t2.to).size() == 0) {
									tails.add(output);
								} else {
									for (List<S> t2Tail: sft2FinalStatesAndTails.get(t2.to)) {
										List<S> newTail = new ArrayList<S>(output);
										newTail.addAll(t2Tail);
										tails.add(newTail);
									}
								}
								finalStatesAndTails.put(currStateId, tails);
							}
						}
					}
				}
			}

			for (SFTInputMove<P, F, S> t1 : sft1.getInputMovesFrom(currState.first)) {
				if (t1.outputFunctions.size() != 0) {
					List<List<SFTMove<P, F, S>>> chains = sft2.getPossibleTransitionChains(currState.second, t1.outputFunctions.size());
					for (List<SFTMove<P, F, S>> chain : chains) {
						// according to the algorithm of method getPossibleTransitionChains, there is at least one SFTMove
						// in a chain.
						SFTInputMove<P, F, S> t2 = (SFTInputMove<P, F, S>) chain.get(0);
						P intersGuard = ba.MkAnd(t1.guard, ba.MkSubstFuncPred(t1.outputFunctions.get(0), t2.guard));
						List<F> outputFunctions = new LinkedList<F>();
						for (F t2OutputFunction : t2.outputFunctions)
							outputFunctions.add(ba.MkSubstFuncFunc(t2OutputFunction, t1.outputFunctions.get(0)));
						// initialize the composed guard.
						// consider the transition p --phi/[f1, f2]--> p' in sft1 and
						// q --psi/[g]--> q' --gamma/[h] --> q'' in sft2, the composed transition should be
						// (p, q) --phi and psi(F) and gamma(f2)/[g(f1), h(f2)]--> (p', q'')
						for (int i = 1; i < chain.size(); i++) {
							t2 = (SFTInputMove<P, F, S>) chain.get(i);
							intersGuard = ba.MkAnd(intersGuard, ba.MkSubstFuncPred(t1.outputFunctions.get(i), t2.guard));
							for (F t2OutputFunction : t2.outputFunctions)
								outputFunctions.add(ba.MkSubstFuncFunc(t2OutputFunction, t1.outputFunctions.get(i)));
						}
						if (ba.IsSatisfiable(intersGuard)) {
							Pair<Integer, Integer> nextState = new Pair<Integer, Integer>(t1.to, chain.get(chain.size() - 1).to);
							int nextStateId = getStateId(nextState, reached, toVisit);

							SFTInputMove<P, F, S> newTrans = new SFTInputMove<P, F, S>(currStateId, nextStateId,
									intersGuard, outputFunctions);

							transitions.add(newTrans);
						}
					}
				} else { // t1.outputFunctions.size() == 0
					if (ba.IsSatisfiable(t1.guard)) {
						Pair<Integer, Integer> nextState = new Pair<Integer, Integer>(t1.to, currState.second);
						int nextStateId = getStateId(nextState, reached, toVisit);

						SFTInputMove<P, F, S> newTrans = new SFTInputMove<>(currStateId, nextStateId,
								t1.guard, new LinkedList<F>());

						transitions.add(newTrans);
					}
				}
			}

		}

		return MkSFT(transitions, initialState, finalStatesAndTails, ba);
	}

	/**
	 * return an equivalent copy without epsilon moves
	 */
	protected SFT<P, F, S> removeEpsilonMoves(BooleanAlgebraSubst<P, F, S> ba) {
		return removeEpsilonMovesFrom(this, ba);
	}

	/**
	 * return an equivalent copy without epsilon moves
	 */
	protected static <P extends CharPred, F extends TermInterface, S> SFT<P, F, S> removeEpsilonMovesFrom(SFT<P, F, S> sft,
																		 BooleanAlgebraSubst<P, F, S> ba) {

		if (sft.isEpsilonFree)
			return sft;

		Collection<SFTMove<P, F, S>> transitions = new ArrayList<SFTMove<P, F, S>>();
		Map<Integer, Set<List<S>>> finalStatesAndTails = new HashMap<Integer, Set<List<S>>>();

		for (Integer state: sft.getStates())
			transitions.addAll(sft.getInputMovesFrom(state));

		for (Integer state: sft.getStates()) {
			Map<Integer, List<SFTEpsilon<P, F, S>>> epsilonClosureAndPath = sft.getSFTEpsClosureAndPath(state);
			if (!sft.isFinalState(state)) // update the final state
				for (Integer nextState: epsilonClosureAndPath.keySet()) {
					if (sft.isFinalState(nextState)) {
						List<S> outputAlongPath = new ArrayList<S>();
						for (SFTEpsilon<P, F, S> transition: epsilonClosureAndPath.get(nextState)) {
							for (S output: transition.outputs)
								outputAlongPath.add(output);
						}
						Set<List<S>> newTails = new HashSet<List<S>>();
						if (sft.getFinalStatesAndTails().get(nextState).size() == 0) {
							newTails.add(outputAlongPath);
						} else {
							for (List<S> tail: sft.getFinalStatesAndTails().get(nextState)) {
								List<S> newTail = new ArrayList<S>(outputAlongPath);
								newTail.addAll(tail);
							}
						}
						if (finalStatesAndTails.containsKey(state))
							newTails.addAll(finalStatesAndTails.get(state));
						finalStatesAndTails.put(state, newTails);
					}
				}
			for (Integer nextState: epsilonClosureAndPath.keySet())
				if (!nextState.equals(state)) {
					List<F> outputFuncAlongPath = new ArrayList<>();
					for (SFTEpsilon<P, F, S> transition: epsilonClosureAndPath.get(nextState)) {
						for (S output: transition.outputs)
							outputFuncAlongPath.add(ba.MkFuncConst(output));
					}
					for (SFTInputMove<P, F, S> nextInputMove: sft.getInputMovesFrom(nextState)) {
						List<F> combinedOutputFunctions = new ArrayList<F>(outputFuncAlongPath);
						combinedOutputFunctions.addAll(nextInputMove.outputFunctions);
						transitions.add(new SFTInputMove<>(state, nextInputMove.to, nextInputMove.guard, combinedOutputFunctions));
					}
				}
		}

		return MkSFT(transitions, sft.initialState, finalStatesAndTails, ba);
	}

	private Map<Integer, List<SFTEpsilon<P, F, S>>> getSFTEpsClosureAndPath(Integer currentState) {
		return getSFTEpsClosureAndPath(this, currentState);
	}

	/**
	 * get the set of all states reachable from <code>currentState</code> using zero or more epsilon transitions
	 * and all corresponding transition paths.
	 * @param sft symbolic finite transducer
	 * @param currentState the start point
	 */
	private static <P extends CharPred, F extends TermInterface, S> Map<Integer, List<SFTEpsilon<P, F, S>>> getSFTEpsClosureAndPath(SFT<P, F, S> sft, Integer currentState) {
		Map<Integer, List<SFTEpsilon<P, F, S>>> epsilonClosureAndPath = new HashMap<>();

		Collection<Integer> reached = new HashSet<Integer>(currentState);
		LinkedList<Integer> toVisit = new LinkedList<Integer>();
		toVisit.add(currentState);
		List<SFTEpsilon<P, F, S>> path = new ArrayList<SFTEpsilon<P, F, S>>();
		epsilonClosureAndPath.put(currentState, path);

		while (toVisit.size() > 0) {
			int fromState = toVisit.pop();
			for (SFTEpsilon<P, F, S> t : sft.getEpsilonMovesFrom(fromState)) {
				if (!reached.contains(t.to)) {
					reached.add(t.to);
					toVisit.add(t.to);
					List<SFTEpsilon<P, F, S>> newPath = epsilonClosureAndPath.get(fromState);
					newPath.add(t);
					epsilonClosureAndPath.put(t.to, newPath);
				} else {
					throw new IllegalArgumentException(
							"the epsilon transitions cause ambiguity (their relation is not a tree)");
				}
			}
		}

		return epsilonClosureAndPath;
	}

	private <P, F, S> Collection<Integer> getSFTEpsClosure(Integer currentState) {
		return getSFTEpsClosure(this, currentState);
	}

	/**
	 * get the set of all states reachable from <code>currentState</code> using zero or more epsilon transitions
	 * @param sft symbolic finite transducer
	 * @param currentState the start point
	 */
	private static <P extends CharPred, F extends TermInterface, S> Collection<Integer> getSFTEpsClosure(SFT<P, F, S> sft, Integer currentState) {
		Collection<Integer> epsilonClosure = new HashSet<Integer>();

		Collection<Integer> reached = new HashSet<Integer>(currentState);
		LinkedList<Integer> toVisit = new LinkedList<Integer>();
		toVisit.add(currentState);
		epsilonClosure.add(currentState);

		while (toVisit.size() > 0) {
			int fromState = toVisit.pop();
			for (SFTEpsilon<P, F, S> t : sft.getEpsilonMovesFrom(fromState)) {
				if (!reached.contains(t.to)) {
					reached.add(t.to);
					toVisit.add(t.to);
					epsilonClosure.add(t.to);
				} else {
					throw new IllegalArgumentException(
							"the epsilon transitions cause ambiguity (their relation is not a tree)");
				}
			}
		}

		return epsilonClosure;
	}

	private List<List<SFTMove<P, F, S>>> getPossibleTransitionChains(Integer startState, int steps) {
		return possibleTransitionChains(this, startState, steps);
	}

	/**
	 * Given a symbolic finite transducer and the start state, return all possible transition chains in
	 * <code>steps</code> steps
	 * @param sft symbolic finite transducer
	 * @param startState the start state which could be any state, including but not limited to the initial state
	 * @param steps the number of steps, which should be a natural number
	 * @return
	 */
	private static <P extends CharPred, F extends TermInterface, S> List<List<SFTMove<P, F, S>>> possibleTransitionChains(SFT<P, F, S> sft, Integer startState, int steps) {
		List<List<SFTMove<P, F, S>>> chains = new ArrayList<>();
		for (SFTMove<P, F, S> initialTransition: sft.getInputMovesFrom(startState)) {
			List<SFTMove<P, F, S>> tempList = new LinkedList<>();
			tempList.add(initialTransition);
			backtrack(chains, tempList, sft, steps - 1);
		}
		return chains;
	}

	// With help of: https://github.com/dineshappavoo/bfs-shortestpath/blob/master/src/com/bfs/shortestpath/graph/BFSShortestPath.java
	public static List<Character> getAccessString(SFT<CharPred, CharFunc, Character> automaton, int endState) throws TimeoutException {
		// End state == initial state thus the empty list suffices
		if (automaton.isInitialState(endState)) {
			return new ArrayList<>();
		}
		// Initial state
		int beginState = automaton.getInitialState();
//		System.out.println("Start in begin state "+beginState);

		// Maintain map of which states have been visited
		Map<Integer, Boolean> visited = new HashMap<>();
		for (int i=0; i < automaton.stateCount(); i++) {
			visited.put(i, false);
		}
		visited.put(beginState, true);
//		System.out.println("Visited: "+visited.toString());
		Queue<Pair<Integer, List<Integer>>> queue = new LinkedList<>();
		queue.add(new Pair<>(beginState, new ArrayList<>()));
//		System.out.println("Queue: "+queue);
		while (!queue.isEmpty()) {
			Pair<Integer, List<Integer>> pair = queue.poll();
//			System.out.println("Considering the pair "+pair.toString());
			int current = pair.first;
			List<Integer> path = pair.second;
			path.add(current);
			visited.put(current, true);

			Collection<SFTInputMove<CharPred, CharFunc, Character>> trans = automaton.getInputMovesFrom(current);
			List<Integer> neighbours = new ArrayList<>();
			for (SFTInputMove<CharPred, CharFunc, Character> t : trans) {
				neighbours.add(t.to);
			}

//			System.out.println("Neighbours: "+neighbours);
			for (int neighbour : neighbours) {
				if (neighbour == endState) {
//					System.out.println("Neighbour is end state "+neighbour);
					path.add(current);
					path.add(neighbour);
					return toInput(automaton, path);
				}
				if (visited.get(neighbour)) {
//					System.out.println("Have already visisted "+neighbour);
					continue;
				}
				path.add(current);
				queue.add(new Pair<>(neighbour, path));
				visited.put(neighbour, true);
//				System.out.println("Current path: "+path);
//				System.out.println("Current queue: "+queue);
//				System.out.println("Current visited set: "+visited);
			}
		}

		return null;
	}

	// Construct a path from first state (represented by first integer in list) to last state (last integer in list)
	// NOTE: ASSUMES Characters satisfy predicates!!
	private static List<Character> toInput(SFT<CharPred, CharFunc, Character> automaton, List<Integer> path) throws TimeoutException {
		// First state will be the first element in the list
		List<Character> input = new ArrayList<>();
		int i=0;
		while (i < path.size()-1) {
			int state = path.get(i);
			Collection<SFTInputMove<CharPred, CharFunc, Character>> transitions = automaton.getInputMovesFrom(state);
			for (SFTInputMove<CharPred, CharFunc, Character> t : transitions) {
				if (t.to == path.get(i+1)) {
					Character c = null;
					boolean initialChar = true;
					// Find random character that satisfies the transition
					// NOTE: It chooses a random character from a RANGE of the actual complete algebra!
					while (initialChar || !t.guard.isSatisfiedBy(c)) {
						int randomChar = ThreadLocalRandom.current().nextInt(1, 400);
						c = CharPred.MIN_CHAR;
						for (int k = 0; k < randomChar; k++) {
							c++;
						}
						initialChar = false;
					}
					input.add(c);
				}
			}
			i++;
		}
		return input;
	}

	// use backtrack method to get all possible transition chains
	private static <P extends CharPred, F extends TermInterface, S> void backtrack(List<List<SFTMove<P, F, S>>> chains, List<SFTMove<P, F, S>> tempList, SFT<P, F, S> sft, int remainSteps) {
		if (remainSteps < 0)
			return; // no solution
		else if (remainSteps == 0)
			chains.add(new ArrayList<SFTMove<P, F, S>>(tempList));
		else {
			Integer currentState = tempList.get(tempList.size() - 1).to;
			for (SFTMove<P, F, S> transition: sft.getTransitionsFrom(currentState)) {
				tempList.add(transition);
				backtrack(chains, tempList, sft, remainSteps - 1);
				tempList.remove(tempList.size() - 1);
			}
		}
	}

	public boolean decide1equality(SFT<P, F, S> otherSftWithEps, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		return decide1equality(this, otherSftWithEps, ba);
	}

	/**
	 * judge whether <code>sft1withEps</code> and <code>sft2withEps</code> are 1-equality
	 * (partial equivalent)
	 * Page 6, in the middle of left column, figure 3
	 *
	 * @param sft1withEps symbolic finite transducer 1 who may have epsilon transitions
	 * @param sft2withEps symbolic finite transducer 2 who may have epsilon transitions
	 */
	public static <P extends CharPred, F extends TermInterface, S> boolean decide1equality(SFT<P, F, S> sft1withEps,
													SFT<P, F, S> sft2withEps,
													BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		// Figure 3 line 1: C := A  \times B;
		SFTProduct<P, F, S> product = SFTProduct.MkSFTProduct(sft1withEps, sft2withEps, ba);
		// <code>product</code> is a product without epsilon transitions.

		// Figure 3 line 1: Q := \{q_c^0 \mapsto (\epsilon, \epsilon)\};
		HashMap<Integer, Pair<List<S>, List<S>>> reached = new HashMap<Integer, Pair<List<S>, List<S>>>();
		reached.put(product.getInitialState(), new Pair(new ArrayList<S>(), new ArrayList<S>()));

		// Figure 3 line 1: S := stack(q_c^0);
		LinkedList<Integer> toVisit = new LinkedList<Integer>();
		toVisit.add(product.getInitialState());

		// Figure 3 line 2: \textbf{while} \ S \neq \emptyset
		while (!toVisit.isEmpty()) {

			// Figure 3 line 3: p := pop(S);
			Integer currState = toVisit.pop();

			// Figure 3 line 3: (\alpha, \beta) := Q(p);
			Pair<List<S>, List<S>> promise = reached.get(currState);

			// Figure 3 line 4: \textbf{foreach} \  (p, \varphi, (\textbf f, \textbf g), q) \in R_C(p)
			for (SFTProductInputMove transition: product.getInputMovesFrom(currState)) {

				// Figure 3 line 5: (u, v) := (\alpha \cdot \textbf f,\beta \cdot \textbf g);
				List<F> u = new ArrayList<F>();
				List<F> v = new ArrayList<F>();
				for (S a: promise.first)
					u.add(ba.MkFuncConst(a));
				for (S b: promise.second)
					v.add(ba.MkFuncConst(b));
				u.addAll(transition.outputFunctions1);
				v.addAll(transition.outputFunctions2);

				// Figure 3 line 6: \textbf{if} \ q \in F_C \wedge |u| \neq |v| \ \textbf{return} \ f;
				/* the following code is the precise implementation of Figure 3 line 6.
				if (product.isFinalState(currState) && u.size() != v.size())
					return false;
				*/
				// However, we add tails for final states, so the SFT here is different from the SFT described on paper.
				// Here is the modification of Figure 3 line 6 so that the method decide1equality adapts to SFT with tails
				if (product.isFinalState(currState)) {
					Set<List<S>> tails1 = checkNotNull(product.getFinalStatesAndTails().get(currState).first);
					Set<List<S>> tails2 = checkNotNull(product.getFinalStatesAndTails().get(currState).second);
					// since product is constructed by 2 SFTs in MkSFTProduct in SFTProduct.java, tails1 and tails2
					// could not be null pointers.
					if (tails1.size() > 1 || tails2.size() > 1) // if any final state in any SFT has many tails, their
						// outputs are uncertain, so 2 SFTs cannot be 1-equality
						return false;
					List<F> finalU = new ArrayList<>(u);
					List<F> finalV = new ArrayList<>(v);
					for (List<S> tail: tails1)
						for (S element: tail)
							finalU.add(ba.MkFuncConst(element));
					for (List<S> tail: tails2)
						for (S element: tail)
							finalV.add(ba.MkFuncConst(element));
					if (finalU.size() != finalV.size())
						return false;
					for (int i = 0; i < finalU.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, finalU.get(i), finalV.get(i)))
							return false;
				}

				// Figure 3 line 7: \textbf{if} \ |u| \geq |v|
				if (u.size() >= v.size()) {

					// Figure 3 line 8: \textbf{if} \ \vee_{i=0}^{|v|-1}u_i \not\equiv_\varphi v_i \ \textbf{return} \ f;
					for (int i = 0; i < v.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, u.get(i), v.get(i)))
							return false;

					// Figure 3 line 9: w := [u_{|v|,\dots,u_{|u|-1}}];
					List<F> w = new ArrayList<>();
					for (int i = v.size(); i < u.size(); i++)
						w.add(u.get(i));

					// Figure 3 line 9: c := [\![ w]\!](witness(\varphi));
					S witness = (S)transition.getWitness(ba);
					List<S> c = new ArrayList<S>();
					List<F> cF = new ArrayList<>(); // a sequence of lamda x.alpha, where alpha is a constant S
					// stored in List<S> c
					for (int i = 0; i < u.size() - v.size(); i++) {
						c.add(ba.MkSubstFuncConst(w.get(i), witness));
						cF.add(ba.MkSubstFuncFunc(w.get(i), ba.MkFuncConst(witness)));
					}

					// Figure 3 line 10: \textbf{if} \ w \not\equiv_\varphi c \vee (q \in Dom(Q) \wedge Q(q) \neq
					// (\textbf c, \epsilon)) \textbf{return} \ f;
					for (int i = 0; i < u.size() - v.size(); i++)
						if (!ba.AreEquivalent(ba.MkSubstFuncPred(w.get(i), (P)transition.guard),
								ba.MkSubstFuncPred(cF.get(i), (P)transition.guard)))
							return false;

					if (reached.containsKey(currState) && !reached.get(currState).equals(new Pair(c, new ArrayList<S>())))
						return false;

					// Figure 3 line 11: \textbf{if} \ q \not\in Dom(Q) \ push(q,S);
					if (!reached.containsKey(currState)) {
						toVisit.push(currState);

						// Figure 3 line 11: Q(q):=(\textbf c, \epsilon);
						reached.put(currState, new Pair(c, new ArrayList<S>()));
					}

				// Figure 3 line 12: \textbf{if} \ |u|<|v| \dots (symmetrical \ to \ the \  case \ |u|>|v|)
				} else { // u.size() < v.size()
					for (int i = 0; i < u.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, u.get(i), v.get(i)))
							return false;
					List<F> w = new ArrayList<>();
					for (int i = u.size(); i < v.size(); i++)
						w.add(v.get(i));
					S witness = (S)transition.getWitness(ba);
					List<S> c = new ArrayList<S>();
					List<F> cF = new ArrayList<>(); // a sequence of lamda x.alpha, where alpha is a constant S
					// stored in List<S> c
					for (int i = 0; i < v.size() - u.size(); i++) {
						c.add(ba.MkSubstFuncConst(w.get(i), witness));
						cF.add(ba.MkSubstFuncFunc(w.get(i), ba.MkFuncConst(witness)));
					}
					for (int i = 0; i < v.size() - u.size(); i++)
						if (!ba.AreEquivalent(ba.MkSubstFuncPred(w.get(i), (P)transition.guard),
								ba.MkSubstFuncPred(cF.get(i), (P)transition.guard)))
							return false;
					if (reached.containsKey(currState) && !reached.get(currState).equals(new Pair(new ArrayList<S>(), c)))
						return false;
					if (!reached.containsKey(currState)) {
						toVisit.push(currState);
						reached.put(currState, new Pair(new ArrayList<S>(), c));
					}
				}
			}
		}

		// Figure 3 line 13: \textbf{return} \ t;
		return true;
	}

	public List<S> witness1disequality(SFT<P, F, S> otherSft, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		return witness1disequality(this, otherSft, ba);
	}

	/**
	 * generate a witness if <code>sft1withEps</code> and <code>sft2withEps</code> are not
	 * 1-equality
	 *
	 * @param sft1withEps symbolic finite transducer 1 who may has epsilon transitions
	 * @param sft2withEps symbolic finite transducer 2 who may has epsilon transitions
	 */
	public static <P extends CharPred, F extends TermInterface, S> List<S> witness1disequality(SFT<P, F, S> sft1withEps,
														SFT<P, F, S> sft2withEps,
														BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		SFTProduct<P, F, S> product = SFTProduct.MkSFTProduct(sft1withEps, sft2withEps, ba);
		// a product without epsilon transitions

		HashMap<Integer, Pair<List<S>, List<S>>> reached = new HashMap<Integer, Pair<List<S>, List<S>>>();
		LinkedList<Integer> toVisit = new LinkedList<Integer>();
		HashMap<Integer, List<S>> path = new HashMap<Integer, List<S>>();

		reached.put(product.getInitialState(), new Pair(new ArrayList<S>(), new ArrayList<S>()));
		// stores reached states and its promise
		toVisit.add(product.getInitialState());
		path.put(product.getInitialState(), new ArrayList<S>());
		// stores reached states and the latest path from the initial state to it, which means if there are many possible
		// paths from the initial state to it, only store the latest used one in the following loop

		while (!toVisit.isEmpty()) {

			Integer currState = toVisit.pop();
			Pair<List<S>, List<S>> promise = reached.get(currState);

			for (SFTProductInputMove transition: product.getInputMovesFrom(currState)) {

				List<S> previousPath = new ArrayList<S>(path.get(transition.from));
				previousPath.add((S) transition.getWitness(ba));
				path.put(transition.to, previousPath);

				List<F> u = new ArrayList<>();
				List<F> v = new ArrayList<>();
				for (S a: promise.first)
					u.add(ba.MkFuncConst(a));
				for (S b: promise.second)
					v.add(ba.MkFuncConst(b));
				u.addAll(transition.outputFunctions1);
				v.addAll(transition.outputFunctions2);

				if (product.isFinalState(currState)) {
					Set<List<S>> tails1 = checkNotNull(product.getFinalStatesAndTails().get(currState).first);
					Set<List<S>> tails2 = checkNotNull(product.getFinalStatesAndTails().get(currState).second);

					if (tails1.size() > 1 || tails2.size() > 1)
						return previousPath;
					List<F> finalU = new ArrayList<>(u);
					List<F> finalV = new ArrayList<>(v);
					for (List<S> tail: tails1)
						for (S element: tail)
							finalU.add(ba.MkFuncConst(element));
					for (List<S> tail: tails2)
						for (S element: tail)
							finalV.add(ba.MkFuncConst(element));
					if (finalU.size() != finalV.size())
						return previousPath;
					for (int i = 0; i < finalU.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, finalU.get(i), finalV.get(i)))
							return previousPath;
				}

				if (u.size() >= v.size()) {
					for (int i = 0; i < v.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, u.get(i), v.get(i))) {
							previousPath.addAll(product.getWitness(transition.to, ba));
							return previousPath;
						}
					List<F> w = new ArrayList<>();
					for (int i = v.size(); i < u.size(); i++)
						w.add(u.get(i));
					S witness = (S) transition.getWitness(ba);
					List<S> c = new ArrayList<S>();
					List<F> cF = new ArrayList<>(); // a sequence of lamda x.alpha, where alpha is a constant S
					// stored in List<S> c
					for (int i = 0; i < u.size() - v.size(); i++) {
						c.add(ba.MkSubstFuncConst(w.get(i), witness));
						cF.add(ba.MkSubstFuncFunc(w.get(i), ba.MkFuncConst(witness)));
					}
					for (int i = 0; i < u.size() - v.size(); i++)
						if (!ba.AreEquivalent(ba.MkSubstFuncPred(w.get(i), (P)transition.guard),
								ba.MkSubstFuncPred(cF.get(i), (P)transition.guard))) {
							previousPath.addAll(product.getWitness(transition.to, ba));
							return previousPath;
						}
					if (reached.containsKey(currState) && !reached.get(currState).equals(new Pair(c, new ArrayList<S>()))) {
						previousPath.addAll(product.getWitness(transition.to, ba));
						return previousPath;
					}
					if (!reached.containsKey(currState)) {
						toVisit.push(currState);
						reached.put(currState, new Pair(c, new ArrayList<S>()));
					}
				} else { // u.size() < v.size()
					for (int i = 0; i < u.size(); i++)
						if (!ba.CheckGuardedEquality((P) transition.guard, u.get(i), v.get(i))) {
							previousPath.addAll(product.getWitness(transition.to, ba));
							return previousPath;
						}
					List<F> w = new ArrayList<>();
					for (int i = u.size(); i < v.size(); i++)
						w.add(v.get(i));
					S witness = (S) transition.getWitness(ba);
					List<S> c = new ArrayList<S>();
					List<F> cF = new ArrayList<>(); // a sequence of lamda x.alpha, where alpha is a constant S
					// stored in List<S> c
					for (int i = 0; i < v.size() - u.size(); i++) {
						c.add(ba.MkSubstFuncConst(w.get(i), witness));
						cF.add(ba.MkSubstFuncFunc(w.get(i), ba.MkFuncConst(witness)));
					}
					for (int i = 0; i < v.size() - u.size(); i++)
						if (!ba.AreEquivalent(ba.MkSubstFuncPred(w.get(i), (P)transition.guard),
								ba.MkSubstFuncPred(cF.get(i), (P)transition.guard))) {
							previousPath.addAll(product.getWitness(transition.to, ba));
							return previousPath;
						}
					if (reached.containsKey(currState) && !reached.get(currState).equals(new Pair(new ArrayList<S>(), c))) {
						previousPath.addAll(product.getWitness(transition.to, ba));
						return previousPath;
					}
					if (!reached.containsKey(currState)) {
						toVisit.push(currState);
						reached.put(currState, new Pair(new ArrayList<S>(), c));
					}
				}

			}
		}
		return null; // two SFTs are partial equivalent
	}

	/**
	 * Computes the domain automaton of the sft
	 * Page 4, right column, the 17th line from the bottom
	 *
	 * @throws TimeoutException
	 */
	public SFA<P, S> getDomain(BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		Collection<SFAMove<P, S>> transitions = new ArrayList<SFAMove<P, S>>();

		for (SFTInputMove<P, F, S> t : getInputMovesFrom(states))
			transitions.add(new SFAInputMove<P, S>(t.from, t.to, t.guard));

		for (SFTEpsilon<P, F, S> t : getEpsilonMovesFrom(states))
			transitions.add(new SFAEpsilon<P, S>(t.from, t.to));

		Collection<Integer> finalStates = getFinalStates();

		return SFA.MkSFA(transitions, initialState, finalStates, ba);
	}

	/**
	 * compute the inverse image under <code>sfa</code>, which means compose(sft, sfa)
	 * Page 7, left column, the first line
	 *
	 * @throws TimeoutException
	 */
	public SFA<P, S> inverseImage(SFA<P, S> sfaWithEps, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		SFT<P, F, S> composition = this.composeWith(SFAtoSFT(sfaWithEps, ba), ba);
		return composition.getDomain(ba);
	}

	/**
	 * convert a sfa to a sft by adding empty set of output functions in every transition
	 *
	 * @return corresponding sft
	 */
	private static <P extends CharPred, F extends TermInterface, S> SFT<P, F, S> SFAtoSFT(SFA<P, S> sfa, BooleanAlgebraSubst<P, F, S> ba) {
		Collection<SFTMove<P, F, S>> transitions = new ArrayList<>();
		for (Integer state: sfa.getStates()) {
			for (SFAInputMove<P, S> transition: sfa.getInputMovesFrom(state)) {
				List<F> outputFunctions = new LinkedList<>();
				SFTInputMove<P, F, S> newTrans = new SFTInputMove<>(transition.from, transition.to,
						transition.guard, outputFunctions);
				transitions.add(newTrans);
			}
			for (SFAEpsilon<P, S> transition: sfa.getEpsilonFrom(state)) {
				List<S> outputs = new LinkedList<S>();
				SFTEpsilon<P, F, S> newTrans = new SFTEpsilon<>(transition.from, transition.to, outputs);
				transitions.add(newTrans);
			}
		}
		Map<Integer, Set<List<S>>> finalStatesAndTails = new HashMap<Integer, Set<List<S>>>();
		for (Integer finalState: sfa.getFinalStates())
			finalStatesAndTails.put(finalState, new HashSet<List<S>>());
		return MkSFT(transitions, sfa.getInitialState(), finalStatesAndTails, ba);
	}

	/**
	 * check whether <code>input</code> is in the pre image of <code>transducer</code> under <code>output</code>
	 *
	 */
	public static <P extends CharPred, F extends TermInterface, S> boolean typeCheck(SFA<P, S> input, SFT<P, F, S> transducer, SFA<P, S> output,
							 BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException  {
		SFA<P, S> complementOfOutput = output.complement(ba);
		SFA<P, S> preImage = transducer.inverseImage(complementOfOutput, ba);
		SFA<P, S> intersection = input.intersectionWith(preImage, ba);
		return intersection.isEmpty();
	}

	/**
	 * compute the domain restriction of <code>this</code> for <code>sfaWithEps</code>
	 * Page 7, left column, the 8th line
	 *
	 * @return the domain restriction of the current sft
	 */
	public SFT<P, F, S> domainRestriction(SFA<P, S> sfaWithEps, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
		// Remove epsilons
		SFT<P, F, S> sft = this.removeEpsilonMoves(ba);
		SFA<P, S> sfa = sfaWithEps.removeEpsilonMoves(ba);

		Collection<SFTMove<P, F, S>> transitions = new ArrayList<SFTMove<P, F, S>>();
		Integer initialState = 0;

		HashMap<Pair<Integer, Integer>, Integer> reached = new HashMap<Pair<Integer, Integer>, Integer>();
		LinkedList<Pair<Integer, Integer>> toVisit = new LinkedList<Pair<Integer, Integer>>();

		// Add initial state
		Pair<Integer, Integer> p = new Pair<Integer, Integer>(sft.getInitialState(), sfa.getInitialState());

		initialState = 0;
		Map<Integer, Set<List<S>>> finalStatesAndTails = new HashMap<Integer, Set<List<S>>>();

		reached.put(p, initialState);
		toVisit.add(p);

		// Combined has set of variables the disjoint union of the two sets
		while (!toVisit.isEmpty()) {
			Pair<Integer, Integer> currState = toVisit.removeFirst();
			int currStateId = reached.get(currState);

			// If both states are final, combine is final
			if (sft.isFinalState(currState.first) && sfa.isFinalState(currState.second)) {
				finalStatesAndTails.put(currStateId, sft.getFinalStatesAndTails().get(currState.first));
			}

			for (SFTInputMove<P, F, S> t1 : sft.getInputMovesFrom(currState.first))
				for (SFAInputMove<P, S> t2 : sfa.getInputMovesFrom(currState.second)) {
					P intersGuard = ba.MkAnd(t1.guard, t2.guard);
					if (ba.IsSatisfiable(intersGuard)) {
						Pair<Integer, Integer> nextState = new Pair<Integer, Integer>(t1.to, t2.to);
						int nextStateId = getStateId(nextState, reached, toVisit); // update reached and toVisit
						SFTInputMove<P, F, S> newTrans = new SFTInputMove<P, F, S>(currStateId, nextStateId,
								intersGuard, t1.outputFunctions);
						transitions.add(newTrans);
					}
				}
		}
		return MkSFT(transitions, initialState, finalStatesAndTails, ba);
	}

	public SFA<P, S> getOutputSFA(BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {

		Collection<SFAMove<P, S>> transitions = new ArrayList<SFAMove<P, S>>();
		Integer initialState;
		Collection<Integer> finalStates = new HashSet<Integer>();

		Map<Integer, Integer> reached = new HashMap<Integer, Integer>();
		LinkedList<Integer> toVisit = new LinkedList<Integer>();

		Integer moreStateId = this.maxStateId + 1;

		// Add initial state
		initialState = 0;
		reached.put(this.initialState, initialState);
		toVisit.push(this.initialState);

		// depth first search
		while (!toVisit.isEmpty()) {
			Integer currState = toVisit.pop();
			int currStateId = reached.get(currState);

			if (this.isFinalState(currState)) {
				Set<List<S>> tails = this.getFinalStatesAndTails().get(currState);
				if (tails.size() == 0) {
					finalStates.add(currStateId);
				} else { // currState is a final state and it has non-empty tails
					for (List<S> tail: tails) {
						int nextStateId = getStateId(moreStateId++, reached, toVisit);
						SFAInputMove<P, S> newTrans = new SFAInputMove<P, S>(currStateId, nextStateId,
								ba.MkAtom(tail.get(0)));
						transitions.add(newTrans);
						int lastStateId = nextStateId;
						for (int i = 1; i < tail.size(); i++) {
							nextStateId = getStateId(moreStateId++, reached, toVisit);
							newTrans = new SFAInputMove<P, S>(lastStateId, nextStateId,
									ba.MkAtom(tail.get(i)));
							transitions.add(newTrans);
							lastStateId = nextStateId;
						}
						finalStates.add(lastStateId);
					}
				}
			}

			for (SFTEpsilon<P, F, S> t1 : this.getEpsilonMovesFrom(currState)) {
				// if t1.outputs.size() == 0, just do nothing
				if (t1.outputs.size() == 1) {
					int nextStateId = getStateId(t1.to, reached, toVisit);
					SFAInputMove<P, S> newTrans = new SFAInputMove<P, S>(currStateId, nextStateId,
							ba.MkAtom(t1.outputs.get(0)));
					transitions.add(newTrans);
				} else if (t1.outputs.size() > 1) {
					int nextStateId = getStateId(moreStateId++, reached, toVisit);
					SFAInputMove<P, S> newTrans = new SFAInputMove<P, S>(currStateId, nextStateId,
							ba.MkAtom(t1.outputs.get(0)));
					transitions.add(newTrans);
					int lastStateId = nextStateId;
					for (int i = 1; i < t1.outputs.size() - 1; i++) {
						nextStateId = getStateId(moreStateId++, reached, toVisit);
						newTrans = new SFAInputMove<P, S>(lastStateId, nextStateId, ba.MkAtom(t1.outputs.get(i)));
						transitions.add(newTrans);
						lastStateId = nextStateId;
					}
					nextStateId = getStateId(t1.to, reached, toVisit);
					newTrans = new SFAInputMove<P, S>(currStateId, nextStateId, ba.MkAtom(t1.outputs.get(t1.outputs.size() - 1)));
					transitions.add(newTrans);
				}
			}

			for (SFTInputMove<P, F, S> t1 : this.getInputMovesFrom(currState)) {
				// if t1.outputFunctions.size() == 0, just do nothing
				if (t1.outputFunctions.size() == 1) {
					int nextStateId = getStateId(t1.to, reached, toVisit);
					SFAInputMove<P, S> newTrans = new SFAInputMove<P, S>(currStateId, nextStateId,
							ba.getRestrictedOutput(t1.guard, t1.outputFunctions.get(0)));
					transitions.add(newTrans);
				} else if (t1.outputFunctions.size() > 1) {
					throw new UnsupportedOperationException("Not supported yet.");
				}
			}

		}

		return SFA.MkSFA(transitions, initialState, finalStates, ba);
	}

	/**
	 * Add Transition
	 *
	 * @throws TimeoutException
	 */
	private void addTransition(SFTMove<P, F, S> transition, BooleanAlgebraSubst<P, F, S> ba, boolean skipSatCheck)
			throws TimeoutException {

//		if (transition.isEpsilonTransition()) {
//			System.out.println("ADDING EPSILON TRANSITION");
//			getEpsilonMovesFrom(transition.from).add((SFTEpsilon<P, F, S>) transition);
//			getEpsilonMovesTo(transition.to).add((SFTEpsilon<P, F, S>) transition);
//			isEpsilonFree = false;
//		}

		if (skipSatCheck || transition.isSatisfiable(ba)) {

			if (transition.from > maxStateId)
				maxStateId = transition.from;
			if (transition.to > maxStateId)
				maxStateId = transition.to;

			states.add(transition.from);
			states.add(transition.to);

			if (transition.isEpsilonTransition()) {
				getEpsilonMovesFrom(transition.from).add((SFTEpsilon<P, F, S>) transition);
				getEpsilonMovesTo(transition.to).add((SFTEpsilon<P, F, S>) transition);
			} else {
				getInputMovesFrom(transition.from).add((SFTInputMove<P, F, S>) transition);
				getInputMovesTo(transition.to).add((SFTInputMove<P, F, S>) transition);
			}
		}
	}


	// ACCESORIES METHODS

	// GET INPUT MOVES

	/**
	 * Returns the set of transitions from state <code>s</code>
	 */
	public Collection<SFTInputMove<P, F, S>> getInputMovesFrom(Integer state) {
		Collection<SFTInputMove<P, F, S>> trset = transitionsFrom.get(state);
		if (trset == null) {
			trset = new HashSet<SFTInputMove<P, F, S>>();
			transitionsFrom.put(state, trset);
		}
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<SFTInputMove<P, F, S>> getInputMovesFrom(Collection<Integer> stateSet) {
		Collection<SFTInputMove<P, F, S>> transitions = new LinkedList<SFTInputMove<P, F, S>>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesFrom(state));
		return transitions;
	}

	/**
	 * Returns the set of input transitions to state <code>s</code>
	 */
	public Collection<SFTInputMove<P, F, S>> getInputMovesTo(Integer state) {
		Collection<SFTInputMove<P, F, S>> trset = transitionsTo.get(state);
		if (trset == null) {
			trset = new HashSet<SFTInputMove<P, F, S>>();
			transitionsTo.put(state, trset);
		}
		return trset;
	}

	/**
	 * Returns the set of transitions to set of states
	 */
	public Collection<SFTInputMove<P, F, S>> getInputMovesTo(Collection<Integer> stateSet) {
		Collection<SFTInputMove<P, F, S>> transitions = new LinkedList<SFTInputMove<P, F, S>>();
		for (Integer state : stateSet)
			transitions.addAll(getInputMovesTo(state));
		return transitions;
	}

	// GET Epsilon MOVES

	/**
	 * Returns the set of transitions to state <code>s</code>
	 */
	public Collection<SFTEpsilon<P, F, S>> getEpsilonMovesFrom(Integer state) {
		Collection<SFTEpsilon<P, F, S>> trset = epsTransitionsFrom.get(state);
		if (trset == null) {
			trset = new HashSet<>();
			epsTransitionsFrom.put(state, trset);
		}
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<SFTEpsilon<P, F, S>> getEpsilonMovesFrom(Collection<Integer> stateSet) {
		Collection<SFTEpsilon<P, F, S>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getEpsilonMovesFrom(state));
		return transitions;
	}

	/**
	 * Returns the set of input transitions to state <code>s</code>
	 */
	public Collection<SFTEpsilon<P, F, S>> getEpsilonMovesTo(Integer state) {
		Collection<SFTEpsilon<P, F, S>> trset = epsTransitionsTo.get(state);
		if (trset == null) {
			trset = new HashSet<SFTEpsilon<P, F, S>>();
			epsTransitionsTo.put(state, trset);
		}
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<SFTEpsilon<P, F, S>> getEpsilonMovesTo(Collection<Integer> stateSet) {
		Collection<SFTEpsilon<P, F, S>> transitions = new LinkedList<>();
		for (Integer state : stateSet)
			transitions.addAll(getEpsilonMovesTo(state));
		return transitions;
	}

	// GET ALL MOVES
	/**
	 * Returns the set of transitions starting at state <code>s</code>
	 */
	public Collection<SFTMove<P, F, S>> getTransitionsFrom(Integer state) {
		Collection<SFTMove<P, F, S>> trset = new HashSet<>();
		trset.addAll(getInputMovesFrom(state));
		trset.addAll(getEpsilonMovesFrom(state));
		return trset;
	}

	/**
	 * Returns the set of transitions starting at a set of states
	 */
	public Collection<SFTMove<P, F, S>> getTransitionsFrom(Collection<Integer> stateSet) {
		Collection<SFTMove<P, F, S>> trset = new HashSet<>();
		trset.addAll(getInputMovesFrom(stateSet));
		trset.addAll(getEpsilonMovesFrom(stateSet));
		return trset;
	}

	/**
	 * Returns the set of transitions to state <code>s</code>
	 */
	public Collection<SFTMove<P, F, S>> getTransitionsTo(Integer state) {
		Collection<SFTMove<P, F, S>> trset = new HashSet<>();
		trset.addAll(getInputMovesTo(state));
		trset.addAll(getEpsilonMovesTo(state));
		return trset;
	}

	/**
	 * Returns the set of transitions to a set of states
	 */
	public Collection<SFTMove<P, F, S>> getTransitionsTo(Collection<Integer> stateSet) {
		Collection<SFTMove<P, F, S>> trset = new HashSet<>();
		trset.addAll(getInputMovesTo(stateSet));
		trset.addAll(getEpsilonMovesTo(stateSet));
		return trset;
	}

	/**
	 * Returns the set of transitions starting set of states
	 */
	public Collection<SFTMove<P, F, S>> getTransitions() {
		return getTransitionsFrom(states);
	}

	// Methods for superclass
	@Override
	public Collection<Move<P, S>> getMoves() {
		Collection<Move<P, S>> transitions = new LinkedList<Move<P, S>>();
		transitions.addAll(getTransitions());
		return transitions;
	}

	@Override
	public Collection<Move<P, S>> getMovesFrom(Integer state) {
		Collection<Move<P, S>> transitions = new LinkedList<Move<P, S>>();
		transitions.addAll(getTransitionsFrom(state));
		return transitions;
	}

	@Override
	public Collection<Move<P, S>> getMovesTo(Integer state) {
		Collection<Move<P, S>> transitions = new LinkedList<Move<P, S>>();
		transitions.addAll(getTransitionsTo(state));
		return transitions;
	}

	@Override
	public Collection<Integer> getFinalStates() {
		return finalStatesAndTails.keySet();
	}

	public Map<Integer, Set<List<S>>> getFinalStatesAndTails() {
		return finalStatesAndTails;
	}

	@Override
	public Integer getInitialState() {
		return initialState;
	}

	@Override
	public Collection<Integer> getStates() {
		return states;
	}

	@Override
	public Object clone() {
		SFT<P, F, S> cl = new SFT<P, F, S>();

		cl.isDeterministic = isDeterministic;
		cl.isTotal = isTotal;
		cl.isEmpty = isEmpty;
		cl.isEpsilonFree = isEpsilonFree;

		cl.maxStateId = maxStateId;

		cl.states = new HashSet<Integer>(states);
		cl.initialState = initialState;

		cl.transitionsFrom = new HashMap<Integer, Collection<SFTInputMove<P, F, S>>>(transitionsFrom);
		cl.transitionsTo = new HashMap<Integer, Collection<SFTInputMove<P, F, S>>>(transitionsTo);

		cl.epsTransitionsFrom = new HashMap<Integer, Collection<SFTEpsilon<P, F, S>>>(epsTransitionsFrom);
		cl.epsTransitionsTo = new HashMap<Integer, Collection<SFTEpsilon<P, F, S>>>(epsTransitionsTo);

		cl.finalStatesAndTails = new HashMap<Integer, Set<List<S>>>(finalStatesAndTails);

		return cl;
	}

	@Override
	public String toString() {
		String s = "";
		s = "SFT: " + getMoves().size() + " transitions, " + getStates().size() + " states" + "\n";
		s += "Transitions \n";
		for (Move<P, S> t : getMoves())
			s = s + t + "\n";

		s += "Initial State \n";
		s = s + getInitialState() + "\n";

		s += "Final States \n";
		for (Integer fs : getFinalStates())
			if (getFinalStatesAndTails().get(fs) == null)
				s = s + fs + "\n";
			else
				s = s + fs + " " + getFinalStatesAndTails().get(fs) + "\n";
		return s;
	}

	public static SFT<CharPred, CharFunc, Character> getSpec() {
		UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
		int numStates = 1;
		Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
		for (int i=0; i<numStates; i++) {
			finalStatesAndTails.put(i, new HashSet<>());
		}

		int initial = 0;
		List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();
		List<CharPred> guards = new ArrayList<>();

		List<CharFunc> terms = new ArrayList<>();
		terms.add(new CharConstant('&'));
		terms.add(new CharConstant('a'));
		terms.add(new CharConstant('m'));
		terms.add(new CharConstant('p'));
		terms.add(new CharConstant(';'));
		CharPred g = new CharPred('&', '&');
		transitions.add(new SFTInputMove<>(0, 1, g, terms));
		guards.add(g);

		transitions.add(new SFTInputMove<>(1, 2, g, terms));
		guards.add(g);

		terms = new ArrayList<>();
		terms.add(CharOffset.IDENTITY);
		transitions.add(new SFTInputMove<>(0, 2, ba.MkNot(g), terms));


		CharPred largeGuard = ba.False();
		List<CharFunc> largeTerms = new ArrayList<>();
		for (CharPred gg : guards) {
			largeGuard = ba.MkOr(largeGuard, gg);
		}
		largeGuard = ba.MkNot(largeGuard);
		largeTerms.add(CharOffset.IDENTITY);
		transitions.add(new SFTInputMove<>(2, 2, largeGuard, largeTerms));

		return MkSFT(transitions, initial, finalStatesAndTails, ba);
	}


	public static void main(String[] args) throws TimeoutException {

//		SFT spec = CyberchefSpecifications.getRemoveWhitespaceSpec();
//		System.out.println(spec);
//		for (int i=0; i<spec.stateCount(); i++) {
//			try {
//				System.out.println("Access string for "+i+" is "+getAccessString(spec, i));
//			} catch (TimeoutException e) {
//				e.printStackTrace();
//			}
//
//		}


		SFT spec = getSpec();
		List<Integer> path = new ArrayList<>();
		path.add(0);
		path.add(2);
		System.out.println(toInput(spec, path));

	}

}

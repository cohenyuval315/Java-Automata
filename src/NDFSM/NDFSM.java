package NDFSM;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import Alphabet.Alphabet;
import State.IdentifiedDFSMState;
import State.IdentifiedState;
import State.State;
import Transitions.Transition;
import Transitions.TransitionMapping;
import Transitions.TransitionTuple;

import java.util.LinkedList;
import java.util.Queue;

public class NDFSM {

	protected TransitionMapping transitions;
	protected Set<State> states;
	protected Set<State> acceptingStates;
	protected State initialState;
	protected Alphabet alphabet;
	
	/**
	 * Builds a NDFSM from a string representation (encoding) 
	 *  
	 * @param encoding	the string representation of a NDFSM
	 * @throws Exception if the encoding is incorrect or if the transitions contain invalid states or symbols
	 */
	public NDFSM(String encoding) throws Exception {
		parse(encoding);
		
		transitions.verify(states,alphabet);
	}
	
	
	/**
	 * Build a NDFSM from its components
	 * 
	 * @param states			the set of states for this machine
	 * @param alphabet			this machine's alphabet
	 * @param transitions		the transition mapping of this machine
	 * @param initialState		the initial state (must be a member of states)
	 * @param acceptingStates	the set of accepting states (must be a subset of states)
	 * @throws Exception if the components do not represent a valid non deterministic machine
	 */
	public NDFSM(Set<State> states, Alphabet alphabet, Set<Transition> transitions, State initialState,
			Set<State> acceptingStates) throws Exception {
		
		initializeFrom(states, alphabet, transitions, initialState, acceptingStates);
		this.transitions.verify(this.states,alphabet);
	}

	
	protected void initializeFrom(Set<State> states, Alphabet alphabet, Set<Transition> transitions, State initialState, Set<State> acceptingStates) {

		this.states = states;
		this.alphabet = alphabet;
		this.transitions = createMapping(transitions);
		this.initialState = initialState;
		this.acceptingStates = acceptingStates;
	}

	protected NDFSM() { }
	
	/** Overrides this machine with the machine encoded in string.
	 * 
	 *  <p>Here's an example of the encoding:</p>
	 <pre>
	0 1/a b/0 , a , 0; 0,b, 1 ;1, a, 0 ; 1, b, 1/0/ 1
	</pre>
	<p>This is the encoding of a finite state machine with two states (identified as 0 and 1), 
	an alphabet that consists of the two characters 'a' and 'b', and four transitions:</p>
	<ol>
	<li>From state 0 on character a it moves to state 0</li>
	<li>from state 0 on character b it moves to state 1,</li>
	<li>from state 1 on character a it moves to state 0,</li>
	<li>from state 1 on character b it moves to state 1.</li>
	</ol>
	<p>The initial state of this machine is 0, and the set of accepting states consists of 
	just one state 1. Here is the format in general:</p>
	  
	 <pre>
	 {@code
	<states> / <alphabet> / <transitions> / <initial state> / <accepting states>
	}
	</pre>
	
	where:
	
	<pre>
	{@code
	<alphabet> is <char> <char> ...
	
	<transitions> is <transition> ; <transition> ...
	
	<transition> is from , char, to
	
	<initial state> is an integer
	
	<accepting states> is <state> <state> ...
	
	<state> is an integer
	}
	</pre>
	
	@param string the string encoding 
	@throws Exception if the string encoding is invalid
	*/
	public void parse(String string) throws Exception {
		
		Scanner scanner = new Scanner(string);
		
		scanner.useDelimiter("\\s*/");	
		
		Map<Integer, State> states = new HashMap<Integer, State>();
		
		for(Integer stateId : IdentifiedState.parseStateIdList(scanner.next())) {
			states.put(stateId, new IdentifiedState(stateId));
		}

		Alphabet alphabet = Alphabet.parse(scanner.next());
		
		Set<Transition> transitions = new HashSet<Transition>();
		
		for (TransitionTuple t: TransitionTuple.parseTupleList(scanner.next())) {
			transitions.add(new Transition(states.get(t.fromStateId()), t.symbol(), states.get(t.toStateId())));
		}
		
		State initialState = states.get(scanner.nextInt());
		
		Set<State> acceptingStates = new HashSet<State>();

		if (scanner.hasNext())
			for(Integer stateId : IdentifiedState.parseStateIdList(scanner.next())) {
				acceptingStates.add(states.get(stateId));
			}
		
		scanner.close();
		
		initializeFrom(new HashSet<State>(states.values()), alphabet, transitions, initialState, acceptingStates);
		this.transitions.verify(this.states, alphabet);
	}

	private String parseElement(Set<State> e) {
		String re = "{";
		for (State s : e) {
			re = re +  " " + s.toString();
		}		
		Map<Integer, State> states = new HashMap<Integer, State>();
		State initialState = states.get(e);
		
		String r = re + "}";
		return r;
	}
	
	protected TransitionMapping createMapping(Set<Transition> transitions) {
		return new TransitionRelation(transitions);
	}
		
	
	/** Returns a version of this state machine with all the unreachable states removed.
	 * 
	 * @return NDFSM that recognizes the same language as this machine, but has no unreachable states.
	 */
	public NDFSM removeUnreachableStates() {

		Set<State> reachableStates = reachableStates();

		Set<Transition> transitionsToReachableStates = new HashSet<Transition>();
		
		for(Transition t : transitions.transitions()) {
			if (reachableStates.contains(t.fromState()) && reachableStates.contains(t.toState()))
				transitionsToReachableStates.add(t);
		}
		
		Set<State> reachableAcceptingStates = new HashSet<State>();
		for(State s : acceptingStates) {
			if (reachableStates.contains(s))
				reachableAcceptingStates.add(s);
		}
		
		NDFSM aNDFSM = (NDFSM)create();
		
		aNDFSM.initializeFrom(reachableStates, alphabet, transitionsToReachableStates, initialState, reachableAcceptingStates);
		
		return aNDFSM;
	}

	
	protected NDFSM create() {
		return new NDFSM();
	}

	
	// returns a set of all states that are reachable from the initial state
	private Set<State> reachableStates() {
		
		List<Character> symbols = new ArrayList<Character>();
		
		symbols.add(Alphabet.EPSILON);
		
		for(Character c : alphabet) {
			symbols.add(c);
		}
		
		Alphabet alphabetWithEpsilon = new Alphabet(symbols);
		
		Set<State> reachable = new HashSet<State>();

		Set<State> newlyReachable = new HashSet<State>();

		newlyReachable.add(initialState);

		while(!newlyReachable.isEmpty()) {
			reachable.addAll(newlyReachable);
			newlyReachable = new HashSet<State>();
			for(State state : reachable) {
				for(Character symbol : alphabetWithEpsilon) {
					for(State s : transitions.at(state, symbol)) {
						if (!reachable.contains(s))
							newlyReachable.add(s);
					}
				}
			}
		}
		
		return reachable;
	}

	
	/** Encodes this state machine as a string
	 * 
	 * @return the string encoding of this state machine
	 */
	public String encode() {
		return  State.encodeStateSet(states) + "/" +
				alphabet.encode() + "/" + 
				transitions.encode() + "/" + 
				initialState.encode() + "/" +
				State.encodeStateSet(acceptingStates);
	}
	
	
	/** Prints a set notation description of this machine.
	 * 
	 * <p>To see the Greek symbols on the console in Eclipse, go to Window -&gt; Preferences -&gt; General -&gt; Workspace 
	 * and change <tt>Text file encoding</tt> to <tt>UTF-8</tt>.</p>
	 * 
	 * @param out the output stream on which the description is printed.
	 */
	public void prettyPrint(PrintStream out) {
		out.print("K = ");
		State.prettyPrintStateSet(states, out);
		out.println("");
		
		out.print("\u03A3 = ");
		alphabet.prettyPrint(out);
		out.println("");
		
		out.print(transitions.prettyName() + " = ");
		transitions.prettyPrint(out);
		out.println("");
		
		out.print("s = ");
		initialState.prettyPrint(out);
		out.println("");
		
		out.print("A = ");
		State.prettyPrintStateSet(acceptingStates, out);
		out.println("");		
	}

	/** Returns a canonic version of this machine. 
<p>The canonic encoding of two minimal state machines that recognize the same language is identical.</p>

@return a canonic version of this machine. 
*/
	public NDFSM toCanonicForm() {
	
		Set<Character> alphabetAndEpsilon = new HashSet<Character>();
		
		for(Character symbol : alphabet) {
			alphabetAndEpsilon.add(symbol);
		}
		alphabetAndEpsilon.add(Alphabet.EPSILON);
		
		Set<Transition> canonicTransitions = new HashSet<Transition>();
		Stack<State> todo = new Stack<State>();
		Map<State, State> canonicStates = new HashMap<State, State>();
		Integer free = 0;
		
		todo.push(initialState);
		canonicStates.put(initialState, new IdentifiedState(free));
		free++;
		
		while (!todo.isEmpty()) {
			State top = todo.pop();
			for(Character symbol : alphabetAndEpsilon) {
				for(State nextState : transitions.at(top, symbol)) {
					if (!canonicStates.containsKey(nextState)) {
						canonicStates.put(nextState, new IdentifiedState(free));
						todo.push(nextState);
						free++;
					}
					canonicTransitions.add(new Transition(canonicStates.get(top), symbol, canonicStates.get(nextState)));
				}
			}			
		}

		Set<State> canonicAcceptingStates = new HashSet<State>();
		for(State s : acceptingStates) {
			if (canonicStates.containsKey(s)) // unreachable accepting states will not appear in the canonic form of the state machine
				canonicAcceptingStates.add(canonicStates.get(s));
		}
		
		NDFSM aNDFSM = create();
		
		aNDFSM.initializeFrom(new HashSet<State>(canonicStates.values()), alphabet, canonicTransitions, canonicStates.get(initialState), canonicAcceptingStates);

		return aNDFSM;
	}
	
	public boolean compute(String input) throws Exception {
		return toDFSM().compute(input);
	}
	
//	public DFSM toDFSM() throws Exception {
//		
//		int i = 1;
//		boolean acceptingState = false;
//		
//		
//		Set<State> initialStates = epsilon(initialState);
//		
//		
//		Set<State> acceptingStates = new HashSet<>();
//		
//		List<Set<State>> activeStates = new LinkedList<>();
//		
//		Set<Transition> newTransitions = new HashSet<>();
//		
//		Set<State> allStates = new HashSet<>();
//		
//		Map<Set<State>, IdentifiedState> map = new HashMap<>();
//		
//		activeStates.add(initialStates);
//		
//
//		map.put(initialStates, new IdentifiedState(Integer.valueOf(0)));
//		
//		while (!activeStates.isEmpty()) { 
//			for (Character c : alphabet) {
//				Set<State> newState = new HashSet<>();
//				Set<State> activeState = new HashSet<>();
//				for (State s : activeStates.get(0)) {
//					if ((!acceptingState) && (this.acceptingStates.contains(s))) {
//						acceptingState = true;
//					}
//					activeState = transitions.at(s, c);
//					if (!activeState.isEmpty()) {
//						newState.addAll(activeState);
//						for (State st : activeState) {
//							newState.addAll(eps(st));
//						}
//					}
//				}
//				if (!map.containsKey(newState)) {
//					activeStates.add(newState);
//					map.put(newState, new IdentifiedState(Integer.valueOf(i)));
//					i++;
//				}
//				if (acceptingState) {
//					acceptingStates.add((State) map.get(activeStates.get(0)));
//				}
//				acceptingState = false;
//				newTransitions.add(new Transition((State) map.get(activeStates.get(0)), c, (State) map.get(newState)));
//			}
//			activeStates.remove(0);
//		}
//		allStates.addAll(map.values());
//		
//		
//		
//		return new DFSM(allStates, alphabet, newTransitions, (State) map.get(initialStates), acceptingStates);
//	}

//	private Set<State> eps(State theState) {
//      //  TransitionRelation tr = new TransitionRelation(transitions.transitions());
//		Set<State> eps = new HashSet<>();
//		
//		Set<State> reachable = new HashSet<>();
//		
//		reachable.add(theState);
//			
//		for(State s: reachable) {
//			eps.addAll(reachable);
//			reachable = new HashSet<>();
//			for (State e : transitions.at(s, Alphabet.EPSILON)) {
//				System.out.println("e:");
//				e.prettyPrint(System.out);
//				
//				if (!eps.contains(e)) {
//					reachable.add(e);
//				}
//			}
//		}
//		return eps;
//	}
//	
	
	
	private State normalizeElement(Set<State> e) {
		String re = "{";
		int i = 0;
		for (State s : e) {
			if ( i == 0) {
				re = re +  "" + s.encode();
				i += 1;
			}else {
				re = re +  "," + s.encode();
				
			}
		}		
		State normalElement = new IdentifiedDFSMState(re + "}");
		return normalElement;
	}
	
	public DFSM toDFSM() throws Exception{
		List<Set<State>> activeStates = new LinkedList<>();
		Set<State> allStates = new HashSet<>();
		Set<Transition> allTransitions = new HashSet<>();
		Set<State> acceptingStates = new HashSet<>();
		Set<State> initialStates = epsilon(initialState);
		allStates.add(normalizeElement(initialStates));
		
		
		List<Set<State>> check = new LinkedList<>();
		
		activeStates.add(initialStates);
		
		while (!activeStates.isEmpty()) { 
			Set<State> element = activeStates.get(0);
			if (check.contains(element)) {
				activeStates.remove(0);
				continue;
			}
			check.add(element);
			
			
			
			for (State e:element) {
				boolean accepted = false;
				for (State a : this.acceptingStates ){
					if (a.compareTo(e) == 0) {
						accepted = true;
						break;
					}
				}
				if (accepted) {
					acceptingStates.add(normalizeElement(element));
				}
			}

			
			Iterator<Character> alpha = alphabet.iterator();
			
			
			while (alpha.hasNext()) {
				char c = alpha.next().charValue();
				if (c == ' ') {
					continue;
				}
				if (c == Alphabet.EPSILON) {
					continue;
				}
				
				Set<State> row = getMatrixByInput(element,c);
				if (row.size() > 0) {
					if(!allStates.contains(normalizeElement(row))) {
						allStates.add(normalizeElement(row));
					}
					if (!activeStates.contains(row)) {
						activeStates.add(row);
					}
					Transition t = new Transition(normalizeElement(element), c, normalizeElement(row));
					allTransitions.add(t);
				}else {
					Transition t = new Transition(normalizeElement(element), c, new IdentifiedDFSMState("{-1}"));
					allTransitions.add(t);
				}
			}
			
			
			activeStates.remove(0);
		}
		
		Iterator<Character> alpha = alphabet.iterator();
		
		
		while (alpha.hasNext()) {
			char c = alpha.next().charValue();
			if (c == ' ') {
				continue;
			}
			if (c == Alphabet.EPSILON) {
				continue;
			}
			Transition t = new Transition(new IdentifiedDFSMState("{-1}"), c, new IdentifiedDFSMState("{-1}"));
			allTransitions.add(t);

		}
		
		allStates.add(new IdentifiedDFSMState("{-1}"));
		
		return new DFSM(allStates,getAlphabetNoEpsilon(),allTransitions,normalizeElement(initialStates) ,acceptingStates);
	}
	
	private Alphabet getAlphabetNoEpsilon() {
		Iterator<Character> alphaIterator = alphabet.iterator();
		List<Character> newAlpha = new ArrayList<>();
		while (alphaIterator.hasNext()) {
			char c = alphaIterator.next().charValue();
			if (c == ' ') {
				continue;
			}
			if (c == Alphabet.EPSILON) {
				continue;
			}
			newAlpha.add(c);
		}
		
		Alphabet alp = new Alphabet(newAlpha); 
		return alp;
	}
	
	private Set<State> getMatrixByInput(Set<State> states,char c){
		Set<State> row = getTranformByInput(states, c);
		return row;
	}
	
	private Set<State> getTranformByInput(Set<State> states,char c){
		Set<State> element = new HashSet<>();
		for (State source: states) {
			for (State dest: transitions.at(source, c)) {
				 for (State child: epsilon(dest)) {
					 element.add(child);
				 }
			}
		}
		return element;
	}
	
	private Set<State> epsilon(State state){
		Set<State> states = new HashSet<>();
		states.add(state);
		Set<State> collector = new HashSet<>();
		Iterate(states,collector);
		return collector;
		
	}
	
	private void Iterate(Set<State> states,Set<State> collector){
	    if (states.isEmpty()) {
	    	return;
	    }
	    
		for(State src: states) {
			collector.add(src);
			Set<State> children = transitions.at(src,Alphabet.EPSILON);
			Iterate(children,collector);
		}
	}
	
	}

	


	/*
	 * eps{
	 * n =[ids] =transitions.at(s,epsiolion)
	 * while n.legnth > 0:
	 * 
	 *  for s in n:
	 * 		n =[ids] =transitions.at(s,epsiolion)
	 * 
	 * 
	 * {t,t,t}
	 * 
	 * 
	 * 
	 * return set states
	 * }
	 * 
	 * 
	 * initial = eps(initial state)
	 * 
	 * 
	 * 
	 * */

	
	
	


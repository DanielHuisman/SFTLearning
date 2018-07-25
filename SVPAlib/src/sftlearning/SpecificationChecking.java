/**
 * This file has been made by Sophie Lathouwers
 */

package sftlearning;

import automata.sfa.SFA;
import org.sat4j.specs.TimeoutException;
import specifications.CyberchefSpecifications;
import theory.BooleanAlgebraSubst;
import theory.characters.CharConstant;
import theory.characters.CharFunc;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * This class contains methods for checking specifications of an SFT
 * The possibilities to check are:
 * - Equality
 * - Blacklist (input or output)
 * - Whitelist equality (input or output)
 * - Whitelist subset (input or output)
 * - Length equality (input or output)
 * - Length inequality (input or output)
 * - Idempotency
 * - Commutativity
 * - Get (bad) input which results in given (bad) output
 */
public class SpecificationChecking {

    /**
     * Checks whether two specified SFTs (symbolic finite transducers) are equal
     * @param sft1 first SFT
     * @param sft2 second SFT
     * @return true if sft1 is the same as sft2
     * @throws TimeoutException
     */
    public static boolean areEqual(SFT<CharPred, CharFunc, Character> sft1, SFT<CharPred, CharFunc, Character> sft2) throws TimeoutException {
        return SFT.equals(sft1, sft2);
    }

    /**
     * Checks whether there is any 'bad' input that the symbolic finite transducer accepts
     * @param sft automaton whose input language is compared to the blacklist
     * @param blacklist list containing all bad inputs that should not be accepted by the symbolic finite transducer
     * @return true if there is NO bad input that is accepted by the symbolic finite transducer
     * @throws TimeoutException
     */
    public static boolean checkBlacklistInput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> blacklist) throws TimeoutException {
        SFA<CharPred, Character> inputLanguage = sft.getDomain(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = inputLanguage.intersectionWith(blacklist, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether there is any 'bad' output that the symbolic finite transducer may output
     * @param sft automaton whose output language is compared to the blacklist
     * @param blacklist list containing all bad outputs that should not be outputted by the symbolic finite transducer
     * @return true if there is NO bad output that can be given by the symbolic finite transducer
     * @throws TimeoutException
     */
    public static boolean checkBlacklistOutput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> blacklist) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = outputLanguage.intersectionWith(blacklist, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether ALL specified inputs are accepted by the symbolic finite transducer
     * @param sft automaton whose input language is compared to the whitelist
     * @param whitelist list containing all good inputs that should be accepted by the symbolic finite transducer
     * @return true if the symbolic finite transducer accepts ALL & ONLY the inputs specified in the whitelist
     */
    public static boolean checkEqualWhitelistInput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> whitelist) throws TimeoutException {
        SFA<CharPred, Character> inputLanguage = sft.getDomain(new UnaryCharIntervalSolver());
        return inputLanguage.isEquivalentTo(whitelist, new UnaryCharIntervalSolver());
    }

    /**
     * Checks whether ALL specified outputs are accepted by the symbolic finite transducer
     * @param sft automaton whose output language is compared to the whitelist
     * @param whitelist list containing all good outputs that should be outputted by the symbolic finite transducer
     * @return true if the symbolic finite transducer outputs ALL & ONLY outputs specified in the whitelist
     */
    public static boolean checkEqualWhitelistOutput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> whitelist) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        return outputLanguage.isEquivalentTo(whitelist, new UnaryCharIntervalSolver());
    }

    /**
     * Checks whether only specified inputs are accepted by the symbolic finite transducer
     * i.e. whether there exists a non-specified input that is accepted by the symbolic finite transducer
     * @param sft automaton whose input language is compared to the whitelist
     * @param whitelist list containing all good inputs that may be outputted by the symbolic finite transducer
     * @return true if the symbolic finite transducer accepts ONLY inputs specified in the whitelist
     */
    public static boolean checkSubsetWhitelistInput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> whitelist) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> complementWhitelist = whitelist.complement(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = complementWhitelist.intersectionWith(outputLanguage, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether only specified outputs are accepted by the symbolic finite transducer
     * i.e. whether there exists a non-specified output that is accepted by the symbolic finite transducer
     * @param sft automaton whose output language is compared to the whitelist
     * @param whitelist list containing all good outputs that may be outputted by the symbolic finite transducer
     * @return true if the symbolic finite transducer outputs ONLY outputs specified in the whitelist
     */
    public static boolean checkSubsetWhitelistOutput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> whitelist) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> complementWhitelist = whitelist.complement(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = complementWhitelist.intersectionWith(outputLanguage, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Returns whether all inputs have an acceptable length as specified in the lengthAutomaton
     * @param sft symbolic finite transducer whose inputs' lengths are compared to the specification
     * @param lengthAutomaton automaton which accepts all words with an acceptable length
     * @return true if all accepted inputs have an acceptable length
     * @throws TimeoutException
     */
    public static boolean checkHasLengthInput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> lengthAutomaton) throws TimeoutException {
        SFA<CharPred, Character> inputLanguage = sft.getDomain(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> complementLength = lengthAutomaton.complement(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = inputLanguage.intersectionWith(complementLength, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Returns whether all outputs have an acceptable length as specified in the lengthAutomaton
     * @param sft symbolic finite transducer whose outputs' lengths are compared to the specification
     * @param lengthAutomaton automaton which accepts all words with an acceptable length
     * @return true if all possible outputs have an acceptable length
     * @throws TimeoutException
     */
    public static boolean checkHasLengthOutput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> lengthAutomaton) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> complementLength = lengthAutomaton.complement(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = outputLanguage.intersectionWith(complementLength, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether all inputs do not have an unacceptable length as specified in the lengthAutomaton
     * @param sft symbolic finite transducer whose inputs' lengths are compared to the specification
     * @param lengthAutomaton automaton which accepts all words with an unacceptable length
     * @return true if there exists no input with an unacceptable length
     * @throws TimeoutException
     */
    public static boolean checkHasNotLengthInput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> lengthAutomaton) throws TimeoutException {
        SFA<CharPred, Character> inputLanguage = sft.getDomain(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = inputLanguage.intersectionWith(lengthAutomaton, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether all outputs do not have an unacceptable length as specified in the lengthAutomaton
     * @param sft symbolic finite transducer whose outputs' lengths are compared to the specification
     * @param lengthAutomaton automaton which accepts all words with an unacceptable length
     * @return true if there exists no output with an unacceptable length
     */
    public static boolean checkHasNotLengthOutput(SFT<CharPred, CharFunc, Character> sft, SFA<CharPred, Character> lengthAutomaton) throws TimeoutException {
        SFA<CharPred, Character> outputLanguage = sft.getOutputSFA(new UnaryCharIntervalSolver());
        SFA<CharPred, Character> intersection = outputLanguage.intersectionWith(lengthAutomaton, new UnaryCharIntervalSolver());
        return intersection.isEmpty();
    }

    /**
     * Checks whether a symbolic finite transducer is idempotent
     * @param sft symbolic finite transducer
     * @return true if the symbolic finite transducer is idempotent
     */
    public static boolean checkIdempotency(SFT<CharPred, CharFunc, Character> sft) throws TimeoutException {
        SFT<CharPred, CharFunc, Character> composedSFT = sft.composeWith(sft, new UnaryCharIntervalSolver());
        return SFT.equals(composedSFT, sft);
    }

    /**
     * Checks whether two symbolic finite transducers commute (whether the order of execution is irrelevant for the result)
     * @param sft1 symbolic finite transducer
     * @param sft2 symbolic finite transducer
     * @return true if the symbolic finite transducers commute
     */
    public static boolean checkCommutativity(SFT<CharPred, CharFunc, Character> sft1, SFT<CharPred, CharFunc, Character> sft2) throws TimeoutException {
        SFT<CharPred, CharFunc, Character> composed1 = sft1.composeWith(sft2, new UnaryCharIntervalSolver());
        SFT<CharPred, CharFunc, Character> composed2 = sft2.composeWith(sft1, new UnaryCharIntervalSolver());
        return SFT.equals(composed1, composed2);
    }

    public static List<Character> getBadInput(SFT<CharPred, CharFunc, Character> sft, String output) throws TimeoutException {
        // TODO: Make sure this times out after a certain time?
        List<Character> result = null;
        BadInputTask badInputTask = new BadInputTask(sft, output);

        // Program may run for a maximum of 5 minutes
        long maxRuntime = 5;
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            result = executor.submit(badInputTask).get(maxRuntime, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (java.util.concurrent.TimeoutException e) {
            System.out.println("Could not find an input corresponding to the output within the available time");
        }
        executor.shutdown();
        try {
            if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static class BadInputTask implements Callable<List<Character>> {
        List<Character> result = null;
        SFT<CharPred, CharFunc, Character> sft;
        String output;

        public BadInputTask(SFT<CharPred, CharFunc, Character> sft, String output) {
            this.sft = sft;
            this.output = output;
        }

        @Override
        public List<Character> call() throws Exception {
            try {
                return dfs(sft, output, sft.getInitialState());
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static List<Character> dfs(SFT<CharPred, CharFunc, Character> sft, String output, int state) throws TimeoutException {
        if (output.isEmpty()) {
            return new ArrayList<>();
        }

        BooleanAlgebraSubst ba = new UnaryCharIntervalSolver();
        List<Character> input = new ArrayList<>();

        boolean noIDFunctions = true;
        for (SFTInputMove<CharPred, CharFunc, Character> trans : sft.getInputMovesFrom(state)) {
            boolean foundTrans = false;
            // First get output given when taking this transition
            List<Character> outputTrans = new ArrayList<>();
            List<CharFunc> typeOutputTrans = new ArrayList<>();
            for (int i=0; i<trans.outputFunctions.size(); i++) {
                CharFunc f = trans.outputFunctions.get(i);
                if (f instanceof CharConstant) {
                    outputTrans.add(((CharConstant) f).c);
                    typeOutputTrans.add(f);
                    foundTrans = true;
                    break;
                } else {
                    // It was an identity function, assume value on location i in specified output
                    if (trans.hasModel(output.charAt(i), ba)) {
                        noIDFunctions = false;
                        outputTrans.add(output.charAt(i));
                        typeOutputTrans.add(CharOffset.IDENTITY);
                        foundTrans = true;
                        break;
                    }
                    // Not the correct output¢ functions so move on to next transition
                    // TODO: FIX deze continue, hij gaat naar de volgende in outputFunctions maar je moet naar de volgende transitie
                    break;
                }
            }
            if (!foundTrans) {
                continue;
            }


            String limitedOutput = output.substring(0, outputTrans.size());
            List<Character> subsetOutput = new ArrayList<>();
            for (int i=0; i<limitedOutput.length(); i++) {
                subsetOutput.add(limitedOutput.charAt(i));
            }

            // If the functions match with the expected output
            // Then consider this transition
            if (subsetOutput.equals(outputTrans)) {
                // Find character which is needed to take the transition
                // if there are no identity functions, then this will simply be some character that satisfies the transition
                char c = CharPred.MIN_CHAR;
                if (noIDFunctions) {
                    boolean first = true;
                    while (first || !trans.hasModel(c, ba)) {
                        int random = ThreadLocalRandom.current().nextInt(1, 400);
                        for (int i=0; i<random; i++) {
                            c++;
                        }
                        first = false;
                    }
                } else {
                    // If there are identity functions in the set of output functions,
                    // Then we need to find the location of the output function, and find the corresponding character in the specified output
                    for (int i=0; i<typeOutputTrans.size(); i++) {
                        if (typeOutputTrans.get(i) == CharOffset.IDENTITY) {
                            c = subsetOutput.get(i);
                            break;
                        }
                    }
                    if (!trans.hasModel(c, ba)) {
                        continue;
                    }
                }

                input.add(c);
                String newOutput = output.substring(trans.outputFunctions.size(), output.length());
                input.addAll(dfs(sft, newOutput, trans.to));
                return input;
            } else {
                // Output functions do not match the expected output so move on to next transition
                continue;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        try {
            SFT sft = CyberchefSpecifications.getLowercaseSpec();
            System.out.println(sft);
            System.out.println(getBadInput(sft, "dmphjkolf"));

        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}

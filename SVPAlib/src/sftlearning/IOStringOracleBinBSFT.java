/**
 * NOTICE: This file has been made by Sophie Lathouwers!
 * @author Sophie Lathouwers
 */

package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IOStringOracleBinBSFT extends SymbolicOracle<CharPred, CharFunc, Character>{

    private Scanner sc;

    public IOStringOracleBinBSFT() {
        sc = new Scanner(System.in);
    }

    @Override
    public List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) {
        System.out.println(compareTo);
        char in = '\u0000';
        // Ask whether the hypothesis automaton is correct
        // This question will be repeated until valid input (either 'n' or 'y') has been given
        while (in != 'n' && in != 'y') {
            System.out.println("Is that your automaton? (y/n):");
            String inLine = sc.nextLine();
            if (inLine != null && !inLine.isEmpty() && inLine.length()==1) {
                in = inLine.charAt(0);
            }
        }
        if (in == 'y') {
            return null;
        }
        System.out.println("Enter counterexample string a1,a2,a3... :");
        String cex = sc.nextLine();
        String[] parts = cex.split(",");
        List<Character> chars = new ArrayList<>();
        for (String s : parts) {
            for (Character c : s.toCharArray()) {
                chars.add(c);
            }
        }
        return chars;
    }

    @Override
    public List<Character> checkMembershipImpl(List<Character> w) {
        System.out.println("What output does your automaton produce on " + w + " ?:");
        String in = sc.nextLine();
        List<Character> answer = new ArrayList<>();
        for (int i=0; i<in.length(); i++) {
            answer.add(in.charAt(i));
        }
        return answer;
    }

    public static void main(String[] args) {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        BinBSFTLearner<CharPred, CharFunc, Character> ell = new BinBSFTLearner<>();
        SymbolicOracle o = new IOStringOracleBinBSFT();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba, 120);

//            transducers.sft.SFT.backtrack(outputs, new ArrayList<Character>, learned, 3, new ArrayList<Character>, 0, ba);
//            learned.createDotFile("testStringOracle", "/Users/NW/Documents/Djungarian/SVPAlib/src/transducers/sft");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

}

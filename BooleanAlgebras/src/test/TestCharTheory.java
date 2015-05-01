package test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import theory.CharPred;
import theory.CharSolver;

public class TestCharTheory {

	final CharSolver solver = new CharSolver();
	
	@Test
    public void solverTestBasic() {
        

        final CharPred isDigit = new CharPred('0','9');
        assertTrue(solver.HasModel(isDigit, '5'));
        assertTrue(solver.HasModel(isDigit, '0'));
        assertTrue(solver.HasModel(isDigit, '9'));
        assertFalse(solver.HasModel(isDigit, 'a'));

        final CharPred notIsDigit = solver.MkNot(isDigit);
        assertFalse(solver.HasModel(notIsDigit, '5'));
        assertFalse(solver.HasModel(notIsDigit, '0'));
        assertFalse(solver.HasModel(notIsDigit, '9'));
        assertTrue(solver.HasModel(notIsDigit, 'a'));

        final CharPred empty = solver.MkAnd(isDigit, notIsDigit);
        final CharPred full = solver.MkOr(isDigit, notIsDigit);

        assertFalse(solver.IsSatisfiable(empty));
        assertTrue(solver.AreEquivalent(full, solver.True()));

        assertTrue(solver.HasModel(CharPred.alpha(), 'a'));
        assertFalse(solver.HasModel(CharPred.alpha(), '3'));
        assertTrue(solver.HasModel(CharPred.alphaNum(), '4'));
        assertTrue(solver.HasModel(CharPred.num(), '4'));
        assertFalse(solver.HasModel(CharPred.num(), 'a'));
    }
	
	@Test
    public void test1() {
        final CharPred p = new CharPred();
        assertFalse(solver.HasModel(p,'a'));
        assertFalse(solver.HasModel(p,'b'));
        assertFalse(solver.HasModel(p,'A'));
        assertFalse(solver.HasModel(p,'B'));
        assertFalse(solver.HasModel(p,'0'));
        assertFalse(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }

    @Test
    public void test2() {
        final CharPred p = new CharPred('a');
        assertTrue(solver.HasModel(p,'a'));
        assertFalse(solver.HasModel(p,'b'));
        assertFalse(solver.HasModel(p,'A'));
        assertFalse(solver.HasModel(p,'B'));
        assertFalse(solver.HasModel(p,'0'));
        assertFalse(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }

    @Test
    public void test3() {
        final CharPred p = new CharPred('a', 'z');
        assertTrue(solver.HasModel(p,'a'));
        assertTrue(solver.HasModel(p,'b'));
        assertFalse(solver.HasModel(p,'A'));
        assertFalse(solver.HasModel(p,'B'));
        assertFalse(solver.HasModel(p,'0'));
        assertFalse(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }


    @Test
    public void test7() {
        final CharPred p = CharPred.alpha();
        assertTrue(solver.HasModel(p,'a'));
        assertTrue(solver.HasModel(p,'b'));
        assertTrue(solver.HasModel(p,'A'));
        assertTrue(solver.HasModel(p,'B'));
        assertFalse(solver.HasModel(p,'0'));
        assertFalse(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }

    @Test
    public void test8() {
        final CharPred p = CharPred.num();
        assertFalse(solver.HasModel(p,'a'));
        assertFalse(solver.HasModel(p,'b'));
        assertFalse(solver.HasModel(p,'A'));
        assertFalse(solver.HasModel(p,'B'));
        assertTrue(solver.HasModel(p,'0'));
        assertTrue(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }

    @Test
    public void test9() {
        final CharPred p = CharPred.alphaNum();
        assertTrue(solver.HasModel(p,'a'));
        assertTrue(solver.HasModel(p,'b'));
        assertTrue(solver.HasModel(p,'A'));
        assertTrue(solver.HasModel(p,'B'));
        assertTrue(solver.HasModel(p,'0'));
        assertTrue(solver.HasModel(p,'1'));
        assertFalse(solver.HasModel(p,' '));
        assertFalse(solver.HasModel(p,'\t'));
        assertFalse(solver.HasModel(p,'\n'));
    }
	

}

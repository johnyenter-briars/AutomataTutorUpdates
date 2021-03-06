﻿using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Generic;
using AutomataPDL.CFG;
using System;

namespace Games.Tests
{
    [TestClass()]
    public class CFGTests
    {
        Func<char, char> f1 = delegate (char x)
        {
            return x;
        };

        [TestMethod()]
        public void CYK_Test1()
        {
            String sg1 = "S->aX X->bS|b"; // =  (ab)*       Aufg 1.1a
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ab"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "abab"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ababab"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ababababababababababababababababababababab"));

            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "a"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "b"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "c"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "aab"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "aabaabbabb"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ababababababababababababababababababa"));
        }

        [TestMethod()]
        public void CYK_Test2()
        {
            String sg1 = "S->aSb|S X| X->cX"; // = a^n b^n            Aufg 1.1b
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, ""));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ab"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "aabb"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "aaabbb"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "aaaabbbb"));

            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "a"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "b"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "c"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "aab"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "aabaabbabb"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ababababababababababababababababababab"));
        }

        [TestMethod()]
        public void CYK_Test3()
        {
            String sg1 = "S -> A B C   A->   B->   C->"; // = {""}       Testing epsilon
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, ""));

            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ca"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "d"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "abcc"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "cba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ababababababababababababababababababab"));
        }

        [TestMethod()]
        public void CYK_Test4()
        {
            String sg1 = "S->A B C    A->|a    B->b|    C->|c"; // = {"", a, b, c, ab, ac, bc, abc} 
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, ""));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "a"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "b"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "c"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ab"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "ac"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "bc"));
            Assert.IsTrue(GrammarUtilities.isWordInGrammar(g, "abc"));

            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ca"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "d"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "abcc"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "cba"));
            Assert.IsFalse(GrammarUtilities.isWordInGrammar(g, "ababababababababababababababababababab"));
        }

        [TestMethod()]
        public void prefixTest1() //   balanced parenthesis
        {
            String sg1 = "S -> S S|(S)|";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.AreEqual(-2, GrammarUtilities.longestPrefixLength(g, "()(())()()"));  // full
            Assert.AreEqual("()(())".Length, GrammarUtilities.longestPrefixLength(g, "()(()))()()"));
            Assert.AreEqual("()(())((((".Length, GrammarUtilities.longestPrefixLength(g, "()(())(((("));
            Assert.AreEqual("".Length, GrammarUtilities.longestPrefixLength(g, ")(()()())"));
        }

        [TestMethod()]
        public void prefixTest2() //   a^n b^n
        {
            String sg1 = "S->aSb|S X| X->cX";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.AreEqual(-2, GrammarUtilities.longestPrefixLength(g, "aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb"));  // full
            Assert.AreEqual("a".Length, GrammarUtilities.longestPrefixLength(g, "a"));
            Assert.AreEqual("".Length, GrammarUtilities.longestPrefixLength(g, "b"));
            Assert.AreEqual("aab".Length, GrammarUtilities.longestPrefixLength(g, "aab"));
            Assert.AreEqual("aabb".Length, GrammarUtilities.longestPrefixLength(g, "aabbb"));
            Assert.AreEqual("aaaaaaaaaaaabbbbbbbbbbbb".Length, GrammarUtilities.longestPrefixLength(g, "aaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbababc"));
        }

        [TestMethod()]
        public void prefixTest3() //   balanced parenthesis () and {}   Exercise 8.5
        {
            String sg1 = "S -> S S|(S)|{S}|";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.AreEqual(-2, GrammarUtilities.longestPrefixLength(g, "(){()}"));  // full
            Assert.AreEqual("(){(".Length, GrammarUtilities.longestPrefixLength(g, "(){("));
            Assert.AreEqual("()".Length, GrammarUtilities.longestPrefixLength(g, "()}"));

            String sg2 = "S->A B| C D | A T | C U | S S  T-> A B   U->S D  A->(  B->) C->{ D->}";
            g = GrammarParser<char>.Parse(f1, sg2);
            g = GrammarUtilities.getEquivalentCNF(g);

            Assert.AreEqual(-2, GrammarUtilities.longestPrefixLength(g, "(){()}"));  // full
            Assert.AreEqual("(){(".Length, GrammarUtilities.longestPrefixLength(g, "(){("));
            Assert.AreEqual("()".Length, GrammarUtilities.longestPrefixLength(g, "()}"));
        }

        [TestMethod()]
        public void EqualityTest1()
        {
            String sg1 = "S->aSb|";
            String sg2 = "S->aSb|aaSbb|";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 100);

            Assert.IsTrue(res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest2()
        {
            String sg1 = "S->aSb|absjjfhghs|";
            String sg2 = "S->aSb|aaSbb|";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 100);

            Assert.IsTrue(res.Item2[0].Equals("absjjfhghs") && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest3() //   a^n b^n
        {
            String sg1 = "S->aT T->aT U|b U->b";
            String sg2 = "P->aR R->abb|aRb|b";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 100);

            Assert.IsTrue(res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest4() //   (a|b|c|d|e|f|g|h|i|j)*
        {
            String sg1 = "S->|a|b|c|d|e|f|g|h|i|j|S S";
            String sg2 = "S->X|X S|   X->a|b|c|d|e|f|g|h|i|j";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 50);

            Assert.IsTrue(res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest5() //   (a|b)^n
        {
            String sg1 = "S -> a|b|aa|aS|bS|bbbbS";
            String sg2 = "S->X|X S   X->a|b";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 100);

            Assert.IsTrue(res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest6() //   balanced parenthesis
        {
            String sg1 = "S -> S S|(S)|";
            String sg2 = "X -> | X X | (L    L->X) | X X)";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, g2, true, 100);

            Assert.IsTrue(res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void EqualityTest7() //   empty grammars and invariants
        {
            String sg1 = "S -> S S|(S)|";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);

            var res = GrammarUtilities.findDifferenceWithTimelimit(g1, null, true, 25);
            Assert.IsTrue(res.Item1 == 0 && res.Item2.Count > 0 && res.Item3.Count == 0);
            res = GrammarUtilities.findDifferenceWithTimelimit(null, g1, true, 25);
            Assert.IsTrue(res.Item1 == 0 && res.Item2.Count == 0 && res.Item3.Count > 0);
            res = GrammarUtilities.findDifferenceWithTimelimit(g1, g1, true, 25);
            Assert.IsTrue(res.Item1 > 0 && res.Item2.Count == 0 && res.Item3.Count == 0);
            res = GrammarUtilities.findDifferenceWithTimelimit(null, null, true, 30);
            Assert.IsTrue(res.Item1 == 0 && res.Item2.Count == 0 && res.Item3.Count == 0);
        }

        [TestMethod()]
        public void GrammarGradingTest1() //   balanced parenthesis (wordsInGrammar)        !!!could change if grading scale is changed!!!
        {
            String sg1 = "S -> S S|(S)|";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg1);
            var res = GrammarGrading.gradeWordsInGrammar(g, new[] { "()", "())()()(", "()((" }, new[] { "()(", "())()(", "xyz" }, 10);
            Assert.IsTrue(res.Item1 == 5);
        }

        [TestMethod()]
        public void GrammarGradingTest2() //  a^n b^n (grammar equality)        !!!could change if grading scale is changed!!!
        {
            String sg1 = "S->absjjfhghs|X X->aXb|";
            String sg2 = "S->aSb|aaSbb|";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg1);
            ContextFreeGrammar g2 = GrammarParser<char>.Parse(f1, sg2);
            var res = GrammarGrading.gradeGrammarEquality(g1, g2, 10, 50);
            var res2 = GrammarGrading.gradeGrammarEquality(g2, g1, 10, 50);

            Assert.IsTrue(res.Item1 == 9);
            Assert.IsTrue(res2.Item1 == 9); //mirrored
        }

        [TestMethod()]
        public void GrammarDerivationTest1() // basics
        {
            String sg = "S->aSb|aaSbb|";
            ContextFreeGrammar g1 = GrammarParser<char>.Parse(f1, sg);
            Assert.IsTrue(Derivation.findAllDerivations(g1.GetProductions(), new GrammarSymbol[] {g1.StartSymbol}).Count == 3);

            Assert.IsTrue(Derivation.isValidDerivationStep(g1.GetProductions(), 
                new GrammarSymbol[] { g1.StartSymbol }, 
                new GrammarSymbol[] { new Exprinal<char>('a', "a"), g1.StartSymbol, new Exprinal<char>('b', "b") }));
            Assert.IsTrue(Derivation.isValidDerivationStep(g1.GetProductions(),
                new GrammarSymbol[] { g1.StartSymbol },
                new GrammarSymbol[] { new Exprinal<char>('a', "a"), new Exprinal<char>('a', "a"), g1.StartSymbol, new Exprinal<char>('b', "b"), new Exprinal<char>('b', "b")}));
            Assert.IsTrue(Derivation.isValidDerivationStep(g1.GetProductions(),
                new GrammarSymbol[] { g1.StartSymbol },
                new GrammarSymbol[] {}));

            Assert.IsFalse(Derivation.isValidDerivationStep(g1.GetProductions(),
                new GrammarSymbol[] { g1.StartSymbol },
                new GrammarSymbol[] { g1.StartSymbol }));
        }

        [TestMethod()]
        public void GrammarDerivationParseTest()
        {
            var der1 = DerivationParser<char>.Parse(f1, "S \n S S S \n S a b S \n x a b S \n x a b S S S \n x a b x S S \n x a b x x S \n x a b x x x");

            Assert.IsTrue(der1.Count == 8);
            var der1L = new int[] { 1, 3, 4, 4, 6, 6, 6, 6 };
            for(int i = 0; i < der1L.Length; i++) Assert.IsTrue(der1[i].Length == der1L[i]);
        }

        [TestMethod()]
        public void GrammarDerivationTest() //tests derivation computation in combination with parsing (grammar and derivation)
        {
            String sg = "S->S S S | x | A | ab |   A -> A A | S | a | ";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg);
            var der1 = DerivationParser<char>.Parse(f1, "S \n A \n A A \n A \n A A \n A S \n A \n S \n S S S \n S a b S \n x a b S \n x a b S S S \n x a b x S S \n x a b x x S \n x a b x x x");
            
            Assert.IsTrue(Derivation.comparator.Equals(der1[0], new GrammarSymbol[] { g.StartSymbol })); //check start
            for(int i = 1; i < der1.Count; i++)
            {
                Assert.IsTrue(Derivation.isValidDerivationStep(g.GetProductions(), der1[i - 1], der1[i])); //check step
            }
        }

        [TestMethod()]
        public void GrammarFindDerivationTest() //tests problem type "find derivation"
        {
            String sg = "S->S S S | x | A | ab |   A -> A A | S | a | ";
            ContextFreeGrammar g = GrammarParser<char>.Parse(f1, sg);
            var wrong_start = DerivationParser<char>.Parse(f1, "S S S \n S a b S \n x a b S \n x a b S S S \n x a b x S S \n x a b x x S \n x a b x x x");
            var wrong_end = DerivationParser<char>.Parse(f1, "S \n S S S \n S a b S \n x a b S");
            var wrong_step = DerivationParser<char>.Parse(f1, "S \n S S S \n S a b S \n x a b S \n x a b S S S \n x a b x x x");
            var correct = DerivationParser<char>.Parse(f1, "S \n S S S \n S a b S \n x a b S \n x a b S S S \n x a b x S S \n x a b x x S \n x a b x x x");
            var correct_long = DerivationParser<char>.Parse(f1, "S \n A \n A A \n A \n A A \n A S \n A \n S \n S S S \n S a b S \n x a b S \n x a b S S S \n x a b x S S \n x a b x x S \n x a b x x x");

            Assert.IsFalse(GrammarGrading.gradeFindDerivation(g, "xabxxx", wrong_start, 10).Item1 == 10);
            Assert.IsFalse(GrammarGrading.gradeFindDerivation(g, "xabxxx", wrong_end, 10).Item1 == 10);
            Assert.IsFalse(GrammarGrading.gradeFindDerivation(g, "xabxxx", wrong_step, 10).Item1 == 10);
            Assert.IsTrue(GrammarGrading.gradeFindDerivation(g, "xabxxx", correct, 10).Item1 == 10);
            Assert.IsTrue(GrammarGrading.gradeFindDerivation(g, "xabxxx", correct_long, 10).Item1 == 10);
        }
    }
}
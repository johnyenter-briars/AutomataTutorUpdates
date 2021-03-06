﻿using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace AutomataPDL.CFG
{
    internal enum TokenType { NT, T, ARR, OR, ERR, IG, EOS, NEL }

    internal class Token
    {
        public TokenType t;
        public string content;
        public int length;
        public int pos;
        override public string ToString()
        {
            string type = "";

            switch (t)
            {
                case TokenType.NT:
                    type = "NonExprinal(";
                    break;
                case TokenType.T:
                    type = "Exprinal(";
                    break;
                case TokenType.ARR:
                    type = "ARROW(";
                    break;
                case TokenType.OR:
                    type = "OR(";
                    break;
                case TokenType.ERR:
                    type = "ERR(";
                    break;
                case TokenType.IG:
                    type = "IG(";
                    break;
                case TokenType.EOS:
                    type = "END(";
                    break;
                case TokenType.NEL:
                    type = "NEL(";
                    break;
            }

            return type + content + ")";
        }
    }

    internal class Lexer
    {
        private string lexbuf;
        public string input;
        private Dictionary<TokenType, Regex> tokendescs = new Dictionary<TokenType, Regex>();

        public Lexer(string buf)
        {
            lexbuf = buf.Trim();
            input = buf;
            tokendescs[TokenType.NT] = new Regex(@"^([A-Z])"); // NonExprinal
            tokendescs[TokenType.T] = new Regex(@"^([a-z<>\[\]()\{\}])");   // Exprinal
            tokendescs[TokenType.ARR] = new Regex(@"^(->|=>)");             // Arrow
            tokendescs[TokenType.OR] = new Regex(@"^\|");                   // Or
            tokendescs[TokenType.IG] = new Regex(@"^[\s_-[\n]]+");               // Ignorables
            tokendescs[TokenType.NEL] = new Regex(@"^\n");
        }

        private Token DoMatch()
        {
            Token next = new Token();
            next.t = TokenType.ERR;
            next.length = 1;
            next.content = "";
            next.pos = input.Length - lexbuf.Length;

            foreach (KeyValuePair<TokenType, Regex> pair in tokendescs)
            {
                Match m = pair.Value.Match(lexbuf);
                if (m.Success)
                {
                    next.length = m.Groups[0].Length;
                    next.t = pair.Key;

                    if (m.Groups.Count > 0)
                    {
                        next.content = m.Groups[1].Value;
                    }

                    break;
                }
                else next.content = lexbuf;
            } // end loop over token types
            return next;
        }

        public Token Next()
        {
            foreach (Token t in GetTokens())
            {
                return t;
            }

            return null;
        }

        public IEnumerable<Token> GetTokens()
        {
            while (lexbuf.Length > 0)
            {
                Token next = DoMatch();
                lexbuf = lexbuf.Substring(next.length);

                if (next.t == TokenType.ERR)
                {
                    yield return next;
                    yield break;
                }

                if (next.t != TokenType.IG)
                {
                    yield return next;
                }
            }
            Token end = new Token();
            end.t = TokenType.EOS;
            end.content = "";
            end.length = 0;

            yield return (end);
            yield break;
        }
    }

    public class ParseException : System.ApplicationException {
        public ParseException() : base() { }
        public ParseException(string message) : base(message) { }
    }

    public class GrammarParser<T>
    {
        private Lexer lexer;
        private Func<char, T> mkExprinal;
        private Nonterminal startvar;
        private List<Production> productions;

        private GrammarParser(Lexer lex, Func<char, T> mkExprinal)
        {
            lexer = lex;
            this.mkExprinal = mkExprinal;
            startvar = null;
            productions = new List<Production>();
        }

        public static ContextFreeGrammar Parse(Func<char, T> mkExprinal, string buf)
        {
            Lexer lex = new Lexer(buf);
            var gp = new GrammarParser<T>(lex, mkExprinal);
            gp.Parse();
            ContextFreeGrammar G = gp.GetGrammar();
            return G;
        }


        private Token ExpectNT()
        {
            Token next = lexer.Next();
            if (next.t != TokenType.NT)
            {
                throw new ParseException(string.Format("Expected Nonterminal... ({0}). Reminder: A Nonterminal is a SINGLE upper case letter", generateLocationString(next)));
            }

            return next;
        }

        private void ExpectArrow()
        {
            Token next = lexer.Next();
            if (next.t != TokenType.ARR)
            {
                throw new ParseException(string.Format("Expected arrow... ({0}). Left hand side must contain a single Nonterminal, represented by a single upper case letter.", generateLocationString(next)));
            }
        }

        private void Parse()
        {
            bool done = false;
            Token cur = null;
            Token last = null;

            Nonterminal curlhs = new Nonterminal(ExpectNT().content);
            startvar = curlhs;

            ExpectArrow();
            List<GrammarSymbol> currhs = new List<GrammarSymbol>();

            last = cur;
            cur = lexer.Next();
            while (!done)
            {
                switch (cur.t)
                {
                    case TokenType.NT:
                        currhs.Add(new Nonterminal(cur.content));
                        last = cur;
                        cur = lexer.Next();
                        break;
                    case TokenType.T:
                        currhs.Add(new Exprinal<T>(mkExprinal(cur.content[0]), cur.content));
                        last = cur;
                        cur = lexer.Next();
                        break;
                    case TokenType.OR:
                        productions.Add(new Production(curlhs, currhs.ToArray()));
                        currhs.Clear();
                        last = cur;
                        cur = lexer.Next();
                        break;
                    //case TokenType.ARR:
                    //    if (currhs.Count < 1)
                    //    {
                    //        throw new ParseException(string.Format("A production cannot start with an arrow... ({0})", generateLocationString(cur)));
                    //    }
                    //    if (last.t != TokenType.NT)
                    //    {
                    //        throw new ParseException(string.Format("On the left hand side of every arrow has to be a Nonterminal... ({0})", generateLocationString(cur)));
                    //    }

                    //    Nonterminal newlhs = (Nonterminal)currhs[currhs.Count - 1];
                    //    currhs.RemoveAt(currhs.Count - 1);
                    //    productions.Add(new Production(curlhs, currhs.ToArray()));
                    //    currhs.Clear();
                    //    curlhs = newlhs;
                    //    break;
                    case TokenType.NEL:
                        productions.Add(new Production(curlhs, currhs.ToArray()));
                        currhs.Clear();
                        last = cur;
                        cur = ExpectNT();
                        if (cur.t == TokenType.NT)
                        {
                            curlhs = new Nonterminal(cur.content);
                            ExpectArrow();
                            last = cur;
                            cur = lexer.Next();
                        }
                        break;
                    case TokenType.EOS:
                        productions.Add(new Production(curlhs, currhs.ToArray()));
                        currhs.Clear();
                        done = true;
                        break;
                    default:
                        throw new ParseException(string.Format("The grammar couldn't be parsed. Please check the syntax... ({0})", generateLocationString(cur)));
                }
            }
        }

        private ContextFreeGrammar GetGrammar()
        {
            return new ContextFreeGrammar(startvar, productions);
        }

        //Generates the location string for a parsing error message. The location is given by a token.
        private string generateLocationString(Token t)
        {
            string res = lexer.input.Substring(t.pos);
            if (res.Length > 20) res = res.Substring(0, 20);
            res = string.Format("error occured just before \"{0}\"", res);
            res = res.Replace("&", "&amp;").Replace("'", "&apos;").Replace("\"", "&quot;").Replace(">", "&gt;").Replace("<", "&lt;");
            return res;
        }
    }

    public class DerivationParser<T>
    {
        private Lexer lex;
        private Func<char, T> mkExprinal;
        private List<GrammarSymbol> partialWord;

        private DerivationParser(Lexer lex, Func<char, T> mkExprinal)
        {
            this.lex = lex;
            this.mkExprinal = mkExprinal;
            this.partialWord = new List<GrammarSymbol>();
        }

        public static List<GrammarSymbol[]> Parse(Func<char, T> mkExprinal, string buf)
        {
            var result = new List<GrammarSymbol[]>();

            foreach(string line in Regex.Split(buf, "\r\n|\r|\n"))
            {
                if (line.Length == 0) continue;
                Lexer clex = new Lexer(line);
                var dp = new DerivationParser<T>(clex, mkExprinal);
                dp.Parse();
                result.Add(dp.partialWord.ToArray());
            }

            return result;
        }

        private void Parse()
        {
            bool done = false;
            Token cur = null;
            Token last = null;

            while (!done)
            {
                last = cur;
                cur = lex.Next();

                switch (cur.t)
                {
                    case TokenType.NT:
                        partialWord.Add(new Nonterminal(cur.content));
                        break;
                    case TokenType.T:
                        partialWord.Add(new Exprinal<T>(mkExprinal(cur.content[0]), cur.content));
                        break;
                    case TokenType.OR:
                        throw new ParseException(string.Format("A derivation cannot contain an OR ... ({0})", generateLocationString(cur)));
                    case TokenType.ARR:
                        throw new ParseException(string.Format("Don't use arrows in a derivation ... ({0})", generateLocationString(cur)));
                    case TokenType.EOS:
                        done = true;
                        break;
                    default:
                        throw new ParseException(string.Format("The derivation couldn't be parsed. Please check the syntax... ({0})", generateLocationString(cur)));
                }
            }
        }

        //Generates the location string for a parsing error message. The location is given by a token.
        private string generateLocationString(Token t)
        {
            string res = lex.input.Substring(t.pos);
            if (res.Length > 20) res = res.Substring(0, 20);
            res = string.Format("error occured just before \"{0}\"", res);
            res = res.Replace("&", "&amp;").Replace("'", "&apos;").Replace("\"", "&quot;").Replace(">", "&gt;").Replace("<", "&lt;");
            return res;
        }
    }
}

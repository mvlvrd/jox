package com.Jlox;

import java.lang.Character;
import java.util.ArrayList;
import java.util.List;

import static com.Jlox.TokenType.*;

enum NumberPart {
    INTEGER, FRACT, EXP
}

public class LoxScanner {
    int start = 0;
    int current = 0;
    int line = 0;
    int column = 0;
    final String src;
    List<Token> tokens;

    LoxScanner(String src) {
        this.src = src;
        this.tokens = new ArrayList<>();
    }

    List<Token> scanTokens() {
        while (notEOF()) {
            start = current;
            scanToken();
        }
        addToken(TokenType.EOF);
        return tokens;
    }

    void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '/': if (match('/'))  parseComment(); else addToken(SLASH); break;

            case '\n': line++; break;

            case '"': parseString(); break;
            default: {
                if (isWhiteSpace(c)) break;
                else if (isDigit(c)) { parseNumber(); break; }
                else if (isAlpha(c)) { parseIdentifier(); break; }
                else {
                    Jlox.error(line, String.format("Unexpected character %s", src.charAt(current)));
                }
            }
        }
    }

    void parseIdentifier() {
        char chr = src.charAt(current-1);
        StringBuffer buffer = new StringBuffer(chr);
        while (notEOF()) {
            if (chr == '\n') {
                line++;
                break;
            } else if (isAlphaNumeric(chr))
                buffer.append(chr);
            else if (isWhiteSpace(chr))
                break;
            else Jlox.error(line, String.format("Unexpected character %s", src.charAt(current)));
            chr = advance();
        }
        String str = buffer.toString();
        switch (str) {
            case "and": addToken(AND); break;
            case "class": addToken(CLASS); break;
            case "else": addToken(ELSE); break;
            case "false": addToken(FALSE); break;
            case "fun": addToken(FUN); break;
            case "for": addToken(FOR); break;
            case "if": addToken(IF); break;
            case "nil": addToken(NIL); break;
            case "or" : addToken(OR); break;
            case "print": addToken(PRINT); break;
            case "return": addToken(RETURN); break;
            case "super": addToken(SUPER); break;
            case "this": addToken(THIS); break;
            case "true": addToken(TRUE); break;
            case "var": addToken(VAR); break;
            case "while": addToken(WHILE); break;
            default:
                addToken(IDENTIFIER, str); break;
        }

    }

    void parseString() {
        StringBuffer buffer = new StringBuffer();
        while (notEOF()) {
            char chr = advance();
            if (chr == '"') {
                addToken(STRING, buffer.toString());
                return;
            } else if (chr == '\n') {
                line++;
            }
            else buffer.append(chr);
        }
        Jlox.error(line, String.format("Unterminated string"));
    }

    void parseNumber() {
        StringBuffer buffer = new StringBuffer(src.substring(current-1, current));
        NumberPart part = NumberPart.INTEGER;
        loop: while (notEOF()) {
            char chr = advance();
            switch (chr) {
                case '.': {
                    if (part == NumberPart.INTEGER) {
                        part = NumberPart.FRACT; break;
                    } else break loop;
                }
                case 'e': {
                    if ((part == NumberPart.INTEGER) || (part == NumberPart.FRACT)) {
                        part = NumberPart.EXP; break;
                    } else break loop;
                }
                default: {
                    if (isDigit(chr)) break;
                    else if (chr == '\n') {
                        line++;
                        break loop;
                    }
                    else if (isWhiteSpace(chr)) break loop;
                    else Jlox.error(line, String.format("Bad formatted number")); }
            }
            buffer.append(chr);
        }
        String number = buffer.toString();
        if (part == NumberPart.INTEGER) addToken(INTEGER, number, Integer.parseInt(number));
        else addToken(FLOAT, number, Double.parseDouble(number));
    }

    void parseComment() {
        while (notEOF() && (peek() != '\n')) advance();
    }

    static private boolean isWhiteSpace(Character chr) {
        return Character.isWhitespace(chr);
    }

    static private boolean isDigit(Character chr) {
        return chr >= '0' && chr <= '9';
    }

    static private boolean isAlpha(Character chr) {
        return (chr >= 'a' && chr <= 'z') ||
                (chr >= 'A' && chr <= 'Z') ||
                chr == '_';
    }

    static private boolean isAlphaNumeric(Character chr) {
        return isAlpha(chr) || isDigit(chr);
    }

    boolean match(char candidate) {
        if (notEOF() && src.charAt(current) == candidate) {
            current++;
            return true;
        }
        return false;
    }

    void addToken(TokenType tokenType) {
        tokens.add(new Token(tokenType, null, null, line));
    }

    void addToken(TokenType tokenType, String lexeme) {
        tokens.add(new Token(tokenType, lexeme, null, line));
    }

    void addToken(TokenType tokenType, String lexeme, Object obj) {
        tokens.add(new Token(tokenType, lexeme, obj, line));
    }

    private char peek() {
        return notEOF() ? src.charAt(current + 1) : '\0';
    }

    private char advance() {
        if (notEOF()) {
            char chr = src.charAt(current++);
            if (chr == '\n') column = 0;
            else column++;
            return chr;
        } else
            return '\0';
    }

    boolean notEOF() {
        return current < src.length();
    }
}

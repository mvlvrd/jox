package com.Jlox;

import static com.Jlox.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

enum NumberPart {
    INTEGER,
    FRACT,
    EXP
}

public class LoxScanner {
    int start = 0;
    int current = 0;
    int line = 0;
    int column = 0;
    final String src;
    List<Token> tokens;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
    }

    public LoxScanner(String src) {
        this.src = src;
        this.tokens = new ArrayList<>();
    }

    public List<Token> scanTokens() {
        while (notEOF()) {
            start = current;
            scanToken();
        }
        addToken(TokenType.EOF, "\\0");
        return tokens;
    }

    void scanToken() {
        char chr = advance();
        switch (chr) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '/' -> {
                if (match('/')) parseComment();
                else addToken(SLASH);
            }

            case '"' -> parseString();
            default -> {
                if (isWhiteSpace(chr) || chr == '\n') break;
                else if (isDigit(chr)) parseNumber();
                else if (isAlpha(chr)) parseIdentifier();
                else
                    Jlox.error(line, String.format("Unexpected character %s", src.charAt(current)));
            }
        }
    }

    void parseIdentifier() {
        while (isValidIdChar(peek())) advance();
        addToken();
    }

    void parseString() {
        int origLine = line;
        while (peek() != '"' && notEOF()) advance();
        if (!notEOF()) Jlox.error(origLine, "Unterminated string");
        advance();
        addStringToken(origLine);
    }

    void parseNumber() {
        NumberPart part = NumberPart.INTEGER;
        loop:
        while (notEOF()) {
            char chr = peek();
            switch (chr) {
                case '.' -> {
                    if (part == NumberPart.INTEGER) {
                        part = NumberPart.FRACT;
                    } else throw new Parser.ParseError();
                }
                case 'e' -> {
                    if ((part == NumberPart.INTEGER) || (part == NumberPart.FRACT)) {
                        part = NumberPart.EXP;
                    } else throw new Parser.ParseError();
                }
                default -> {
                    if (!isDigit(chr)) break loop;
                }
            }
            advance();
        }
        String lexeme = src.substring(start, current);
        if (part == NumberPart.INTEGER) addToken(INTEGER, lexeme, Integer.parseInt(lexeme));
        else addToken(FLOAT, lexeme, Double.parseDouble(lexeme));
    }

    void parseComment() {
        while (notEOF() && (peek() != '\n')) advance();
    }

    private static boolean isWhiteSpace(Character chr) {
        return Character.isWhitespace(chr);
    }

    private static boolean isDigit(Character chr) {
        return chr >= '0' && chr <= '9';
    }

    private static boolean isAlpha(Character chr) {
        return (chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z') || chr == '_';
    }

    private static final Set<Character> ValidInId = new HashSet<>();

    private static boolean isValidIdChar(Character chr) {
        return isAlpha(chr) || isDigit(chr) || ValidInId.contains(chr);
    }

    boolean match(char candidate) {
        if (notEOF() && src.charAt(current) == candidate) {
            current++;
            return true;
        }
        return false;
    }

    void addToken() {
        String lexeme = src.substring(start, current);
        addToken(keywords.getOrDefault(lexeme, IDENTIFIER), lexeme);
    }

    void addToken(TokenType tokenType) {
        String lexeme = src.substring(start, current).replace("\n", "\\n");
        tokens.add(new Token(tokenType, lexeme, null, line));
    }

    void addToken(TokenType tokenType, String lexeme) {
        tokens.add(new Token(tokenType, lexeme, null, line));
    }

    void addStringToken(int origLine) {
        String lexeme = src.substring(start + 1, current - 1);
        tokens.add(new Token(STRING, lexeme, lexeme, origLine));
    }

    void addToken(TokenType tokenType, String lexeme, Object obj) {
        tokens.add(new Token(tokenType, lexeme, obj, line));
    }

    private char peek() {
        return notEOF() ? src.charAt(current) : '\0';
    }

    private char advance() {
        if (notEOF()) {
            char chr = src.charAt(current++);
            if (chr == '\n') {
                column = 0;
                line++;
            } else column++;
            return chr;
        } else return '\0';
    }

    boolean notEOF() {
        return current < src.length();
    }
}

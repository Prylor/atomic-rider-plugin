package com.jetbrains.rider.plugins.atomic.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes;
import com.intellij.psi.TokenType;

%%

%class AtomicLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
END_OF_LINE_COMMENT="#"[^\r\n]*

// Keywords
HEADER="header"
ENTITY_TYPE="entityType"
AGGRESSIVE_INLINING="aggressiveInlining"
UNSAFE="unsafe"
NAMESPACE="namespace"
CLASS_NAME="className"
DIRECTORY="directory"
IMPORTS="imports"
TAGS="tags"
VALUES="values"

// Values
TRUE="true"
FALSE="false"
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
STRING_VALUE=\"([^\\\"]|\\.)*\"
PATH_LIKE=[a-zA-Z_][a-zA-Z0-9_/\\.]*

SIMPLE_TYPE=[a-zA-Z_][a-zA-Z0-9_\.\[\]]*
ANGLE_OPEN="<"
ANGLE_CLOSE=">"
COMMA=","

// Separators
COLON=":"
HYPHEN="-"

%state WAITING_VALUE
%state WAITING_NAMESPACE
%state IN_IMPORTS
%state IN_TAGS  
%state IN_VALUES
%state IN_VALUE_NAME
%state IN_VALUE_TYPE

%%

<YYINITIAL> {END_OF_LINE_COMMENT}                           { yybegin(YYINITIAL); return AtomicTypes.COMMENT; }

<YYINITIAL> {HEADER}                                        { yybegin(WAITING_VALUE); return AtomicTypes.HEADER_KEYWORD; }
<YYINITIAL> {ENTITY_TYPE}                                   { yybegin(WAITING_VALUE); return AtomicTypes.ENTITY_TYPE_KEYWORD; }
<YYINITIAL> {AGGRESSIVE_INLINING}                           { yybegin(WAITING_VALUE); return AtomicTypes.AGGRESSIVE_INLINING_KEYWORD; }
<YYINITIAL> {UNSAFE}                                        { yybegin(WAITING_VALUE); return AtomicTypes.UNSAFE_KEYWORD; }
<YYINITIAL> {NAMESPACE}                                     { yybegin(WAITING_NAMESPACE); return AtomicTypes.NAMESPACE_KEYWORD; }
<YYINITIAL> {CLASS_NAME}                                    { yybegin(WAITING_VALUE); return AtomicTypes.CLASS_NAME_KEYWORD; }
<YYINITIAL> {DIRECTORY}                                     { yybegin(WAITING_VALUE); return AtomicTypes.DIRECTORY_KEYWORD; }
<YYINITIAL> "solution"                                      { yybegin(WAITING_VALUE); return AtomicTypes.SOLUTION_KEYWORD; }
<YYINITIAL> {WHITE_SPACE}+                                 { return TokenType.WHITE_SPACE; }

<YYINITIAL> {IMPORTS}                                       { yybegin(IN_IMPORTS); return AtomicTypes.IMPORTS_KEYWORD; }
<YYINITIAL> {TAGS}                                          { yybegin(IN_TAGS); return AtomicTypes.TAGS_KEYWORD; }
<YYINITIAL> {VALUES}                                        { yybegin(IN_VALUES); return AtomicTypes.VALUES_KEYWORD; }

<WAITING_VALUE> {COLON}                                     { return AtomicTypes.COLON; }
<WAITING_VALUE> {WHITE_SPACE}+                              { return TokenType.WHITE_SPACE; }
<WAITING_VALUE> {STRING_VALUE}                              { yybegin(YYINITIAL); return AtomicTypes.STRING; }
<WAITING_VALUE> {TRUE}                                      { yybegin(YYINITIAL); return AtomicTypes.TRUE; }
<WAITING_VALUE> {FALSE}                                     { yybegin(YYINITIAL); return AtomicTypes.FALSE; }
<WAITING_VALUE> {IDENTIFIER}                                { yybegin(YYINITIAL); return AtomicTypes.IDENTIFIER; }
<WAITING_VALUE> {PATH_LIKE}                                 { yybegin(YYINITIAL); return AtomicTypes.TYPE_REFERENCE; }
<WAITING_VALUE> {CRLF}                                      { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

<WAITING_NAMESPACE> {COLON}                                 { return AtomicTypes.COLON; }
<WAITING_NAMESPACE> {WHITE_SPACE}+                          { return TokenType.WHITE_SPACE; }
<WAITING_NAMESPACE> {IDENTIFIER}(\.{IDENTIFIER})*           { yybegin(YYINITIAL); return AtomicTypes.NAMESPACE_VALUE; }
<WAITING_NAMESPACE> {CRLF}                                  { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

<IN_IMPORTS, IN_TAGS, IN_VALUES> {END_OF_LINE_COMMENT}     { return AtomicTypes.COMMENT; }
<IN_IMPORTS, IN_TAGS, IN_VALUES> {COLON}                   { return AtomicTypes.COLON; }
<IN_IMPORTS, IN_TAGS> {HYPHEN}                             { return AtomicTypes.HYPHEN; }

<IN_IMPORTS> {IMPORTS}                                      { yybegin(IN_IMPORTS); return AtomicTypes.IMPORTS_KEYWORD; }
<IN_IMPORTS> {TAGS}                                         { yybegin(IN_TAGS); return AtomicTypes.TAGS_KEYWORD; }
<IN_IMPORTS> {VALUES}                                       { yybegin(IN_VALUES); return AtomicTypes.VALUES_KEYWORD; }
<IN_IMPORTS> {HEADER}                                       { yybegin(WAITING_VALUE); return AtomicTypes.HEADER_KEYWORD; }
<IN_IMPORTS> {ENTITY_TYPE}                                  { yybegin(WAITING_VALUE); return AtomicTypes.ENTITY_TYPE_KEYWORD; }
<IN_IMPORTS> {AGGRESSIVE_INLINING}                          { yybegin(WAITING_VALUE); return AtomicTypes.AGGRESSIVE_INLINING_KEYWORD; }
<IN_IMPORTS> {UNSAFE}                                       { yybegin(WAITING_VALUE); return AtomicTypes.UNSAFE_KEYWORD; }
<IN_IMPORTS> {NAMESPACE}                                    { yybegin(WAITING_VALUE); return AtomicTypes.NAMESPACE_KEYWORD; }
<IN_IMPORTS> {CLASS_NAME}                                   { yybegin(WAITING_VALUE); return AtomicTypes.CLASS_NAME_KEYWORD; }
<IN_IMPORTS> {DIRECTORY}                                    { yybegin(WAITING_VALUE); return AtomicTypes.DIRECTORY_KEYWORD; }
<IN_IMPORTS> "solution"                                     { yybegin(WAITING_VALUE); return AtomicTypes.SOLUTION_KEYWORD; }
<IN_IMPORTS> {IDENTIFIER}(\.{IDENTIFIER})*                  { return AtomicTypes.IMPORT_PATH; }

<IN_TAGS> {IMPORTS}                                         { yybegin(IN_IMPORTS); return AtomicTypes.IMPORTS_KEYWORD; }
<IN_TAGS> {TAGS}                                            { yybegin(IN_TAGS); return AtomicTypes.TAGS_KEYWORD; }
<IN_TAGS> {VALUES}                                          { yybegin(IN_VALUES); return AtomicTypes.VALUES_KEYWORD; }
<IN_TAGS> {HEADER}                                          { yybegin(WAITING_VALUE); return AtomicTypes.HEADER_KEYWORD; }
<IN_TAGS> {ENTITY_TYPE}                                     { yybegin(WAITING_VALUE); return AtomicTypes.ENTITY_TYPE_KEYWORD; }
<IN_TAGS> {AGGRESSIVE_INLINING}                             { yybegin(WAITING_VALUE); return AtomicTypes.AGGRESSIVE_INLINING_KEYWORD; }
<IN_TAGS> {UNSAFE}                                          { yybegin(WAITING_VALUE); return AtomicTypes.UNSAFE_KEYWORD; }
<IN_TAGS> {NAMESPACE}                                       { yybegin(WAITING_VALUE); return AtomicTypes.NAMESPACE_KEYWORD; }
<IN_TAGS> {CLASS_NAME}                                      { yybegin(WAITING_VALUE); return AtomicTypes.CLASS_NAME_KEYWORD; }
<IN_TAGS> {DIRECTORY}                                       { yybegin(WAITING_VALUE); return AtomicTypes.DIRECTORY_KEYWORD; }
<IN_TAGS> "solution"                                        { yybegin(WAITING_VALUE); return AtomicTypes.SOLUTION_KEYWORD; }
<IN_TAGS> {IDENTIFIER}                                      { return AtomicTypes.TAG_NAME; }

<IN_VALUES> {WHITE_SPACE}+                                  { return TokenType.WHITE_SPACE; }
<IN_VALUES> {HYPHEN}                                        { yybegin(IN_VALUE_NAME); return AtomicTypes.HYPHEN; }

<IN_VALUE_NAME> {IDENTIFIER}                                { yybegin(IN_VALUE_TYPE); return AtomicTypes.VALUE_NAME; }
<IN_VALUE_NAME> {WHITE_SPACE}+                              { return TokenType.WHITE_SPACE; }
<IN_VALUE_NAME> {CRLF}                                      { yybegin(IN_VALUES); return TokenType.WHITE_SPACE; }

<IN_VALUE_TYPE> {COLON}                                     { return AtomicTypes.COLON; }
<IN_VALUE_TYPE> {WHITE_SPACE}+                              { return TokenType.WHITE_SPACE; }
<IN_VALUE_TYPE> {CRLF}                                      { yybegin(IN_VALUES); return TokenType.WHITE_SPACE; }
<IN_VALUE_TYPE> {HYPHEN}                                    { yypushback(1); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {END_OF_LINE_COMMENT}                       { yybegin(IN_VALUES); return AtomicTypes.COMMENT; }
<IN_VALUE_TYPE> {IMPORTS}                                   { yypushback(yylength()); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {TAGS}                                      { yypushback(yylength()); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {VALUES}                                    { yypushback(yylength()); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {HEADER}                                    { yypushback(yylength()); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {ENTITY_TYPE}                               { yypushback(yylength()); yybegin(IN_VALUES); }
<IN_VALUE_TYPE> {ANGLE_OPEN}                                { return AtomicTypes.TYPE_REFERENCE; }
<IN_VALUE_TYPE> {ANGLE_CLOSE}                               { return AtomicTypes.TYPE_REFERENCE; }
<IN_VALUE_TYPE> {COMMA}                                     { return AtomicTypes.TYPE_REFERENCE; }
<IN_VALUE_TYPE> {SIMPLE_TYPE}                               { return AtomicTypes.TYPE_REFERENCE; }
<IN_VALUE_TYPE> {IDENTIFIER}                                { return AtomicTypes.TYPE_REFERENCE; }

<YYINITIAL> {IDENTIFIER}                                    { return AtomicTypes.IDENTIFIER; }

({CRLF}|{WHITE_SPACE})+                                     { return TokenType.WHITE_SPACE; }

[^]                                                         { return TokenType.BAD_CHARACTER; }
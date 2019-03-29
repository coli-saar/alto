grammar DottedCommand;

@header{
    package de.up.ling.irtg.laboratory;
}

TOKEN: [A-Za-z$<#_][A-Za-z0-9_$#<>]*;
NUMBERTOKEN: [0-9][.0-9]*;

INTERPLEFT: '[';
INTERPRIGHT: ']';
BKOPEN: '(';
BKCLOSE: ')';
DOT: '.';
COMMA: ','
     | ', '
     ;

expr: dottedCommand BKOPEN argument BKCLOSE
    | TOKEN BKOPEN argument BKCLOSE
    | dottedExpr
    | TOKEN
    | NUMBERTOKEN
    ;

argument: expr COMMA argument
        | expr
        ;

dottedCommand: dottedExpr DOT TOKEN;

dottedExpr: dottedExpr DOT TOKEN
          | dottedExpr DOT interpretation
          | TOKEN
          ;
            
interpretation: INTERPLEFT TOKEN INTERPRIGHT;
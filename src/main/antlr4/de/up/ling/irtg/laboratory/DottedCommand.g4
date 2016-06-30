/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar DottedCommand;

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
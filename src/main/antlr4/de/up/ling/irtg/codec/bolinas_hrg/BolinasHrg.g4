grammar BolinasHrg;

ARROW    : '->';
SEMICOLON : ';';
//COLON : ':';
OPBK     : '(';
CLBK     : ')';
DOT      : '.';
STAR : '*';
//DOLLAR: '$';

NAME: [a-zA-Z_] ([a-zA-Z0-9_']*);
NAME_WITH_DOLLAR: [a-zA-Z_] ([a-zA-Z0-9_]*);
INT_NUMBER : [0-9]+;
FLOAT_NUMBER: (([0-9]+)? '.')? [0-9]+ ([eE][-+]?[0-9]+)?;
EDGELABEL: [:] ((~[ \t\n])+);

WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;


hrg: hrgRule+;

hrgRule: nonterminal ARROW term SEMICOLON weight?;

term: OPBK node edgeWithChildren* CLBK;

node: (id? DOT label? externalMarker?) | (label externalMarker?);

edgeWithChildren: edgelabel child*;
// COLON (edgelabel|nonterminalEdgelabel)
child: node | term;


externalMarker: STAR INT_NUMBER?;

nonterminal: NAME;
id: NAME|INT_NUMBER;
label: NAME|INT_NUMBER;
weight: FLOAT_NUMBER;

edgelabel: EDGELABEL;
//edgelabel: NAME|INT_NUMBER;
//nonterminalEdgelabel: NAME DOLLAR;


//edgelabel: NAME #EL_RAW | NAME_WITH_DOLLAR #EL_NONTERMINAL;
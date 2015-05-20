grammar BolinasHrg;

@lexer::members {
    boolean ignore=true;
}


//NODE_WITH_DOT: { ignore = false; } (NODE_NAME? DOT NODE_LABEL? NODE_EXTERNAL_MARKER?) { ignore = true; };
//NODE_WITHOUT_DOT: { ignore = false; } (NODE_LABEL NODE_EXTERNAL_MARKER?) { ignore = true; };


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
QUOTED_NAME: ['] (~['])* [']; 
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
INT_NUMBER : [0-9]+;
FLOAT_NUMBER: (([0-9]+)? '.')? [0-9]+ ([eE][-+]?[0-9]+)?;
EDGELABEL: [:] ((~[ \t\n)])+);

//NODE_NAME: (NAME|INT_NUMBER);
//NODE_LABEL: (NAME|INT_NUMBER|QUOTED_NAME|DOUBLE_QUOTED_NAME);
//NODE_EXTERNAL_MARKER: (STAR INT_NUMBER?);


//NODE_COMPLEX: { ignore = false; }  (((NAME|INT_NUMBER)? DOT (NAME|INT_NUMBER|QUOTED_NAME|DOUBLE_QUOTED_NAME)? (STAR INT_NUMBER?)?) | ((NAME|INT_NUMBER) (STAR INT_NUMBER?)?)) { ignore = true; } ;


WS: [ \n\t\r]+ ;
//{ if(ignore) skip(); } ;
//skip;

COMMENT
    :   ( '#' ~[\r\n]* '\r'? '\n' ) -> skip
    ;


hrg: WS* (hrgRule WS*)+;

hrgRule: nonterminal WS* ARROW  WS* term WS* SEMICOLON WS* weight?;

term: OPBK WS* node WS* (edgeWithChildren WS*)* CLBK;

//node: NODE_WITH_DOT | NODE_WITHOUT_DOT;
//{ ignore = false; }  ((id? DOT label? externalMarker?) | (label externalMarker?)) { ignore = true; } ;

node: (id? DOT label? externalMarker?) | (label externalMarker?);

edgeWithChildren: edgelabel WS* (child WS*)*;
// COLON (edgelabel|nonterminalEdgelabel)
child: node | term;


externalMarker: STAR INT_NUMBER?;

nonterminal: NAME;
id: NAME|INT_NUMBER;
label: NAME|INT_NUMBER|QUOTED_NAME|DOUBLE_QUOTED_NAME;
weight: FLOAT_NUMBER;

edgelabel: EDGELABEL;
//edgelabel: NAME|INT_NUMBER;
//nonterminalEdgelabel: NAME DOLLAR;


//edgelabel: NAME #EL_RAW | NAME_WITH_DOLLAR #EL_NONTERMINAL;
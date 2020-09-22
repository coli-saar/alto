grammar Irtg;

@header{
    package de.up.ling.irtg.codec.irtg;
}

INTERPRETATION : 'interpretation';
FEATURE        : 'feature';

NAME: [a-zA-Z_*$@+] ([a-zA-Z0-9_<>*$@+/.-]*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
VARIABLE : [?] [a-zA-Z0-9_-]*;
NUMBER : '-'? [0-9.] ([0-9.eE-]*);

FIN_MARK : '!' | '\u00b0';
ARROW    : '->';
OPEN_BK  : '(';
CLOSE_BK : ')';
OPEN_SQBK: '[';
CLOSE_SQBK: ']';
COMMA    : ',';
COLON    : ':';

WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;

//Two intended top-level rules:
// 1. an interpreted regular tree grammar
irtg       : interpretation_decl+ feature_decl* irtg_rule+;
// 2. a tree automaton
fta        : auto_rule+;

interpretation_decl: INTERPRETATION name ':' name;

feature_decl: FEATURE name ':' name state_list #CONSTRUCTOR_FEATURE
            | FEATURE name ':' name ':' ':' name state_list #STATIC_FEATURE;

irtg_rule  : auto_rule hom_rule*;

auto_rule  : state '->' name state_list weight?;
state_list : ('(' (state ',')* state ')')?;

hom_rule   : '[' name ']' term;
term       : name ('(' (term ',')* term ')')? #CONSTANT_TERM
           | variable                         #VARIABLE_TERM;

weight     : '[' NUMBER ']';


name     : NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
state    : name FIN_MARK?;
variable : VARIABLE;
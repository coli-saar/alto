grammar BottomUpTreeAutomaton;

@header{
    package de.up.ling.irtg.codec.bottomup_treeautomaton;
}

ARROW    : '->';
OPBK : '(';
CLBK : ')';
COMMA: ',';

ANGLE_IDENTIFIER: '<' (~[>])* '>';
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
QUOTED_NAME: ['] (~['])* ['];
NAME: ~('<' | '(' | ')' | '"' | '\'' | ','| '\t' | ' ' | '\r' | '\n'| '\u000C'  ) ((~( '(' | ')' | ',' | '\t' | ' ' | '\r' | '\n'| '\u000C' ))*); 

WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '#' ~[\r\n]* '\r'? '\n'
        ) -> skip
    ;




fta       : state+ auto_rule+;



auto_rule  : name state_list ARROW state weight?;
state_list : ('(' (state ',')* state ')')?;

weight: ANGLE_IDENTIFIER;

name     : NAME #RAW | DOUBLE_QUOTED_NAME #QUOTED | QUOTED_NAME #QUOTED | ANGLE_IDENTIFIER #ANGLE;
state    : name;


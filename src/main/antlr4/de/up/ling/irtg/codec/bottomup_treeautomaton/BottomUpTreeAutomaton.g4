/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar BottomUpTreeAutomaton;

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


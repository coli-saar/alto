/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar TiburonTreeAutomaton;

ARROW    : '->';
OPBK : '(';
CLBK : ')';
HASH: '#';


DOUBLE_QUOTED_NAME: ["] (~["])* ["];
QUOTED_NAME: ['] (~['])* ['];
NAME: ~('#' | '(' | ')' | '"' | '\'' | '\t' | ' ' | '\r' | '\n'| '\u000C'  ) ((~( '(' | ')' | '\t' | ' ' | '\r' | '\n'| '\u000C' ))*); 

WS: [ \n\t\r]+ -> skip;


COMMENT:   ( '%' ~[\r\n]* '\r'? '\n' ) -> skip;


fta       : state+ auto_rule+;


auto_rule  : state '->' name state_list weight?;
state_list : ('(' state+ ')')?;

weight     : '#' name;


name     : NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
state    : name;


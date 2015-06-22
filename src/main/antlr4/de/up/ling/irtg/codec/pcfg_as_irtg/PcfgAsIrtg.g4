/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar PcfgAsIrtg;


ARROW    : '->';
NUMBER_IN_BRACKETS: '[' [0-9.] ([0-9.eE-]*) ']';

//QUOTED_NAME: ['] (~['])* ['];
//DOUBLE_QUOTED_NAME: ["] (~["])* ["];

// all non-whitespace strings that are not -> or [...] are symbols
NAME: ~( '[' | '\t' | ' ' | '\r' | '\n'| '\u000C' ) ((~( '\t' | ' ' | '\r' | '\n'| '\u000C' ))*);


WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;


pcfg: startsymbol pcfg_rule+;

startsymbol: name;

pcfg_rule: name ARROW name+ NUMBER_IN_BRACKETS?;


name     : NAME #RAW ; //| QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
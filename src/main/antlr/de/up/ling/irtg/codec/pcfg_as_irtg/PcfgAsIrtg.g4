grammar PcfgAsIrtg;

@header{
    package de.up.ling.irtg.codec.pcfg_as_irtg;
}

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
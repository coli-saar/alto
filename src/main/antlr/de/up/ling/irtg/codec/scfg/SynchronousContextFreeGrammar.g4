grammar SynchronousContextFreeGrammar;

@header{
    package de.up.ling.irtg.codec.scfg;
}

ARROW    : '->';
NUMBER_IN_BRACKETS: '[' ([0-9]+) ']';

//QUOTED_NAME: ['] (~['])* ['];
//DOUBLE_QUOTED_NAME: ["] (~["])* ["];

// all non-whitespace strings that are not -> or [...] are symbols
NAME: ~( '[' | '\t' | ' ' | '\r' | '\n'| '\u000C' ) ((~( '\t' | ' ' | '\r' | '\n'| '\u000C' | '[' ))*);


WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;


scfg: startsymbol rulepair+;

startsymbol: name;

rulepair: cfg_rule cfg_rule;
cfg_rule: name ARROW name_with_optional_bracket+ NUMBER_IN_BRACKETS?;

name     : NAME #RAW ; //| QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
name_with_optional_bracket : name (NUMBER_IN_BRACKETS)?;

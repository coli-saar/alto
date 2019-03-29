grammar Rdg;

@header{
    package de.up.ling.irtg.codec.rdg;
}

LABEL: '<' (~[>])+ '>';
NAME: [a-zA-Z_*$@+] ([a-zA-Z0-9_*$@+/.-]*);

COLON: ':';
OPEN_ABK: '<';
CLOSE_ABK: '>';
OPEN_BK: '(';
CLOSE_BK: ')';
ARROW: '->';


WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;

rdg       : rdg_rule+;

rdg_rule: NAME ARROW LABEL state_list;
state_list: ('(' (NAME ',')* NAME ')')?;


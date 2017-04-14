grammar FeatStruct;



NAME: [a-zA-Z_*$@+] ([a-zA-Z0-9_<>*$@+/.-]*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
VARIABLE : [#] [a-zA-Z0-9_-]*;
INT: [0-9]+;
//NUMBER : [0-9.] ([0-9.eE-]*);

OP_SQBK: '[';
CL_SQBK: ']';
COLON: ':';
COMMA: ',';


WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;


featstruct: avm | primitive;

avm: OP_SQBK (avpair COMMA)* avpair? CL_SQBK;

avpair: name COLON featstruct;

primitive: name | INT;
             
name     : NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #DQUOTED ;
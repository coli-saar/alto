grammar FeatStruct;



NAME: [a-zA-Z_*$@+] ([a-zA-Z0-9_<>*$@+/.-]*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
INDEX : [#] [a-zA-Z0-9_-]*;
INT: [0-9]+;

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


featstruct: index | (index? (avm|primitive));

avm: OP_SQBK (avpair COMMA)* avpair? CL_SQBK;

avpair: name COLON featstruct;

primitive: name | number;
             
name     : NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #DQUOTED ;

index : INDEX;

number: INT;
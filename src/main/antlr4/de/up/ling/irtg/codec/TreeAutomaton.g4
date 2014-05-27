/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar TreeAutomaton;

NAME: [a-zA-Z_*$@+] ([a-zA-Z0-9_<>*$@+/.-]*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
NUMBER : [0-9.] ([0-9.eE-]*);

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

fta       : auto_rule+;


auto_rule  : state '->' name state_list weight?;
state_list : ('(' (state ',')* state ')')?;

//hom_rule   : '[' name ']' term;
//term       : name ('(' (term ',')* term ')')? #CONSTANT_TERM
//           | variable                         #VARIABLE_TERM;

weight     : '[' NUMBER ']';


name     : NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
state    : name FIN_MARK?;
//variable : VARIABLE;
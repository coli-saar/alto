/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar TemplateIrtg;

INTERPRETATION : 'interpretation';
FEATURE        : 'feature';
FOREACH        : 'foreach';
AND            : 'and';

NAME: ([a-zA-Z_*$@+]|[$$]) (([a-zA-Z0-9_<>*$@+/.-]|[$$])*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
VARIABLE : [?] [a-zA-Z0-9_-]*;
NUMBER : [0-9.] ([0-9.eE-]*);

FIN_MARK : '!' | '\u00b0';
ARROW    : '->';
OPEN_BK  : '(';
CLOSE_BK : ')';
OPEN_SQBK: '[';
CLOSE_SQBK: ']';
OPEN_BRC : '{';
CLOSE_BRC: '}';
PIPE     : '|';
COMMA    : ',';
COLON    : ':';

WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;

template_irtg       : interpretation_decl+ feature_decl* template_irtg_rule+;


interpretation_decl: INTERPRETATION name ':' name;

feature_decl: FEATURE name ':' name state_list;

template_irtg_rule : irtg_rule | guarded_irtg_rule;

guarded_irtg_rule : guard irtg_rule;
irtg_rule  : auto_rule hom_rule*;

auto_rule  : state '->' name state_list weight?;
state_list : ('(' (state ',')* state ')')?;

hom_rule   : '[' name ']' term;
term       : name ('(' (term ',')* term ')')? #CONSTANT_TERM
           | variable                         #VARIABLE_TERM;

weight     : '[' NUMBER ']';

guard      : FOREACH OPEN_BRC name_list PIPE guard_condition CLOSE_BRC COLON;
name_list : (name ',')* name;

guard_condition : atomic_guard_condition | conjunctive_guard_condition;
atomic_or_bracketed_guard_condition: atomic_guard_condition | OPEN_BK guard_condition CLOSE_BK;
atomic_guard_condition: term;
conjunctive_guard_condition: atomic_or_bracketed_guard_condition AND guard_condition;


name     : NAME #RAW | FOREACH #RAW | AND #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #QUOTED ;
state    : name FIN_MARK?;
variable : VARIABLE;


grammar Tulipac;



// keywords
TREE: 'tree';
FAMILY: 'family';
WORD: 'word';
LEMMA: 'lemma';
INCLUDE: '#include';


NAME: [a-zA-Z_] ([a-zA-Z0-9_]*);
QUOTED_NAME: ['] (~['])* ['];
DOUBLE_QUOTED_NAME: ["] (~["])* ["];
FAMILY_NAME: [<] (~[>])* [>];
ANNOTATION: [@] ([a-zA-Z0-9_]+);
VARIABLE: [?] ([a-zA-Z0-9_]+);

SUBSTITUTION_MARKER: '!';
FOOT_MARKER: '*';
ANCHOR_MARKER: '+';

COLON: ':';
OP_CBK: '{';
CL_CBK: '}';
OP_SBK: '[';
CL_SBK: ']';
COMMA: ',';
EQ: '=';


WS: [ \n\t\r]+ -> skip;

COMMENT
    :   ( '//' ~[\r\n]* '\r'? '\n'
        | '/*' .*? '*/'
        ) -> skip
    ;



grmr: (tr | family | wordByItself | lemma | include)+;



/**** trees ****/

tr: TREE identifier COLON node;

node: identifier (annotation|marker)* fs? fs? (OP_CBK node+ CL_CBK)?;

fs: OP_SBK (ft COMMA)* ft? CL_SBK;

ft: identifier EQ (identifier|variable);



/**** tree families ****/

family: FAMILY identifier COLON OP_CBK (identifier COMMA)* identifier CL_CBK;



/**** words ****/

wordByItself: WORD identifier COLON (identifier|familyIdentifier) fs?;

wordInLemma: WORD identifier (COLON fs)?;

lemma: LEMMA identifier COLON (identifier|familyIdentifier) fs? OP_CBK wordInLemma+ CL_CBK;



/**** #include ****/
include: INCLUDE identifier;




identifier: NAME #RAW | QUOTED_NAME #QUOTED | DOUBLE_QUOTED_NAME #DQUOTED ;

familyIdentifier: FAMILY_NAME;

marker: SUBSTITUTION_MARKER #SUBST | FOOT_MARKER #FOOT | ANCHOR_MARKER #ANCHOR;

annotation: ANNOTATION;

variable: VARIABLE;
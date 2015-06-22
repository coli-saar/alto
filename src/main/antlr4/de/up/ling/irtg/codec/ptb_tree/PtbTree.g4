grammar PtbTree;


BKOPEN: '(';
BKCLOSE: ')';
NAME: (~ [() \n\t\r])+;

WS: [ \n\t\r]+ -> skip;

corpus: (BKOPEN (tree) BKCLOSE)*;

tree: (BKOPEN NAME tree+ BKCLOSE)
    | NAME;


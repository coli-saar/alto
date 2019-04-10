
family alphanx0Vnx1: { alphanx0Vnx1_nn, alphanx0Vnx1_wn, alphanx0Vnx1_nw }

tree alphanx0Vnx1_nn:
     S[][inv=no] {
       NP! [wh=no]
       VP  [][] {
         V+ [fin=yes]
	 NP! [wh=no, case=acc]
       }
     }


tree alphanx0Vnx1_wn:
     S[] {
       NP! [wh=yes, case=nom]
       S {
	 VP  [][] {
           V+ [fin=yes]
	   NP! [wh=no, case=acc]
         }
       }
     }

tree alphanx0Vnx1_nw:
     S[] {
       NP! [wh=yes, case=acc]
       S[inv=yes][inv=no] {
         NP! [wh=no]
	 V+ [fin=no]
       }
     }


word 'likes': <alphanx0Vnx1> [fin=yes]
word 'like':  <alphanx0Vnx1> [fin=no]



tree aux:
     S[][inv=yes] {
       Aux+
       S* [inv=no]
     }

word 'does': aux



/////////////////////////////////////////////////////////////////////////////////


tree pn:
     NP[][wh=no] {
       PN+
     }

tree np:
     NP[][wh=no, case=?c] {
       Det!
       N+ [case=?c]
     }

tree det:
     Det+

tree whnp:
     NP[][wh=yes, case=?c] {
       WH+ [case=?c]
     }


word 'john': pn
word 'mary': pn

word 'book': np

word 'every': det

word 'whom': whnp[case=acc]
word 'who':  whnp[case=nom]


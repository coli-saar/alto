// This grammar has multiple tree families with the same name.
// It reproduces issue #45.


//family alphanx0Vnx1: { alphanx0Vnx1_nn, alphanx0Vnx1_wn, alphanx0Vnx1_nw }

//family alphanx0Vnx1: { alphanx0Vnx1_nn, alphanx0Vnx1_wn }

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


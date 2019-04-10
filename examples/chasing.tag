tree trans:
  S {
    NP![case=nom][]
    VP {
      V+
      NP![case=acc][]
    }
}


tree intrans:
  S {
    NP![case=nom][]
    V+
  }


tree np_n:
  NP[][case=?case] {
    Det! [case=?case][]
    N+   [case=?case][]
  }

tree aux_adj:
  N [][case=?case] {
    Adj+ [case=?case][]
    N* [case=?case][]
  }
    

tree det:
  Det+


word 'jagt': trans

word 'hund': np_n[case=nom]
word 'hund': np_n[case=acc]

word 'hase': np_n[case=nom]
word 'hasen': np_n[case=acc]

word 'der': det[case=nom]
word 'den': det[case=acc]

lemma 'schnell': aux_adj {
  word 'schnelle': [case=nom]
  word 'schnellen': [case=acc]
}

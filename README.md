# The Alto parser

Welcome to Alto, the Algebraic Language Toolkit.

Alto is a parser and decoder for Interpreted Regular Tree Grammars (IRTGs). Its main features are:

- Represents grammars from a wide variety of popular grammar formalisms as IRTGs, including:
	- Context-free grammars
	- Tree-adjoining grammars (TAG)
	- Tree automata and bottom-up tree transducers
	- Synchronous context-free grammars, TAG, etc.
	- Tree-to-string and string-to-tree transducers
	- Synchronous Hyperedge Replacement Grammars (HRG): Alto is the [fastest published HRG parser in the world](http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=sgraph-parsing-15)
	- and many more
- Implements chart-based algorithms for
	- parsing
	- synchronous parsing (with inputs from multiple sides of a synchronous grammar)
	- decoding (to another side of a synchronous grammar)
	- maximum likelihood and expectation maximization (EM) training
	- binarization
- Supports weighted/PFCG-style and log-linear probability models for all of these grammar formalisms.
- Built for easy extensibility: implement your own grammar formalism by adding an [Algebra](wiki/Algebras) class, and use any of the Alto algorithms directly.
- Comes with a GUI that provides access to most of these algorithms and visualizes parsing results.

The basic theory of IRTGs is explained in [Koller & Kuhlmann, IWPT 2011](http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=irtg-11). You can find more details on the [Literature](wiki/Literature) page.

Alto is implemented in Java and can be downloaded [here](https://bitbucket.org/tclup/alto/downloads).

See the [Wiki](wiki/Home) for more details on how to use Alto.
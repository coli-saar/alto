# The Alto parser

Welcome to Alto, the Algebraic Language Toolkit.

Alto is a parser and decoder for Interpreted Regular Tree Grammars (IRTGs). It is being developed at the [University of Potsdam](http://www.ling.uni-potsdam.de/en/) in the Theoretical Computational Linguistics group, led by [Alexander Koller](http://www.ling.uni-potsdam.de/~koller/). Its main features are:

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
	- computing 1-best (Viterbi) and k-best derivations
	- maximum likelihood and expectation maximization (EM) training
	- binarization
- Supports weighted/PCFG-style and log-linear probability models for all of these grammar formalisms.
- Built for easy extensibility: implement your own grammar formalism by adding an [Algebra](https://bitbucket.org/tclup/alto/wiki/Algebras) class, and use any of the Alto algorithms directly.
- Comes with a GUI that provides access to most of these algorithms and visualizes parsing results.

The basic theory of IRTGs is explained in [Koller & Kuhlmann, IWPT 2011](http://www.ling.uni-potsdam.de/~koller/showpaper.php?id=irtg-11). You can find more details on the [Literature](https://bitbucket.org/tclup/alto/wiki/Literature) page.

Alto is implemented in Java and can be downloaded [here](https://bitbucket.org/tclup/alto/downloads).

See the [Wiki](https://bitbucket.org/tclup/alto/wiki/Home) for more details on how to use Alto.

## Screenshots ##

Here are some screenshots of the Alto GUI. Here's an IRTG with one string and one graph interpretation (equivalent to a synchronous HRG):

![Screen Shot 2015-05-27 at 16.18.42.png](https://bitbucket.org/repo/ny94Mo/images/1617309522-Screen%20Shot%202015-05-27%20at%2016.18.42.png)

Here's the result of parsing "the boy wants to go" with this grammar:

![Screen Shot 2015-05-27 at 16.23.49.png](https://bitbucket.org/repo/ny94Mo/images/1198790576-Screen%20Shot%202015-05-27%20at%2016.23.49.png)


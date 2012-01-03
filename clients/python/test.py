from irtgclient import IrtgConnection

conn = IrtgConnection("localhost", 2001)

result = conn.command('i = irtg("interpretation english: de.saar.penguin.irtg.algebra.StringAlgebra r7 -> NP [english] john r8(NP) -> S! [english] *(?1, foo)")')
print '>' + result + '<'

result = conn.command('i.automaton()')
print '>' + result + '<'

result = conn.command('i = irtg(')  # syntax error raises an exception

conn.close()


import socket

output_end_marker = "---"

class IrtgConnection:
    def __init__(self, hostname, port):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setblocking(True)
        self.sock.connect((hostname, port))
        self.f = self.sock.makefile()
        
    def command(self, cmd):
        firstline = True
        result = ''
        line = ''
        
        self.f.write(cmd + "\n")
        self.f.flush()
        
        while True:
            line = self.f.readline().rstrip('\n')
            if line == output_end_marker:
                return result
            else:
                if firstline:
                    result = line
                    firstline = False
                else:
                    result += "\n" + line

    def close(self):
        self.sock.close()


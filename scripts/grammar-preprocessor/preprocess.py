from jinja2 import *
import sys

class _Counter(object):
  def __init__(self, start_value=1):
    self.value=start_value

  def current(self):
    return self.value

  def next(self):
    v=self.value
    self.value+=1
    return v

env = Environment(loader=FileSystemLoader("."))
env.globals['counter'] = _Counter

template = env.get_template(sys.argv[1])
print(template.render())



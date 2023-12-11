import logging
import ray
import math
import random
from fractions import Fraction

if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)


@ray.remote
class PiComputing:
    def __init__(self):
        self.candidates = []

    def add(self, value: int):
        self.candidates.append(value)

    def get(self):
        if len(self.candidates) > 0:
            return sum(self.candidates) * 4 / len(self.candidates)
        return 0


@ray.remote
def pi4_sample(actor: PiComputing, sample_count: int):
    in_count = 0
    for i in range(sample_count):
        x = random.random()
        y = random.random()
        if x * x + y * y <= 1:
            in_count += 1

    fraction = Fraction(in_count, sample_count)
    actor.add.remote(fraction)
    return fraction


actor_x = PiComputing.remote()
pi4s = [pi4_sample.remote(actor_x, 1_000_000) for _ in range(100)]

while True:
    res = ray.wait(pi4s, num_returns=len(pi4s), timeout=1)
    print(str((len(res[0]) / 100) * 100) + '%')
    if len(res[1]) == 0:
        break

pi = float(ray.get(actor_x.get.remote()))
print(pi)
print(abs(pi - math.pi)/pi)

ray.shutdown()

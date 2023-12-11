import logging
import time
import ray
import random

if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)

CALLERS = ["A", "B", "C"]


@ray.remote
class MethodStateCounter:
    def __init__(self):
        self.invokers = {"A": 0, "B": 0, "C": 0}
        self.values = {"A": [], "B": [], "C": []}

    def invoke(self, name):
        time.sleep(0.5)
        self.invokers[name] += 1
        self.values[name].append(7)
        return self.invokers[name]

    def get_invoker_state(self, name):
        return self.invokers[name]

    def get_all_invoker_state(self):
        return self.invokers

    def get_invoker_values(self, name):
        return self.values[name]

    def get_all_invoker_values(self):
        return self.values


worker_invoker = MethodStateCounter.remote()
print(worker_invoker)

print('method callers')
for _ in range(10):
    random_name_invoker = random.choice(CALLERS)
    times_invoked = ray.get(worker_invoker.invoke.remote(random_name_invoker))
    print(f"Caller: {random_name_invoker} called {times_invoked} time")

print(ray.get(worker_invoker.get_all_invoker_state.remote()))

print(ray.get(worker_invoker.get_all_invoker_values.remote()))

ray.shutdown()

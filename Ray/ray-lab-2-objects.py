import logging
import ray

if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)


@ray.remote
def process(data):
    x = 0
    for i in data:
        x = (x + i) % 172
    return x


lists = [[i for i in range(1_000_000)] for _ in range(20)]
dicts = [{i * 2: i * 3 for i in range(1_000_000)} for _ in range(20)]
lists_ray = [ray.put(i) for i in lists]
dicts_ray = [ray.put(i) for i in dicts]
lists_result = [process.remote(i) for i in lists_ray]
dicts_result = [process.remote(i) for i in dicts_ray]

print(ray.get(lists_result))
print(ray.get(dicts_result))

ray.shutdown()

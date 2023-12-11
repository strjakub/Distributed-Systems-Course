import logging
import ray
import cProfile

if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)

def bubble_sort(array):
    n = len(array)
    for i in range(n):
        for j in range(n - i - 1):
            if array[j] > array[j + 1]:
                array[j], array[j + 1] = array[j + 1], array[j]
    return array


def local_bubble_sort(array):
    bubble_sort(array)


@ray.remote
def remote_bubble_sort(array):
    bubble_sort(array)


def run_local():
    results = [local_bubble_sort([i for i in range(1000, 0, -1)]) for _ in range(100)]
    return results


def run_remote():
    results = ray.get([remote_bubble_sort.remote([i for i in range(1000, 0, -1)]) for _ in range(100)])
    return results


print('local')
cProfile.run("run_local()")

print('remote')
cProfile.run("run_remote()")

ray.shutdown()

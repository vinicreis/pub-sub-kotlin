from core.client import Client

if __name__ == '__main__':
    client = Client('localhost', 10090)

    for queue in client.list():
        print(queue)

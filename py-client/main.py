import argparse

from core.grpc.client import Client

parser = argparse.ArgumentParser(
    prog="Pub-sub client",
    description="Client to interact with the Pub-sub server",
)

parser.add_argument("-a", "--address", type=str, default="localhost", help="Server address")
parser.add_argument("-p", "--port", type=int, default="10090", help="Server port")

if __name__ == '__main__':
    args = parser.parse_args()
    address = args.address
    port = args.port
    print(f"Connecting to server on {address}:{port}...")

    client = Client('localhost', 10090)

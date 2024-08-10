import argparse
import traceback

from grpc import RpcError
from grpc.beta.interfaces import StatusCode

from core.domain.queue import Queue
from core.domain.text_message import TextMessage
from core.grpc.client_grpc import ClientGrpc
from core.service.model.response import Response
from ui.cli.component.components import select_from_list, read_int_or_none, select_queue, read_multiple_texts
from ui.cli.menu.menu import MenuOption

DEFAULT_SERVER_ADDRESS = "localhost"
DEFAULT_SERVER_PORT = 10090

parser = argparse.ArgumentParser(
    prog="Pub-sub client",
    description="Client to interact with the Pub-sub server",
)

parser.add_argument("-a", "--address", type=str, required=False, help="Server address")
parser.add_argument("-p", "--port", type=int, required=False, help="Server port")

if __name__ == '__main__':
    args = parser.parse_args()
    address = args.address if args.address is not None else input(f"Enter the server address [{DEFAULT_SERVER_ADDRESS}]: ")
    address = address if address else DEFAULT_SERVER_ADDRESS

    port = args.port if args.port is not None else input(f"Enter the server port [{DEFAULT_SERVER_PORT}]: ")
    port = port if port else DEFAULT_SERVER_PORT

    print(f"Connecting to server on {address}:{port}...")

    client = ClientGrpc(address, port)

    while True:
        try:
            selected_option = select_from_list(list(MenuOption), MenuOption.EXIT.value.numerator)

            if selected_option == MenuOption.LIST_QUEUES:
                response = client.list()
                if response.result == Response.Result.SUCCESS:
                    print("Queues:")
                    for queue in response.data:
                        print(queue)
                else:
                    print(f"Failed to list queues: {response.error}")
            elif selected_option == MenuOption.PUBLISH_QUEUE:
                code = input("Enter the queue code: ")
                name = input("Enter the queue readable name: ")
                queue_type = select_from_list(list(Queue.Type), message="Select a queue type")
                response = client.publish(Queue(code=code, name=name, queue_type=queue_type))
            elif selected_option == MenuOption.POST_MESSAGE:
                queue = select_queue(client)
                text_messages = list(map(TextMessage, read_multiple_texts("Enter the message content: ")))
                response = client.post(queue, text_messages)

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print("Message posted!")
            elif selected_option == MenuOption.POLL_QUEUE:
                queue = select_queue(client)
                timeout_seconds = read_int_or_none("Enter a timeout in seconds [None]: ")
                response = client.poll(queue, timeout_seconds)

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print(f"Polled message: {response.data}")
            elif selected_option == MenuOption.SUBSCRIBE_QUEUE:
                selected_queue = select_queue(client)
                timeout_seconds = read_int_or_none("Enter a timeout in seconds [None]: ")

                try:
                    print("Press Ctrl+C to stop the subscription")

                    for subscription_event in client.subscribe(selected_queue, timeout_seconds):
                        print(subscription_event)
                except KeyboardInterrupt:
                    print("Subscription cancelled by user")
                    continue
                except RpcError as e:
                    if e.code() == StatusCode.DEADLINE_EXCEEDED:
                        print("Subscription timed out")
                    else:
                        print(f"Subscription closed by server: {e}")

                    continue
            elif selected_option == MenuOption.REMOVE_QUEUE:
                queue = select_queue(client)
                response = client.remove(queue)

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print(f"Removed queue: {response.data}")
            elif selected_option == MenuOption.EXIT:
                print("Exiting...")
                exit(0)
            else:
                print(f"Unknown option read: {selected_option}")
                print("Exiting...")
                exit(-1)
        except KeyboardInterrupt as e:
            print("\nExiting...")
            exit(0)
        except ValueError as e:
            print(f"Failed to process the selected option: {e}")
        except Exception as e:
            print(f"Unknown error: {e}")
            traceback.print_exc()
            exit(-1)

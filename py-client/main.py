import argparse
import traceback

from core.domain.queue import Queue
from core.domain.text_message import TextMessage
from core.grpc.client_grpc import ClientGrpc
from core.service.client import Client
from core.service.model.response import Response
from ui.cli.component.components import select_from_list, read_text, read_int_or_none
from ui.cli.menu.menu import MenuOption

parser = argparse.ArgumentParser(
    prog="Pub-sub client",
    description="Client to interact with the Pub-sub server",
)

parser.add_argument("-a", "--address", type=str, default="localhost", help="Server address")
parser.add_argument("-p", "--port", type=int, default="10090", help="Server port")


def select_queue(client: Client) -> Queue | None:
    list_response = client.list()
    if list_response.result == Response.Result.FAIL:
        print(f"Failed to list queues: {list_response.error}")
        return None

    return select_from_list(list_response.data)

if __name__ == '__main__':
    args = parser.parse_args()
    address = args.address
    port = args.port
    print(f"Connecting to server on {address}:{port}...")

    client = ClientGrpc(address, port)

    while True:
        try:
            selectedOption = select_from_list(list(MenuOption), MenuOption.EXIT.value.numerator)

            if selectedOption == MenuOption.LIST_QUEUES:
                response = client.list()
                if response.result == Response.Result.SUCCESS:
                    print("Queues:")
                    for queue in response.data:
                        print(queue)
                else:
                    print(f"Failed to list queues: {response.error}")
            elif selectedOption == MenuOption.PUBLISH_QUEUE:
                code = input("Enter the queue code: ")
                name = input("Enter the queue readable name: ")
                queue_type = select_from_list(list(Queue.Type), message="Select a queue type")
                response = client.publish(Queue(code=code, name=name, queue_type=queue_type))
            elif selectedOption == MenuOption.POST_MESSAGE:
                queue = select_queue(client)
                text_message = TextMessage(read_text("Enter the message content: "))
                response = client.post(queue, [text_message])

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print("Message posted!")
            elif selectedOption == MenuOption.POLL_QUEUE:
                queue = select_queue(client)
                timeout_seconds = read_int_or_none("Enter a timeout in seconds [None]: ")
                response = client.poll(queue, timeout_seconds)

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print(f"Polled message: {response.data}")
            elif selectedOption == MenuOption.SUBSCRIBE_QUEUE:
                try:
                    print("Press Ctrl+C to stop the subscription")

                    for subscription_event in client.subscribe(select_queue(client)):
                        print(subscription_event)
                except KeyboardInterrupt:
                    print("Finoshing subscription...")
                    continue
            elif selectedOption == MenuOption.REMOVE_QUEUE:
                queue = select_queue(client)
                response = client.remove(queue)

                if response.result == Response.Result.FAIL:
                    print(f"Failed to post message: {response.error}")
                else:
                    print(f"Removed queue: {response.data}")
            elif selectedOption == MenuOption.EXIT:
                print("Exiting...")
                exit(0)
            else:
                print(f"Unknown option read: {selectedOption}")
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

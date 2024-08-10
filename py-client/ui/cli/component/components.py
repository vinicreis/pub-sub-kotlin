from core.domain.queue import Queue
from core.service.client import Client
from core.service.model.response import Response


def select_from_list[T](options: list[T], default_index: int = 0, message: str = "Select an option") -> T:
    if len(options) == 0:
        raise ValueError("No options to select from")

    print(message)

    for i, option in enumerate(options):
        print(f"\t{i + 1} - {option}")

    read_option = input("Selected: ")
    selected_option = int(read_option) - 1 if read_option else default_index

    if selected_option is None:
        return options[default_index]
    if selected_option < 0 or selected_option >= len(options):
        raise ValueError("Invalid option")

    print("Selected: ", options[selected_option])

    return options[selected_option]


def read_text(prompt: str) -> str:
    return input(prompt)


def read_multiple_texts(prompt: str) -> list[str]:
    texts = []
    while True:
        text = input(prompt)
        if not text:
            break

        texts.append(text)

    return texts


def read_int(prompt: str) -> int:
    return int(input(prompt))


def read_int_or_none(prompt: str) -> int | None:
    value = input(prompt)

    return int(value) if value else None


def select_queue(client: Client) -> Queue | None:
    list_response = client.list()
    if list_response.result == Response.Result.FAIL:
        print(f"Failed to list queues: {list_response.error}")
        return None

    return select_from_list(list_response.data)

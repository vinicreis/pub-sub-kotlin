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


def read_int(prompt: str) -> int:
    return int(input(prompt))


def read_int_or_none(prompt: str) -> int | None:
    value = input(prompt)

    return int(value) if value else None

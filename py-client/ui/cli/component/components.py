def select_from_list[T](options: list[T], default_index: int = 0) -> T:
    if len(options) == 0:
        raise ValueError("No options to select from")

    print("Select an option:")

    for i, option in enumerate(options):
        print(f"{i + 1} - {option}")

    selected_option = int(input("Selected: ")) - 1

    if selected_option < 0 or selected_option >= len(options):
        raise ValueError("Invalid option")
    if selected_option >= len(options):
        return options[default_index]


def read_text(prompt: str) -> str:
    return input(prompt)


def read_int(prompt: str) -> int:
    return int(input(prompt))


def read_int_or_none(prompt: str) -> int | None:
    value = input(prompt)

    return value if value else None

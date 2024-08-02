def select_from_list[T](options: list[T], default_index: int = 0) -> T:
    print("Select an option:")

    for i, option in enumerate(options):
        print(f"{i + 1} - {option}")

    selected_option = int(input("Selected: ")) - 1

    if selected_option < 0 or selected_option >= len(options):
        raise ValueError("Invalid option")
    if selected_option >= len(options):
        return options[default_index]

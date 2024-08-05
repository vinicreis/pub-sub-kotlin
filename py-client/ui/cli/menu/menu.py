from enum import Enum

from ui.cli.component.components import select_from_list


class MenuOption(Enum):
    LIST_QUEUES = 0
    PUBLISH_QUEUE = 1
    POST_MESSAGE = 2
    POLL_QUEUE = 3
    SUBSCRIBE_QUEUE = 4
    REMOVE_QUEUE = 5
    EXIT = 6


def read_menu_option() -> MenuOption:
    return select_from_list(list(MenuOption))

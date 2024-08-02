from enum import Enum

from ui.cli.component.components import select_from_list


class MenuOption(Enum):
    LIST_QUEUES = 1
    PUBLISH_QUEUE = 2
    POST_MESSAGE = 3
    POLL_QUEUE = 4
    SUBSCRIBE_QUEUE = 5
    REMOVE_QUEUE = 6
    EXIT = 7


def read_menu_option() -> MenuOption:
    return select_from_list(list(MenuOption))

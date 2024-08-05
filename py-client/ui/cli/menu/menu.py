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

    def __str__(self):
        if self == MenuOption.LIST_QUEUES:
            return "List queues"

        elif self == MenuOption.PUBLISH_QUEUE:
            return "Publish a new queue"

        elif self == MenuOption.POST_MESSAGE:
            return "Post a message on a queue"

        elif self == MenuOption.POLL_QUEUE:
            return "Poll oldest message from queue"

        elif self == MenuOption.SUBSCRIBE_QUEUE:
            return "Subscribe on a queue"

        elif self == MenuOption.REMOVE_QUEUE:
            return "Remove/unpublish queue"

        elif self == MenuOption.EXIT:
            return "Leave program"


def read_menu_option() -> MenuOption:
    return select_from_list(list(MenuOption))

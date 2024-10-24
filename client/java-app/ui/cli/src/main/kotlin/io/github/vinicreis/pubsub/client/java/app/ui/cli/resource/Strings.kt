package io.github.vinicreis.pubsub.client.java.app.ui.cli.resource

import io.github.vinicreis.pubsub.client.core.model.Queue as DomainQueue

object StringResource {
    object ViewComponent {
        const val DIVIDER = "----------------------------------------"
    }

    object Error {
        const val GENERIC = "Oops! Something went wrong. Please try again"
    }

    object Queue {
        object Type {
            fun name(type: DomainQueue.Type) = when (type) {
                DomainQueue.Type.SIMPLE -> "Simple queue"
                DomainQueue.Type.MULTIPLE -> "Multiple queue"
            }
        }

        object Input {
            object Message {
                const val ENTER_CODE = "Enter a code for queue"
                const val ENTER_NAME = "Enter queue readable name"
                const val ENTER_TIMEOUT_SECONDS = "Enter the timeout in seconds"
                const val SELECT_QUEUE_TYPE = "Select queue type"
                const val SELECT_AVAILABLE_QUEUES = "Select one of the available queues"
            }

            object Validation {
                const val EMPTY_CODE = "Queue code cannot be empty"
                const val INVALID_TYPE = "Select a valid queue type"
            }
        }

        object Message {
            const val PROCESSING_SUBSCRIPTION = "Processing subscription..."
            const val SUBSCRIPTION_ACTIVE = "Subscription on queue %s is active!"
            const val MESSAGE_RECEIVED = "Message received on queue %s: \"%s\""
            const val SUBSCRIPTION_FINISHED = "Subscription has finished: %s"
            const val QUEUE_REMOVED_SUCCESSFULLY = "Queue removed successfully"
        }
    }

    object Message {
        object Input {
            const val ENTER_MULTIPLE_MESSAGES = "Enter multiple messages and leave it blank to finish"
            const val ENTER_CONTENT = "Enter message content"
            const val PRESS_ENTER_TO_STOP_SUBSCRIPTION = "Press Enter key to stop subscription..."
        }

        object Validation {
            const val EMPTY_CONTENT = "Message content cannot be empty"
        }
    }

    object Operation {
        object List {
            object Error {
                const val GENERIC = "Oops! Something while listing queues. Please try again"
            }
        }
    }
}

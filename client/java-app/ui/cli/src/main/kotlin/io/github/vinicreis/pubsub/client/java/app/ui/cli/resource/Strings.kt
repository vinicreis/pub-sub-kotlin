package io.github.vinicreis.pubsub.client.java.app.ui.cli.resource

import io.github.vinicreis.pubsub.client.core.model.Channel as DomainChannel

object StringResource {
    object ViewComponent {
        const val DIVIDER = "----------------------------------------"
    }

    object Error {
        const val GENERIC = "Oops! Something went wrong. Please try again"
    }

    object Channel {
        object Type {
            fun name(type: DomainChannel.Type) = when (type) {
                DomainChannel.Type.SIMPLE -> "Simple channel"
                DomainChannel.Type.MULTIPLE -> "Multiple channel"
            }

            fun description(type: DomainChannel.Type) = when (type) {
                DomainChannel.Type.SIMPLE -> "Channel that delivers messages to only one subscriber"
                DomainChannel.Type.MULTIPLE -> "Channel that delivers messages to all subscribers"
            }
        }

        object Input {
            object Message {
                const val ENTER_CODE = "Enter a code for channel"
                const val ENTER_NAME = "Enter channel readable name"
                const val SELECT_CHANNEL_TYPE = "Select channel type"
                const val SELECT_AVAILABLE_CHANNELS = "Select one of the available channels"
            }

            object Validation {
                const val EMPTY_CODE = "Channel code cannot be empty"
                const val INVALID_TYPE = "Select a valid channel type"
            }
        }

        object Message {
            const val PROCESSING_SUBSCRIPTION = "Processing subscription..."
            const val SUBSCRIPTION_ACTIVE = "Subscription on channel %s is active!"
            const val MESSAGE_RECEIVED = "Message received on channel %s: %s"
            const val SUBSCRIPTION_FINISHED = "Subscription on channel %s has finished: %s"
            const val CHANNEL_REMOVED_SUCCESSFULLY = "Channel removed successfully"
        }
    }

    object Message {
        object Input {
            const val ENTER_CONTENT = "Enter message content"
        }

        object Validation {
            const val EMPTY_CONTENT = "Message content cannot be empty"
        }
    }

    object Operation {
        object List {
            object Error {
                const val GENERIC = "Oops! Something while listing channels. Please try again"
            }
        }
    }
}
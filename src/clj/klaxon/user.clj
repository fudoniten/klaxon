(ns klaxon.user
  ;; This namespace defines user-related specifications.
  (:require [clojure.spec.alpha :as s]))

(s/def ::user (s/keys :req [::id]))
# Klaxon Trade Alerts

Klaxon is a trade alert system designed to monitor and notify users of significant trading events on the Coinbase platform. It leverages the Coinbase API to fetch order data and uses ntfy for sending notifications.

## Features

- Monitors orders for specific conditions such as delayed fills and large trades.
- Sends notifications via ntfy to alert users of important events.
- Configurable polling intervals and notification settings.

## Installation

To install Klaxon, ensure you have Nix installed on your system. Then, use the following command to build the project:

```bash
nix build .#klaxon
```

## Configuration

Klaxon requires a configuration file in EDN format. The configuration should include the following keys:

- `:ntfy-server` - The ntfy server hostname.
- `:poll-seconds` - The frequency in seconds for polling order data.
- `:key-file` - Path to the JSON file containing Coinbase key data.

## Usage

To run Klaxon, use the following command:

```bash
nix run .#klaxon -- --key-file path/to/keyfile.json --ntfy-server ntfy.sh --ntfy-topic your-topic
```

## Development

For development, you can enter a Nix shell with all necessary dependencies using:

```bash
nix develop
```

## Testing

To run the test suite, use the following command:

```bash
nix flake check
```

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any bugs or feature requests.

## License

This project is licensed under the MIT License.

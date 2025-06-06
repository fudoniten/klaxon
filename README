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

## Configuration and Usage

Klaxon is configured via command-line arguments. The following options are available:

- `--key-file` - Path to the JSON file containing Coinbase key data.
- `--ntfy-server` - The ntfy server hostname (default: `ntfy.sh`).
- `--ntfy-topic` - The ntfy channel to which notifications will be sent.
- `--poll-seconds` - The frequency in seconds for polling order data.

To run Klaxon, use the following command:

```bash
nix run .#klaxon -- --key-file path/to/keyfile.json --ntfy-server ntfy.sh --ntfy-topic your-topic --poll-seconds 60
```

## Testing

To run the test suite, use the following command:

```bash
nix flake check
```

## License

This project is licensed under the MIT License.

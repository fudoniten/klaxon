packages:

{ config, lib, pkgs, ... }:

with lib;
let
  cfg = config.fudo.services.klaxon;

  inherit (packages."${pkgs.system}") klaxon;

in {
  options.fudo.services.klaxon = with types; {
    enable = mkEnableOption "Enable or disable the Klaxon server. When enabled, the server will start as a systemd service.";

    keyFile = mkOption {
      type = str;
      description = "Path to the JSON file containing Coinbase key data. This file is used for authenticating API requests.";
    };

    ntfy = {
      server = mkOption {
        type = str;
        description = "Hostname of the ntfy server used for sending notifications. Default is 'ntfy.sh'.";
        default = "ntfy.sh";
      };

      topic = mkOption {
        type = str;
        description = "The ntfy channel to which notifications will be sent. This should be a valid channel name on the ntfy server.";
      };

      tokenFile = mkOption {
        type = nullOr str;
        description = "File holding an ntfy access token (tk_...), for a server that doesn't allow anonymous publishing. Read at start-up, never copied to the store.";
        default = null;
      };
    };
  };

  config = mkIf cfg.enable {
    systemd = {
      services.klaxon = {
        path = [ klaxon ];
        wantedBy = [ "multi-user.target" ];
        after = [ "network-online.target" ];
        requires = [ "network-online.target" ];
        serviceConfig = {
          DynamicUser = true;
          Restart = "on-failure";
          RestartSec = "120s";
          LoadCredential = [ "key.json:${cfg.keyFile}" ]
            ++ optional (cfg.ntfy.tokenFile != null) "ntfy-token:${cfg.ntfy.tokenFile}";
          ExecStart = pkgs.writeShellScript "klaxon-launch.sh"
            (concatStringsSep " " ([
              "klaxon"
              "--ntfy-server=${cfg.ntfy.server}"
              "--ntfy-topic=${cfg.ntfy.topic}"
              "--key-file=$CREDENTIALS_DIRECTORY/key.json"
            ] ++ optional (cfg.ntfy.tokenFile != null)
              "--ntfy-token-file=$CREDENTIALS_DIRECTORY/ntfy-token"));
        };
        unitConfig.ConditionPathExists = [ cfg.keyFile ];
      };
    };
  };
}

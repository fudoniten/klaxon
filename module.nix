packages:

{ config, lib, pkgs, ... }:

with lib;
let
  cfg = config.fudo.services.klaxon;

  inherit (packages."${pkgs.system}") klaxon;

in {
  options.fudo.services.klaxon = with types; {
    enable = mkEnableOption "Enable Klaxon server.";

    keyFile = mkOption {
      type = str;
      description = "File containing Coinbase key data in JSON format.";
    };

    ntfy = {
      server = mkOption {
        type = str;
        description = "ntfy server hostname.";
        default = "ntfy.sh";
      };

      topic = mkOption {
        type = str;
        description = "ntfy channel to which notifications will be sent.";
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
          LoadCredential = [ "key.json:${cfg.keyFile}" ];
          ExecStart = pkgs.writeShellScript "klaxon-launch.sh"
            (concatStringsSep " " ([
              "klaxon"
              "--ntfy-server=${cfg.ntfy.server}"
              "--ntfy-topic=${cfg.ntfy.topic}"
              "--key-file=$CREDENTIALS_DIRECTORY/key.json"
            ]));
        };
        unitConfig.ConditionPathExists = [ cfg.keyFile ];
      };
    };
  };
}

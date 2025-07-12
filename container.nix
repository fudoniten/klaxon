{ lib, nixosSystem, klaxon, system, writeShellScript, ... }:

with lib;
let
  klaxonSystem = { pkgs, ... }: {
    systemd.services.klaxon = {
      description = "Klaxon Trade Monitor Daemon";
      wantedBy = [ "multi-user.target" ];
      serviceConfig = {
        ExecStart = writeShellScript "run-klaxon.sh" (concatStringsSep " " [
          "${klaxon}/bin/klaxon"
          "--ntfy-server=$NTFY_SERVER"
          "--ntfy-topic=$NTFY_TOPIC"
          "--key-file=/etc/klaxon/key.json"
        ]);
        Restart = "always";
      };
    };

    networking.firewall.enable = false;
    system.stateVersion = "25.05";
  };

in nixosSystem {
  inherit system;
  modules = [ klaxonSystem ];
}

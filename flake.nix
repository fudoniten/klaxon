{
  description = "Klaxon Trade Alerts";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-25.11";
    utils.url = "github:numtide/flake-utils";
    nix-helpers = {
      url = "github:fudoniten/fudo-nix-helpers";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    fudo-clojure = {
      url = "github:fudoniten/fudo-clojure";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    pinger = {
      url = "github:fudoniten/pinger";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, nix-helpers, fudo-clojure, pinger, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages."${system}";
        fudoClojureLib = fudo-clojure.packages."${system}".fudo-clojure.preppedSrc;
        pingerLib = pinger.packages."${system}".pinger;
        cljLibs = {
          "org.fudo/fudo-clojure" = fudoClojureLib.preppedSrc;
          "org.fudo/pinger" = pingerLib.preppedSrc;
        };
        helpers = nix-helpers.legacyPackages."${system}";

      in {
        packages = rec {
          default = klaxon;

          klaxon = helpers.mkClojureBin {
            name = "org.fudo/klaxon";
            primaryNamespace = "klaxon.cli.core";
            src = ./.;
            inherit cljLibs;
          };

          deployContainers = helpers.deployContainers {
            name = "klaxon";
            repo = "registry.kube.sea.fudo.link";
            tags = [ "latest" ];
            entrypoint = [ "${klaxon}/bin/klaxon" ];
            env = [ "NTFY_SERVER" "NTFY_TOPIC" ];
            verbose = true;
          };
        };

        checks = {
          clojureTests = helpers.mkClojureTests {
            name = "org.fudo/klaxon";
            src = ./.;
            inherit cljLibs;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = [ (helpers.updateClojureDeps { deps = cljLibs; }) ];
          };
          klaxonServer = pkgs.mkShell {
            buildInputs = with self.packages."${system}"; [ klaxon ];
          };
        };

        apps = rec {
          default = deployContainers;
          deployContainers = {
            type = "app";
            program =
              let deployContainers = self.packages."${system}".deployContainers;
              in "${deployContainers}/bin/deployContainers";
          };
        };
      }) // {
        nixosModules = rec {
          default = klaxonServer;
          klaxonServer = import ./module.nix self.packages;
        };
      };
}

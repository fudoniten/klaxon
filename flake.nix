{
  description = "Klaxon Trade Alerts";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-24.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
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
    nix2container.url = "github:nlewo/nix2container";
  };

  outputs =
    { self, nixpkgs, utils, helpers, fudo-clojure, pinger, nix2container, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages."${system}";
        fudoClojureLib = fudo-clojure.packages."${system}".fudo-clojure;
        pingerLib = pinger.packages."${system}".pinger;
        cljLibs = {
          "org.fudo/fudo-clojure" = fudoClojureLib;
          "org.fudo/pinger" = pingerLib;
        };
      in {
        packages = rec {
          default = klaxon;

          klaxon = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/klaxon";
            primaryNamespace = "klaxon.cli.core";
            src = ./.;
            inherit cljLibs;
          };

          klaxonContainer =
            let containerPkgs = nix2container.packages."${system}";
            in containerPkgs.nix2container.buildImage {
              name = "klaxon";
              config = {
                entrypoint = [ "${klaxon}/bin/klaxon" ];
                env = [ "NFTY_SERVER" "NTFY_TOPIC" ];
              };
            };
        };

        checks = {
          clojureTests = pkgs.runCommand "clojure-tests" { } ''
            mkdir -p $TMPDIR
            cd $TMPDIR
            ${pkgs.clojure}/bin/clojure -M:test
          '';
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ (updateClojureDeps cljLibs) ];
          };
          klaxonServer = pkgs.mkShell {
            buildInputs = with self.packages."${system}"; [ klaxon ];
          };
        };
      }) // {
        nixosModules = rec {
          default = klaxonServer;
          klaxonServer = import ./module.nix self.packages;
        };
      };
}

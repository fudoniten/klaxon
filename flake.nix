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

          klaxonContainer = let
            # nix2containerPkgs = nix2container.packages."${system}";
            klaxonImage = pkgs.callPackage ./container.nix {
              inherit (nixpkgs.lib) nixosSystem;
              inherit system klaxon;
            };
          in klaxonImage; # .config.system.build.ociImage;

          deployContainer = pkgs.writeShellApplication {
            name = "deploy-container";
            runtimeInputs = with pkgs; [ scopeo ];
            text = ''
              set -euo pipefail

              if [ "$#" -ne 2 ]; then
                echo "usage: deploy-container <registry>/<image> <tag>" >&2
                exit 1
              fi

              REGISTRY="$1"
              TAG="$2"

              echo "pushing container image $REGISTRY..."
              skopeo copy oci:"${klaxonContainer}" docker://$IMAGE:$TAG
              if [ $? -eq 0 ]; then
                echo "done."
                exit 0
              else
                echo "FAILED"
                exit 1
              fi
            '';
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

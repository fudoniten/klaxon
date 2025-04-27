{
  description = "Klaxon Trade Alerts";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-24.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "github:fudoniten/fudo-nix-helpers";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    milquetoast = {
      url = "github:fudoniten/milquetoast";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    fudo-clojure = {
      url = "github:fudoniten/fudo-clojure";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, fudo-clojure, milquetoast, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages."${system}";
        fudoClojureLib = fudo-clojure.packages."${system}".fudo-clojure;
        milquetoastLib = milquetoast.packages."${system}".milquetoast;
        cljLibs = {
          "org.fudo/fudo-clojure" = fudoClojureLib;
          "org.fudo/milquetoast" = milquetoastLib;
        };
      in {
        packages = rec {
          default = klaxonServer;
          klaxonServer = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/klaxon-server";
            primaryNamespace = "klaxon.cli";
            src = ./.;
            inherit cljLibs;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ (updateClojureDeps cljLibs) ];
          };
          klaxonServer = pkgs.mkShell {
            buildInputs = with self.packages."${system}"; [ klaxonServer ];
          };
        };
      }) // {
        nixosModules = rec {
          default = klaxonServer;
          klaxonServer = import ./module.nix self.packages;
        };
      };
}

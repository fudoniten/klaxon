{ pkgs, klaxon }:

pkgs.dockerTools.buildImage {
  name = "klaxon";
  tag = "latest";
  contents = [ klaxon ];
  config = {
    Cmd = [ "klaxon" ];
  };
}

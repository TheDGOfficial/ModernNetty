{
  description = "cutie flake ~";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    gitignore = { url = "github:hercules-ci/gitignore.nix"; flake = false; };
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = inputs@{ self, nixpkgs, flake-utils, ... }:
  flake-utils.lib.eachSystem [ "x86_64-linux" ] (system: let
    # overlays = [ (import rustup) ];
    pkgs = import nixpkgs { inherit system; }; #overlays; };
    libPath = with pkgs; lib.makeLibraryPath [
      libGL libxkbcommon
      wayland wayland-utils
    ];
    gitignoreSrc = pkgs.callPackage inputs.gitignore { };
  in rec {
    packages.hello = pkgs.callPackage ./default.nix { inherit gitignoreSrc; };
    legacyPackages = packages;
    defaultPackage = packages.hello;
    devShell = pkgs.mkShell {
      buildInputs = with pkgs; [];

      CARGO_INSTALL_ROOT = "${toString ./.}/.cargo";
      RUST_SRC_PATH = "${pkgs.rustPlatform.rustLibSrc}";
      LD_LIBRARY_PATH = libPath;
    };
  });
}

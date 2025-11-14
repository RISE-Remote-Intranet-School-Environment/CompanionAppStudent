{
  description = "Companion App Backend";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };

  outputs =
    {
      self,
      nixpkgs,
      gradle2nix,
      ...
    }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

      # Define the package using gradle2nix's builder
      companion-backend-pkg = gradle2nix.builders.${system}.buildGradlePackage {
        pname = "companion-backend";
        version = "0.1.0"; # Set a version for your package
        src = ./.;
        lockFile = ./gradle.lock;
        gradleBuildTasks = [ ":server:shadowJar" ];
        # The installPhase is now handled by gradle2nix's setup hook
      };
    in
    {
      packages.${system}.default = pkgs.callPackage ./server/default.nix {
        buildGradlePackage = companion-backend-pkg;
      };

      nixosModules.default =
        { config, lib, ... }:
        with lib;
        let
          cfg = config.services.companion-backend;
        in
        {
          options.services.companion-backend = {
            enable = mkEnableOption "Companion App Backend";
            port = mkOption {
              type = types.int;
              default = 8080;
            };
          };

          config = mkIf cfg.enable {
            systemd.services.companion-backend = {
              wantedBy = [ "multi-user.target" ];
              serviceConfig = {
                ExecStart = "${self.packages.${system}.default}/bin/server";
                WorkingDirectory = "/var/lib/companion-backend";
                User = "companion-backend";
                Group = "companion-backend";
              };
              environment = {
                PORT = toString cfg.port;
              };
            };

            users.users.companion-backend = {
              isSystemUser = true;
              group = "companion-backend";
            };
            users.groups.companion-backend = { };
          };
        };
    };
}

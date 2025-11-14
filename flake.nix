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

      # Package direct avec buildGradlePackage
      companion-backend-pkg = gradle2nix.builders.${system}.buildGradlePackage {
        pname = "companion-backend";
        version = "0.1.0";
        src = ./.;
        lockFile = ./gradle.lock;
        gradleBuildFlags = [ ":server:shadowJar" ];

        # Ajoutez cette phase pour installer le JAR
        installPhase = ''
          mkdir -p $out/share/java
          cp server/build/libs/*.jar $out/share/java/
        '';
      };
    in
    {
      # The final, runnable package that includes the wrapper script.
      # This is what `nix build .#` and `nix run .#` will target.
      packages.${system}.default = pkgs.callPackage ./server/default.nix {
        buildGradlePackage = companion-backend-pkg;
      };

      # Module NixOS ajusté pour lancer manuellement le JAR
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
            user = mkOption {
              type = types.str;
              default = "companion-backend";
            };
            group = mkOption {
              type = types.str;
              default = "companion-backend";
            };
          };

          config = mkIf cfg.enable {
            systemd.services.companion-backend = {
              wantedBy = [ "multi-user.target" ];
              serviceConfig = {
                ExecStart = "${companion-backend-pkg}/bin/server";
                WorkingDirectory = "/var/lib/${cfg.user}";
                User = cfg.user;
                Group = cfg.group;
              };
              environment = {
                PORT = toString cfg.port;
              };
            };

            users.users.${cfg.user} = {
              isSystemUser = true;
              group = cfg.group;
              home = "/var/lib/${cfg.user}";
              createHome = true;
            };
            users.groups.${cfg.group} = { };
          };
        };
    };
}

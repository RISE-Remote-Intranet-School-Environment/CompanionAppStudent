{
  description = "Companion App Backend + Frontend";

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

      # Construction du JAR backend avec gradle2nix
      companion-backend-pkg = gradle2nix.builders.${system}.buildGradlePackage {
        pname = "companion-backend";
        version = "0.1.0";
        src = ./.;
        lockFile = ./gradle.lock;
        gradleBuildFlags = [ ":server:shadowJar" ];
        installPhase = ''
          mkdir -p $out/share/java
          cp server/build/libs/*.jar $out/share/java/
        '';
      };

      # 🔥 Frontend WASM - Fichiers statiques pré-construits
      # Les fichiers doivent être construits localement avec:
      # ./gradlew :composeApp:wasmJsBrowserDistribution
      companion-wasm-pkg = pkgs.stdenv.mkDerivation {
        pname = "companion-wasm";
        version = "0.1.0";

        # Copier uniquement le dossier des fichiers WASM construits
        src = ./composeApp/build/dist/wasmJs/productionExecutable;

        installPhase = ''
          mkdir -p $out/share/www
          cp -r $src/* $out/share/www/
        '';
      };
    in
    {
      packages.${system} = {
        default = pkgs.callPackage ./server/default.nix {
          buildGradlePackage = companion-backend-pkg;
        };

        # 🔥 Paquet WASM séparé
        wasm = companion-wasm-pkg;

        # 🔥 NOUVEAU : Paquet combiné backend + frontend
        full = pkgs.symlinkJoin {
          name = "companion-full";
          paths = [
            (pkgs.callPackage ./server/default.nix { buildGradlePackage = companion-backend-pkg; })
          ];
          postBuild = ''
            mkdir -p $out/share/www
            cp -r ${companion-wasm-pkg}/share/www/* $out/share/www/
          '';
        };
      };

      # Module NixOS
      nixosModules.default =
        {
          config,
          lib,
          pkgs,
          ...
        }:
        with lib;
        let
          cfg = config.services.companion-app;
        in
        {
          options.services.companion-app = {
            enable = mkEnableOption "Companion App (Backend + Frontend)";

            port = mkOption {
              type = types.int;
              default = 28088;
              description = "Port for the backend API";
            };

            domain = mkOption {
              type = types.str;
              default = "clacoxygen.msrl.be";
              description = "Domain name for the app";
            };

            user = mkOption {
              type = types.str;
              default = "clacoxygen";
            };

            group = mkOption {
              type = types.str;
              default = "clacoxygen";
            };

            jwtSecretFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Path to JWT secret file";
            };

            msClientIdFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Path to Microsoft Client ID file";
            };

            msClientSecretFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Path to Microsoft Client Secret file";
            };
          };

          config = mkIf cfg.enable {
            # Utilisateur système
            users.users.${cfg.user} = {
              isSystemUser = true;
              group = cfg.group;
              home = "/var/lib/${cfg.user}";
              createHome = true;
            };
            users.groups.${cfg.group} = { };

            # Service systemd pour le backend
            systemd.services.companion-backend = {
              description = "Companion App Backend";
              after = [ "network.target" ];
              wantedBy = [ "multi-user.target" ];

              environment = {
                PORT = toString cfg.port;
              }
              // optionalAttrs (cfg.jwtSecretFile != null) {
                JWT_SECRET_FILE = cfg.jwtSecretFile;
              }
              // optionalAttrs (cfg.msClientIdFile != null) {
                MS_CLIENT_ID_FILE = cfg.msClientIdFile;
              }
              // optionalAttrs (cfg.msClientSecretFile != null) {
                MS_CLIENT_SECRET_FILE = cfg.msClientSecretFile;
              };

              serviceConfig = {
                ExecStart = "${self.packages.${pkgs.system}.default}/bin/companion-backend";
                User = cfg.user;
                Group = cfg.group;
                WorkingDirectory = "/var/lib/${cfg.user}";
                Restart = "always";

                # Hardening
                NoNewPrivileges = true;
                ProtectSystem = "strict";
                ReadWritePaths = [ "/var/lib/${cfg.user}" ];
                PrivateTmp = true;
                ProtectHome = "tmpfs";
                ProtectKernelTunables = true;
                ProtectControlGroups = true;
              };
            };

            # 🔥 Configuration Nginx pour servir le WASM + proxy API
            services.nginx.virtualHosts.${cfg.domain} = {
              enableACME = true;
              forceSSL = true;

              # Servir les fichiers statiques WASM
              root = "${self.packages.${pkgs.system}.wasm}/share/www";

              # Headers requis pour WASM
              extraConfig = ''
                add_header Cross-Origin-Opener-Policy "same-origin" always;
                add_header Cross-Origin-Embedder-Policy "require-corp" always;
              '';

              locations = {
                # SPA fallback pour le frontend
                "/" = {
                  tryFiles = "$uri $uri/ /index.html";
                  extraConfig = ''
                    # Cache pour les fichiers statiques
                    location ~* \.(wasm|js|mjs|css|png|jpg|svg|ico)$ {
                      add_header Cache-Control "public, max-age=31536000, immutable";
                      add_header Cross-Origin-Opener-Policy "same-origin" always;
                      add_header Cross-Origin-Embedder-Policy "require-corp" always;
                    }
                  '';
                };

                # Proxy vers le backend pour les API
                "/api" = {
                  proxyPass = "http://127.0.0.1:${toString cfg.port}";
                  proxyWebsockets = true;
                };

                # Route pour les callbacks OAuth
                "/auth-callback.html" = {
                  proxyPass = "http://127.0.0.1:${toString cfg.port}";
                };
              };
            };
          };
        };
    };
}

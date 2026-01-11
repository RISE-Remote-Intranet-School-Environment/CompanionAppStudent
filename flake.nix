{
  description = "ClacOxygen - ECAM Student Companion App";

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
      clacoxygen-backend-pkg = gradle2nix.builders.${system}.buildGradlePackage {
        pname = "clacoxygen-backend";
        version = "0.1.0";
        src = ./.;
        lockFile = ./gradle.lock;
        gradleBuildFlags = [ ":server:shadowJar" ];
        installPhase = ''
          mkdir -p $out/share/java
          cp server/build/libs/*.jar $out/share/java/
        '';
      };

      clacoxygen-wasm-pkg = pkgs.stdenv.mkDerivation {
        pname = "clacoxygen-wasm";
        version = "0.1.0";
        src = ./wasm-dist;
        installPhase = ''
          mkdir -p $out/share/www
          cp -r $src/* $out/share/www/
        '';
      };
    in
    {
      packages.${system} = {
        default = pkgs.callPackage ./server/default.nix {
          buildGradlePackage = clacoxygen-backend-pkg;
        };

        wasm = clacoxygen-wasm-pkg;

        full = pkgs.symlinkJoin {
          name = "clacoxygen-full";
          paths = [
            (pkgs.callPackage ./server/default.nix { buildGradlePackage = clacoxygen-backend-pkg; })
          ];
          postBuild = ''
            mkdir -p $out/share/www
            cp -r ${clacoxygen-wasm-pkg}/share/www/* $out/share/www/
          '';
        };
      };

      nixosModules.default =
        {
          config,
          lib,
          pkgs,
          ...
        }:
        with lib;
        let
          cfg = config.services.clacoxygen;
        in
        {
          options.services.clacoxygen = {
            enable = mkEnableOption "ClacOxygen - ECAM Student Companion App";

            port = mkOption {
              type = types.int;
              default = 28088;
              description = "Port interne pour l'API backend (non exposé publiquement)";
            };

            domain = mkOption {
              type = types.str;
              description = "Nom de domaine pour l'application (ex: clacoxygen.msrl.be)";
              example = "clacoxygen.msrl.be";
            };

            user = mkOption {
              type = types.str;
              default = "clacoxygen";
              description = "Utilisateur système pour le service";
            };

            group = mkOption {
              type = types.str;
              default = "clacoxygen";
              description = "Groupe système pour le service";
            };

            jwtSecretFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Chemin vers le fichier contenant le secret JWT";
            };

            msClientIdFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Chemin vers le fichier contenant le Client ID Microsoft";
            };

            msClientSecretFile = mkOption {
              type = types.nullOr types.path;
              default = null;
              description = "Chemin vers le fichier contenant le Client Secret Microsoft";
            };

            nginx = {
              enable = mkOption {
                type = types.bool;
                default = true;
                description = "Configurer automatiquement Nginx comme reverse proxy";
              };

              enableACME = mkOption {
                type = types.bool;
                default = true;
                description = "Activer Let's Encrypt pour HTTPS";
              };

              extraConfig = mkOption {
                type = types.lines;
                default = "";
                description = "Configuration Nginx supplémentaire";
              };
            };

            microsoftRedirectUri = mkOption {
              type = types.nullOr types.str;
              default = null;
              description = "URI de redirection Microsoft OAuth (défaut: https://<domain>/api/auth/microsoft/callback)";
            };
          };

          config = mkIf cfg.enable {

            # Assertions pour valider la configuration
            assertions = [
              {
                assertion = cfg.domain != "";
                message = "services.clacoxygen.domain doit être défini";
              }
              {
                assertion = cfg.jwtSecretFile != null;
                message = "services.clacoxygen.jwtSecretFile doit être défini pour la production";
              }
            ];

            users.users.${cfg.user} = {
              isSystemUser = true;
              group = cfg.group;
              home = "/var/lib/${cfg.user}";
              createHome = true;
            };
            users.groups.${cfg.group} = { };

            systemd.services.clacoxygen = {
              description = "Clacoxygen Backend API";
              after = [ "network.target" ];
              wantedBy = [ "multi-user.target" ];

              environment = {
                PORT = toString cfg.port;
                APP_DOMAIN = cfg.domain;
                APP_BASE_URL = "https://${cfg.domain}";
                MS_REDIRECT_URI =
                  if cfg.microsoftRedirectUri != null then
                    cfg.microsoftRedirectUri
                  else
                    "https://${cfg.domain}/api/auth/microsoft/callback";
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
                ExecStart = "${self.packages.${pkgs.system}.default}/bin/clacoxygen-backend";
                User = cfg.user;
                Group = cfg.group;
                WorkingDirectory = "/var/lib/${cfg.user}";
                Restart = "always";
                RestartSec = "5s";

                # Hardening
                NoNewPrivileges = true;
                ProtectSystem = "strict";
                ReadWritePaths = [ "/var/lib/${cfg.user}" ];
                PrivateTmp = true;
                ProtectHome = "tmpfs";
                ProtectKernelTunables = true;
                ProtectControlGroups = true;
                CapabilityBoundingSet = "";
                SystemCallFilter = [
                  "@system-service"
                  "~@privileged"
                ];
              };
            };

            # Configuration Nginx séparée et optionnelle
            services.nginx = mkIf cfg.nginx.enable {
              enable = true;

              recommendedProxySettings = true;
              recommendedTlsSettings = true;
              recommendedOptimisation = true;
              recommendedGzipSettings = true;

              virtualHosts.${cfg.domain} = {
                enableACME = cfg.nginx.enableACME;
                forceSSL = cfg.nginx.enableACME;

                root = "${self.packages.${pkgs.system}.wasm}/share/www";

                extraConfig = ''
                  # Headers requis pour WASM avec SharedArrayBuffer
                  add_header Cross-Origin-Opener-Policy "same-origin" always;
                  add_header Cross-Origin-Embedder-Policy "require-corp" always;
                  ${cfg.nginx.extraConfig}
                '';

                locations = {
                  "/" = {
                    tryFiles = "$uri $uri/ /index.html";
                    extraConfig = ''
                      # Cache long pour assets statiques
                      location ~* \.(wasm|js|mjs|css|png|jpg|jpeg|gif|svg|ico|woff|woff2)$ {
                        add_header Cache-Control "public, max-age=31536000, immutable";
                        add_header Cross-Origin-Opener-Policy "same-origin" always;
                        add_header Cross-Origin-Embedder-Policy "require-corp" always;
                      }
                    '';
                  };

                  # API Backend
                  "/api" = {
                    proxyPass = "http://127.0.0.1:${toString cfg.port}";
                    proxyWebsockets = true;
                    extraConfig = ''
                      proxy_read_timeout 300s;
                      proxy_connect_timeout 75s;
                    '';
                  };

                  # OAuth callback (servi par le backend)
                  "/auth-callback.html" = {
                    proxyPass = "http://127.0.0.1:${toString cfg.port}";
                  };

                  # Health check
                  "/health" = {
                    proxyPass = "http://127.0.0.1:${toString cfg.port}";
                    extraConfig = ''
                      access_log off;
                    '';
                  };
                };
              };
            };

            # ACME (Let's Encrypt)
            security.acme = mkIf (cfg.nginx.enable && cfg.nginx.enableACME) {
              acceptTerms = true;
              defaults.email = mkDefault "admin@${cfg.domain}";
            };
          };
        };
    };
}

{
  pkgs ? import <nixpkgs> { },
  buildGradlePackage,
}:

# Ce wrapper prend le JAR construit par gradle2nix et crée un script de lancement
pkgs.stdenv.mkDerivation (finalAttrs: {
  inherit (buildGradlePackage) pname version src;

  installPhase = ''
    mkdir -p $out/bin
    # Lien symbolique vers le JAR
    ln -s ${buildGradlePackage}/share/java/*.jar $out/bin/companion-backend.jar

    # Création du script de lancement
    cat > $out/bin/companion-backend <<EOF
    #!/bin/sh
    exec ${pkgs.jre}/bin/java -jar $out/bin/companion-backend.jar
    EOF

    chmod +x $out/bin/companion-backend
  '';
})

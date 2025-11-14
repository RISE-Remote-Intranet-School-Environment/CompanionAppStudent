{
  pkgs ? import <nixpkgs> { },
  buildGradlePackage,
}:

# This derivation now only wraps the final JAR produced by buildGradlePackage
# into a runnable script.
pkgs.stdenv.mkDerivation (finalAttrs: {
  inherit (buildGradlePackage) pname version src;

  installPhase = ''
    # The buildGradlePackage hook places the final JAR in a predictable location.
    mkdir -p $out/bin
    ln -s ${buildGradlePackage}/share/java/*.jar $out/bin/server.jar
    cat > $out/bin/server <<EOF
    #!/bin/sh
    exec ${pkgs.jre}/bin/java -jar $out/bin/server.jar
    EOF
    chmod +x $out/bin/server
  '';
})

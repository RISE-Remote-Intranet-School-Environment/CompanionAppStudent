{
  pkgs ? import <nixpkgs> { },
  buildGradlePackage,
}:

# This derivation now only wraps the final JAR produced by buildGradlePackage
# into a runnable script.
pkgs.stdenv.mkDerivation (finalAttrs: {
  inherit (buildGradlePackage) pname version src;

  installPhase = ''
    # The JAR is located in the `dist` directory created by buildGradlePackage
    # The output of buildGradlePackage is the directory containing the JAR.
    local jar_path=$(find ${buildGradlePackage} -name "server-*.jar")

    mkdir -p $out/bin
    cp $jar_path $out/bin/server.jar

    cat > $out/bin/server <<EOF
    #!/bin/sh
    exec ${pkgs.jre}/bin/java -jar $out/bin/server.jar
    EOF
    chmod +x $out/bin/server
  '';
})

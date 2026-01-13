{
  pkgs ? import <nixpkgs> { },
  buildGradlePackage,
  version ? "1.1.42",
}:

pkgs.stdenv.mkDerivation {
  pname = "clacoxygen-backend";
  inherit version;

  dontUnpack = true;

  nativeBuildInputs = [ pkgs.makeWrapper ];

  installPhase = ''
    mkdir -p $out/bin $out/share/java

    # Copier le JAR
    cp ${buildGradlePackage}/share/java/*.jar $out/share/java/clacoxygen-backend.jar

    # Créer le script de lancement
    makeWrapper ${pkgs.jre}/bin/java $out/bin/clacoxygen-backend \
      --add-flags "-Xmx512m" \
      --add-flags "-XX:+UseG1GC" \
      --add-flags "-jar $out/share/java/clacoxygen-backend.jar"
  '';

  meta = with pkgs.lib; {
    description = "Clacoxygen Backend API";
    mainProgram = "clacoxygen-backend";
  };
}

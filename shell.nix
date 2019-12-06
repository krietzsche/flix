with import <nixpkgs> {};
let 
  jdk = jetbrains.jdk;
  gradle = pkgs.gradle.override { java = jdk; };
  mill = pkgs.mill.override { jre = jdk; };
in pkgs.mkShell {
	name = "flix";
	src = ".";
	buildInputs = [ jdk mill gradle ];
}

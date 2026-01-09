{ pkgs, ... }: {
  channel = "stable-23.11";

  packages = [
    pkgs.jdk17
    pkgs.android-tools
  ];

  env = {
    JAVA_HOME = "${pkgs.jdk17.home}";
  };

  idx = {
    extensions = [
      "vscjava.vscode-java-pack"
      "muhammad-sammy.android-extensions-for-vscode"
    ];

    workspace = {
      # Move your permission fix here
      onStart = {
        chmod-gradlew = "chmod +x gradlew";
        # You can also add a command to install the app automatically on start
        install-app = "./gradlew installDebug";
      };
      
      # You can leave onCreate empty or keep it for one-time setup
      onCreate = {
        # e.g., git config --global user.name "Your Name"
      };
    };

    previews = {
      enable = true;
      previews = {
        android = {
          # Use 'assembleDebug' to build the APK for the emulator
          command = ["./gradlew" "assembleDebug" "--dry-run"];
          manager = "android";
        };
      };
    };
  };
}
function configureAndroidSdk {
    $ANDROID_HOME = "${env:ANDROID_HOME}";
    $ANDROID_SUBDIRS = "emulator", "tools", "tools/bin", "platform-tools";
    ForEach ($subdir in $ANDROID_SUBDIRS) {
        ${env:PATH} = "${env:PATH}:${ANDROID_HOME}/$subdir";
    }
    ${env:ANDROID_NDK_ROOT} = "$ANDROID_HOME/ndk/26.1.10909125";
}

function configureAndroidToolchain {
    $hostTag = $IsLinux ? "linux-x86_64" : "darwin-x86_64";
    $sdkVersion = "29";

    $archs = @{}
    $archs["aarch64-linux-android"] = $null;
    $archs["armv7-linux-androideabi"] = "armv7a-linux-androideabi";
    $archs["x86_64-linux-android"] = $null;
    $archs["i686-linux-android"] = $null;

    $prebuiltDir = "${env:ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/$hostTag/bin";

    foreach ($rustArch in $archs.Keys) {
        $androidArch = $archs[$rustArch];
        if ($null -eq $androidArch) {
            $androidArch = $rustArch;
        }

        Write-Output "CC_$rustArch=$prebuiltDir/$androidArch$sdkVersion-clang";
        Write-Output "CXX_$rustArch=$prebuiltDir/$androidArch$sdkVersion-clang++";
        Write-Output "AR_$rustArch=$prebuiltDir/llvm-ar";
        Write-Output "RANLIB_$rustArch=$prebuiltDir/llvm-ranlib";
        Write-Output "CFLAGS_$rustArch=-D__ANDROID_MIN_SDK_VERSION__=$sdkVersion";
        Write-Output "CXXFLAGS_$rustArch=-D__ANDROID_MIN_SDK_VERSION__=$sdkVersion";
    }
}

configureAndroidSdk;
configureAndroidToolchain;
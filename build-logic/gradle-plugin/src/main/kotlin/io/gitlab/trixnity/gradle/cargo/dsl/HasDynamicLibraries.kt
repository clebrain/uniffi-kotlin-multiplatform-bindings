/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.provider.SetProperty
import java.io.File

interface HasDynamicLibraries {
    /**
     * The set of directories containing the dynamic libraries required in runtime. This property is effective only for
     * Android and JVM targets.
     *
     * The plugin will automatically configure NDK library paths considering the API level. For example, the following
     * DSL will copy `libc++_shared.so` and `<API Level>/libaaudio.so` from the NDK directory to the app, without
     * manually modifying this property.
     * ```kotlin
     * cargo {
     *   builds.android {
     *     dynamicLibraries = arrayOf("aaudio", "c++_shared")
     *   }
     * }
     * ```
     *
     * This property will also contain the target directory where the Rust library built by Cargo resides.
     */
    val dynamicLibrarySearchPaths: SetProperty<File>

    /**
     * The names of dynamic libraries required in runtime without the prefix and the file extension. This property is
     * effective only for Android and JVM targets.
     *
     * The following DSL will copy `libfoo.so` from `/path/to/libraries`.
     * ```kotlin
     * cargo {
     *   builds.jvm {
     *     dynamicLibrarySearchPaths.add(File("/path/to/libraries"))
     *     dynamicLibraries.add("foo")
     *   }
     * }
     * ```
     */
    val dynamicLibraries: SetProperty<String>
}

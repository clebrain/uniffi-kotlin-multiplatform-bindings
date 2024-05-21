/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.tasks.GloballyLockedTask
import io.gitlab.trixnity.gradle.tasks.globalLock
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Cleaning task cannot be cached")
abstract class CargoCleanTask : CargoPackageTask(), GloballyLockedTask {
    @TaskAction
    fun cleanPackage() {
        globalLock("cargoClean/${cargoPackage.get().name}") {
            cargo("clean").get().assertNormalExitValue()
        }
    }
}

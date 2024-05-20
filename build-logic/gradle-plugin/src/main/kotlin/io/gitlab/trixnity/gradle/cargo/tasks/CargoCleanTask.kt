/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.concurrent.locks.ReentrantLock

@DisableCachingByDefault(because = "Cleaning task cannot be cached")
abstract class CargoCleanTask : CargoPackageTask() {
    @TaskAction
    fun cleanPackage() {
        // TODO: Rewrite the following using a proper Gradle API, as well as CargoBuildTask which uses a file lock
        cleanLock.lock()
        try {
            cargo("clean").get().assertNormalExitValue()
        } finally {
            cleanLock.unlock()
        }
    }

    companion object {
        private val cleanLock = ReentrantLock()
    }
}

/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.database.upgrade;

import org.akvo.flow.database.DatabaseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UpgraderFactoryTest {

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenResponse() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_RESPONSE_ITERATION, null, null);

        assertEquals(1, upgrader.getUpgraders().size());
        assertTrue(containsResponsesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgraderShouldCreateNoUpgraderWhenTransmissionIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_TRANSMISSION_ITERATION, null, null);

        assertEquals(0, upgrader.getUpgraders().size());
    }

    private boolean containsResponsesUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof ResponsesUpgrader) {
                return true;
            }
        }
        return false;
    }
}

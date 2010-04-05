/*
 * Copyright 2010 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace;

/**
 * Collecting static variables and counters for easier modification when running
 * more stressful tests with more elements.
 */
public class StressTestConstants {
    public static final int ITERATIONS_TO_PERFORM = 100; // For a more comprehensive test, use 20000 (checked in with 100)
    public static final int NUMBER_OF_ELEMENTS_OR_LISTENERS = 5000; // For a more comprehensive test, use 50000 (checked in with 5000)
    static final int LARGE_QUANTITY_NUMBER=100; // For a more comprehensive tests, use 20000 (checked in with 100)
}

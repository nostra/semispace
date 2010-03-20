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

package org.semispace.take;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see org.semispace.take.TakeUniquenessTest
 */
public class UniqueWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(UniqueWorker.class);
    private final int iterations;
    private final int objectperiteration;

    public UniqueWorker(int iterations, int objectperiteration) {
        this.iterations = iterations;
        this.objectperiteration = objectperiteration;
    }

    public int getObjectperiteration() {
        return objectperiteration;
    }

    @Override
    public void run() {
        SemiSpaceInterface ss = SemiSpace.retrieveSpace();
        Storage st = Storage.getInstance();
        String myId = st.addWriter();
        Item tmpl = new Item();
        long writecnt = 0;
        long takecnt = 0;
        long begin = System.nanoTime();
        try {

            log.debug("Writer " + myId + " starting.");

            for (int it = 0; it < iterations; it++) {
                for (int c = 0; c < objectperiteration; c++) {
                    Item item = new Item(myId, myId + "_" + it + "_" + c, System.nanoTime());

                    st.addItem(item);
                    ss.write(item, SemiSpace.ONE_DAY);
                    writecnt++;
                }

                for (int c = 0; c < objectperiteration; c++) {
                    Item item = ss.take(tmpl, 250);
                    if (item != null) {
                        st.removeItem(item);
                        takecnt++;
                    }
                }
            }


        } catch (Exception e) {
            log.debug("Trouble running", e);
        }
        long dur = System.nanoTime() - begin;
        log.debug("Writer " + myId + " completed " + iterations + " iterations. Wrote " + writecnt + " objects and took " + takecnt + ". (" + dur + " nanoseconds)");
    }
}

/**
 * Copyright 2019 Pramati Prism, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.srujankujmar.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class StringOutputStream extends OutputStream {

    private StringBuilder data = new StringBuilder();
    
    @Override
    public synchronized void write(int b) throws IOException {
        data.append((char) b);
    }
    
    public String toString() {
        return data.toString();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
       data.append(new String(b,off,len));
    }

}

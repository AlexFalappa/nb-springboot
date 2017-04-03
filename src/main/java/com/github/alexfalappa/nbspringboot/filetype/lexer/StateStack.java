/*
 * Copyright 2017 Alessandro Falappa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.alexfalappa.nbspringboot.filetype.lexer;

/**
 *
 * @author Alessandro Falappa
 */
public class StateStack {

    public byte[] stack;
    private int lastIn = -1;

    public StateStack() {
        this(5);
    }

    public StateStack(int stackSize) {
        stack = new byte[stackSize];
        lastIn = -1;
    }

    public boolean isEmpty() {
        return lastIn == -1;
    }

    public int popStack() {
        int result = stack[lastIn];
        lastIn--;
        return result;
    }

    public void pushStack(int state) {
        lastIn++;
        if (lastIn == stack.length) {
            multiplySize();
        }
        stack[lastIn] = (byte) state;
    }

    private void multiplySize() {
        int length = stack.length;
        byte[] temp = new byte[length * 2];
        System.arraycopy(stack, 0, temp, 0, length);
        stack = temp;
    }

    public int clear() {
        lastIn = -1;
        return lastIn;
    }

    public int size() {
        return lastIn + 1;
    }

    public StateStack createClone() {
        StateStack rv = new StateStack(this.size());
        rv.copyFrom(this);
        return rv;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof StateStack)) {
            return false;
        }
        StateStack s2 = (StateStack) obj;
        if (this.lastIn != s2.lastIn) {
            return false;
        }
        for (int i = lastIn; i >= 0; i--) {
            if (this.stack[i] != s2.stack[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + lastIn;
        for (int i = lastIn; i >= 0; i--) {
            hash = 31 * hash + this.stack[i];
        }
        return hash;
    }

    public void copyFrom(StateStack s) {
        while (s.lastIn >= this.stack.length) {
            this.multiplySize();
        }
        this.lastIn = s.lastIn;
        for (int i = 0; i <= s.lastIn; i++) {
            this.stack[i] = s.stack[i];
        }
    }

    public boolean contains(int state) {
        for (int i = 0; i <= lastIn; i++) {
            if (stack[i] == state) {
                return true;
            }
        }
        return false;
    }

    public int get(int index) {
        return stack[index];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        for (int i = 0; i <= lastIn; i++) {
            sb.append(" stack[").append(i).append("]= ").append(stack[i]); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return sb.toString();
    }

}

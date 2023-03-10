package dev.cuican.staypro.utils;

import dev.cuican.staypro.concurrent.task.VoidTask;
import org.lwjgl.input.Keyboard;

public class KeyBind {

    private int keyCode;
    private final VoidTask action;

    public KeyBind(int keyCode, VoidTask action) {
        this.keyCode = keyCode;
        this.action = action;
    }

    public void test(int keyCode) {
        if (this.keyCode == keyCode) action.invoke();
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
    public int getKey() {
        return this.keyCode;
    }

    public int getKeyCode() {
        return keyCode;
    }
    public boolean isEmpty() {
        return this.keyCode < 0;
    }

    public boolean isDown() {
        return !this.isEmpty() && Keyboard.isKeyDown(this.getKey());
    }

}

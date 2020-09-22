package com.github.cameltooling.idea;

import com.intellij.util.ReflectionUtil;

import javax.swing.Timer;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CamelTestHelper {

    static public void checkJavaSwingTimersAreDisposed() {
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006918780-Tests-Fail-due-to-Java-Swing-Timers-Not-Disposed
        // NOTE: added this otherwise plugin tests fail due to swing timers not being disposed which have nothing to do with the plugin test
        try {
            Class<?> timerQueueClass = Class.forName("javax.swing.TimerQueue");
            Method sharedInstance = timerQueueClass.getMethod("sharedInstance");
            sharedInstance.setAccessible(true);
            Object timerQueue = sharedInstance.invoke(null);
            DelayQueue<?> delayQueue = ReflectionUtil.getField(timerQueueClass, timerQueue, DelayQueue.class, "queue");
            while (true) {
                Delayed timer = delayQueue.peek();

                if (timer == null) {
                    return;
                }

                int delay = Math.toIntExact(timer.getDelay(TimeUnit.MILLISECONDS));
                String text = "(delayed for " + delay + "ms)";
                Method getTimer = ReflectionUtil.getDeclaredMethod(timer.getClass(), "getTimer");
                Timer swingTimer = (Timer) getTimer.invoke(timer);
                text = "Timer (listeners: " + Arrays.toString(swingTimer.getActionListeners()) + ") " + text;
                try {
                    System.out.println("Not disposed javax.swing.Timer: " + text + "; queue:" + timerQueue);
                } finally {
                    swingTimer.stop();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}

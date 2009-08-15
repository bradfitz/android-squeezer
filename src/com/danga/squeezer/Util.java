package com.danga.squeezer;

import java.util.concurrent.atomic.AtomicReference;

public class Util {
    private Util() {}
    
    public static String nonNullString(AtomicReference<String> ref) {
        String string = ref.get();
        return string == null ? "" : string;
    }
    
    // Update target and return true iff it's null or different from newValue. 
    public static boolean atomicStringUpdated(AtomicReference<String> target,
            String newValue) {
        String currentValue = target.get();
        if (currentValue == null || !currentValue.equals(newValue)) {
            target.set(newValue);
            return true;
        }
        return false;
    }    
}

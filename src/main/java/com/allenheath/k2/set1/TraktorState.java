package com.allenheath.k2.set1;

import java.util.Arrays;
import java.util.Optional;

public class TraktorState {
    private String[] deckKeys = new String[4];
    
    public TraktorState() {
        Arrays.fill(deckKeys, "ERR");
    }
    
    public synchronized void setKey(int deckIndex, String key) {
        if(key == null || !key.matches("\\d{1,2}[AB]")) {
            return;
        }
        if(deckIndex>=0 && deckIndex<deckKeys.length) {
            deckKeys[deckIndex] = key;
            AllenHeathK2ControllerExtension.println("#### KEY %d => %s",deckIndex,key);
        }
    }
    
    public synchronized Optional<String> getKey(int deckIndex) {
        if(deckIndex>=0 && deckIndex<deckKeys.length) {
            if(!"ERR".equals(deckKeys[deckIndex])) {
                return Optional.of(deckKeys[deckIndex]);
            }
        }
        return Optional.empty();
    }
}

package com.yaeltex.seqarp168mk2;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

@Component
public class SendsLayer extends Layer {
    
    private final BitwigViewControl viewControl;
    
    public SendsLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "SENDS_LAYER");
        this.viewControl = viewControl;
    }
}

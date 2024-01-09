package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.bitwig.extension.controller.ControllerExtension;

public class Layers {
    private final List<Layer> layers = new ArrayList<>(4);
    @SuppressWarnings("rawtypes")
    private final List<Binding> activeBindings = new ArrayList<>();
    private final ControllerExtension controllerExtension;
    private double globalSensitivity = 1;
    
    public Layers(final ControllerExtension controllerExtension) {
        super();
        this.controllerExtension = controllerExtension;
    }
    
    public ControllerExtension getControllerExtension() {
        return controllerExtension;
    }
    
    void addLayer(final Layer layer) {
        layers.add(layer);
    }
    
    public Layer addLayer(final String name) {
        return new Layer(this, name);
    }
    
    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }
    
    @SuppressWarnings("rawtypes")
    private void updateActiveBindings() {
        activeBindings.clear();
        layers.stream().forEach(layer -> updateBindings(layer));
        activeBindings.stream().forEach(binding -> binding.setIsActive(true));
    }
    
    private void updateBindings(Layer layer) {
        if (layer.isActive()) {
            layer.bindings.forEach(binding -> activateBinding(layer, binding));
        } else {
            layer.bindings.forEach(binding -> binding.setIsActive(false));
        }
    }
    
    private void activateBinding(final Layer layer, final Binding binding) {
        if (layer.shouldReplaceBindingsInLayersBelow()) {
            replaceBindingsBelow(layer, binding);
        }
        activeBindings.add(binding);
    }
    
    private void replaceBindingsBelow(final Layer layer, final Binding binding) {
        final Object exclusivityObject = binding.getExclusivityObject();
        
        for (final Iterator<Binding> i = activeBindings.iterator(); i.hasNext(); ) {
            final Binding activeBinding = i.next();
            
            if (Objects.equals(activeBinding.getExclusivityObject(), exclusivityObject)
                && activeBinding.getLayer() != layer) {
                i.remove();
                activeBinding.setIsActive(false);
            }
        }
    }
    
    protected void activeLayersChanged() {
        updateActiveBindings();
    }
    
    public List<Binding> getActiveBindings() {
        return Collections.unmodifiableList(activeBindings);
    }
    
    public double getGlobalSensitivity() {
        return globalSensitivity;
    }
    
    public void setGlobalSensitivity(final double value) {
        if (value != globalSensitivity) {
            globalSensitivity = value;
            
            for (final Layer layer : layers) {
                for (final Binding binding : layer.getBindings()) {
                    if (binding instanceof BindingWithSensitivity) {
                        ((BindingWithSensitivity) binding).setGlobalSensitivity(value);
                    }
                }
            }
        }
    }
    
}
